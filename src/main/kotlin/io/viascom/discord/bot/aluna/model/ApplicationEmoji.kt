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

import io.viascom.discord.bot.aluna.bot.emoji.EmojiCache
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.slf4j.Logger

/**
 * Application emoji interface.
 * By implementing this interface, you get some QOL functions for your defined emoji.
 * This interface is designed to work with application emojis that are managed by the EmojiManager.
 *
 * Example on how to use this interface:
 * ```
 * enum class MyEmojis(
 *     override val emojiName: String,
 *     override val animated: Boolean = false,
 *     override val logger: Logger = LoggerFactory.getLogger(MyEmojis::class.java)
 * ) : ApplicationEmoji {
 *     MY_EMOJI("my_emoji"),
 *     ANIMATED_EMOJI("animated_emoji", true),
 *
 *     ;
 *
 *     override fun toString(): String {
 *         return formatted()
 *     }
 * }
 * ```
 */
@JvmDefaultWithCompatibility
public interface ApplicationEmoji {

    /**
     * The name of the emoji as registered in Discord
     */
    public val emojiName: String

    /**
     * Whether the emoji is animated
     */
    public val animated: Boolean

    /**
     * Logger for this emoji
     */
    public val logger: Logger

    /**
     * Emoji manager instance
     */
    public val cache: EmojiCache?
        get() = globalCache ?: run {
            logger.error("EmojiCache not yet initialized. This is probably because the first shard is not yet connected or emoji management is disabled.")
            null
        }

    /**
     * The ID of the emoji
     */
    public val id: String
        get() = cache?.getEmojiCache()[emojiName]?.id ?: run {
            printLog()
            return ""
        }

    /**
     * Get emoji as JDA Emoji object
     *
     * @return Emoji object or null if not found
     */
    public val emoji: Emoji?
        get() = cache?.getEmojiCache()[emojiName]?.asEmoji() ?: run {
            printLog()
            return null
        }

    /**
     * Print a log message based on the current state of the emoji cache
     */
    private fun printLog() {
        when {
            (cache?.getEmojiCache()?.isEmpty() == true && cache?.isEmojisLoaded() == true) -> {
                logger.info("Emojis map is empty but emojis are marked as loaded. There seems to be an issue with retrieving the emojis. - $emojiName")
            }

            (cache?.isEmojisLoaded() == false) -> {
                logger.debug("Emojis not yet loaded. This may be because Aluna is still updating the emojis. - $emojiName")
            }

            else -> {
                val useFallback = if (cache?.getFallbackEmoji() != null) " -> Using fallback emoji '${cache?.getFallbackEmoji()}'" else ""
                logger.warn("Emote '$emojiName' not found in discord emojis.$useFallback")
            }
        }
    }

    /**
     * Get emoji as mention
     *
     * @return Emoji as mention (<[a]:name:12345>)
     */
    public fun formatted(): String {
        val emoji = cache?.getEmojiCache()[emojiName] ?: run {
            printLog()
            return cache?.getFallbackEmoji() ?: ""
        }
        val animatedSuffix = if (emoji.animated) "a" else ""
        return "<$animatedSuffix:${emoji.name}:${emoji.id}>"
    }

    /**
     * Get emoji as mention in short form (with a single character name).
     * This should always be used on large embeds as the emojis counts to the total size of the embed.
     *
     * @param spaceAfter Add addition space after the emoji
     * @return Emoji as mention without name (<[a]:e:12345>)
     */
    public fun toStringShort(spaceAfter: Boolean = false): String {
        val emoji = cache?.getEmojiCache()[emojiName] ?: run {
            printLog()
            return cache?.getFallbackEmoji() ?: ""
        }
        val animatedSuffix = if (emoji.animated) "a" else ""
        return "<$animatedSuffix:e:${emoji.id}>" + if (spaceAfter) " " else ""
    }

    /**
     * Get emoji as reaction code
     *
     * @return Reaction code of the Emote
     */
    public fun toReactionCode(): String {
        val id = cache?.getEmojiCache()[emojiName]?.id ?: run {
            printLog()
            return ""
        }
        return "$emojiName:$id"
    }

    /**
     * Get emoji as JDA Emoji object
     *
     * @return Emoji object or a question mark emoji if not found
     */
    public fun toEmoji(): Emoji {
        val emoji = cache?.getEmojiCache()[emojiName] ?: run {
            printLog()
            return Emoji.fromUnicode(cache?.getFallbackEmoji() ?: "?")
        }
        return emoji.asEmoji()
    }

    public companion object {
        @JvmStatic
        public var globalCache: EmojiCache? = null
    }
}
