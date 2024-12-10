// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.j2k.post.processing.processings

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.j2k.post.processing.inference.common.elementInfo
import org.jetbrains.kotlin.j2k.PostProcessing
import org.jetbrains.kotlin.j2k.PostProcessingApplier
import org.jetbrains.kotlin.j2k.PostProcessingTarget
import org.jetbrains.kotlin.j2k.elements
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.log.ConversionRecorder
import org.jetbrains.kotlin.nj2k.log.ConversionType
import org.jetbrains.kotlin.nj2k.log.JKElementInfoForLog
import org.jetbrains.kotlin.psi.*

class LoggingProcessing : PostProcessing {
    override fun runProcessing(target: PostProcessingTarget, converterContext: NewJ2kConverterContext) {
        runReadAction {
            target.elements().forEach { element ->
                val visitor = LoggingProcessingVisitor(converterContext)
                element.accept(visitor)
            }
        }
    }

    context(KaSession)
    override fun computeAppliers(
        target: PostProcessingTarget,
        converterContext: NewJ2kConverterContext
    ): List<PostProcessingApplier> {
        error("Not supported in K1 J2K")
    }
}

class LoggingProcessingVisitor(val context: NewJ2kConverterContext) : PsiElementVisitor() {
    override fun visitElement(element: PsiElement) {
        when (element) {
            // プライマリコンストラクタ
            is KtPrimaryConstructor -> {
                element.valueParameters.filter { it.hasValOrVar() }.forEach { parameter ->
                    // プライマリコンストラクタのパラメータの場合ラベルと名前の間に空白が入るため，prevSiblingを挟む
                    parameter.nameIdentifier?.prevSibling?.elementInfo(context)?.forEach {
                        if (it is JKElementInfoForLog) {
                            ConversionRecorder.add(it.javaPsi, parameter, ConversionType.PROPERTY)
                        }
                    }
                }
            }

            // メソッド
            is KtFunction -> {
                element.nameIdentifier?.elementInfo(context)?.forEach {
                    if (it is JKElementInfoForLog) {
                        ConversionRecorder.add(it.javaPsi, element, ConversionType.FUNCTION)
                    }
                }
            }

            // プロパティ
            is KtProperty -> {
                // クラスボディ内のプロパティのみを対象とする(ローカルなプロパティは対象外)
                if (element.parent is KtClassBody) {
                    element.nameIdentifier?.elementInfo(context)?.forEach {
                        if (it is JKElementInfoForLog) {
                            ConversionRecorder.add(it.javaPsi, element, ConversionType.PROPERTY)
                        }
                    }
                }
            }
        }
        element.acceptChildren(this)
    }
}