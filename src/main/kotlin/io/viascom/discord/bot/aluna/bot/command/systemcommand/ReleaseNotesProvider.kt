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

package io.viascom.discord.bot.aluna.bot.command.systemcommand

import com.fasterxml.jackson.databind.ObjectMapper
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.model.ReleaseNotes
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class ReleaseNotesProvider(
    private val alunaProperties: AlunaProperties,
    private val objectMapper: ObjectMapper,
    private val discordBot: DiscordBot
) : SystemCommandDataProvider(
    "send_release_notes",
    "Send Release Notes",
    true,
    false,
    false,
    false
) {

    private lateinit var latestHook: InteractionHook
    private var latestEditMessage: MessageEditBuilder = MessageEditBuilder()
    private var latestReleaseEmbed: EmbedBuilder = EmbedBuilder()
    private var releaseNotes: ReleaseNotes = ReleaseNotes()
    private var channelId: String? = alunaProperties.command.systemCommand.releaseNotes.channel
    private var tempChannelId: String? = null
    private var wrongJson: Boolean = false

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        if (!event.isFromGuild) {
            event.reply("⛔ This command can only be used on a server directly.").queue()
            return
        }

        createOverviewMessage()
        event.reply(latestEditMessage.build().toCreateData()).setEphemeral(true).setComponents(getActionRow()).queueAndRegisterInteraction(
            command,
            arrayListOf(EventRegisterType.BUTTON, EventRegisterType.MODAL, EventRegisterType.ENTITY_SELECT),
            true
        ) {
            latestHook = it
        }
    }

    private fun createOverviewMessage() {
        val channelMention = channelId?.let { "<#$it>" } ?: "*not set*"
        latestEditMessage = MessageEditBuilder()
        val error = if (wrongJson) {
            "\n⛔ Release Notes json could not be parsed! Please try again."
        } else {
            ""
        }
        latestEditMessage.setContent("**Send Release Notes**\nMessage will be sent to: $channelMention\n\nPreview:$error")

        latestReleaseEmbed.clearFields()
        latestReleaseEmbed = EmbedBuilder()
            .setColor(Color.decode(releaseNotes.color))
            .setTitle(replaceText(releaseNotes.title))
            .setDescription(replaceText(releaseNotes.description))
            .setImage(releaseNotes.image)
            .setThumbnail(releaseNotes.thumbnail)

        if (releaseNotes.creator.isNotEmpty()) {
            latestReleaseEmbed.setFooter("Created by ${releaseNotes.creator}")
        }

        if (releaseNotes.newCommands.isNotEmpty()) {
            latestReleaseEmbed.addFields(
                splitListInFields(
                    releaseNotes.newCommands.map { "${alunaProperties.command.systemCommand.releaseNotes.newCommandEmote} ${replaceText(it)}" },
                    "New Commands",
                    false
                )
            )
        }
        if (releaseNotes.newFeatures.isNotEmpty()) {
            latestReleaseEmbed.addFields(
                splitListInFields(
                    releaseNotes.newFeatures.map { "${alunaProperties.command.systemCommand.releaseNotes.newFeatureEmote} ${replaceText(it)}" },
                    "New Features",
                    false
                )
            )
        }
        if (releaseNotes.bugFixes.isNotEmpty()) {
            latestReleaseEmbed.addFields(
                splitListInFields(
                    releaseNotes.bugFixes.map { "${alunaProperties.command.systemCommand.releaseNotes.bugFixEmote} ${replaceText(it)}" },
                    "Bug Fixes",
                    false
                )
            )
        }
        if (releaseNotes.internalChanges.isNotEmpty()) {
            latestReleaseEmbed.addFields(
                splitListInFields(
                    releaseNotes.internalChanges.map { "${alunaProperties.command.systemCommand.releaseNotes.internalChangeEmote} ${replaceText(it)}" },
                    "Internal Changes",
                    false
                )
            )
        }

        latestEditMessage.setEmbeds(latestReleaseEmbed.build())
    }

    private fun replaceText(input: String): String {
        return input.replace("\\{now:(SHORT_TIME|LONG_TIME|SHORT_DATE|LONG_DATE|SHORT_DATE_TIME|LONG_DATE_TIME|RELATIVE_TIME)}".toRegex()) { match ->
            TimestampFormat.entries.firstOrNull { it.name == match.groupValues[1] }?.let { LocalDateTime.now(ZoneOffset.UTC).toDiscordTimestamp(it) } ?: match.groupValues[0]
        }
    }

    private fun createSelectChannelMessage() {
        if (tempChannelId == null) {
            tempChannelId = channelId
        }
        val channelMention = tempChannelId?.let { "<#$it>" } ?: "*not set*"
        latestEditMessage = MessageEditBuilder()
        latestEditMessage.setContent("**Send Release Notes**\nMessage will be sent to: $channelMention\n\nPlease select a channel below if you want to change this.")
    }

    private fun getActionRow(): ArrayList<ActionRow> {
        val rows = arrayListOf<ActionRow>()

        val row1 = arrayListOf<ItemComponent>()
        row1.add(primaryButton("set-json", "Set JSON"))
        row1.add(primaryButton("set-channel", "Set Channel"))
        rows.add(ActionRow.of(row1))

        val row2 = arrayListOf<ItemComponent>()
        row2.add(dangerButton("cancel", "Cancel"))
        row2.add(successButton("send", "Send Release Notes", disabled = channelId == null))
        rows.add(ActionRow.of(row2))

        return rows
    }

    private fun getSetChannelActionRow(): ArrayList<ActionRow> {
        val rows = arrayListOf<ActionRow>()

        val row1 = arrayListOf<ItemComponent>()
        val channelSelect = EntitySelectMenu.create("channel-select", EntitySelectMenu.SelectTarget.CHANNEL)
        channelSelect.setChannelTypes(ChannelType.TEXT, ChannelType.NEWS)
        channelSelect.maxValues = 1
        channelSelect.minValues = 1
        row1.add(channelSelect.build())
        rows.add(ActionRow.of(row1))

        val row2 = arrayListOf<ItemComponent>()
        row2.add(dangerButton("back", "Back"))
        row2.add(successButton("select", "Set Channel"))
        rows.add(ActionRow.of(row2))

        return rows
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        when (event.componentId) {
            "set-json" -> {
                wrongJson = false
                event.replyModal(
                    Modal.create("set-json", "Set Release Notes JSON")
                        .addTextField("json", "JSON", TextInputStyle.PARAGRAPH, value = objectMapper.writeValueAsString(releaseNotes))
                        .build()
                ).queue()
            }

            "set-channel" -> {
                event.deferEdit().queue()
                createSelectChannelMessage()
                latestHook.editOriginal(latestEditMessage.build()).setComponents(getSetChannelActionRow()).queue()
            }

            "back" -> {
                event.deferEdit().queue()
                createOverviewMessage()
                tempChannelId = null
                latestHook.editOriginal(latestEditMessage.build()).setComponents(getActionRow()).queue()
            }

            "select" -> {
                event.deferEdit().queue()
                channelId = tempChannelId
                createOverviewMessage()
                latestHook.editOriginal(latestEditMessage.build()).setComponents(getActionRow()).queue()
            }

            "cancel" -> {
                event.deferEdit().queue {
                    it.deleteOriginal().queue()
                }
            }

            "send" -> {
                event.deferEdit().queue {
                    it.deleteOriginal().queue()
                }
                event.guild!!.channels.first { it.id == channelId }
                    .let { channel -> (channel as StandardGuildMessageChannel).sendMessageEmbeds(latestReleaseEmbed.build()).queue() }
            }
        }

        return true
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent): Boolean {
        when (event.componentId) {
            "channel-select" -> {
                event.deferEdit().queue()
                tempChannelId = event.getChannelSelection<StandardGuildMessageChannel>()?.id

                createSelectChannelMessage()
                latestHook.editOriginal(latestEditMessage.build()).setComponents(getSetChannelActionRow()).queue()
            }
        }
        return true
    }

    override fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        when (event.modalId) {
            "set-json" -> {
                event.deferEdit().queue()

                val json = event.getValueAsString("json", "")

                try {
                    releaseNotes = objectMapper.readValue(json, ReleaseNotes::class.java)
                } catch (e: Exception) {
                    wrongJson = true
                }

                createOverviewMessage()
                latestHook.editOriginal(latestEditMessage.build()).setComponents(getActionRow()).queue()
            }
        }
        return true
    }
}
