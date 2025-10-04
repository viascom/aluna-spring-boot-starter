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

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.emoji.Emoji

/**
 * Kotlin DSL for building Action Rows (message components V1).
 *
 * This DSL intentionally reuses utility helpers from Utils.kt such as
 * primaryButton/secondaryButton/... and StringSelectMenu.Builder.addOption/selectOption.
 *
 * Example:
 * val rows = ActionRows {
 *   row {
 *     primaryButton("confirm", label = "Confirm")
 *     dangerButton("cancel", label = "Cancel")
 *   }
 *   row {
 *     stringSelect("choice", min = 1, max = 1) {
 *       addOption("A", "a")
 *       addOption("B", "b")
 *     }
 *   }
 * }
 */
@DslMarker
private annotation class ActionRowDslMarker

/** Entry function producing a list of [ActionRow] */
public fun ActionRows(block: ActionRowsBlock.() -> Unit): List<ActionRow> {
    val b = ActionRowsBlock().apply(block)
    return b.build()
}

@ActionRowDslMarker
public class ActionRowsBlock internal constructor() {
    private val rows: MutableList<ActionRow> = mutableListOf()

    /** Define one Action Row. */
    public fun row(block: ActionRowBlock.() -> Unit) {
        val rb = ActionRowBlock().apply(block)
        if (rb.components.isNotEmpty()) {
            rows += ActionRow.of(rb.components)
        }
    }

    internal fun build(): List<ActionRow> = rows.toList()
}

@ActionRowDslMarker
public class ActionRowBlock internal constructor() {
    internal val components: MutableList<ActionRowChildComponent> = mutableListOf()

    // region Buttons (use Utils.kt helpers)
    @JvmOverloads
    public fun primaryButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false) {
        components += io.viascom.discord.bot.aluna.util.primaryButton(id, label, emoji, disabled)
    }

    @JvmOverloads
    public fun secondaryButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false) {
        components += io.viascom.discord.bot.aluna.util.secondaryButton(id, label, emoji, disabled)
    }

    @JvmOverloads
    public fun successButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false) {
        components += io.viascom.discord.bot.aluna.util.successButton(id, label, emoji, disabled)
    }

    @JvmOverloads
    public fun dangerButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false) {
        components += io.viascom.discord.bot.aluna.util.dangerButton(id, label, emoji, disabled)
    }

    @JvmOverloads
    public fun linkButton(url: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false) {
        components += io.viascom.discord.bot.aluna.util.linkButton(url, label, emoji, disabled)
    }

    @JvmOverloads
    public fun premiumButton(url: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false) {
        components += io.viascom.discord.bot.aluna.util.premiumButton(url, label, emoji, disabled)
    }

    /** Add any pre-built [Button] directly */
    public fun button(button: Button) {
        components += button
    }
    // endregion

    // region Select Menus
    /** Create a StringSelect and add it to the row. Use Utils.kt extension addOption/selectOption in [configure]. */
    @JvmOverloads
    public fun stringSelect(
        id: String,
        min: Int = 1,
        max: Int = 1,
        configure: (StringSelectMenu.Builder.() -> Unit)? = null
    ) {
        val builder = StringSelectMenu.create(id).setMinValues(min).setMaxValues(max)
        if (configure != null) builder.configure()
        components += builder.build()
    }

    /** Create an EntitySelect and add it to the row. */
    @JvmOverloads
    public fun entitySelect(
        id: String,
        target: EntitySelectMenu.SelectTarget,
        configure: (EntitySelectMenu.Builder.() -> Unit)? = null
    ) {
        val builder = EntitySelectMenu.create(id, target)
        if (configure != null) builder.configure()
        components += builder.build()
    }

    /** Convenience: user selection */
    @JvmOverloads
    public fun userSelect(
        id: String,
        configure: (EntitySelectMenu.Builder.() -> Unit)? = null
    ) {
        entitySelect(id, EntitySelectMenu.SelectTarget.USER, configure)
    }

    /** Convenience: role selection */
    @JvmOverloads
    public fun roleSelect(
        id: String,
        configure: (EntitySelectMenu.Builder.() -> Unit)? = null
    ) {
        entitySelect(id, EntitySelectMenu.SelectTarget.ROLE, configure)
    }

    /** Convenience: channel selection (defaults to TEXT channel type) */
    @JvmOverloads
    public fun channelSelect(
        id: String,
        channelTypes: Array<out ChannelType> = arrayOf(ChannelType.TEXT),
        configure: (EntitySelectMenu.Builder.() -> Unit)? = null
    ) {
        entitySelect(id, EntitySelectMenu.SelectTarget.CHANNEL) {
            setChannelTypes(*channelTypes)
            if (configure != null) configure()
        }
    }
    // endregion
}
