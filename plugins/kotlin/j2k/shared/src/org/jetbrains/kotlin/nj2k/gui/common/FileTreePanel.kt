// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.gui.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeListener
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

open class FileTreePanel(
    rootFile: VirtualFile,
    private val selectedFiles: MutableList<VirtualFile> = mutableListOf(),
    hasCheckBox: Boolean = false
) : JBPanel<JBPanel<*>>(BorderLayout()) {

    protected val tree: CheckboxTree
    protected val rootNode: CheckedTreeNode
    val fileSelectionListeners = mutableListOf<FileTreeListener>()

    init {
        // ツリー構築
        rootNode = createNode(rootFile)
        tree = CheckboxTree(getCellRenderer(hasCheckBox), rootNode)
        uncheckAllNodes(rootNode)
        checkFilesNodes(selectedFiles)
        expandFilesNodes(selectedFiles)
        // イベント登録
        registerEvents(tree)
        // スクロールパネルを追加
        val scrollpane = JBScrollPane()
        scrollpane.setViewportView(tree)
        add(scrollpane, BorderLayout.CENTER)
    }

    // 表示,選択可能なファイルかどうかを取得する
    open fun isEnabledFile(file: VirtualFile): Boolean = true

    // セルレンダラを取得する
    open fun getCellRenderer(hasCheckBox: Boolean): CheckboxTree.CheckboxTreeCellRenderer = CustomCheckBoxTreeCellRenderer(hasCheckBox)

    // 再帰的にノード作成
    protected fun createNode(file: VirtualFile): CheckedTreeNode {
        val node = CheckedTreeNode(file)
        if (file.isDirectory) {
            file.children.filter { isEnabledFile(it) }.forEach { child ->
                node.add(createNode(child))
            }
        }
        return node
    }

    // 条件にマッチしたノードを操作
    protected fun operateMatchedNodes(
        node: CheckedTreeNode,
        condition: (CheckedTreeNode) -> Boolean,
        operation: (CheckedTreeNode) -> Unit
    ) {
        if (condition(node)) operation(node)
        node.children().asSequence().filterIsInstance<CheckedTreeNode>().forEach {
            operateMatchedNodes(it, condition, operation)
        }
    }

    // 全てのノードをチェック解除
    protected fun uncheckAllNodes(node: CheckedTreeNode) {
        operateMatchedNodes(node, { true }, { it.isChecked = false })
    }

    // 指定したノードをチェック
    protected fun checkNodes(node: CheckedTreeNode, files: List<VirtualFile>) {
        operateMatchedNodes(node, { files.contains(it.userObject) }, { it.isChecked = true })
    }

    // 指定したファイルのノードをチェック
    fun checkFilesNodes(files: List<VirtualFile>) {
        checkNodes(rootNode, files)
    }

    // マッチしたノードが有効かどうかを切り替える
    protected fun setActiveNodes(condition: (CheckedTreeNode) -> Boolean, active: Boolean) {
        operateMatchedNodes(rootNode, condition) {
            if (!active) it.isChecked = false
            it.isEnabled = active
            //activateParent(it)
        }
    }

    // 全てのノードが有効かどうかを切り替える
    fun setActiveAllNodes(active: Boolean){
        setActiveNodes({ true }, active)
    }

    // 指定したファイルのノードが有効かどうかを切り替える
    fun setActiveFilesNodes(files: List<VirtualFile>, active: Boolean) {
        setActiveNodes({ files.contains(it.userObject) }, active)
    }

    // 指定したノードを展開する.Leafをexpandしても反映されない不具合があるため、親ディレクトリをexpandする
    protected fun expandNodes(files: List<VirtualFile>) {
        val dirs = files.mapTo(mutableSetOf()) { if (it.isFile) it.parent else it }
        operateMatchedNodes(rootNode, { dirs.contains(it.userObject) }, {
            tree.expandPath(TreePath(it.path))
        })
    }

    // 指定したファイルのノードを展開
    protected fun expandFilesNodes(files: List<VirtualFile>) {
        expandNodes(files)
    }

    // ノードがアクティブな子ノードを持つか
    private fun hasActiveNode(node: CheckedTreeNode): Boolean {
        if (node.isEnabled) return true
        return node.children().asSequence().filterIsInstance<CheckedTreeNode>().any { hasActiveNode(it) }
    }

    // 親ノードをアクティブにする
    private fun activateParent(node: CheckedTreeNode) {
        println((node.userObject as VirtualFile).path)
        if (node.parent is CheckedTreeNode) {
            val parent = node.parent as CheckedTreeNode
            parent.isEnabled = true
            activateParent(parent)
        }
    }

    // イベント登録
    protected fun registerEvents(checkBoxTree: CheckboxTree) {
        // チェクボックスの状態変更イベント
        checkBoxTree.addCheckboxTreeListener(object : CheckboxTreeListener {
            override fun nodeStateChanged(node: CheckedTreeNode) {
                super.nodeStateChanged(node)
                (node.userObject as? VirtualFile)?.let { file ->
                    if (node.isChecked) selectedFiles.add(file) else selectedFiles.remove(file)
                    fileSelectionListeners.forEach { listener ->
                        listener.onFilePick(selectedFiles)
                    }
                }
            }
        })
        // クリックイベント
        checkBoxTree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                val node = checkBoxTree.getLastSelectedPathComponent() as? DefaultMutableTreeNode
                (node?.userObject as? VirtualFile)?.let { file ->
                    fileSelectionListeners.forEach { listener ->
                        listener.onFocus(file)
                    }
                }
            }
        })
    }

}

// カスタムセルレンダラ
class CustomCheckBoxTreeCellRenderer(private val hasCheckBox: Boolean) :
    CheckboxTree.CheckboxTreeCellRenderer() {
    override fun customizeRenderer(
        tree: JTree?,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        if (value !is DefaultMutableTreeNode) return
        // チェックボックス表示設定
        checkbox.isVisible = hasCheckBox
        // アイコン/テキスト設定
        (value.userObject as? VirtualFile)?.let { file ->
            textRenderer.append(file.name)
            textRenderer.icon = when {
                file.isDirectory -> AllIcons.Nodes.Folder
                file.isFile -> FileTypeManager.getInstance().getFileTypeByFile(file).icon
                else -> null
            }
            // 有効/無効時に色を変更
            textRenderer.isEnabled = true
            if (value is CheckedTreeNode && !value.isEnabled) {
                textRenderer.isEnabled = false
            }
        }
    }
}

interface FileTreeListener {
    fun onFilePick(selectedFiles: List<VirtualFile>)
    fun onFocus(file: VirtualFile)
}
