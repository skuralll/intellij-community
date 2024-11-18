// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.previewer

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.psi.PsiJavaFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.util.isJavaFileType
import org.jetbrains.kotlin.nj2k.gui.common.FileTreeListener
import org.jetbrains.kotlin.nj2k.gui.common.JKFileTreePanel
import org.jetbrains.kotlin.psi.KtFile
import java.awt.Dimension
import javax.swing.JComponent

// 変換プレビュー
class Previewer(private val project: Project, private val rootFile: VirtualFile, private val javaFiles : List<PsiJavaFile>, private val ktFiles : List<KtFile>) : DialogWrapper(true), FileTreeListener {

    // UI
    private val fileExplorer: JKFileTreePanel
    private val diffView : SourceDiffPanel

    init {
        title = KotlinBundle.message("action.j2k.gui.title")
        // ファイルエクスプローラ
        fileExplorer = JKFileTreePanel(rootFile)
        fileExplorer.setActiveAllNodes(false)
        fileExplorer.setActiveFilesNodes(ktFiles.map { it.virtualFile }, true)
        fileExplorer.fileSelectionListeners.add(this)
        fileExplorer.expandFilesNodes(ktFiles.map { it.virtualFile })
        // diff
        diffView = SourceDiffPanel(project, rootFile)
        ktFiles.firstOrNull()?.let {
            fileExplorer.focusFile(it.virtualFile) // 最初のファイルにフォーカスする
            switchFile(it.virtualFile) // 最初のファイルを初期表示
        }
        init()
    }

    override fun createCenterPanel(): JComponent {
        // ファイルエクスプローラ
        fileExplorer.preferredSize = Dimension(400, 600)
        // Diff
        diffView.preferredSize = Dimension(800, 600)
        // パネル作成
        return panel {
            row {
                cell(JBLabel(KotlinBundle.message("action.j2k.gui.preview.header")).apply { font = JBFont.h3().asBold() })
            }
            row {
                label(KotlinBundle.message("action.j2k.gui.preview.description"))
                bottomGap(BottomGap.SMALL)
            }
            //separator()
            row {
                cell(fileExplorer).align(Align.FILL).resizableColumn()
                cell(diffView).align(Align.FILL)
            }.resizableRow()
        }
    }

    override fun onFilePick(selectedFiles: List<VirtualFile>) {}

    override fun onFocus(file: VirtualFile) {
        switchFile(file)
    }

    // 対象のファイルを切り替える
    private fun switchFile(file: VirtualFile) {
        // 初期化
        diffView.setBefore(null, FileTypes.UNKNOWN)
        diffView.setAfter(null, FileTypes.UNKNOWN)
        // ファイル切り替え
        diffView.setAfter(file.findDocument(), file.fileType)
        javaFiles.firstOrNull{ it.virtualFile.equals(file) }?.let {
            diffView.setBefore(EditorFactory.getInstance().createDocument(it.text), FileTypeManager.getInstance().getFileTypeByExtension("java"))
        }
    }

}