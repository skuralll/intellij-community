// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.previewer

import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBPanel
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.nj2k.gui.filepicker.SourceViewPanel
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.Icon

class SourceDiffPanel(project: Project, rootFile: VirtualFile) : JBPanel<JBPanel<*>>(GridLayout(1, 2)) {

    // ソースビューア
    private val beforeViewer = SourceViewPanel(null, project, JavaFileType.INSTANCE)
    private val afterViewer = SourceViewPanel(null, project, KotlinFileType.INSTANCE)

    override fun setPreferredSize(preferredSize: Dimension?) {
        super.setPreferredSize(preferredSize)
        super.setMinimumSize(preferredSize)
        super.setMaximumSize(preferredSize)
    }

    init {
        // ラベル設定
        beforeViewer.label.text = "Before"
        beforeViewer.label.icon = AllIcons.FileTypes.Any_type
        afterViewer.label.text = "After"
        afterViewer.label.icon = AllIcons.FileTypes.Any_type
        // 追加
        add(beforeViewer.getLabeledPanel())
        add(afterViewer.getLabeledPanel())
    }

    // 変換前情報をセット
    fun setBefore(document: Document?, fileType: FileType) {
        beforeViewer.switchFile(document, fileType)
        beforeViewer.label.icon = fileType.icon
        beforeViewer.label.icon = getFileIcon(document, fileType)
    }

    // 変換後情報をセット
    fun setAfter(document: Document?, fileType: FileType) {
        afterViewer.switchFile(document, fileType)
        afterViewer.label.icon = getFileIcon(document, fileType)
    }

    // ファイルに適したアイコンを取得するメソッド
    private fun getFileIcon(document: Document?, fileType: FileType): Icon? {
        return when{
            fileType.icon != AllIcons.FileTypes.Unknown -> fileType.icon
            else -> AllIcons.FileTypes.Any_type
        }
    }

}