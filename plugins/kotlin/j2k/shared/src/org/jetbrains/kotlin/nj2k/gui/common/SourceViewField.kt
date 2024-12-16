// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.common

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.EditorTextField
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtPsiFactory

class SourceViewField(document: Document?, project: Project, fileType: FileType, isViewer: Boolean) :
    EditorTextField(document ?: getEmptyDocument(), project, fileType, isViewer) {

    // 対象のPSI
    var psiFile: PsiFile? = null
        private set

    // イベントハンドラ管理
    private val eventListeners = mutableListOf<SourceViewFieldListener>()

    companion object {
        // 空のDocumentを作成する
        private fun getEmptyDocument() = EditorFactory.getInstance().createDocument("")
    }

    init {
        // ファイルの最初の行にフォーカスを当てる
        setCaretPosition(0)
        // 複数行での表示にする
        isOneLineMode = false
        // イベントハンドラ登録
        val disposable = Disposer.newDisposable("EditorFactoryListenerDisposable")
        EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
            override fun editorCreated(event: EditorFactoryEvent) {
                eventListeners.forEach { it.onEditorCreated(event.editor) }
            }
        }, disposable)
    }

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        // 常にスクロールバーを表示する
        editor.setVerticalScrollbarVisible(true)
        editor.setHorizontalScrollbarVisible(true)
        // イベント
        editor.addEditorMouseListener(object : EditorMouseListener {
            override fun mouseClicked(event: EditorMouseEvent) {
                psiFile?.getPsiElement(event.offset)?.let { element ->
                    onClickElement(element)
                }
            }
        })
        return editor
    }

    // イベントハンドラを追加する
    fun addEventListener(listener: SourceViewFieldListener) {
        eventListeners.add(listener)
    }

    // ファイル内の要素をクリックしたときに呼ばれる
    private fun onClickElement(element: PsiElement) {
        eventListeners.forEach { it.onClickElement(this, element) }
    }

    // ファイル切り替えメソッド
    fun switchFile(document: Document?, fileType: FileType) {
        CommandProcessor.getInstance().executeCommand(project, {
            CommandProcessor.getInstance().runUndoTransparentAction {
                runWriteAction {
                    this.document.setText(document?.text ?: "")
                    this.fileType = fileType
                    setPsi(fileType, document?.text ?: "")
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

    // ファイルタイプに応じたPsiFileを作成する
    private fun setPsi(fileType: FileType, content: String) {
        psiFile = when (fileType) {
            JavaFileType.INSTANCE -> {
                PsiFileFactory.getInstance(project).createFileFromText(JavaLanguage.INSTANCE, content)
            }

            KotlinFileType.INSTANCE -> {
                KtPsiFactory(project).createFile(content)
            }

            else -> {
                null
            }
        }
    }

    // N文字目のPsiElementを取得する
    private fun PsiFile.getPsiElement(offset: Int): PsiElement? {
        return this.findElementAt(offset)
    }

    // 特定の要素をスタイリングする
    fun markupElement(element: PsiElement, style: TextAttributes) {
        val range = element.textRange ?: return
        markup(range, style)
    }

    // 特定の範囲をスタイリングする
    fun markup(range: TextRange, style: TextAttributes){
        val markupModel = editor?.markupModel ?: return
        ApplicationManager.getApplication().invokeLater {
            markupModel.addRangeHighlighter(
                range.startOffset,
                range.endOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                style,
                HighlighterTargetArea.EXACT_RANGE
            )
        }
    }

}

// イベントハンドラ (SourceViewFieldで起きたイベントを扱いたい場合，このクラスを継承して実装する)
abstract class SourceViewFieldListener {
    open fun onEditorCreated(editor: Editor) {}
    open fun onClickElement(viewer: SourceViewField, element: PsiElement) {}
}