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
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class UpdateStaticReferenceProcessing : FileBasedPostProcessing() {
    override fun runProcessing(file: KtFile, allFiles: List<KtFile>, rangeMarker: RangeMarker?, converterContext: NewJ2kConverterContext) {
        val staticRefs = getStaticReferences(file, converterContext)
        staticRefs.forEach outer@ { (element, call) ->
            allFiles.forEach inner@ { targetFile ->
                if (targetFile.packageFqName != call.containingPackage()) return@inner
                val namedDeclarations = targetFile.collectDescendantsOfType<KtNamedDeclaration>()
                if(namedDeclarations.any{declaration -> declaration.fqName == call.fqNameOrNull()}) return@outer // 修正が不要であればスキップ
                println(targetFile.name + " -> " + element.text)
                // todo 修正処理，なぜか二回ここが呼ばれる部分の修正
            }
        }
    }

    context(KaSession) override fun computeApplier(
        file: KtFile,
        allFiles: List<KtFile>,
        rangeMarker: RangeMarker?,
        converterContext: NewJ2kConverterContext
    ): PostProcessingApplier {
        error("Not supported in K1 J2K")
    }

    // staticなgetter/setter呼び出しを収集する
    private fun getStaticReferences(
        file: KtFile,
        converterContext: NewJ2kConverterContext
    ): List<Pair<KtCallExpression, CallableDescriptor>> {
        val visitor = CollectStaticReferencesVisitor(converterContext)
        file.accept(visitor)
        return visitor.getCollectedReferences()
    }

    // staticなgetter/setter呼び出しを収集するためのVisitor
    internal class CollectStaticReferencesVisitor(val converterContext: NewJ2kConverterContext) : PsiElementVisitor() {
        private val refs = mutableListOf<Pair<KtCallExpression, CallableDescriptor>>()
        override fun visitElement(element: PsiElement) {
            when (element) {
                is KtCallExpression -> {
                    runReadAction {
                        getStaticReferenceDescriptorOrNull(element)?.let { descriptor ->
                            refs += element to descriptor
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