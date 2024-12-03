// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.log

import com.intellij.openapi.project.Project
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 変換内容管理用シングルトン
object ConversionRecorder {

    // ログ保存ディレクトリ名
    const val LOG_DIR = "j2k"

    // プロジェクト
    var project : Project? = null
    // 変換内容記録用のリスト
    val entries: MutableList<ConversionEntry> = mutableListOf()

    // 変換内容を破棄する
    fun clear() {
        //println("Clear")
        entries.clear()
        project = null
    }

    // 変換内容を出力する
    fun output() {
        // JSONにエンコード
        val json = Json.encodeToString(ListSerializer(ConversionEntry.serializer()), entries)
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

}