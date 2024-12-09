// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.log

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import java.io.File
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
        // ディレクトリ作成
        val dir = File("${project?.basePath}/${LOG_DIR}")
        dir.mkdirs()
        // ログ作成
        val dateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
        val formatted = dateTime.format(formatter)
        val file = File("${project?.basePath}/${LOG_DIR}/$formatted.json")
        file.writeText(json)
    }

    // 変換内容を追加する
    fun add(psiElement: PsiElement, type: ConversionType) {
        val file = psiElement.containingFile ?: return
        //val range = ConversionRange(psiElement.textRange.startOffset, psiElement.textRange.endOffset)
        //val javaFq = getJavaFqName(psiElement)
        //val ktFq = getKotlinFqName(psiElement)
        //val entry = type.createEntry(file.name, range, javaFq, ktFq, psiElement)
        val entry = type.createEntry(file.name)
        entries.add(entry)
    }

    // Javaの完全修飾名を取得する
    private fun getJavaFqName(psiElement: PsiElement): String {
        return when (psiElement) {
            is PsiClass -> psiElement.qualifiedName ?: "" // クラスの完全修飾名
            is PsiMethod -> "${psiElement.containingClass?.qualifiedName}.${psiElement.name}" // メソッドの完全修飾名
            is PsiField -> "${psiElement.containingClass?.qualifiedName}.${psiElement.name}" // フィールドの完全修飾名
            else -> ""
        }
    }

    // Kotlinの完全修飾名を取得する
    private fun getKotlinFqName(psiElement: PsiElement): String {
        return psiElement.kotlinFqName?.toString() ?: ""
    }

}