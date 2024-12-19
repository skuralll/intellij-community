// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.resultViewer

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
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
import org.jetbrains.kotlin.nj2k.gui.common.FileTreeListener
import org.jetbrains.kotlin.nj2k.gui.common.JKFileTreePanel
import org.jetbrains.kotlin.psi.KtFile
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.Timer

// 変換プレビュー
class ResultViewer(
    private val project: Project,
    private val rootFile: VirtualFile,
    private val javaFiles: List<PsiJavaFile>,
    private val ktFiles: List<KtFile>
) : DialogWrapper(true), FileTreeListener {

    companion object {
        private const val INIT_CHECK_PERIOD = 100 // エディタが初期化されているかを確認する感覚
    }

    // UI
    private val fileExplorer: JKFileTreePanel
    private val diffView: ConversionDiffPanel

    init {
        title = KotlinBundle.message("action.j2k.gui.title")
        // ファイルエクスプローラ
        fileExplorer = JKFileTreePanel(rootFile)
        fileExplorer.setActiveAllNodes(false)
        fileExplorer.setActiveFilesNodes(ktFiles.map { it.virtualFile }, true)
        fileExplorer.fileSelectionListeners.add(this)
        fileExplorer.expandFilesNodes(ktFiles.map { it.virtualFile })
        // diff
        // Timerを使用して1秒後に処理を実行
        diffView = ConversionDiffPanel(project, rootFile)
        // エディタが初期化されているかを確認し，初期化されていれば最初のファイルを表示する
        Timer(INIT_CHECK_PERIOD) { event ->
            if(diffView.isEditorCreated()){
                ktFiles.firstOrNull()?.let { file ->
                    fileExplorer.focusFile(file.virtualFile)
                    switchFile(file.virtualFile)
                }
                event.source?.let { timerSource ->
                    (timerSource as Timer).stop()
                }
            }
        }.apply {
            isRepeats = true
            start()
        }
        // 初期化
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
                cell(JBLabel(KotlinBundle.message("action.j2k.gui.result_viewer.header")).apply { font = JBFont.h3().asBold() })
            }
            row {
                label(KotlinBundle.message("action.j2k.gui.result_viewer.description"))
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
        // 変換後のファイルをセット
        diffView.setAfter(file.findDocument(), file.fileType)
        // 変換前のファイルをセット
        val beforeFile = javaFiles.firstOrNull { it.virtualFile.equals(file) }
        if(beforeFile == null){
            diffView.setBefore(file.findDocument(), file.fileType)
        } else{
            diffView.setBefore(
                EditorFactory.getInstance().createDocument(beforeFile.text),
                FileTypeManager.getInstance().getFileTypeByExtension("java")
            )
        }
    }

}