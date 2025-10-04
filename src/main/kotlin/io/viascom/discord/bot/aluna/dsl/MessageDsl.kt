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

@file:JvmName("AlunaMessageDsl")

package io.viascom.discord.bot.aluna.util

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.filedisplay.FileDisplay
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.section.SectionAccessoryComponent
import net.dv8tion.jda.api.components.section.SectionContentComponent
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.thumbnail.Thumbnail
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.awt.Color

/**
 * Kotlin DSL for building JDA 6 component messages using the new Message components.
 */
@DslMarker
private annotation class MessageDslMarker

/**
 * Entry point for the DSL. Builds a [MessageCreateData] with provided containers/components.
 */
public fun MessageCreate(block: MessageCreateDsl.() -> Unit): MessageCreateData {
    val dsl = MessageCreateDsl()
    dsl.block()
    return dsl.build()
}

@MessageDslMarker
public class MessageCreateDsl internal constructor() {
    private val containers: MutableList<Container> = mutableListOf()
    private var content: String? = null
    private val embeds: MutableList<MessageEmbed> = mutableListOf()

    /**
     * Define a container for the message.
     *
     * @param color Optional accent color for this container.
     * @param colorHex Optional accent color as hex string (e.g., "#5865F2"). If [color] is not null, it takes precedence.
     */
    @JvmOverloads
    public fun container(color: Color? = null, colorHex: String? = null, block: ContainerBlock.() -> Unit) {
        val built = ContainerBlock().apply(block).build()
        var c = Container.of(built.components)
        c = when {
            color != null -> c.withAccentColor(color)
            colorHex != null -> c.withAccentColor(colorHex)
            else -> c
        }
        containers += c
    }

    /**
     * Set plain text message content.
     */
    public fun content(text: String) {
        this.content = text
    }

    /**
     * Add one or more embeds to the message.
     */
    public fun embeds(vararg embed: MessageEmbed) {
        this.embeds += embed
    }

    /**
     * Build and add a single embed via the Embed DSL.
     */
    public fun embed(block: EmbedBlock.() -> Unit) {
        this.embeds += EmbedCreate(block)
    }

    internal fun build(): MessageCreateData {
        val builder = MessageCreateBuilder()
        if (content != null) builder.setContent(content)
        if (embeds.isNotEmpty()) builder.setEmbeds(embeds)

        if (containers.isNotEmpty()) {
            // Enable Components V2 when using Container components
            builder.useComponentsV2()
            builder.setComponents(containers)
        }
        return builder.build()
    }
}

@MessageDslMarker
public class ContainerBlock internal constructor() {
    private val components: MutableList<ContainerChildComponent> = mutableListOf()

    /**
     * Defines a logical section in the container.
     * If [accessory] is provided, a proper Section component is created with up to 3 TextDisplays.
     * If [accessory] is null, the content is added directly to the container (backwards compatible).
     */
    @JvmOverloads
    public fun section(accessory: SectionAccessoryComponent? = null, block: SectionBlock.() -> Unit) {
        val section = SectionBlock().apply(block)
        if (accessory != null) {
            components += Section.of(accessory, section.components)
        } else {
            // Fallback: add text components directly to the container
            section.components.forEach { comp ->
                if (comp is TextDisplay) components += comp
            }
        }
    }

    /**
     * Convenience overload to create a Section with a Thumbnail accessory.
     */
    @JvmOverloads
    public fun section(
        thumbnailUrl: String,
        spoiler: Boolean = false,
        description: String? = null,
        block: SectionBlock.() -> Unit
    ) {
        var t = Thumbnail.fromUrl(thumbnailUrl)
        if (description != null) t = t.withDescription(description)
        if (spoiler) t = t.withSpoiler(true)
        section(t, block)
    }

    /**
     * Adds a visual separator with configurable spacing. If [divider] is true, a visible divider is used, otherwise an invisible spacer.
     */
    @JvmOverloads
    public fun separator(spacing: Separator.Spacing = Separator.Spacing.SMALL, divider: Boolean = false) {
        val sep = if (divider) Separator.createDivider(spacing) else Separator.createInvisible(spacing)
        components += sep
    }

