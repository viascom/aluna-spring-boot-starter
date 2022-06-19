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
import io.viascom.discord.bot.aluna.bot.emotes.AlunaEmote
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.util.createDangerButton
import io.viascom.discord.bot.aluna.util.createSuccessButton
import io.viascom.discord.bot.aluna.util.getOptionAsString
import io.viascom.discord.bot.aluna.util.removeActionRows
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.sharding.ShardManager
import java.awt.Color
import java.time.Duration

@Command
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class LeaveServerProvider(
    private val shardManager: ShardManager
) : SystemCommandDataProvider(
    "leave_server",
    "Leave Server",
    true,
    false,
    true
) {

    lateinit var lastHook: InteractionHook
    lateinit var lastEmbed: EmbedBuilder
    lateinit var selectedServer: Guild

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        lastHook = hook!!

        val id = event.getOptionAsString("args", "")!!
        if (id.isEmpty()) {
            lastHook.editOriginal("${AlunaEmote.BOT_CROSS.asMention()} Please specify an ID as argument for this command").queue()
            return
        }

        val server = shardManager.getGuildById(id)
        if (server == null) {
            lastHook.editOriginal("${AlunaEmote.BOT_CROSS.asMention()} Please specify a valid server ID as argument for this command").queue()
            return
        }

        selectedServer = server

        lastEmbed = EmbedBuilder()
            .setColor(Color.RED)
            .setDescription("${AlunaEmote.DOT_RED.asMention()} Do you really want that this Bot leaves the server **${server.name}**?")
        lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(
            ActionRow.of(
                createDangerButton("yes", "Yes", AlunaEmote.SMALL_TICK_WHITE.toEmoji()),
                createSuccessButton("no", "No", AlunaEmote.SMALL_CROSS_WHITE.toEmoji())
            )
        ).queueAndRegisterInteraction(lastHook, command, duration = Duration.ofMinutes(2))
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        event.deferEdit().queue { hook ->
            lastHook = hook

            if (event.componentId == "yes") {
                lastEmbed.setDescription("${AlunaEmote.DOT_GREEN.asMention()} Bot left **${selectedServer.name}**")
                selectedServer.leave().queue()
            } else {
                lastEmbed.setDescription("${AlunaEmote.DOT_YELLOW.asMention()} Canceled")
            }
            lastHook.editOriginalEmbeds(lastEmbed.build()).removeActionRows().queue()
        }
        return true
    }

    override fun onButtonInteractionTimeout() {
        lastHook.editOriginalEmbeds(lastEmbed.build()).removeActionRows().queue()
    }

    override fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        val input = event.getOptionAsString("args", "")!!
        val options = shardManager.guilds.filter { it.name.lowercase().contains(input.lowercase()) }.take(25).map {
            net.dv8tion.jda.api.interactions.commands.Command.Choice(it.name, it.id)
        }

        event.replyChoices(options).queue()
    }
}
