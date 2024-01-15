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
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class DiscordPingProvider : SystemCommandDataProvider(
    "discord_ping",
    "Get Discord Ping",
    true,
    true,
    false,
    false
) {
    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        val builder = EmbedBuilder()

        builder.setTitle("Discord Connection")
            .setColor(Color.ORANGE)
            .setDescription("\uD83D\uDCE1 Pinging...")

        val replyHook = event.deferReply().complete()

        val gatewayPing: Long = event.jda.gatewayPing
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val apiPing = event.jda.restPing.complete()

        replyHook.editOriginalEmbeds(builder.build()).queue {
            builder.setTitle("Discord Connection")
                .setDescription(
                    "❯ **Gateway Ping:** `${gatewayPing}ms`\n" +
                            "❯ **Api Ping:** `${apiPing}ms`\n" +
                            "❯ **Roundtrip Latency:** `${ChronoUnit.MILLIS.between(now, LocalDateTime.now(ZoneOffset.UTC))}ms`\n"
                )
                .setColor(Color.GREEN)
                .build()
            replyHook.editOriginalEmbeds(builder.build()).queue()
        }
    }
}
