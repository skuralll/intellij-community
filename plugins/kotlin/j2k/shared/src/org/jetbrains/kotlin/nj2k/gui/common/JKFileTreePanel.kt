// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.common

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckedTreeNode
import org.jetbrains.kotlin.idea.util.isJavaFileType
import org.jetbrains.kotlin.idea.util.isKotlinFileType

class JKFileTreePanel(
    rootFile: VirtualFile,
    selectedFiles: MutableList<VirtualFile> = mutableListOf(),
    hasCheckBox: Boolean = false
) : FileTreePanel(rootFile, selectedFiles, hasCheckBox) {

    // オプション
    var enableKotlin = true // Kotlinファイルを選択可能にするか
        set(value) {
            field = value
            setActiveNodes({ it.userObject is VirtualFile && (it.userObject as VirtualFile).isKotlinFileType() }, value)
            repaint()
        }

    init {
        enableKotlin = false
        disableNonSourceDirectories(rootNode)
    }

    // ディレクトリ, Java, Kotlinファイルのみ有効にする
    override fun isEnabledFile(file: VirtualFile): Boolean {
        return file.isDirectory || file.isJavaFileType() || file.isKotlinFileType()
    }

    // Java, Kotlinファイルが含まれていないディレクトリは無効化する
    private fun disableNonSourceDirectories(node: CheckedTreeNode) {
        setActiveNodes({ it.userObject is VirtualFile && (it.userObject as VirtualFile).isDirectory && !hasSourceFile(it) }, false)
    }

    // Java, Kotlinファイルがノードに含まれているか
    private fun hasSourceFile(node: CheckedTreeNode): Boolean {
        val file = node.userObject as? VirtualFile
        if (file != null && (file.isJavaFileType() || file.isKotlinFileType())) {
            return true
        }
        return node.children().asSequence().filterIsInstance<CheckedTreeNode>().any { hasSourceFile(it) }
    }

}