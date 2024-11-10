// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.common

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.util.isJavaFileType
import org.jetbrains.kotlin.idea.util.isKotlinFileType

class JKFileTreePanel(
    rootFile: VirtualFile,
    selectedFiles: MutableList<VirtualFile> = mutableListOf(),
    hasCheckBox: Boolean = false
) : FileTreePanel(rootFile, selectedFiles, hasCheckBox){

    // オプション
    var enableKotlin = false // Kotlinファイルを選択可能にするか
        set(value) {
            field = value
            tree.repaint()
        }

    // ディレクトリ, Java, Kotlinファイルのみ有効にする
    override fun isEnabledFile(file: VirtualFile): Boolean {
        return file.isDirectory || file.isJavaFileType() || file.isKotlinFileType()
    }

}