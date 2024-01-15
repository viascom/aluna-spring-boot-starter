/*
 * Copyright 2024 Viascom Ltd liab. Co
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
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.dangerButton
import io.viascom.discord.bot.aluna.util.removeComponents
import io.viascom.discord.bot.aluna.util.successButton
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.ActionRow
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
            if (alunaProperties.debug.showDetailTimeMarks) systemCommandEmojiProvider.tickEmoji().formatted + " Yes" else systemCommandEmojiProvider.crossEmoji().formatted + " No",
            true
        )

        lastHook.editOriginalEmbeds(lastEmbed.build()).setComponents(getComponents()).queueAndRegisterInteraction(
            lastHook,
            command,
            arrayListOf(EventRegisterType.BUTTON),
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

            "disable_detailed_time_marks" -> {
                lastHook = event.deferEdit().complete()

                alunaProperties.debug.showDetailTimeMarks = false
                showEmbed()
            }

            "enable_detailed_time_marks" -> {
                lastHook = event.deferEdit().complete()

                alunaProperties.debug.showDetailTimeMarks = true
                showEmbed()
            }

        }
        return true
    }

    override fun onButtonInteractionTimeout() {
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

        if (alunaProperties.debug.showDetailTimeMarks) {
            row.add(dangerButton("disable_detailed_time_marks", "Disable Detailed Time Marks"))
        } else {
            row.add(successButton("enable_detailed_time_marks", "Enable Detailed Time Marks"))
        }

        rows.add(ActionRow.of(row))
        return rows
    }

}
