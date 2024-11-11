// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.filepicker

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.ui.components.JBPanel
import org.jetbrains.kotlin.nj2k.gui.common.SourceViewField
import java.awt.BorderLayout

class SourceViewPanel(project: Project, document: Document?, fileType: FileType) : JBPanel<JBPanel<*>>(BorderLayout()) {

    // VirtualFileから作成するためのセカンダリコンストラクタ
    constructor(project: Project, file: VirtualFile) : this(project, file.findDocument(), file.fileType)

    // エディタ
    private val sourceViewer: SourceViewField = SourceViewField(document, project, fileType, true)

    init {
        // リサイズさせるためにCENTERに配置
        add(sourceViewer, BorderLayout.CENTER)
    }

    // ファイル切り替えメソッド
    fun switchFile(file: VirtualFile) {
        sourceViewer.switchFile(file)
    }
}