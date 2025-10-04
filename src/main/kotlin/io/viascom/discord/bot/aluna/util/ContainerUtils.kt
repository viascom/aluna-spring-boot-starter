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

@file:JvmName("AlunaContainerUtils")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.util

import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import java.awt.Color

/**
 * Applies the given accent color to the container.
 *
 * @param hexColor A string representing the color in hexadecimal format (e.g., "#FF5733").
 */
public fun Container.withAccentColor(hexColor: String): Container = this.withAccentColor(Color.decode(hexColor))

/**
 * Converts the string into a Discord-styled `Container` component, optionally including a title.
 *
 * @param title An optional title to display above the string content. Defaults to null.
 * @return A `Container` containing the string content and optionally a title, styled with a Discord-themed accent color.
 */
@JvmOverloads
public fun String.toDiscordComponent(title: String? = null): Container {
    return Container.of(buildList {
        if (title != null) {
            add(textDisplay(title))
        }
        add(textDisplay(this@toDiscordComponent))
    }).withAccentColor("#2c2d31")
}

/**
 * Creates a new [TextDisplay] component with the given text.
 *
 * @param text The text to display in the component.
 * @return A new [TextDisplay] component with the given text.
 */
public fun textDisplay(text: String): TextDisplay = TextDisplay.of(text)
