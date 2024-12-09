// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.log

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 文字列の範囲指定
@Serializable
data class ConversionRange(val start : Int, val end : Int)

// ベースの変換内容エントリ
@Serializable
sealed class ConversionEntry{
    abstract val file : String
    abstract val javaFq : String
    abstract val ktFq : String
}

// 共通の変換エントリ
// TODO : Serializableを変換タイプに応じて追加する
@Serializable
@SerialName("common")
data class CommonModifierEntry(
    override val file: String,
    override val javaFq : String,
    override val ktFq : String
) : ConversionEntry()

/*
* TODO : 変換処理ごとにエントリを追加する
*/

//// const修飾子追加
//@Serializable
//data class ConstModifierEntry(
//    override val file: String,
//    override val range: ConversionRange,
//    override val javaFq : String,
//    override val ktFq : String
//) : ConversionEntry()
//
//// コンストラクタ変換
//@Serializable
//data class ConstructorEntry(
//    override val file: String,
//    override val range: ConversionRange,
//    override val javaFq : String,
//    override val ktFq : String
//) : ConversionEntry()