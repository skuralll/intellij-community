// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.log

import com.intellij.psi.PsiElement

enum class ConversionType {

    // const 修飾子の追加
    ADD_CONST_MODIFIER {
        override fun createEntry(
            file: String,
            range: ConversionRange,
            javaFq: String,
            ktFq: String,
            psiElement: PsiElement
        ): ConversionEntry {
            return ConstModifierEntry(file, range, javaFq, ktFq)
        }
    };

    abstract fun createEntry(file: String, range: ConversionRange, javaFq: String, ktFq: String, psiElement: PsiElement): ConversionEntry
}