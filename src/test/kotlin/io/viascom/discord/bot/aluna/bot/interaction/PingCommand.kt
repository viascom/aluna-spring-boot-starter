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

package io.viascom.discord.bot.aluna.bot.interaction

import io.viascom.discord.bot.aluna.bot.CoroutineDiscordCommand
import io.viascom.discord.bot.aluna.bot.Interaction
import io.viascom.discord.bot.aluna.bot.coQueue
import io.viascom.discord.bot.aluna.util.toDiscordComponent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import java.awt.Color

@Interaction
class PingCommand : CoroutineDiscordCommand("ping", "Send a ping") {

    override fun initCommandOptions() {
        this.setContexts(InteractionContextType.ALL)
        this.setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val startTime = System.currentTimeMillis()

        event.replyComponents("↔\uFE0F Ping ...".toDiscordComponent())
            .useComponentsV2()
            .setEphemeral(true)
            .coQueue {
                //As soon as it got successfully sent, we edit the message with the duration between now and the first message.
                it.editOriginalComponents("↔\uFE0F Pong: ${System.currentTimeMillis() - startTime}ms".toDiscordComponent().withAccentColor(Color.GREEN))
                    .useComponentsV2()
                    .queue()
            }
    }
}
