/*
 * Copyright 2022 Viascom Ltd liab. Co
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

import io.viascom.discord.bot.aluna.bot.Command
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.util.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.sharding.ShardManager
import java.awt.Color

@Command
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class StatusChangerProvider(
    private val shardManager: ShardManager,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : SystemCommandDataProvider(
    "change_status",
    "Change Bot Status & Activity",
    true,
    false,
    false,
    true
) {

    private val lastEmbed = EmbedBuilder()
    private lateinit var lastHook: InteractionHook
    private lateinit var command: SystemCommand

    private var status: OnlineStatus = OnlineStatus.ONLINE
    private var activityId: String = "null"
    private var activityText: String = ""
    private var activityUrl: String = ""

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        lastHook = hook!!
        this.command = command
        showEmbed()
    }

    private fun showEmbed() {
        lastEmbed.clearFields()
        lastEmbed.setTitle("Change Bot Status & Activity")
        lastEmbed.setColor(Color.GREEN)
        lastEmbed.setDescription("Select a status and activity to which the bot should be changed.")
        lastEmbed.setThumbnail(shardManager.shards.first().selfUser.avatarUrl)

        lastEmbed.addField("New Status", status.name, true)
        lastEmbed.addField("New Activity", if (activityId == "null") "*no activity*" else activityId, true)
        if (activityId != "null") {
            lastEmbed.addField("New Activity Text", activityText, false)
        }
        if ("streaming" == activityId) {
            lastEmbed.addField(
                "New Activity Url",
                activityUrl + "\n" +
                        if (Activity.isValidStreamingUrl(activityUrl)) {
                            "${systemCommandEmojiProvider.tickEmoji().asMention} Valid"
                        } else {
                            "${systemCommandEmojiProvider.crossEmoji().asMention} Invalid"
                        },
                false
            )
        }

        lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(getActionRow()).queueAndRegisterInteraction(
            lastHook,
            command,
            arrayListOf(
                EventRegisterType.BUTTON,
                EventRegisterType.SELECT,
                EventRegisterType.MODAL
            ),
            true
        )
    }

    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent): Boolean {
        lastHook = event.deferEdit().complete()

        when (event.componentId) {
            "status" -> {
                status = OnlineStatus.fromKey(event.getSelection())
                showEmbed()
            }
            "activity" -> {
                activityId = event.getSelection()
                showEmbed()
            }
        }

        return true
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        when (event.componentId) {
            "set_text" -> {
                val modal = Modal.create("set_text", "Set activity text")
                    .addTextField("text", "Text", TextInputStyle.SHORT, min = 0, max = 128, value = activityText)
                if ("streaming" == activityId) {
                    modal.addTextField("url", "Url", TextInputStyle.SHORT, value = activityUrl)
                }
                event.replyModal(modal.build()).queue()
            }
            "save" -> {
                lastHook = event.deferEdit().complete()
                shardManager.setStatus(status)
                if (activityText.isNotEmpty() || activityId == "null") {

                    val activity = when (activityId) {
                        "playing" -> Activity.playing(activityText)
                        "competing" -> Activity.competing(activityText)
                        "listening" -> Activity.listening(activityText)
                        "watching" -> Activity.watching(activityText)
                        "streaming" -> Activity.streaming(activityText, activityUrl)
                        else -> null
                    }

                    shardManager.setActivity(activity)
                }

                lastEmbed.clearFields()
                lastEmbed.setThumbnail(null)
                lastEmbed.setDescription("${systemCommandEmojiProvider.tickEmoji().asMention} Changed bot status")

                lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(arrayListOf()).queue()
            }
            "cancel" -> {
                lastHook = event.deferEdit().complete()

                lastEmbed.clearFields()
                lastEmbed.setThumbnail(null)
                lastEmbed.setColor(Color.RED)
                lastEmbed.setDescription("${systemCommandEmojiProvider.crossEmoji().asMention} Canceled")

                lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(arrayListOf()).queue()
            }
        }
        return true
    }

    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        lastHook = event.deferEdit().complete()
        activityText = event.getValueAsString("text", "")!!
        activityUrl = event.getValueAsString("url", "")!!
        showEmbed()
        return true
    }

    private fun getActionRow(): ArrayList<ActionRow> {
        val rows = arrayListOf<ActionRow>()

        val statusSelect = SelectMenu.create("status")
        OnlineStatus.values().filter { it != OnlineStatus.UNKNOWN }.forEach {
            statusSelect.addOption(it.name, it.key, isDefault = (it == status))
        }
        rows.add(ActionRow.of(statusSelect.build()))

        val activitySelect = SelectMenu.create("activity")
        activitySelect.addOption("Nothing", "null", isDefault = ("null" == activityId))
        activitySelect.addOption("Playing", "playing", isDefault = ("playing" == activityId))
        activitySelect.addOption("Competing", "competing", isDefault = ("competing" == activityId))
        activitySelect.addOption("Listening", "listening", isDefault = ("listening" == activityId))
        activitySelect.addOption("Watching", "watching", isDefault = ("watching" == activityId))
        activitySelect.addOption("Streaming", "streaming", isDefault = ("streaming" == activityId))
        rows.add(ActionRow.of(activitySelect.build()))

        val setTextButton = createPrimaryButton("set_text", "Set Text")
        val save =
            createSuccessButton(
                "save",
                "Save",
                disabled = ((activityId != "null" && activityText.isEmpty()) || (activityId == "streaming" && !Activity.isValidStreamingUrl(activityUrl)))
            )
        val cancel = createDangerButton("cancel", "Cancel")
        rows.add(ActionRow.of(setTextButton, save, cancel))

        return rows
    }
}
