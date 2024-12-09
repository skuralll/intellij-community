// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.log

import com.intellij.psi.PsiElement

enum class ConversionType {

    // 共有のConversionType
    COMMON{
        override val id = "common"
        override fun createEntry(file: String, javaFq: String, ktFq: String): ConversionEntry {
            return CommonModifierEntry(file, javaFq, ktFq)
        }
    };

    // TODO : 変換処理ごとにエントリを追加する
    // const 修飾子の追加
    //ADD_CONST_MODIFIER {
    //    override val id = "add_const_modifier"
    //    override fun createEntry(
    //        file: String,
    //        range: ConversionRange,
    //        javaFq: String,
    //        ktFq: String,
    //        psiElement: PsiElement
    //    ): ConversionEntry {
    //        return ConstModifierEntry(file, range, javaFq, ktFq)
    //    }
    //},
    //// コンストラクタ変換
    //CONSTRUCTOR {
    //    override val id = "constructor"
    //    override fun createEntry(
    //        file: String,
    //        range: ConversionRange,
    //        javaFq: String,
    //        ktFq: String,
    //        psiElement: PsiElement
    //    ): ConversionEntry {
    //        return ConstructorEntry(file, range, javaFq, ktFq)
    //    }
    //}
    abstract val id : String;
    abstract fun createEntry(file: String, javaFq: String, ktFq: String): ConversionEntry
}