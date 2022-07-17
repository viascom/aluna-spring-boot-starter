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
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.scope.DiscordContext
import io.viascom.discord.bot.aluna.util.NanoId
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
open class GenericInteractionListener(
    private val discordBot: DiscordBot,
    private val context: ConfigurableApplicationContext,
) : ListenerAdapter() {

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        runBlocking {
            launch {
                val commandId = discordBot.commandsWithAutocomplete.firstOrNull { it == event.commandId }
                if (commandId != null) {
                    discordBot.commands[commandId]?.let { command ->
                        DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.AUTO_COMPLETE, NanoId.generate())
                        context.getBean(command).onAutoCompleteEventCall(event.focusedOption.name, event)
                    }
                }
            }

            launch {
                val handler = discordBot.autoCompleteHandlers.entries.firstOrNull { entry ->
                    entry.key.first == event.commandId && (entry.key.second == null || entry.key.second == event.focusedOption.name)
                }
                if (handler != null) {
                    DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.AUTO_COMPLETE, NanoId.generate())
                    context.getBean(handler.value).onRequestCall(event)
                }
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (discordBot.messagesToObserveButton.containsKey(event.message.id)) {
            val entry = discordBot.messagesToObserveButton[event.message.id]!!
            DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.OTHER, entry.uniqueId)
            if (entry.interactionUserOnly && event.user.id !in (entry.authorIds ?: arrayListOf())) {
                return
            }

            val result = context.getBean(entry.interaction.java).onButtonInteraction(event, entry.additionalData)
            if (!entry.stayActive && result) {
                discordBot.removeMessageForButtonEvents(event.message.id)
            }
        }
    }


    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent) {
        if (discordBot.messagesToObserveSelect.containsKey(event.message.id)) {
            val entry = discordBot.messagesToObserveSelect[event.message.id]!!
            DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.OTHER, entry.uniqueId)
            if (entry.interactionUserOnly && event.user.id !in (entry.authorIds ?: arrayListOf())) {
                return
            }

            val result = context.getBean(entry.interaction.java).onSelectMenuInteraction(event, entry.additionalData)
            if (!entry.stayActive && result) {
                discordBot.removeMessageForSelectEvents(event.message.id)
            }
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (discordBot.messagesToObserveModal.containsKey(event.user.id)) {
            val entry = discordBot.messagesToObserveModal[event.user.id]!!
            DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.OTHER, entry.uniqueId)
            if (entry.interactionUserOnly && event.user.id !in (entry.authorIds ?: arrayListOf())) {
                return
            }

            val result = context.getBean(entry.interaction.java).onModalInteraction(event, entry.additionalData)
            if (!entry.stayActive && result) {
                discordBot.removeMessageForModalEvents(event.user.id)
            }
        }
    }
}
