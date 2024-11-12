// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.previewer

import com.intellij.lang.documentation.ide.impl.DocumentationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
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
        // diff
        diffView = SourceDiffPanel(project, rootFile)
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
                cell(fileExplorer).align(Align.FILL).resizableColumn()
                cell(diffView).align(Align.FILL)
            }.resizableRow()
        }
    }

    override fun onFilePick(selectedFiles: List<VirtualFile>) {}

    override fun onFocus(file: VirtualFile) {
        // ビューア初期化
        diffView.beforeViewer.switchFile(EditorFactory.getInstance().createDocument(""), FileTypeManager.getInstance().getFileTypeByExtension("java"))
        diffView.afterViewer.switchFile(EditorFactory.getInstance().createDocument(""), FileTypeManager.getInstance().getFileTypeByExtension("kt"))
        // ファイル切り替え
        diffView.afterViewer.switchFile(file)
        javaFiles.firstOrNull{ it.virtualFile.equals(file) }?.let {
            diffView.beforeViewer.switchFile(EditorFactory.getInstance().createDocument(it.text), FileTypeManager.getInstance().getFileTypeByExtension("java"))
        }
    }
}