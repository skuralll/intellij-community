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
    },

    FUNCTION{
        override val id = "function"
        override fun createEntry(file: String, javaFq: String, ktFq: String): ConversionEntry {
            return FunctionModifierEntry(file, javaFq, ktFq)
        }
    },

    PROPERTY{
        override val id = "property"
        override fun createEntry(file: String, javaFq: String, ktFq: String): ConversionEntry {
            return PropertyModifierEntry(file, javaFq, ktFq)
        }
    };
    abstract val id : String;
    abstract fun createEntry(file: String, javaFq: String, ktFq: String): ConversionEntry
}