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

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.scope.DiscordContext
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
        discordBot.asyncExecutor.execute {
            val commandId = discordBot.commandsWithAutocomplete.firstOrNull { it == event.commandId }
            if (commandId != null) {
                discordBot.commands[commandId]?.let { command ->
                    discordBot.commandExecutor.execute {
                        DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.AUTO_COMPLETE, NanoIdUtils.randomNanoId())
                        context.getBean(command).onAutoCompleteEventCall(event.focusedOption.name, event)
                    }
                }
            }
        }

        discordBot.asyncExecutor.execute {
            val handler = discordBot.autoCompleteHandlers.entries.firstOrNull { entry ->
                entry.key.first == event.commandId && (entry.key.second == null || entry.key.second == event.focusedOption.name)
            }
            if (handler != null) {
                DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.AUTO_COMPLETE, NanoIdUtils.randomNanoId())
                context.getBean(handler.value).onRequestCall(event)
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        discordBot.asyncExecutor.execute {
            if (discordBot.messagesToObserveButton.containsKey(event.message.id)) {
                discordBot.commandExecutor.execute commandExecutor@{
                    val entry = discordBot.messagesToObserveButton[event.message.id]!!
                    DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.OTHER, entry.uniqueId)
                    if (entry.commandUserOnly && event.user.id !in (entry.authorIds ?: arrayListOf())) {
                        return@commandExecutor
                    }

                    val result = context.getBean(entry.command.java).onButtonInteraction(event, entry.additionalData)
                    if (!entry.stayActive && result) {
                        discordBot.removeMessageForButtonEvents(event.message.id)
                    }
                }
            }
        }
    }


    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent) {
        discordBot.asyncExecutor.execute {
            if (discordBot.messagesToObserveSelect.containsKey(event.message.id)) {
                discordBot.commandExecutor.execute commandExecutor@{
                    val entry = discordBot.messagesToObserveSelect[event.message.id]!!
                    DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.OTHER, entry.uniqueId)
                    if (entry.commandUserOnly && event.user.id !in (entry.authorIds ?: arrayListOf())) {
                        return@commandExecutor
                    }

                    val result = context.getBean(entry.command.java).onSelectMenuInteraction(event, entry.additionalData)
                    if (!entry.stayActive && result) {
                        discordBot.removeMessageForSelectEvents(event.message.id)
                    }
                }
            }
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        discordBot.asyncExecutor.execute {
            if (discordBot.messagesToObserveModal.containsKey(event.user.id)) {
                discordBot.commandExecutor.execute commandExecutor@{
                    val entry = discordBot.messagesToObserveModal[event.user.id]!!
                    DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.OTHER, entry.uniqueId)
                    if (entry.commandUserOnly && event.user.id !in (entry.authorIds ?: arrayListOf())) {
                        return@commandExecutor
                    }

                    val result = context.getBean(entry.command.java).onModalInteraction(event, entry.additionalData)
                    if (!entry.stayActive && result) {
                        discordBot.removeMessageForModalEvents(event.user.id)
                    }
                }
            }
        }
    }
}