    /**
     * Adds a small visual separator. If [divider] is true, a stronger divider is used.
     */
    @JvmOverloads
    public fun smallSeparator(divider: Boolean = false) {
        separator(Separator.Spacing.SMALL, divider)
    }


    /**
     * Adds a large visual separator. If [divider] is true, a stronger divider is used.
     */
    @JvmOverloads
    public fun largeSeparator(divider: Boolean = false) {
        separator(Separator.Spacing.LARGE, divider)
    }

    /**
     * Adds a text display component.
     */
    public fun textDisplay(text: String) {
        components += TextDisplay.of(text)
    }

    /**
     * Adds a single image as a media gallery with one item.
     */
    @JvmOverloads
    public fun image(url: String, spoiler: Boolean = false, description: String? = null) {
        var item = MediaGalleryItem.fromUrl(url)
        if (description != null) item = item.withDescription(description)
        if (spoiler) item = item.withSpoiler(true)
        components += MediaGallery.of(item)
    }

    /**
     * Adds a file display component from a [FileUpload].
     */
    @JvmOverloads
    public fun fileDisplay(file: FileUpload, spoiler: Boolean = false) {
        var fd = FileDisplay.fromFile(file)
        if (spoiler) fd = fd.withSpoiler(true)
        components += fd
    }

    /**
     * Builds a media gallery with multiple items.
     */
    public fun mediaGallery(block: MediaGalleryBlock.() -> Unit) {
        val b = MediaGalleryBlock().apply(block)
        if (b.items.isNotEmpty()) {
            components += MediaGallery.of(b.items)
        }
    }

    // region Action rows as container children (Components V2)
    /** Add prebuilt action rows inside this container DSL as children components. */
    public fun actionRows(vararg rows: ActionRow) {
        components.addAll(rows)
    }

    /** Build action rows using the ActionRow DSL and add them to components. */
    public fun actionRows(block: ActionRowsBlock.() -> Unit) {
        components.addAll(ActionRows(block))
    }

    /** Convenience for single row using the ActionRow DSL. */
    public fun actionRow(block: ActionRowBlock.() -> Unit) {
        components.addAll(ActionRows { row(block) })
    }
    // endregion

    internal fun build(): BuiltContainer = BuiltContainer(
        components = components.toList()
    )
}

internal data class BuiltContainer(
    val components: List<ContainerChildComponent>
)

@MessageDslMarker
public class SectionBlock internal constructor() {
    internal val components: MutableList<SectionContentComponent> = mutableListOf()

    /**
     * Adds a text display inside this section.
     */
    public fun textDisplay(text: String) {
        components += TextDisplay.of(text)
    }

}


/**
 * Builder for MediaGallery items.
 */
@MessageDslMarker
public class MediaGalleryBlock internal constructor() {
    internal val items: MutableList<MediaGalleryItem> = mutableListOf()

    @JvmOverloads
    public fun item(url: String, spoiler: Boolean = false, description: String? = null) {
        var i = MediaGalleryItem.fromUrl(url)
        if (description != null) i = i.withDescription(description)
        if (spoiler) i = i.withSpoiler(true)
        items += i
    }

    @JvmOverloads
    public fun file(file: FileUpload, spoiler: Boolean = false, description: String? = null) {
        var i = MediaGalleryItem.fromFile(file)
        if (description != null) i = i.withDescription(description)
        if (spoiler) i = i.withSpoiler(true)
        items += i
    }
}

/**
 * Helper to create a thumbnail accessory for sections.
 */
@JvmOverloads
public fun thumbnail(url: String, spoiler: Boolean = false, description: String? = null): Thumbnail {
    var t = Thumbnail.fromUrl(url)
    if (description != null) t = t.withDescription(description)
    if (spoiler) t = t.withSpoiler(true)
    return t
}
