// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.nj2k.log

import kotlinx.serialization.Serializable

// 変換内容エントリ
@Serializable
data class ConversionEntry(val file : String)