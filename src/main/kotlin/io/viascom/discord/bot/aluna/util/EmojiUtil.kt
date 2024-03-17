/*
 * Copyright 2024 Viascom Ltd liab. Co
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

package io.viascom.discord.bot.aluna.util

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji
import net.fellbaum.jemoji.EmojiManager

/**
 * Utility class for working with emojis.
 *
 * The EmojiUtil class provides methods for resolving emoji aliases or unicode strings to their corresponding unicode representation.
 * It also provides methods for converting emoji aliases to [UnicodeEmoji] objects.
 *
 * @see UnicodeEmoji
 */
object EmojiUtil {

    private const val REGIONAL_INDICATOR_A_CODEPOINT = 127462
    private const val REGIONAL_INDICATOR_Z_CODEPOINT = 127487

    /**
     * Returns the unicode emoji from a Discord alias (e.g. `:joy:`).
     *
     *
     * **Note:** The input string is case-sensitive!
     *
     *
     * This will return itself if the input is a valid unicode emoji.
     *
     * @param input An emoji alias or unicode
     *
     * @return The unicode string of this emoji
     *
     * @throws NoSuchElementException if no emoji alias or unicode matches
     * @see resolveJDAEmoji
     */
    fun resolveEmoji(input: String): String = resolveEmojiOrNull(input) ?: throw NoSuchElementException("No emoji for input: $input")

    /**
     * Returns the unicode emoji from a Discord alias (e.g. `:joy:`), or `null` if unresolvable.
     *
     *
     * **Note:** The input string is case-sensitive!
     *
     *
     * This will return itself if the input is a valid unicode emoji.
     *
     * @param input An emoji alias or unicode
     *
     * @return The unicode string of this emoji, `null` if unresolvable
     *
     * @see .resolveJDAEmojiOrNull
     */
    fun resolveEmojiOrNull(input: String): String? {
        val emoji = EmojiManager.getByDiscordAlias(input).orElse(EmojiManager.getEmoji(input).orElse(null))

        if (emoji == null) {
            // Try to get regional indicators https://github.com/felldo/JEmoji/issues/44
            val alias = removeColonFromAlias(input)
            if (alias.startsWith("regional_indicator_")) {
                val character = alias[19]
                if (character in 'a'..'z') {
                    return Character.toString(REGIONAL_INDICATOR_A_CODEPOINT + (character.code - 'a'.code))
                }
            } else {
                if (input.codePointAt(0) in REGIONAL_INDICATOR_A_CODEPOINT..REGIONAL_INDICATOR_Z_CODEPOINT) {
                    return input
                }
            }
            return null
        }
        return emoji.unicode
    }

    private fun removeColonFromAlias(alias: String): String {
        return if (alias.startsWith(":") && alias.endsWith(":")) alias.substring(1, alias.length - 1) else alias
    }

    /**
     * Returns the [UnicodeEmoji] from a Discord alias (e.g. `:joy:`).
     *
     *
     * **Note:** The input string is case-sensitive!
     *
     *
     * This will return itself if the input is a valid unicode emoji.
     *
     * @param input An emoji alias or unicode
     *
     * @return The [UnicodeEmoji] of this emoji
     *
     * @throws NoSuchElementException if no emoji alias or unicode matches
     * @see .resolveEmoji
     */
    fun resolveJDAEmoji(input: String): UnicodeEmoji = Emoji.fromUnicode(resolveEmoji(input))

    /**
     * Returns the [UnicodeEmoji] from a Discord alias (e.g. `:joy:`), or `null` if unresolvable.
     *
     *
     * **Note:** The input string is case-sensitive!
     *
     *
     * This will return itself if the input is a valid unicode emoji.
     *
     * @param input An emoji alias or unicode
     *
     * @return The [UnicodeEmoji] of this emoji
     *
     * @see .resolveEmoji
     */
    fun resolveJDAEmojiOrNull(input: String): UnicodeEmoji? {
        val unicode = resolveEmojiOrNull(input) ?: return null
        return Emoji.fromUnicode(unicode)
    }
}
