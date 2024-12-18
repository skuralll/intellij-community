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
import com.intellij.openapi.editor.event.*
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
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.EditorTextField
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.awt.Point

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
        editor.addEditorMouseMotionListener(object : EditorMouseMotionListener {
            override fun mouseMoved(event: EditorMouseEvent) {
                psiFile?.getPsiElement(event.offset)?.let { element ->
                    onHoverElement(element, event.mouseEvent.point)
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

    // ファイル内の要素をホバーしたときに呼ばれる
    private fun onHoverElement(element: PsiElement, point: Point) {
        eventListeners.forEach { it.onHoverElement(this, element, point) }
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

    // N文字目のPsiElementの親Elementを取得する
    private fun PsiFile.getParentPsiElement(offset: Int): PsiElement? {
        return PsiTreeUtil.getParentOfType(this.getPsiElement(offset), PsiMethod::class.java, PsiField::class.java)
    }

    // 特定の要素をスタイリングする
    fun markupElement(element: PsiElement, style: TextAttributes) {
        val range = element.textRange ?: return
        markup(range, style)
    }

    // 特定の範囲をスタイリングする
    fun markup(range: TextRange, style: TextAttributes) {
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

    // N文字目の行までスクロールする
    fun scrollToLine(offset: Int) {
        editor?.scrollingModel?.scrollTo(editor?.offsetToLogicalPosition(offset) ?: return, ScrollType.CENTER)
    }

    // 指定要素までスクロールする
    fun scrollToElement(fqName: String) {
        psiFile ?: return
        val target = PsiTreeUtil.findChildrenOfAnyType(
                psiFile,
                KtNamedFunction::class.java,
                KtProperty::class.java,
                PsiField::class.java,
                PsiMethod::class.java
        ).firstOrNull { element ->
            element.kotlinFqName.toString() == fqName
        }
        target?.nameIdentifier?.textOffset?.let { scrollToLine(it) }
    }

}

// イベントハンドラ (SourceViewFieldで起きたイベントを扱いたい場合，このクラスを継承して実装する)
abstract class SourceViewFieldListener {
    open fun onEditorCreated(editor: Editor) {}
    open fun onClickElement(viewer: SourceViewField, element: PsiElement) {}
    open fun onHoverElement(viewer: SourceViewField, element: PsiElement, point: Point) {}
}