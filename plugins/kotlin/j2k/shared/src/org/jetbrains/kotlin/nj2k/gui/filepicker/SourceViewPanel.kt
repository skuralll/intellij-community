// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.filepicker

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.nj2k.gui.common.SourceViewField
import java.awt.BorderLayout
import javax.swing.JComponent

class SourceViewPanel(project: Project, document: Document?, fileType: FileType) : JBPanel<JBPanel<*>>(BorderLayout()) {

    // VirtualFileから作成するためのセカンダリコンストラクタ
    constructor(project: Project, file: VirtualFile) : this(project, file.findDocument(), file.fileType)

    // ラベル
    val label = JBLabel(" ")
    // エディタ
    private val sourceViewer: SourceViewField = SourceViewField(document, project, fileType, true)

    init {
        // ラベル
        //add(JBLabel("This is test"), BorderLayout.NORTH)
        // コードビューア, リサイズさせるためにCENTERに配置
        add(sourceViewer, BorderLayout.CENTER)
    }

    // ファイル切り替えメソッド
    fun switchFile(file: VirtualFile) {
        sourceViewer.switchFile(file)
    }

    // ラベル付きのパネルを取得
    fun getLabeledPanel() : JComponent {
        return panel {
            row { label }
            row { cell(this@SourceViewPanel).align(Align.FILL) }
        }
    }
}