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

package io.viascom.discord.bot.aluna.util

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color
import java.time.OffsetDateTime

/**
 * Marker for the Embed DSL to avoid scope leakage.
 */
@DslMarker
private annotation class EmbedDslMarker

/**
 * Entry function to build a [MessageEmbed] using a Kotlin DSL.
 *
 * Example:
 * val embed = EmbedCreate {
 *   title("Title", url = "https://example.com")
 *   description("Hello World")
 *   colorHex("#5865F2")
 *   author(name = "Aluna", iconUrl = "https://.../icon.png")
 *   field("A", "B")
 *   footer("Footer text", iconUrl = "https://.../footer.png")
 *   timestampNow()
 * }
 */
public fun EmbedCreate(block: EmbedBlock.() -> Unit): MessageEmbed {
    val b = EmbedBlock().apply(block)
    return b.build()
}

@EmbedDslMarker
public class EmbedBlock internal constructor() {
    private val builder: EmbedBuilder = EmbedBuilder()

    /** Set the title with optional URL */
    @JvmOverloads
    public fun title(text: String, url: String? = null) {
        builder.setTitle(text, url)
    }

    /** Set the description */
    public fun description(text: String) {
        builder.setDescription(text)
    }

    /** Set the color */
    public fun color(color: Color) {
        builder.setColor(color)
    }

    /** Set the color by hex string (e.g., "#5865F2") */
    public fun colorHex(hex: String) {
        builder.setColor(Color.decode(hex))
    }

    /** Set the timestamp */
    public fun timestamp(time: OffsetDateTime) {
        builder.setTimestamp(time)
    }

    /** Convenience to set timestamp to now */
    public fun timestampNow() {
        builder.setTimestamp(OffsetDateTime.now())
    }

    /** Set the author */
    @JvmOverloads
    public fun author(name: String? = null, url: String? = null, iconUrl: String? = null) {
        builder.setAuthor(name, url, iconUrl)
    }

    /** Set the footer */
    @JvmOverloads
    public fun footer(text: String, iconUrl: String? = null) {
        builder.setFooter(text, iconUrl)
    }

    /** Set the thumbnail URL */
    public fun thumbnail(url: String) {
        builder.setThumbnail(url)
    }

    /** Set the image URL */
    public fun image(url: String) {
        builder.setImage(url)
    }

    /** Add a field */
    @JvmOverloads
    public fun field(name: String, value: String, inline: Boolean = true) {
        builder.addField(name, value, inline)
    }

    internal fun build(): MessageEmbed = builder.build()
}
