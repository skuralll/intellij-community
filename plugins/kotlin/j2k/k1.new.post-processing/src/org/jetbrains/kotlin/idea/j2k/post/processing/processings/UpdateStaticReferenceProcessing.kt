// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.j2k.post.processing.processings

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.j2k.ElementsBasedPostProcessing
import org.jetbrains.kotlin.j2k.PostProcessingApplier
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext

// staticなgetter/setterの呼び出しをpropertyの呼び出しに更新する (一時的なバグへの対処)
class UpdateStaticReferenceProcessing : ElementsBasedPostProcessing() {
    override fun runProcessing(elements: List<PsiElement>, converterContext: NewJ2kConverterContext) {
        // TODO: Implement
    }

    context(KaSession) override fun computeApplier(
        elements: List<PsiElement>,
        converterContext: NewJ2kConverterContext
    ): PostProcessingApplier {
        error("Not supported in K1 J2K")
    }
}