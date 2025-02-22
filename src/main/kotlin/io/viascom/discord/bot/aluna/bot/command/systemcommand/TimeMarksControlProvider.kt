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

import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.property.AlunaDebugProperties
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import org.springframework.stereotype.Component
import java.awt.Color

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class TimeMarksControlProvider(
    private val alunaProperties: AlunaProperties,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : SystemCommandDataProvider(
    "change_time_marks",
    "Change Time Marks settings",
    true,
    false,
    false,
    true
) {

    private val lastEmbed = EmbedBuilder()
    private lateinit var lastHook: InteractionHook
    private lateinit var command: SystemCommand

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        lastHook = hook!!
        this.command = command
        showEmbed()
    }

    private fun showEmbed() {
        lastEmbed.clearFields()
        lastEmbed.setTitle("Change Time Marks settings")
        lastEmbed.setColor(Color.GREEN)
        lastEmbed.setDescription("Temporarily change the TimeMarks settings for this commands. This will not be persistent and will be reset after a restart!")
        lastEmbed.addField(
            "Show Time Marks",
            if (alunaProperties.debug.useTimeMarks) systemCommandEmojiProvider.tickEmoji().formatted + " Yes" else systemCommandEmojiProvider.crossEmoji().formatted + " No",
            true
        )
        lastEmbed.addField(
            "Show Detailed Time Marks",

            if (alunaProperties.debug.showDetailTimeMarks != AlunaDebugProperties.ShowDetailTimeMarks.NONE && alunaProperties.debug.useTimeMarks) {
                systemCommandEmojiProvider.tickEmoji().formatted + " Yes - ${alunaProperties.debug.showDetailTimeMarks}"
            } else {
                systemCommandEmojiProvider.crossEmoji().formatted + " No"
            },
            true
        )

        lastHook.editOriginalEmbeds(lastEmbed.build()).setComponents(getComponents()).queueAndRegisterInteraction(
            lastHook,
            command,
            arrayListOf(EventRegisterType.BUTTON, EventRegisterType.STRING_SELECT),
            true
        )
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        when (event.componentId) {
            "disable_time_marks" -> {
                lastHook = event.deferEdit().complete()

                alunaProperties.debug.useTimeMarks = false
                showEmbed()
            }

            "enable_time_marks" -> {
                lastHook = event.deferEdit().complete()

                alunaProperties.debug.useTimeMarks = true
                showEmbed()
            }

        }
        return true
    }

    override fun onButtonInteractionTimeout() {
        lastHook.editOriginalEmbeds(lastEmbed.build()).removeComponents().queue()
    }

    override fun onStringSelectMenuInteraction(event: StringSelectInteractionEvent): Boolean {
        when (event.componentId) {
            "show_detailed_time_marks" -> {
                lastHook = event.deferEdit().complete()
                alunaProperties.debug.showDetailTimeMarks = AlunaDebugProperties.ShowDetailTimeMarks.valueOf(event.getSelection())
                showEmbed()
            }
        }

        return true
    }

    override fun onStringSelectInteractionTimeout() {
        lastHook.editOriginalEmbeds(lastEmbed.build()).removeComponents().queue()
    }

    private fun getComponents(): ArrayList<ActionRow> {
        val rows = arrayListOf<ActionRow>()
        val row = arrayListOf<ActionComponent>()
        if (alunaProperties.debug.useTimeMarks) {
            row.add(dangerButton("disable_time_marks", "Disable Time Marks"))
        } else {
            row.add(successButton("enable_time_marks", "Enable Time Marks"))
        }

        val selectRow = arrayListOf<ActionComponent>()
        val select = StringSelectMenu.create("show_detailed_time_marks")
            .addOption(
                "Disable",
                "NONE",
                "No detailed time marks will be processed",
                systemCommandEmojiProvider.crossEmoji(),
                alunaProperties.debug.showDetailTimeMarks == AlunaDebugProperties.ShowDetailTimeMarks.NONE
            )
            .addOption(
                "Always",
                "ALWAYS",
                "Detailed time marks will be processed and logged",
                systemCommandEmojiProvider.tickEmoji(),
                alunaProperties.debug.showDetailTimeMarks == AlunaDebugProperties.ShowDetailTimeMarks.ALWAYS
            )
            .addOption(
                "On Exception only",
                "ON_EXCEPTION",
                "Detailed time marks will only be processed if an exception is thrown",
                systemCommandEmojiProvider.tickEmoji(),
                alunaProperties.debug.showDetailTimeMarks == AlunaDebugProperties.ShowDetailTimeMarks.ON_EXCEPTION
            )
            .addOption(
                "Add to MDC only",
                "MDC_ONLY",
                "Detailed time marks will be processed and added to MDC",
                systemCommandEmojiProvider.tickEmoji(),
                alunaProperties.debug.showDetailTimeMarks == AlunaDebugProperties.ShowDetailTimeMarks.MDC_ONLY
            )
            .build()
        selectRow.add(select)

        rows.add(ActionRow.of(row))
        rows.add(ActionRow.of(selectRow))
        return rows
    }

}
