// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.previewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.nj2k.gui.common.JKFileTreePanel
import org.jetbrains.kotlin.psi.KtFile
import java.awt.Dimension
import javax.swing.JComponent

// 変換プレビュー
class Previewer(private val project: Project, private val rootFile: VirtualFile, private val ktFiles : List<KtFile>) : DialogWrapper(true) {

    // UI
    private val fileExplorer: JKFileTreePanel
    private val diffView : JKSourceDiffPanel

    init {
        title = KotlinBundle.message("action.j2k.gui.title")
        // ファイルエクスプローラ
        fileExplorer = JKFileTreePanel(rootFile)
        // diff
        diffView = JKSourceDiffPanel(project, rootFile)
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
}