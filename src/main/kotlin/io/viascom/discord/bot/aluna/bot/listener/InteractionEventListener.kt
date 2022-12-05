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

package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.DiscordMessageContextMenu
import io.viascom.discord.bot.aluna.bot.DiscordUserContextMenu
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.scope.DiscordContext
import io.viascom.discord.bot.aluna.util.NanoId
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
open class InteractionEventListener(
    private val discordBot: DiscordBot,
    private val context: ConfigurableApplicationContext
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            NanoId.generate()
        )
        discordBot.commands[event.commandId]?.let { interaction ->
            context.getBean(interaction).run(event)
        }
    }

    override fun onUserContextInteraction(event: UserContextInteractionEvent) {
        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            NanoId.generate()
        )
        discordBot.contextMenus[event.commandId]?.let { interaction ->
            (context.getBean(interaction) as DiscordUserContextMenu).run(event)
        }
    }

    override fun onMessageContextInteraction(event: MessageContextInteractionEvent) {
        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            NanoId.generate()
        )
        discordBot.contextMenus[event.commandId]?.let { interaction ->
            (context.getBean(interaction) as DiscordMessageContextMenu).run(event)
        }
    }
}