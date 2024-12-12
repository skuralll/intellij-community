// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly

inline fun <T> List<T>.mutate(mutate: MutableList<T>.() -> Unit): List<T> {
    val mutableList = toMutableList()
    mutate(mutableList)
    return mutableList
}

// Examples:
//   getMyProperty -> myProperty
//   isMyProperty -> isMyProperty
fun String.asGetterName(): String? =
    takeIf { JvmAbi.isGetterName(it) }
        ?.removePrefix("get")
        ?.takeIf {
            it.isNotEmpty() && it.first().isUpperCase()
                    || it.startsWith("is") && it.length > 2 && it[2].isUpperCase()
        }?.decapitalizeAsciiOnly()
        ?.escaped()

// Example: setMyProperty -> myProperty
fun String.asSetterName(): String? =
    takeIf { JvmAbi.isSetterName(it) }
        ?.removePrefix("set")
        ?.takeIf { it.isNotEmpty() && it.first().isUpperCase() }
        ?.decapitalizeAsciiOnly()
        ?.escaped()

fun String.canBeGetterOrSetterName(): Boolean =
    asGetterName() != null || asSetterName() != null

private val KEYWORDS: Set<String> = KtTokens.KEYWORDS.types.map { (it as KtKeywordToken).value }.toSet()

fun String.escaped(): String {
    val onlyUnderscores = isNotEmpty() && this.count { it == '_' } == length
    return if (this in KEYWORDS || '$' in this || onlyUnderscores) "`$this`" else this
}

// VirtualFileのプロジェクトルートからの相対パスを取得する
fun VirtualFile.getRelativePath(project: Project): String? {
    val projectBasePath = project.basePath ?: return null
    val virtualFilePath = this.path
    return if (virtualFilePath.startsWith(projectBasePath)) {
        virtualFilePath.removePrefix("$projectBasePath/")
    } else {
        null
    }
}