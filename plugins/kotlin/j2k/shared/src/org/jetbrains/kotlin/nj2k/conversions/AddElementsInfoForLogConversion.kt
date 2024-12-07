// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.RecursiveConversion
import org.jetbrains.kotlin.nj2k.log.JKElementInfoForLog
import org.jetbrains.kotlin.nj2k.tree.JKMethod
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement

// ロギングのための情報を追加するためのプロセス
class AddElementsInfoForLogConversion(context: NewJ2kConverterContext) : RecursiveConversion(context) {

    context(KaSession)
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        when (element) {
            is JKMethod -> addInfoForFunction(element)
        }
        return recurse(element)
    }

    // メソッドの場合
    private fun addInfoForFunction(function: JKMethod) {
        // nullの場合，明記されていないコンストラクタなどが考えられる
        context.elementsInfoStorage.addEntry(function, JKElementInfoForLog(function.psi?.kotlinFqName.toString()))
    }

}