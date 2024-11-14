// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.previewer

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBPanel
import org.jetbrains.kotlin.nj2k.gui.common.SourceViewField
import java.awt.BorderLayout

class SourceDiffPanel(project: Project, rootFile: VirtualFile) : JBPanel<JBPanel<*>>(BorderLayout()) {

    // ソースビューア
    val beforeViewer = SourceViewField(null, project, FileTypeManager.getInstance().getFileTypeByExtension("java"), true)
    val afterViewer = SourceViewField(null, project, FileTypeManager.getInstance().getFileTypeByExtension("kt"), true)

    init {
        // Splitterを使って横に分割表示
        val splitter = Splitter(false, 0.5f) // trueは水平方向に分割、0.5fは分割位置（中央）
        splitter.dividerWidth = 2
        splitter.setResizeEnabled(false)
        splitter.firstComponent = beforeViewer
        splitter.secondComponent = afterViewer
        add(splitter, BorderLayout.CENTER)
    }

}