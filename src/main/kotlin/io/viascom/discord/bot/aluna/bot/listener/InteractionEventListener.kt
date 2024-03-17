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

package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.event.CoroutineEventListener
import io.viascom.discord.bot.aluna.bot.handler.DiscordMessageContextMenuHandler
import io.viascom.discord.bot.aluna.bot.handler.DiscordUserContextMenuHandler
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.scope.DiscordContext
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
open class InteractionEventListener(
    private val discordBot: DiscordBot,
    private val context: ConfigurableApplicationContext
) : CoroutineEventListener {

    override suspend fun onEvent(event: GenericEvent) {
        when (event) {
            is SlashCommandInteractionEvent -> onSlashCommandInteraction(event)
            is UserContextInteractionEvent -> onUserContextInteraction(event)
            is MessageContextInteractionEvent -> onMessageContextInteraction(event)
        }
    }

    private suspend fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            DiscordContext.newUniqueId()
        )
        discordBot.commands[event.commandId]?.let { interaction ->
            context.getBean(interaction).run(event)
        }
    }

    private suspend fun onUserContextInteraction(event: UserContextInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            DiscordContext.newUniqueId()
        )
        discordBot.contextMenus[event.commandId]?.let { interaction ->
            (context.getBean(interaction) as DiscordUserContextMenuHandler).run(event)
        }
    }

    private suspend fun onMessageContextInteraction(event: MessageContextInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            DiscordContext.newUniqueId()
        )
        discordBot.contextMenus[event.commandId]?.let { interaction ->
            (context.getBean(interaction) as DiscordMessageContextMenuHandler).run(event)
        }
    }
}
