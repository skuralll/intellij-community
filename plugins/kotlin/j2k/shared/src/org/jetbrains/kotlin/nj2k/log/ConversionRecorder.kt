// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.log

import kotlinx.serialization.Serializable

// 変換内容管理用シングルトン
object ConversionRecorder {

    // 変換内容記録用のリスト
    val entries: MutableList<ConversionEntry> = mutableListOf()

    // 変換内容を破棄する
    fun clear() {
        //println("Clear")
        entries.clear()
    }

    // 変換内容を出力する
    fun output() {
        //println("Output to json")
    }

}