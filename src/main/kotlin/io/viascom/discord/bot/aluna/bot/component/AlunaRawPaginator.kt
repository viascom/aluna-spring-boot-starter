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

package io.viascom.discord.bot.aluna.bot.component

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed.Field
import java.awt.Color
import java.time.temporal.TemporalAccessor
import java.util.function.Function
import kotlin.math.ceil

/**
 * A raw Paginator is a component to paginate through a list of items.
 *
 */
public class AlunaRawPaginator(
    public val author: Function<CurrentPage, String?>? = null,
    public val authorIcon: Function<CurrentPage, String?>? = null,
    public val authorUrl: Function<CurrentPage, String?>? = null,
    public val thumbnail: Function<CurrentPage, String?>? = null,
    public val image: Function<CurrentPage, String?>? = null,
    public val title: Function<CurrentPage, String>? = null,
    public val color: Function<CurrentPage, Color> = Function { Color.DARK_GRAY },
    public val titleLink: Function<CurrentPage, String?>? = null,
    public val description: Function<CurrentPage, String>? = null,
    public val timestamp: TemporalAccessor? = null,
    public val showFooter: Boolean = false,

    /**
     * This field gets added as the last field to every page.
     */
    public val footerElement: Function<CurrentPage, Field?>? = null,

    /**
     * If showFooter is set to true, this string will be used in the footer of the embed.
     */
    public val onPageText: Function<CurrentPage, String>? = null,

    public var elements: ArrayList<Field> = arrayListOf(),

    public val columns: Columns = Columns.THREE,
    public val elementsPerPage: Int = 15
) {

    public var emptyFieldPlaceholder: String = "##EMPTY##"

    public val totalPages: Int
        get() = ceil(elements.size.toDouble() / elementsPerPage).toInt()

    init {
        checkElementsPerPageAmount()
    }

    public fun addElement(field: Field): Boolean = elements.add(field)

    @JvmOverloads
    public fun addElement(name: String, value: String, inline: Boolean = true): Boolean = elements.add(Field(name, value, inline))

    public fun addElements(fields: Collection<Field>): Boolean = elements.addAll(fields)


    public fun renderPage(pageNum: Int): EmbedBuilder {
        val cleanedPageNum = cleanPageNumInput(pageNum)
        val embedBuilder = EmbedBuilder()
        val start = (cleanedPageNum - 1) * elementsPerPage
        val end = if (elements.size < cleanedPageNum * elementsPerPage) elements.size else cleanedPageNum * elementsPerPage

        val currentPage = CurrentPage(cleanedPageNum, totalPages, start, end)

        for (i in start until end) {
            when (columns.amount) {
                1 -> {
                    if (elements[i].name != emptyFieldPlaceholder) {
                        val noneInlineField = Field(elements[i].name, elements[i].value, false)
                        embedBuilder.addField(noneInlineField)
                    }
                }

                2 -> {
                    if (elements[i].name != emptyFieldPlaceholder) {
                        embedBuilder.addField(elements[i])
                    }
                    if (i % 2 != 0) {
                        embedBuilder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, EmbedBuilder.ZERO_WIDTH_SPACE, true)
                    }
                }

                3 -> {
                    if (elements[i].name != emptyFieldPlaceholder) {
                        embedBuilder.addField(elements[i])
                    }
                }
            }
        }

        embedBuilder.setColor(color.apply(currentPage))
        if (description != null) {
            embedBuilder.setDescription(description.apply(currentPage))
        }
        if (title != null) {
            embedBuilder.setTitle(title.apply(currentPage), titleLink?.apply(currentPage))
        }
        if (timestamp != null) {
            embedBuilder.setTimestamp(timestamp)
        }
        if (author != null) {
            embedBuilder.setAuthor(author.apply(currentPage), authorUrl?.apply(currentPage), authorIcon?.apply(currentPage))
        }
        if (thumbnail != null) {
            embedBuilder.setThumbnail(thumbnail.apply(currentPage))
        }
        if (image != null) {
            embedBuilder.setImage(image.apply(currentPage))
        }
        if (footerElement != null) {
            embedBuilder.addField(footerElement.apply(currentPage))
        }
        if (showFooter && onPageText != null) {
            embedBuilder.setFooter(onPageText.apply(currentPage), null)
        }

        return embedBuilder
    }

    private fun cleanPageNumInput(pageNum: Int): Int {
        return when {
            (pageNum < 1) -> 1
            (pageNum > totalPages) -> totalPages
            else -> pageNum
        }
    }

    private fun checkElementsPerPageAmount() {
        when {
            (elementsPerPage == 25 && footerElement != null) -> throw IllegalArgumentException("Embed can only show up to 25 elements. You have set the elementsPerPage to 25 and defined an addition footer element.")
            (elementsPerPage > 25 && footerElement == null) -> throw IllegalArgumentException("Embed can only show up to 25 elements.")
        }
    }

    public inner class CurrentPage(public val number: Int, public val total: Int, public val start: Int, public val end: Int)

    public enum class Columns(public val amount: Int) {
        ONE(1),
        TWO(2),
        THREE(3)
    }
}
