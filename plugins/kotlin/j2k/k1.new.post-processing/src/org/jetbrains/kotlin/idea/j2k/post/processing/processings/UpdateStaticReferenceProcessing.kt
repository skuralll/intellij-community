// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.j2k.post.processing.processings

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
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
    override fun runProcessing(
        ktFile: KtFile,
        allFiles: List<KtFile>,
        rangeMarker: RangeMarker?,
        converterContext: NewJ2kConverterContext
    ) {
        val refExpressions = getReferences(ktFile)
        refExpressions.forEach { refExpression ->
            val calleeDescriptor = getReferenceDescriptorOrNull(refExpression) ?: return@forEach
            if (!DescriptorUtils.isStaticDeclaration(calleeDescriptor)) return@forEach
            val accessorType = getAccessorType(refExpression, calleeDescriptor) ?: return@forEach
            applyProcessing(refExpression, calleeDescriptor, accessorType, allFiles)
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
    private fun applyProcessing(
        refElement: KtCallExpression,
        callee: CallableDescriptor,
        accessorType: AccessorType,
        allFiles: List<KtFile>
    ) {
        val referencedFile = findReferencedFile(callee, allFiles) ?: return
        val referencedObject = findReferencedObject(callee, referencedFile) ?: return
        if (referencedObject.getDeclaration(callee.name.asString(), KtNamedFunction::class.java)
                .isNotEmpty()
        ) return // getter/setterが存在する場合は何もしない
        val propertyNameCandidates = propertyNamesByAccessorName(callee.name)
        val propertyName = propertyNameCandidates.find { referencedObject.hasDeclaration(it.identifier, KtProperty::class.java) } ?: return
        val property = referencedObject.getDeclaration(propertyName.identifier, KtProperty::class.java).firstOrNull() ?: return
        // 適用
        runUndoTransparentActionInEdt(inWriteAction = true) {
            val factory = KtPsiFactory(refElement)
            when(accessorType){
                AccessorType.GETTER -> {
                    refElement.replace(factory.createExpression("${property.name}"))
                }
                AccessorType.SETTER -> {
                    val parent = refElement.parent
                    val prevElement = refElement.getPreviousValidElement() ?: return@runUndoTransparentActionInEdt
                    val argument = refElement.valueArguments.firstOrNull() ?: return@runUndoTransparentActionInEdt
                    // 要素生成
                    val expressionText = "${property.name} = ${argument.text}"
                    val newElement = factory.createExpression(expressionText)
                    val addedElement = parent.addAfter(newElement, prevElement)
                    // 元の要素削除
                    refElement.delete()
                }
            }
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

    private fun getAccessorType(refElement: KtCallExpression, callee: CallableDescriptor): AccessorType? {
        val argumentsSize = refElement.valueArguments.size
        return when {
            callee.name.asString().startsWith("set") && argumentsSize == 1 -> AccessorType.SETTER
            callee.name.asString().startsWith("get") && argumentsSize == 0 -> AccessorType.GETTER
            else -> null
        }
    }

    // 指定した名前の要素（メソッドやプロパティ）があるかどうかを取得する
    private fun <T : KtNamedDeclaration> KtObjectDeclaration.hasDeclaration(name: String, clazz: Class<T>): Boolean {
        return this.declarations.filterIsInstance(clazz).any { it.name == name }
    }

    // 指定した名前の要素があれば取得する
    private fun <T : KtNamedDeclaration> KtObjectDeclaration.getDeclaration(name: String, clazz: Class<T>): List<T> {
        return this.declarations.filterIsInstance(clazz).filter { it.name == name }
    }

    // 参照を収集する
    private fun getReferences(ktFile: KtFile): List<KtCallExpression> {
        return runReadAction { PsiTreeUtil.collectElementsOfType(ktFile, KtCallExpression::class.java).toList() }
    }

    // 参照先のDescriptorを取得する
    private fun getReferenceDescriptorOrNull(expression: KtCallExpression): CallableDescriptor? {
        return runReadAction {
            val bindingContext = expression.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = expression.getResolvedCall(bindingContext)
            resolvedCall?.resultingDescriptor
        }
    }

    // 空白行を無視して前の要素を取得する
    private fun PsiElement.getPreviousValidElement(): PsiElement? {
        var prevElement = this.prevSibling
        while (prevElement is PsiWhiteSpace) {
            prevElement = prevElement.prevSibling
        }
        return prevElement
    }

    private enum class AccessorType {
        GETTER,
        SETTER
    }

}