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

import net.dv8tion.jda.api.entities.emoji.Emoji

/**
 * Discord emoji interface.
 * By implementing this interface, you get some QOL functions for your defined emoji.
 *
 * Example on how to use this interface:
 * ```
 * enum class MyEmojis(override val id: String, override val emojiName: String, override val animated: Boolean = false) : DiscordEmoji {
 *     ASPARKLES("963462938822344794", "asparkles", true),
 *     AUTHORIZED("963463099317362728", "authorized"),
 * }
 * ```
 *
 */
@JvmDefaultWithCompatibility
interface DiscordEmoji {
    val id: String
    val emojiName: String
    val animated: Boolean

    /**
     * Get emoji as mention
     *
     * @return Emoji as mention (<[a]:name:12345>)
     */
    fun formatted(): String {
        val animatedSuffix = if (animated) "a" else ""
        return "<$animatedSuffix:$emojiName:$id>"
    }

    /**
     * Get emoji as mention in short form (with a single character name).
     * This should always be used on large embeds as the emojis counts to the total size of the embed.
     *
     * @param spaceAfter Add addition space after the emoji
     * @return Emoji as mention without name (<[a]:e:12345>)
     */
    fun toStringShort(spaceAfter: Boolean = false): String {
        val animatedSuffix = if (animated) "a" else ""
        return "<$animatedSuffix:e:$id>" + if (spaceAfter) " " else ""
    }

    /**
     * Get emoji as reaction code
     *
     * @return Reaction code of the Emote
     */
    fun toReactionCode(): String {
        return "$emojiName:$id"
    }

    /**
     * Get emoji
     *
     * @return Emoji.
     */
    fun toEmoji(): Emoji {
        return Emoji.fromCustom(emojiName, id.toLong(), animated)
    }
}
