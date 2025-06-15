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
import io.viascom.discord.bot.aluna.util.dangerButton
import io.viascom.discord.bot.aluna.util.removeComponents
import io.viascom.discord.bot.aluna.util.successButton
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Duration
import kotlin.system.exitProcess

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
public class ShutdownProvider(
    private val shardManager: ShardManager,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : SystemCommandDataProvider(
    "shutdown",
    "Shutdown Bot",
    true,
    false,
    false,
    true
) {

    private lateinit var lastHook: InteractionHook
    private lateinit var lastEmbed: EmbedBuilder

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        lastHook = hook!!

        lastEmbed = EmbedBuilder()
            .setColor(Color.RED)
            .setDescription("❓ Do you really want to shutdown the bot?")

        lastHook.editOriginalEmbeds(lastEmbed.build()).setComponents(
            ActionRow.of(
                dangerButton("yes", "Yes"),
                successButton("no", "No")
            )
        ).queueAndRegisterInteraction(lastHook, command, duration = Duration.ofMinutes(2))
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        event.deferEdit().queue { hook ->
            lastHook = hook

            if (event.componentId == "yes") {
                lastEmbed.setDescription("${systemCommandEmojiProvider.tickEmoji().formatted} Bot shutting down now!")
                lastHook.editOriginalEmbeds(lastEmbed.build()).removeComponents().queue()
                logger.warn("Shutdown initiated by /system-command. User: ${event.user.name} (${event.user.id})")
                exitProcess(0)
            } else {
                lastEmbed.setDescription("${systemCommandEmojiProvider.crossEmoji().formatted} Canceled")
                lastHook.editOriginalEmbeds(lastEmbed.build()).removeComponents().queue()
            }

        }
        return true
    }

    override fun onButtonInteractionTimeout() {
        lastHook.editOriginalEmbeds(lastEmbed.build()).removeComponents().queue()
    }
}
