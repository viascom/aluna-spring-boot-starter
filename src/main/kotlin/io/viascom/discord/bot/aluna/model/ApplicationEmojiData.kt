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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji
import net.dv8tion.jda.api.utils.TimeUtil
import java.time.OffsetDateTime

/**
 * Data class for application emoji information
 *
 * @property id The ID of the emoji
 * @property name The name of the emoji
 * @property animated Whether the emoji is animated
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public data class ApplicationEmojiData @JsonCreator constructor(
    @param:JsonProperty("id") val id: String,
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("animated") val animated: Boolean
) {
    /**
     * Get the time when the emoji was created
     *
     * @return The time when the emoji was created
     */
    public fun getTimeCreated(): OffsetDateTime {
        return TimeUtil.getTimeCreated(id.toLong())
    }

    /**
     * Convert to JDA Emoji object
     *
     * @return JDA Emoji object
     */
    public fun asEmoji(): Emoji {
        return Emoji.fromCustom(name, id.toLong(), animated)
    }

    /**
     * Get the emoji as a formatted string
     *
     * @return Formatted emoji string
     */
    override fun toString(): String {
        return asEmoji().formatted
    }

    /**
     * Get the image URL for this emoji
     *
     * @return Image URL
     */
    public fun getImageUrl(): String {
        return String.format("https://cdn.discordapp.com/emojis/%s.%s", this.id, if (this.animated) "gif" else "png")
    }

    public companion object {
        /**
         * Create ApplicationEmojiData from a JDA RichCustomEmoji
         *
         * @param emoji JDA RichCustomEmoji
         * @return ApplicationEmojiData
         */
        public fun fromRichCustomEmoji(emoji: RichCustomEmoji): ApplicationEmojiData {
            return ApplicationEmojiData(emoji.id, emoji.name, emoji.isAnimated)
        }

        /**
         * Create ApplicationEmojiData from a JDA ApplicationEmoji
         *
         * @param emoji JDA ApplicationEmoji
         * @return ApplicationEmojiData
         */
        public fun fromApplicationEmoji(emoji: ApplicationEmoji): ApplicationEmojiData {
            return ApplicationEmojiData(emoji.id, emoji.name, emoji.isAnimated)
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class ApplicationEmojiDataList @JsonCreator constructor(
    @param:JsonProperty("items") val items: ArrayList<ApplicationEmojiData> = ArrayList()
)
