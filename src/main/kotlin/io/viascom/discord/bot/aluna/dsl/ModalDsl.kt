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

import net.dv8tion.jda.api.components.ModalTopLevelComponent
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.components.tree.ModalComponentTree
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.modals.Modal

/**
 * Marker annotation for Aluna message/modals DSL.
 * Reuses the same marker as Container DSL to avoid scope leakage between blocks.
 */
@DslMarker
internal annotation class ModalDslMarker

/**
 * Kotlin DSL to create Discord Modals in a concise and type-safe way.
 *
 * Example:
 * val modal = ModalCreate("feedback", "Add your Feedback") {
 *   shortField("title", "Title")
 *   paragraphField("text", "Text")
 *   text("Pick a user and channel")
 *   entitySelect("user_select", EntitySelectMenu.SelectTarget.USER, label = "Please select a user")
 *   channelSelect("channel_select", label = "Please select a channel")
 * }
 */
public fun ModalCreate(id: String, title: String, block: ModalBlock.() -> Unit): Modal {
    val blockReceiver = ModalBlock()
    blockReceiver.block()
    return Modal.create(id, title)
        .addComponents(blockReceiver.build())
        .build()
}

/**
 * Top-level block for building a modal.
 */
@ModalDslMarker
public class ModalBlock internal constructor() {
    private val components: MutableList<Any> = mutableListOf()

    /**
     * Add plain text to the modal (as TextDisplay).
     */
    public fun text(text: String) {
        components += TextDisplay.of(text)
    }

    /**
     * Add a text input field with full control over options.
     */
    @JvmOverloads
    public fun field(
        id: String,
        label: String,
        style: TextInputStyle = TextInputStyle.SHORT,
        placeholder: String? = null,
        min: Int = -1,
        max: Int = -1,
        value: String? = null,
        required: Boolean = true
    ) {
        components += modalTextField(
            id = id,
            label = label,
            style = style,
            placeholder = placeholder,
            min = min,
            max = max,
            value = value,
            required = required
        )
    }

    /**
     * Convenience for a short single-line text field.
     */
    @JvmOverloads
    public fun shortField(
        id: String,
        label: String,
        placeholder: String? = null,
        min: Int = -1,
        max: Int = -1,
        value: String? = null,
        required: Boolean = true
    ) {
        field(id, label, TextInputStyle.SHORT, placeholder, min, max, value, required)
    }

    /**
     * Convenience for a multi-line paragraph text field.
     */
    @JvmOverloads
    public fun paragraphField(
        id: String,
        label: String,
        placeholder: String? = null,
        min: Int = -1,
        max: Int = -1,
        value: String? = null,
        required: Boolean = true
    ) {
        field(id, label, TextInputStyle.PARAGRAPH, placeholder, min, max, value, required)
    }

    /**
     * Add a labeled StringSelectMenu to the modal.
     */
    @JvmOverloads
    public fun stringSelect(
        id: String,
        label: String,
        min: Int = 1,
        max: Int = 1,
        configure: (StringSelectMenu.Builder.() -> Unit)? = null
    ) {
        val builder = StringSelectMenu.create(id).setMinValues(min).setMaxValues(max)
        if (configure != null) builder.configure()
        components += Label.of(label, builder.build())
    }

    /**
     * Add a labeled EntitySelectMenu to the modal.
     */
    @JvmOverloads
    public fun entitySelect(
        id: String,
        target: EntitySelectMenu.SelectTarget,
        label: String,
        configure: (EntitySelectMenu.Builder.() -> Unit)? = null
    ) {
        val builder = EntitySelectMenu.create(id, target)
        if (configure != null) builder.configure()
        components += Label.of(label, builder.build())
    }

    /**
     * Convenience: user selection with a label.
     */
    @JvmOverloads
    public fun userSelect(
        id: String,
        label: String,
        configure: (EntitySelectMenu.Builder.() -> Unit)? = null
    ): Unit {
        entitySelect(id, EntitySelectMenu.SelectTarget.USER, label, configure)
    }

    /**
     * Convenience: channel selection with a label. Defaults to TEXT channel type when not overridden.
     */
    @JvmOverloads
    public fun channelSelect(
        id: String,
        label: String,
        channelTypes: Array<out ChannelType> = arrayOf(ChannelType.TEXT),
        configure: (EntitySelectMenu.Builder.() -> Unit)? = null
    ) {
        entitySelect(id, EntitySelectMenu.SelectTarget.CHANNEL, label) {
            setChannelTypes(*channelTypes)
            if (configure != null) configure()
        }
    }

    internal fun build(): ModalComponentTree {
        val top: MutableList<ModalTopLevelComponent> = mutableListOf()
        for (c in components) {
            when (c) {
                is TextDisplay -> top.add(c as ModalTopLevelComponent)
                is Label -> top.add(c as ModalTopLevelComponent)
            }
        }
        return ModalComponentTree.of(top)
    }
}
