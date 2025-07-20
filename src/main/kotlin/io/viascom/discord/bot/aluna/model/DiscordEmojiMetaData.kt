/*
 * Copyright 2025 Viascom Ltd liab. Co
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.viascom.discord.bot.aluna.model

import java.time.LocalDateTime

public data class DiscordEmojiMetaData(
    val name: String,
    var type: String,
    var fileName: String,
    var modifyDate: LocalDateTime
)

public data class DiscordEmojiMetaDataWithImage(
    val name: String,
    var type: String,
    var fileName: String,
    var modifyDate: LocalDateTime,
    val image: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DiscordEmojiMetaDataWithImage

        if (name != other.name) return false
        if (type != other.type) return false
        if (fileName != other.fileName) return false
        if (modifyDate != other.modifyDate) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + modifyDate.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}
