// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.common

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorTextField

class SourceViewField(document: Document?, project: Project, fileType: FileType, isViewer: Boolean) :
    EditorTextField(document ?: getEmptyDocument(), project, fileType, isViewer) {

    companion object {
        // 空のDocumentを作成する
        private fun getEmptyDocument() = EditorFactory.getInstance().createDocument("")
    }

    init {
        // ファイルの最初の行にフォーカスを当てる
        setCaretPosition(0)
        // 複数行での表示にする
        setOneLineMode(false)
    }

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        // 常にスクロールバーを表示する
        editor.setVerticalScrollbarVisible(true)
        editor.setHorizontalScrollbarVisible(true)
        return editor
    }

    // ファイル切り替えメソッド
    fun switchFile(document: Document?, fileType: FileType) {
        CommandProcessor.getInstance().executeCommand(project, {
            CommandProcessor.getInstance().runUndoTransparentAction {
                runWriteAction {
                    this.document.setText(document?.text ?: "")
                    this.fileType = fileType
                }
            }
            // カーソル位置を戦闘に戻す
            invokeLater {
                setCaretPosition(0)
                editor?.scrollingModel?.scrollToCaret(ScrollType.RELATIVE)
            }
        }, "SwitchFile", null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
    }

    fun switchFile(file: VirtualFile) {
        switchFile(FileDocumentManager.getInstance().getDocument(file), file.fileType)
    }

}