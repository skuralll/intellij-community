// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.j2k.post.processing.processings

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.j2k.FileBasedPostProcessing
import org.jetbrains.kotlin.j2k.PostProcessingApplier
import org.jetbrains.kotlin.load.java.propertyNamesByAccessorName
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.runUndoTransparentActionInEdt
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class UpdateStaticReferenceProcessing : FileBasedPostProcessing() {
    override fun runProcessing(file: KtFile, allFiles: List<KtFile>, rangeMarker: RangeMarker?, converterContext: NewJ2kConverterContext) {
        // staticなメンバへの参照とその参照先を収集する
        val staticRefs = getStaticReferences(file, converterContext)
        // 収集した参照要素について適用処理を行う
        staticRefs.forEach { (refElement, callee) ->
            applyProcessing(refElement, callee, allFiles)
        }
    }

    context(KaSession) override fun computeApplier(
        file: KtFile,
        allFiles: List<KtFile>,
        rangeMarker: RangeMarker?,
        converterContext: NewJ2kConverterContext,
    ): PostProcessingApplier {
        error("Not supported in K1 J2K")
    }

    // 適用処理
    private fun applyProcessing(refElement: KtCallExpression, callee: CallableDescriptor, allFiles: List<KtFile>) {
        val referencedFile = findReferencedFile(callee, allFiles) ?: return
        val referencedObject = findReferencedObject(callee, referencedFile) ?: return
        if (referencedObject.getDeclaration(callee.name.asString(), KtNamedFunction::class.java).isNotEmpty()) return // getter/setterが存在する場合は何もしない
        val propertyName = propertyNamesByAccessorName(callee.name).firstOrNull()?.identifier ?: return
        val property = referencedObject.getDeclaration(propertyName, KtProperty::class.java).firstOrNull() ?: return
        // 適用
        runUndoTransparentActionInEdt(inWriteAction = true) {
            val factory = KtPsiFactory(refElement)
            val newRefElement = factory.createExpression("${property.name}")
            refElement.replace(newRefElement)
        }
    }

    // 適用対象のオブジェクト(, companion object)があれば取得する
    private fun findReferencedObject(callee: CallableDescriptor, ktFile: KtFile): KtObjectDeclaration? {
        return runReadAction {
            ktFile.findChildrenByClass(KtClassOrObject::class.java).forEach { ktClassOrObject ->
                if (ktClassOrObject.name == callee.containingDeclaration.name.identifier) {
                    return@runReadAction if (ktClassOrObject is KtObjectDeclaration) {
                        ktClassOrObject
                    } else {
                        getCompanionObject(ktClassOrObject as KtClass)
                    }
                }
            }
            null
        }
    }

    // 適用対象のファイルがあれば取得する
    private fun findReferencedFile(callee: CallableDescriptor, allFiles: List<KtFile>): KtFile? {
        return allFiles.firstOrNull { targetFile ->
            targetFile.packageFqName == callee.containingPackage() &&
                    targetFile.name.substringBeforeLast('.') == callee.containingDeclaration.name.identifier
        }
    }

    // companion objectを取得する
    private fun getCompanionObject(klass: KtClass): KtObjectDeclaration? {
        return klass.declarations.filterIsInstance<KtObjectDeclaration>().firstOrNull { it.isCompanion() }
    }

    // setterまたはgetterかどうかを判定する
    private fun isSetterOrGetter(callee: CallableDescriptor): Boolean {
        return callee.name.asString().startsWith("set") || callee.name.asString().startsWith("get")
    }

    // 指定した名前の要素（メソッドやプロパティ）があるかどうかを取得する
    private fun <T : KtNamedDeclaration> KtObjectDeclaration.hasDeclaration(name: String, clazz: Class<T>): Boolean {
        return this.declarations.filterIsInstance(clazz).any { it.name == name }
    }

    // 指定した名前の要素があれば取得する
    private fun <T : KtNamedDeclaration> KtObjectDeclaration.getDeclaration(name: String, clazz: Class<T>): List<T> {
        return this.declarations.filterIsInstance(clazz).filter { it.name == name }
    }

    // staticなgetter/setter呼び出しを収集する
    private fun getStaticReferences(
        file: KtFile,
        converterContext: NewJ2kConverterContext,
    ): List<Pair<KtCallExpression, CallableDescriptor>> {
        val visitor = CollectStaticReferencesVisitor(converterContext)
        file.accept(visitor)
        return visitor.getCollectedReferences()
    }

    // staticなgetter/setter呼び出しを収集するためのVisitor
    inner class CollectStaticReferencesVisitor(val converterContext: NewJ2kConverterContext) : PsiElementVisitor() {
        private val refs = mutableListOf<Pair<KtCallExpression, CallableDescriptor>>()
        override fun visitElement(element: PsiElement) {
            when (element) {
                is KtCallExpression -> {
                    runReadAction {
                        getStaticReferenceDescriptorOrNull(element)?.let { descriptor ->
                            if (isSetterOrGetter(descriptor)) {
                                refs += element to descriptor
                            }
                        }
                    }
                }
            }
            element.acceptChildren(this)
        }

        // 収集した要素を返す
        fun getCollectedReferences(): List<Pair<KtCallExpression, CallableDescriptor>> = refs

        // expressionの参照先がstaticなメンバであれば
        private fun getStaticReferenceDescriptorOrNull(expression: KtCallExpression): CallableDescriptor? {
            val bindingContext = expression.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = expression.getResolvedCall(bindingContext) ?: return null
            val descriptor = resolvedCall.resultingDescriptor
            // Javaのstaticメンバの場合
            if (DescriptorUtils.isStaticDeclaration(descriptor)) {
                return descriptor
            }
            // もし他に条件があれば追加
            return null
        }
    }
}