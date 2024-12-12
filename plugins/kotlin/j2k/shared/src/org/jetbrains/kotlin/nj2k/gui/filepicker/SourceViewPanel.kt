// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.filepicker

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.nj2k.gui.common.SourceViewField
import java.awt.BorderLayout
import javax.swing.JComponent

class SourceViewPanel(document: Document?, project: Project, fileType: FileType) : JBPanel<JBPanel<*>>(BorderLayout()) {

    // Projectから作成するためのセカンダリコンストラクタ
    constructor(project: Project) : this(null, project, PlainTextFileType.INSTANCE)

    // ラベル
    val label = JBLabel(" ")

    // エディタ
    private val sourceViewer: SourceViewField = SourceViewField(document, project, fileType, true)

    // PsiFile
    val psiFile get() = sourceViewer.psiFile

    init {
        // コードビューア, リサイズさせるためにCENTERに配置
        add(sourceViewer, BorderLayout.CENTER)
    }

    // ファイル切り替えメソッド
    fun switchFile(file: VirtualFile) {
        sourceViewer.switchFile(file)
    }

    fun switchFile(document: Document?, fileType: FileType) {
        sourceViewer.switchFile(document, fileType)
    }

    // ラベル付きのパネルを取得 (レイアウトが崩れないようにするため、DSLを用いた専用のメソッドを用意した)
    fun getLabeledPanel(): JComponent {
        return panel {
            row { cell(label) }
            row {
                cell(this@SourceViewPanel)
                    .align(Align.FILL)
                    .resizableColumn()
            }.resizableRow()
        }
    }
}