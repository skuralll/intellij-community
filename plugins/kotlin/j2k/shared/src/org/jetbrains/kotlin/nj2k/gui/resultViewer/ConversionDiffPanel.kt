// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.resultViewer

import com.intellij.codeInsight.hint.HintManager
import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.startOffset
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.nj2k.gui.common.SourceViewField
import org.jetbrains.kotlin.nj2k.gui.common.SourceViewFieldListener
import org.jetbrains.kotlin.nj2k.gui.filepicker.SourceViewPanel
import org.jetbrains.kotlin.nj2k.log.ConversionEntry
import org.jetbrains.kotlin.nj2k.log.ConversionRecorder
import org.jetbrains.kotlin.nj2k.log.FunctionModifierEntry
import org.jetbrains.kotlin.nj2k.log.PropertyModifierEntry
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import java.awt.Dimension
import java.awt.Font
import java.awt.GridLayout
import java.awt.Point
import javax.swing.Icon
import kotlin.reflect.KMutableProperty0

// TODO : beforeViewerとafterViewer共通の処理を一つにまとめる
class ConversionDiffPanel(project: Project, rootFile: VirtualFile) : JBPanel<JBPanel<*>>(GridLayout(1, 2)) {

    companion object {
        private val CONVERTED_TEXT_ATTRIBUTE = TextAttributes(
            null,
            JBColor.LIGHT_GRAY,
            JBColor.LIGHT_GRAY,
            EffectType.SLIGHTLY_WIDER_BOX,
            Font.PLAIN
        )
        private val MATCH_TEXT_ATTRIBUTE = TextAttributes(
            null,
            JBColor.LIGHT_GRAY,
            JBColor.PINK,
            EffectType.SLIGHTLY_WIDER_BOX,
            Font.PLAIN
        )
    }

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
        // イベントハンドラ登録
        beforeViewer.sourceViewer.addEventListener(BeforeViewerListener())
    }

    // 変換前情報をセット
    fun setBefore(document: Document?, fileType: FileType) {
        beforeViewer.switchFile(document, fileType)
        beforeViewer.label.icon = getFileIcon(document, fileType)
        // ハイライト
        highlightBefore()
    }

    // 変換後情報をセット
    fun setAfter(document: Document?, fileType: FileType) {
        afterViewer.switchFile(document, fileType)
        afterViewer.label.icon = getFileIcon(document, fileType)
        // ハイライト
        highlightAfter()
    }

    // ファイルに適したアイコンを取得するメソッド
    private fun getFileIcon(document: Document?, fileType: FileType): Icon? {
        return when {
            fileType.icon != AllIcons.FileTypes.Unknown -> fileType.icon
            else -> AllIcons.FileTypes.Any_type
        }
    }

    // 変換前のハイライト
    private fun highlightBefore() {
        // 探索
        beforeViewer.sourceViewer.psiFile?.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                when (element) {
                    is PsiField -> {
                        ConversionRecorder.getEntryByJavaFqName(element.kotlinFqName.toString())?.let {
                            beforeViewer.sourceViewer.markup(element.nameIdentifier.textRange, CONVERTED_TEXT_ATTRIBUTE)
                        }
                    }

                    is PsiMethod -> {
                        ConversionRecorder.getEntryByJavaFqName(element.kotlinFqName.toString())?.let {
                            beforeViewer.sourceViewer.markup(
                                element.nameIdentifier?.textRange ?: element.textRange,
                                CONVERTED_TEXT_ATTRIBUTE
                            )
                        }
                    }
                }
                element.acceptChildren(this)
            }
        })
    }

    // 変換後のハイライト
    private fun highlightAfter() {
        // 探索
        afterViewer.sourceViewer.psiFile?.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                when (element) {
                    is KtProperty -> {
                        ConversionRecorder.getEntryByKotlinFqName(element.kotlinFqName.toString())?.let {
                            afterViewer.sourceViewer.markup(
                                element.nameIdentifier?.textRange ?: element.textRange,
                                CONVERTED_TEXT_ATTRIBUTE
                            )
                        }
                    }

                    is KtFunction -> {
                        ConversionRecorder.getEntryByKotlinFqName(element.kotlinFqName.toString())?.let {
                            afterViewer.sourceViewer.markup(
                                element.nameIdentifier?.textRange ?: element.textRange,
                                CONVERTED_TEXT_ATTRIBUTE
                            )
                        }
                    }
                }
                element.acceptChildren(this)
            }
        })
    }

    // エディタが両方生成されているか
    fun isEditorCreated(): Boolean {
        return beforeViewer.sourceViewer.editor != null && afterViewer.sourceViewer.editor != null
    }

    // ポップアップヒントに表示するコンポーネントを取得する TODO : 改善
    fun getHintMessage(entry: ConversionEntry): String {
        return when (entry) {
            is PropertyModifierEntry -> {
                val name = entry.javaFq.split(".").last()
                when {
                    name.startsWith("get") -> "Converted to property getter"
                    name.startsWith("set") -> "Converted to property setter"
                    else -> "Converted to property"
                }
            }

            is FunctionModifierEntry -> "Converted to function"
            else -> "Unknown Modifier"
        }
    }

    // 変換前Viewer用イベントハンドラ
    inner class BeforeViewerListener : SourceViewFieldListener() {
        // ホバーしている要素
        private var hoveredBefore: PsiElement? = null

        // マークアップしている要素
        private var markedBefore: PsiElement? = null
        private var markedAfter: PsiElement? = null

        override fun onHoverElement(viewer: SourceViewField, element: PsiElement, point: Point) {
            // 識別子にホバーした時Tooltipを表示する
            // 識別子でない場合は無視
            val parent = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, PsiField::class.java)
            if (parent?.nameIdentifier != element) {
                hoveredBefore = null
                HintManager.getInstance().hideAllHints()
                return
            }
            // 同じ要素にホバーしている場合は無視，違う要素の場合既存のヒントを消す
            if (hoveredBefore == element) return
            HintManager.getInstance().hideAllHints()
            // ヒント表示
            val entry = ConversionRecorder.getEntryByJavaFqName(parent.kotlinFqName.toString()) ?: return
            parent.nameIdentifier?.let { viewer.moveCaret(it.startOffset) }
            viewer.editor?.let {
                HintManager.getInstance().showInformationHint(it, getHintMessage(entry))
            }
            // ホバー中のエレメントを更新
            hoveredBefore = element
        }

        override fun onClickElement(viewer: SourceViewField, element: PsiElement) {
            // 識別子をクリックした時afterViewerの変換後要素に移動する
            val parent = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, PsiField::class.java)
            if (parent?.nameIdentifier != element) return
            val entry = ConversionRecorder.getEntryByJavaFqName(parent.kotlinFqName.toString()) ?: return
            markAndScroll(beforeViewer.sourceViewer, entry.javaFq, ::markedBefore)
            markAndScroll(afterViewer.sourceViewer, entry.ktFq, ::markedAfter, true)
        }

        // 指定したビューアにマークアップし，スクロールする
        private fun markAndScroll(
            viewer: SourceViewField,
            fqName: String,
            markedElement: KMutableProperty0<PsiElement?>,
            shouldScroll: Boolean = false
        ) {
            viewer.getIdentifier(fqName)?.let { target ->
                if (shouldScroll) {
                    viewer.scrollToElement(target)
                }
                markedElement.get()?.let { viewer.markup(it.textRange, CONVERTED_TEXT_ATTRIBUTE) }
                viewer.markup(target.textRange, MATCH_TEXT_ATTRIBUTE)
                markedElement.set(target)
            }
        }
    }

}