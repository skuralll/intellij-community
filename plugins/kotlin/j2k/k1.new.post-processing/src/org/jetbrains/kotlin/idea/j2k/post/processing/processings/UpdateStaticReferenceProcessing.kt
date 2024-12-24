// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.j2k.post.processing.processings

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.j2k.ElementsBasedPostProcessing
import org.jetbrains.kotlin.j2k.PostProcessingApplier
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

// staticなgetter/setterの呼び出しをpropertyの呼び出しに更新する (一時的なバグへの対処)
class UpdateStaticReferenceProcessing : ElementsBasedPostProcessing() {
    override fun runProcessing(elements: List<PsiElement>, converterContext: NewJ2kConverterContext) {
        val staticRefs = getStaticReferences(elements, converterContext)
        staticRefs.forEach { (element, descriptor) ->
            //println("=== Static reference ===")
            //println(element.text)
            //println(descriptor.fqNameOrNull())
        }
    }

    context(KaSession) override fun computeApplier(
        elements: List<PsiElement>,
        converterContext: NewJ2kConverterContext
    ): PostProcessingApplier {
        error("Not supported in K1 J2K")
    }

    // staticなgetter/setter呼び出しを収集する
    private fun getStaticReferences(
        elements: List<PsiElement>,
        converterContext: NewJ2kConverterContext
    ): List<Pair<PsiElement, CallableDescriptor>> {
        val visitor = CollectStaticReferencesVisitor(converterContext)
        elements.forEach { it.accept(visitor) }
        return visitor.getCollectedReferences()
    }

    // staticなgetter/setter呼び出しを収集するためのVisitor
    internal class CollectStaticReferencesVisitor(val converterContext: NewJ2kConverterContext) : PsiElementVisitor() {
        private val refs = mutableListOf<Pair<PsiElement, CallableDescriptor>>()
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
        fun getCollectedReferences(): List<Pair<PsiElement, CallableDescriptor>> = refs

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