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

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.CommandUsage
import io.viascom.discord.bot.aluna.util.TimestampFormat
import io.viascom.discord.bot.aluna.util.addFields
import io.viascom.discord.bot.aluna.util.toDiscordTimestamp
import kotlinx.coroutines.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed.Field
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import org.springframework.stereotype.Component
import java.awt.Color
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
public class LastUsedCommands(
    private val discordBot: DiscordBot
) : SystemCommandDataProvider(
    "last_used_commands",
    "Show last used commands",
    true,
    false,
    false,
    true
) {
    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        AlunaDispatchers.InternalScope.launch {

            val embed = EmbedBuilder()
                .setTitle("\uD83D\uDD04 Last used commands")
                .setColor(Color.MAGENTA)
                .setDescription("Loading...")


            hook!!.editOriginalEmbeds(embed.build()).queue()

            try {
                withTimeout(1.toDuration(DurationUnit.MINUTES)) {
                    val list = ArrayDeque<CommandUsage>()
                    val new = AtomicBoolean(false)

                    launch {
                        while (isActive) {
                            discordBot.commandHistory.collect {
                                if (list.size > 15) {
                                    list.removeFirst()
                                }

                                list.addLast(it)
                                new.set(true)

                                if (list.size == 1) {
                                    sendUpdate(embed, list, hook)
                                }
                            }
                        }
                    }

                    while (isActive) {
                        if (new.get()) {
                            sendUpdate(embed, list, hook)
                        }
                        delay(10.toDuration(DurationUnit.SECONDS))
                    }
                }
            } catch (ex: TimeoutCancellationException) {
                embed.setTitle("Last used commands")
                    .setColor(Color.DARK_GRAY)
                    .setFooter("Live update stopped after 1 minute")
                    .setTimestamp(null)
                hook.editOriginalEmbeds(embed.build()).queue()
            }
        }
    }

    private fun sendUpdate(
        embed: EmbedBuilder,
        list: ArrayDeque<CommandUsage>,
        hook: InteractionHook
    ) {
        embed.setDescription("")
        embed.clearFields()

        embed.addFields(list.sortedByDescending { it.timestamp }.map {
            Field(
                "${it.timestamp.toDiscordTimestamp(TimestampFormat.SHORT_TIME)} **\\${it.command}**",
                "└ ID: `${it.instance}`\n" +
                        "└ User: `${it.userId}`" +
                        if (it.serverId != null) "\n└ Server: `${it.serverId}`" else "",
                false
            )
        })
        embed.setFooter("Automatically updates every 10 sec")
        embed.setTimestamp(list.last().timestamp)

        hook.editOriginalEmbeds(embed.build()).queue()
    }

}
