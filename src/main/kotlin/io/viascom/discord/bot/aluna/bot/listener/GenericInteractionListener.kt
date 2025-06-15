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

package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.event.CoroutineEventListener
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.scope.DiscordContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
public open class GenericInteractionListener(
    private val discordBot: DiscordBot,
    private val context: ConfigurableApplicationContext,
) : CoroutineEventListener {

    override suspend fun onEvent(event: GenericEvent) {
        when (event) {
            is CommandAutoCompleteInteractionEvent -> onCommandAutoCompleteInteraction(event)
            is ButtonInteractionEvent -> onButtonInteraction(event)
            is StringSelectInteractionEvent -> onStringSelectInteraction(event)
            is EntitySelectInteractionEvent -> onEntitySelectInteraction(event)
            is ModalInteractionEvent -> onModalInteraction(event)
        }
    }

    private suspend fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        launch {
            val commandId = discordBot.commandsWithAutocomplete.firstOrNull { it == event.commandId }
            if (commandId != null) {
                discordBot.commands[commandId]?.let { command ->
                    DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.AUTO_COMPLETE, DiscordContext.newUniqueId())
                    context.getBean(command).handleAutoCompleteEventCall(event.focusedOption.name, event)
                }
            }
        }

        launch {
            val handler = discordBot.autoCompleteHandlers.entries.firstOrNull { entry ->
                entry.key.first == event.commandId && (entry.key.second == null || entry.key.second == event.focusedOption.name)
            }
            if (handler != null) {
                DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.AUTO_COMPLETE, DiscordContext.newUniqueId())
                context.getBean(handler.value).onRequestCall(event)
            }
        }
    }

    private suspend fun onButtonInteraction(event: ButtonInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        if (discordBot.messagesToObserveButton.containsKey(event.message.id)) {
            val entry = discordBot.messagesToObserveButton[event.message.id]!!
            DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.OTHER, entry.uniqueId)
            if (entry.interactionUserOnly && event.user.id !in (entry.authorIds ?: arrayListOf())) {
                return@withContext
            }

            val result = context.getBean(entry.interaction.java).handleOnButtonInteraction(event)
            if (!entry.stayActive && result) {
                discordBot.removeMessageForButtonEvents(event.message.id)
            }
        }
    }


    private suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        if (discordBot.messagesToObserveStringSelect.containsKey(event.message.id)) {
            val entry = discordBot.messagesToObserveStringSelect[event.message.id]!!
            DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.OTHER, entry.uniqueId)
            if (entry.interactionUserOnly && event.user.id !in (entry.authorIds ?: arrayListOf())) {
                return@withContext
            }

            val result = context.getBean(entry.interaction.java).handleOnStringSelectInteraction(event)
            if (!entry.stayActive && result) {
                discordBot.removeMessageForStringSelectEvents(event.message.id)
            }
        }
    }

    private suspend fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        if (discordBot.messagesToObserveEntitySelect.containsKey(event.message.id)) {
            val entry = discordBot.messagesToObserveEntitySelect[event.message.id]!!
            DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.OTHER, entry.uniqueId)
            if (entry.interactionUserOnly && event.user.id !in (entry.authorIds ?: arrayListOf())) {
                return@withContext
            }

            val result = context.getBean(entry.interaction.java).handleOnEntitySelectInteraction(event)
            if (!entry.stayActive && result) {
                discordBot.removeMessageForEntitySelectEvents(event.message.id)
            }
        }
    }

    private suspend fun onModalInteraction(event: ModalInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        if (discordBot.messagesToObserveModal.containsKey(event.user.id)) {
            val entry = discordBot.messagesToObserveModal[event.user.id]!!
            DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.OTHER, entry.uniqueId)
            if (entry.interactionUserOnly && event.user.id !in (entry.authorIds ?: arrayListOf())) {
                return@withContext
            }

            val result = context.getBean(entry.interaction.java).handleOnModalInteraction(event)
            if (!entry.stayActive && result) {
                discordBot.removeMessageForModalEvents(event.user.id)
            }
        }
    }
}
