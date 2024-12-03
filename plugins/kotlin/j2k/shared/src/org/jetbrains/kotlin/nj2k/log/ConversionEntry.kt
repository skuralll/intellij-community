// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.log

import kotlinx.serialization.Serializable

// 文字列の範囲指定
@Serializable
data class ConversionRange(val start : Int, val end : Int)
// 変換内容エントリ
@Serializable
data class ConversionEntry(val file : String, val range : ConversionRange, val javaFq : String, val ktFq : String, val details : String)