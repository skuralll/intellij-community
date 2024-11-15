// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.previewer

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBPanel
import org.jetbrains.kotlin.nj2k.gui.filepicker.SourceViewPanel
import java.awt.Dimension
import java.awt.GridLayout

class SourceDiffPanel(project: Project, rootFile: VirtualFile) : JBPanel<JBPanel<*>>(GridLayout(1, 2)) {

    // ソースビューア
    private val beforeViewer = SourceViewPanel(null, project, FileTypeManager.getInstance().getFileTypeByExtension("java"))
    private val afterViewer = SourceViewPanel(null, project, FileTypeManager.getInstance().getFileTypeByExtension("kt"))

    override fun setPreferredSize(preferredSize: Dimension?) {
        super.setPreferredSize(preferredSize)
        super.setMinimumSize(Dimension(800, 600))
        super.setMaximumSize(Dimension(800, 600))
    }

    init {
        // ラベル設定
        beforeViewer.label.text = "Before"
        afterViewer.label.text = "After"
        // 追加
        add(beforeViewer.getLabeledPanel())
        add(afterViewer.getLabeledPanel())
    }

    // 変換前情報をセット
    fun setBefore(document: Document?, fileType: FileType){
        beforeViewer.switchFile(document, fileType)
    }

    // 変換後情報をセット
    fun setAfter(document: Document?, fileType: FileType){
        afterViewer.switchFile(document, fileType)
    }

}