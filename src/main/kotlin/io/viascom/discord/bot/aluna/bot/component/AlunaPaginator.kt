/*
 * Copyright 2023 Viascom Ltd liab. Co
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

import io.viascom.discord.bot.aluna.bot.listener.EventWaiter
import io.viascom.discord.bot.aluna.util.NanoId
import io.viascom.discord.bot.aluna.util.getSelection
import io.viascom.discord.bot.aluna.util.removeComponents
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed.Field
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.requests.FluentRestAction
import net.dv8tion.jda.api.utils.messages.*
import java.awt.Color
import java.time.Duration
import java.time.temporal.TemporalAccessor
import java.util.function.Consumer
import java.util.function.Function
import kotlin.math.ceil

/**
 * A Paginator is a component to paginate through a list of items.
 * It uses the [EventWaiter] to wait for user interactions.
 *
 */
class AlunaPaginator(
    val eventWaiter: EventWaiter,
    val eventWaiterId: String = NanoId.generate(),

    /**
     * Which users are allowed to use the Paginator. If null, all users are allowed.
     */
    val allowedUsers: List<User>? = null,

    val author: Function<CurrentPage, String?>? = null,
    val authorIcon: Function<CurrentPage, String?>? = null,
    val authorUrl: Function<CurrentPage, String?>? = null,
    val thumbnail: Function<CurrentPage, String?>? = null,
    val image: Function<CurrentPage, String?>? = null,
    val title: Function<CurrentPage, String>? = null,
    val color: Function<CurrentPage, Color> = Function { Color.DARK_GRAY },
    val titleLink: Function<CurrentPage, String?>? = null,
    val description: Function<CurrentPage, String>? = null,
    val timestamp: TemporalAccessor? = null,

    /**
     * This field gets added as the last field to every page.
     */
    val footerElement: Function<CurrentPage, Field?>? = null,

    /**
     * If showFooter is set to true, this string will be used in the footer of the embed.
     */
    val onPageText: Function<CurrentPage, String>? = null,

    var elements: ArrayList<Field> = arrayListOf(),

    val columns: Columns = Columns.THREE,
    val elementsPerPage: Int = 15,

    val showButtons: Boolean = true,
    val showBulkSkipButtons: Boolean = false,
    val bulkSkipNumber: Int = 5,
    val showSelections: Boolean = false,
    val showCancel: Boolean = true,
    val showFooter: Boolean = false,
    val wrapPageEnds: Boolean = true,

    val timeout: Duration? = Duration.ofMinutes(10),
    val messageCompleteAction: Consumer<Message> = Consumer { }
) {
    var currentPageNumber: Int = 1
        @JvmSynthetic internal set

    var emptyFieldPlaceholder = "##EMPTY##"
    var timeoutAction: Consumer<Message> = Consumer { message ->
        message.editMessage(renderPage(currentPageNumber, MessageEditBuilder())).removeComponents().queue()
    }
    var cancelAction: Consumer<Message> = timeoutAction

    var bulkLeftButton: Button = Button.primary("big-left", "<<")
    var leftButton: Button = Button.primary("left", "<")
    var bulkRightButton: Button = Button.primary("big-right", ">>")
    var rightButton: Button = Button.primary("right", ">")
    var cancelButton: Button = Button.danger("cancel", "Cancel")

    var selectItem: Function<Int, SelectOption> = Function { SelectOption.of("Page $it", "$it") }

    val totalPages: Int
        get() = ceil(elements.size.toDouble() / elementsPerPage).toInt()

    init {
        checkElementsPerPageAmount()
    }

    fun addElement(field: Field) = elements.add(field)

    @JvmOverloads
    fun addElement(name: String, value: String, inline: Boolean = true) = elements.add(Field(name, value, inline))

    fun addElements(fields: Collection<Field>) = elements.addAll(fields)

    /**
     * Starts pagination on page 1 in the provided [MessageChannel][net.dv8tion.jda.api.entities.channel.middleman.MessageChannel].
     *
     * @param channel The MessageChannel to send the new Message to
     * @param pageNum The page number to begin on
     * @return MessageCreateData
     */
    @JvmOverloads
    fun display(channel: MessageChannel, pageNum: Int = 1): MessageCreateData {
        val cleanedPageNum = cleanPageNumInput(pageNum)
        val msg = renderPage(cleanedPageNum, MessageCreateBuilder())
        initialize(channel.sendMessage(msg), cleanedPageNum)
        return msg
    }

    /**
     * Starts displaying this Pagination by editing the provided
     * [Message][net.dv8tion.jda.api.entities.Message].
     *
     * @param message The Message to display the paginator
     * @param pageNum The page number to begin on
     */
    @JvmOverloads
    fun display(message: Message, pageNum: Int = 1): MessageEditData {
        val cleanedPageNum = cleanPageNumInput(pageNum)
        val msg = renderPage(cleanedPageNum, MessageEditBuilder())
        initialize(message.editMessage(msg), cleanedPageNum)
        return msg
    }

    /**
     *  Starts pagination on page 1 by using the provided
     * [InteractionHook][net.dv8tion.jda.api.interactions.InteractionHook].
     *
     * @param hook The InteractionHook to use
     * @param pageNum The page number to begin on
     */
    @JvmOverloads
    fun display(hook: InteractionHook, pageNum: Int = 1): MessageEditData {
        val cleanedPageNum = cleanPageNumInput(pageNum)
        val msg = renderPage(cleanedPageNum, MessageEditBuilder())
        initialize(hook.editOriginal(msg), cleanedPageNum)
        return msg
    }

    private fun initialize(action: FluentRestAction<Message, *>, pageNum: Int) {
        action as MessageRequest<*>
        if (totalPages > 1) {
            action.setComponents(generateActionRow(pageNum))

            action.queue { message ->
                messageCompleteAction.accept(message)
                if (showButtons) {
                    registerButtonWaiter(message)
                }
                if (showSelections) {
                    registerSelectionWaiter(message)
                }
            }
        } else {
            action.queue { message ->
                messageCompleteAction.accept(message)
            }
        }
    }

    private fun <T : MessageData> renderPage(pageNum: Int, builder: AbstractMessageBuilder<out T, *>): T {
        val embedBuilder = EmbedBuilder()
        val start = (pageNum - 1) * elementsPerPage
        val end = if (elements.size < pageNum * elementsPerPage) elements.size else pageNum * elementsPerPage

        val currentPage = CurrentPage(pageNum, totalPages, start, end)

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

        builder.setEmbeds(embedBuilder.build())
        return builder.build()
    }

    private fun generateActionRow(pageNum: Int): ArrayList<ActionRow> {
        val rows = ArrayList<ActionRow>()

        if (showSelections) {
            val selection = StringSelectMenu.create("page-selection")
            for (i in 1..totalPages) {
                selection.addOptions(selectItem.apply(i).withDefault(i == pageNum))
            }
            rows.add(ActionRow.of(selection.build()))
        }

        val actions = ArrayList<ItemComponent>()
        if (showButtons) {
            if (bulkSkipNumber > 1 && showBulkSkipButtons) {
                actions.add(bulkLeftButton)
            }
            if (pageNum > 1 || wrapPageEnds) {
                actions.add(leftButton)
            }
            if (pageNum < totalPages || wrapPageEnds) {
                actions.add(rightButton)
            }
            if (bulkSkipNumber > 1 && showBulkSkipButtons) {
                actions.add(bulkRightButton)
            }
        }

        if (showCancel) {
            actions.add(cancelButton)
        }

        if (actions.isNotEmpty()) {
            rows.add(ActionRow.of(actions))
        }

        return rows
    }

    private fun registerButtonWaiter(message: Message) {
        timeout?.let { eventWaiter.overrideTimeout(eventWaiterId, it) }

        if (eventWaiter.hasWaiter(eventWaiterId, ButtonInteractionEvent::class.java)) {
            return
        }

        eventWaiter.waitForInteraction(
            type = ButtonInteractionEvent::class.java,
            id = eventWaiterId,
            message = message,
            timeout = timeout,
            timeoutAction = { timeoutAction.accept(message) },
            stayActive = true,
            condition = { event ->
                if (allowedUsers?.isNotEmpty() == true) {
                    event.user.id in allowedUsers.map { it.id }
                } else {
                    true
                }
            },
            action = { event ->
                val hook = event.deferEdit().complete()
                var newPageNum = currentPageNumber
                when (event.componentId) {
                    leftButton.id -> {
                        if (newPageNum == 1 && wrapPageEnds) {
                            newPageNum = totalPages + 1
                        }
                        if (newPageNum > 1) {
                            newPageNum--
                        }
                    }

                    rightButton.id -> {
                        if (newPageNum == totalPages && wrapPageEnds) {
                            newPageNum = 0
                        }
                        if (newPageNum < totalPages) {
                            newPageNum++
                        }
                    }

                    bulkLeftButton.id -> if (newPageNum > 1 || wrapPageEnds) {
                        var i = 1
                        while ((newPageNum > 1 || wrapPageEnds) && i < bulkSkipNumber) {
                            if (newPageNum == 1 && wrapPageEnds) {
                                newPageNum = totalPages + 1
                            }
                            newPageNum--
                            i++
                        }
                    }

                    bulkRightButton.id -> if (newPageNum < totalPages || wrapPageEnds) {
                        var i = 1
                        while ((newPageNum < totalPages || wrapPageEnds) && i < bulkSkipNumber) {
                            if (newPageNum == totalPages && wrapPageEnds) {
                                newPageNum = 0
                            }
                            newPageNum++
                            i++
                        }
                    }

                    cancelButton.id -> {
                        cancelAction.accept(message)
                        eventWaiter.removeWaiter(eventWaiterId, false)
                        return@waitForInteraction
                    }
                }

                this.currentPageNumber = newPageNum
                display(hook, newPageNum)
            }
        )
    }

    private fun registerSelectionWaiter(message: Message) {
        timeout?.let { eventWaiter.overrideTimeout(eventWaiterId, it) }

        if (eventWaiter.hasWaiter(eventWaiterId, StringSelectInteractionEvent::class.java)) {
            return
        }

        eventWaiter.waitForInteraction(
            type = StringSelectInteractionEvent::class.java,
            id = eventWaiterId,
            message = message,
            timeout = timeout,
            timeoutAction = { timeoutAction.accept(message) },
            stayActive = true,
            condition = { event ->
                if (allowedUsers?.isNotEmpty() == true) {
                    event.user.id in allowedUsers.map { it.id }
                } else {
                    true
                }
            },
            action = { event ->
                val hook = event.deferEdit().complete()
                this.currentPageNumber = event.getSelection().toInt()
                display(hook, this.currentPageNumber)
            })
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

    inner class CurrentPage(val number: Int, val total: Int, val start: Int, val end: Int)

    enum class Columns(val amount: Int) {
        ONE(1),
        TWO(2),
        THREE(3)
    }

    class Builder {
        @JvmSynthetic
        var eventWaiter: EventWaiter? = null
            private set

        fun eventWaiter(eventWaiter: EventWaiter) = apply { this.eventWaiter = eventWaiter }

        @JvmSynthetic
        var eventWaiterId: String = NanoId.generate()
            private set

        fun eventWaiterId(eventWaiterId: String) = apply { this.eventWaiterId = eventWaiterId }

        /**
         * Which users are allowed to use the Paginator. If null, all users are allowed.
         */
        @JvmSynthetic
        var allowedUsers: List<User>? = null
            private set

        /**
         * Which users are allowed to use the Paginator. If null, all users are allowed.
         */
        fun allowedUsers(allowedUsers: List<User>?) = apply { this.allowedUsers = allowedUsers }

        @JvmSynthetic
        var author: Function<CurrentPage, String?>? = null
            private set

        fun author(author: Function<CurrentPage, String?>?) = apply { this.author = author }

        @JvmSynthetic
        var authorIcon: Function<CurrentPage, String?>? = null
            private set

        fun authorIcon(authorIcon: Function<CurrentPage, String?>?) = apply { this.authorIcon = authorIcon }

        @JvmSynthetic
        var authorUrl: Function<CurrentPage, String?>? = null
            private set

        fun authorUrl(authorUrl: Function<CurrentPage, String?>?) = apply { this.authorUrl = authorUrl }

        @JvmSynthetic
        var thumbnail: Function<CurrentPage, String?>? = null
            private set

        fun thumbnail(thumbnail: Function<CurrentPage, String?>?) = apply { this.thumbnail = thumbnail }

        @JvmSynthetic
        var image: Function<CurrentPage, String?>? = null
            private set

        fun image(image: Function<CurrentPage, String?>?) = apply { this.image = image }

        @JvmSynthetic
        var title: Function<CurrentPage, String>? = null
            private set

        fun title(title: Function<CurrentPage, String>?) = apply { this.title = title }

        @JvmSynthetic
        var color: Function<CurrentPage, Color> = Function { Color.DARK_GRAY }
            private set

        fun color(color: Function<CurrentPage, Color>) = apply { this.color = color }

        @JvmSynthetic
        var titleLink: Function<CurrentPage, String?>? = null
            private set

        fun titleLink(titleLink: Function<CurrentPage, String?>?) = apply { this.titleLink = titleLink }

        @JvmSynthetic
        var description: Function<CurrentPage, String>? = null
            private set

        fun description(description: Function<CurrentPage, String>?) = apply { this.description = description }

        @JvmSynthetic
        var timestamp: TemporalAccessor? = null
            private set

        fun timestamp(timestamp: TemporalAccessor?) = apply { this.timestamp = timestamp }

        /**
         * This field gets added as last field to every page.
         */
        @JvmSynthetic
        var footerElement: Function<CurrentPage, Field?>? = null
            private set

        fun footerElement(footerElement: Function<CurrentPage, Field?>?) = apply { this.footerElement = footerElement }

        /**
         * If showFooter is set to true, this string will be used in the footer of the embed.
         */
        @JvmSynthetic
        var onPageText: Function<CurrentPage, String>? = null
            private set

        fun onPageText(onPageText: Function<CurrentPage, String>?) = apply { this.onPageText = onPageText }

        @JvmSynthetic
        var elements: ArrayList<Field> = arrayListOf()
            private set

        fun elements(elements: ArrayList<Field>) = apply { this.elements = elements }

        fun addElement(field: Field) = elements.add(field)

        @JvmOverloads
        fun addElement(name: String, value: String, inline: Boolean = true) = elements.add(Field(name, value, inline))

        fun addElements(fields: Collection<Field>) = elements.addAll(fields)

        @JvmSynthetic
        var columns: Columns = Columns.THREE
            private set

        fun columns(columns: Columns) = apply { this.columns = columns }

        @JvmSynthetic
        var elementsPerPage: Int = 15
            private set

        fun elementsPerPage(elementsPerPage: Int) = apply { this.elementsPerPage = elementsPerPage }

        @JvmSynthetic
        var showButtons: Boolean = true
            private set

        fun showButtons(showButtons: Boolean) = apply { this.showButtons = showButtons }

        @JvmSynthetic
        var showBulkSkipButtons: Boolean = false
            private set

        fun showBulkSkipButtons(showBulkSkipButtons: Boolean) = apply { this.showBulkSkipButtons = showBulkSkipButtons }

        @JvmSynthetic
        var bulkSkipNumber: Int = 5
            private set

        fun bulkSkipNumber(bulkSkipNumber: Int) = apply { this.bulkSkipNumber = bulkSkipNumber }

        @JvmSynthetic
        var showSelections: Boolean = false
            private set

        fun showSelections(showSelections: Boolean) = apply { this.showSelections = showSelections }

        @JvmSynthetic
        var showCancel: Boolean = true
            private set

        fun showCancel(showCancel: Boolean) = apply { this.showCancel = showCancel }

        @JvmSynthetic
        var showFooter: Boolean = false
            private set

        fun showFooter(showFooter: Boolean) = apply { this.showFooter = showFooter }

        @JvmSynthetic
        var wrapPageEnds: Boolean = true
            private set

        fun wrapPageEnds(wrapPageEnds: Boolean) = apply { this.wrapPageEnds = wrapPageEnds }


        @JvmSynthetic
        var timeout: Duration? = Duration.ofMinutes(10)
            private set

        fun timeout(timeout: Duration) = apply { this.timeout = timeout }

        @JvmSynthetic
        var messageCompleteAction: Consumer<Message> = Consumer { }
            private set

        fun messageCompleteAction(messageCompleteAction: Consumer<Message>) = apply { this.messageCompleteAction = messageCompleteAction }


        fun build(): AlunaPaginator {
            if (eventWaiter == null) {
                throw IllegalStateException("eventWaiter must be set")
            }
            return AlunaPaginator(
                eventWaiter!!,
                eventWaiterId,
                allowedUsers,
                author,
                authorIcon,
                authorUrl,
                thumbnail,
                image,
                title,
                color,
                titleLink,
                description,
                timestamp,
                footerElement,
                onPageText,
                elements,
                columns,
                elementsPerPage,
                showButtons,
                showBulkSkipButtons,
                bulkSkipNumber,
                showSelections,
                showCancel,
                showFooter,
                wrapPageEnds,
                timeout,
                messageCompleteAction
            )
        }
    }
}
