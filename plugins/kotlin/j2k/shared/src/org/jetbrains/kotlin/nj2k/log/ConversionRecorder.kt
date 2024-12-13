// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.log

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiElement
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.nj2k.getRelativePath
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 変換内容管理用シングルトン
object ConversionRecorder {

    // ログ保存ディレクトリ名
    const val LOG_DIR = "j2k"

    // プロジェクト
    var project: Project? = null

    // 変換内容記録用のリスト
    private val entries: MutableList<ConversionEntry> = mutableListOf()

    // 変換内容を破棄する
    fun clear() {
        entries.clear()
        project = null
    }

    // 変換内容を出力する
    fun output() {
        // JSONにエンコード
        val jsonFormatter = Json { prettyPrint = true }
        val json = jsonFormatter.encodeToString(ListSerializer(ConversionEntry.serializer()), entries)
        // ログ出力
        ApplicationManager.getApplication().runWriteAction {
            val root = project?.guessProjectDir() ?: return@runWriteAction // TODO エラー出力
            // ディレクトリ作成
            val logDir = root.findChild(LOG_DIR) ?: let {
                root.createChildDirectory(this, LOG_DIR)
            }
            // ログ作成
            val dateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
            val formatted = dateTime.format(formatter)
            val fileName = "$formatted.json"
            logDir.createChildData(this, fileName).apply { setBinaryContent(json.toByteArray()) }
        }
    }

    // 変換内容を追加する
    fun add(javaPsi: PsiElement?, ktPsi: PsiElement, type: ConversionType = ConversionType.COMMON) {
        // TODO ファイル名取得処理の改善(javaPsiはnullable)
        if (project == null) return
        val filePath = javaPsi?.containingFile?.virtualFile?.getRelativePath(project!!) ?: return
        val javaFq = getJavaFqName(javaPsi)
        val ktFq = getKotlinFqName(ktPsi)
        val entry = type.createEntry(filePath, javaFq, ktFq)
        entries.add(entry)
    }

    // Javaの完全修飾名を取得する
    private fun getJavaFqName(psiElement: PsiElement): String {
        //return when (psiElement) {
        //    is PsiClass -> psiElement.qualifiedName ?: "" // クラスの完全修飾名
        //    is PsiMethod -> "${psiElement.containingClass?.qualifiedName}.${psiElement.name}" // メソッドの完全修飾名
        //    is PsiField -> "${psiElement.containingClass?.qualifiedName}.${psiElement.name}" // フィールドの完全修飾名
        //    else -> ""
        //}
        return psiElement.kotlinFqName.toString()
    }

    // Kotlinの完全修飾名を取得する
    private fun getKotlinFqName(psiElement: PsiElement): String {
        return psiElement.kotlinFqName?.toString() ?: ""
    }

}