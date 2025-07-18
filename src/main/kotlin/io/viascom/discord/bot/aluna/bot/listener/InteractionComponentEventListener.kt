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
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
public open class InteractionComponentEventListener(
    private val discordBot: DiscordBot,
    private val context: ConfigurableApplicationContext
) : CoroutineEventListener {

    override suspend fun onEvent(event: GenericEvent) {
        when (event) {
            is ButtonInteractionEvent -> onButtonInteraction(event)
            is StringSelectInteractionEvent -> onStringSelectInteraction(event)
            is EntitySelectInteractionEvent -> onEntitySelectInteraction(event)
            is ModalInteractionEvent -> onModalInteraction(event)
        }
    }

    private suspend fun onButtonInteraction(event: ButtonInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        val id = event.componentId
        if (!id.startsWith("/")) {
            return@withContext
        }

        val componentData = id.substring(1).split(":")
        if (componentData.size < 4) {
            return@withContext
        }

        val commandId = componentData[0].split("/")[0]
        if (!discordBot.commandsWithPersistentInteractions.contains(commandId)) {
            return@withContext
        }
        val commandName = discordBot.discordRepresentations.entries.firstOrNull { it.key == commandId }?.key ?: return@withContext

        //If we have data for a subcommand, we need to add it to the command path
        val fullCommandPath = if (componentData[0] != commandId) {
            commandName + " " + componentData[0].substring(commandId.length + 1).replace("/", " ")
        } else {
            commandName
        }

        val uniqueId = componentData[1]
        val userId = componentData[2]
        if (userId != event.user.id && userId != "*") {
            return@withContext
        }

        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            uniqueId,
            event.messageId
        )
        val interaction = discordBot.commands[commandId] ?: return@withContext
        context.getBean(interaction).onButtonGlobalInteraction(event, fullCommandPath)
    }

    private suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        val id = event.componentId
        if (!id.startsWith("/")) {
            return@withContext
        }

        val componentData = id.substring(1).split(":")
        if (componentData.size < 4) {
            return@withContext
        }

        val commandId = componentData[0].split("/")[0]
        if (!discordBot.commandsWithPersistentInteractions.contains(commandId)) {
            return@withContext
        }
        val commandName = discordBot.discordRepresentations.entries.firstOrNull { it.value.id == commandId }?.key ?: return@withContext

        //If we have data for a subcommand, we need to add it to the command path
        val fullCommandPath = if (componentData[0] != commandId) {
            commandName + " " + componentData[0].substring(commandId.length + 1).replace("/", " ")
        } else {
            commandName
        }

        val uniqueId = componentData[1]
        val userId = componentData[2]
        if (userId != event.user.id && userId != "*") {
            return@withContext
        }

        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            uniqueId,
            event.messageId
        )
        val interaction = discordBot.commands[commandId] ?: return@withContext
        context.getBean(interaction).onStringSelectGlobalInteraction(event, fullCommandPath)
    }

    private suspend fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        val id = event.componentId
        if (!id.startsWith("/")) {
            return@withContext
        }

        val componentData = id.substring(1).split(":")
        if (componentData.size < 4) {
            return@withContext
        }

        val commandId = componentData[0].split("/")[0]
        if (!discordBot.commandsWithPersistentInteractions.contains(commandId)) {
            return@withContext
        }
        val commandName = discordBot.discordRepresentations.entries.firstOrNull { it.value.id == commandId }?.key ?: return@withContext

        //If we have data for a subcommand, we need to add it to the command path
        val fullCommandPath = if (componentData[0] != commandId) {
            commandName + " " + componentData[0].substring(commandId.length + 1).replace("/", " ")
        } else {
            commandName
        }

        val uniqueId = componentData[1]
        val userId = componentData[2]
        if (userId != event.user.id && userId != "*") {
            return@withContext
        }

        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            uniqueId,
            event.messageId
        )
        val interaction = discordBot.commands[commandId] ?: return@withContext
        context.getBean(interaction).onEntitySelectGlobalInteraction(event, fullCommandPath)
    }

    private suspend fun onModalInteraction(event: ModalInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        val id = event.modalId
        if (!id.startsWith("/")) {
            return@withContext
        }

        val componentData = id.substring(1).split(":")
        if (componentData.size < 4) {
            return@withContext
        }

        val commandId = componentData[0].split("/")[0]
        if (!discordBot.commandsWithPersistentInteractions.contains(commandId)) {
            return@withContext
        }
        val commandName = discordBot.discordRepresentations.entries.firstOrNull { it.value.id == commandId }?.key ?: return@withContext

        //If we have data for a subcommand, we need to add it to the command path
        val fullCommandPath = if (componentData[0] != commandId) {
            commandName + " " + componentData[0].substring(commandId.length + 1).replace("/", " ")
        } else {
            commandName
        }

        val uniqueId = componentData[1]
        val userId = componentData[2]
        if (userId != event.user.id && userId != "*") {
            return@withContext
        }

        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            uniqueId
        )
        val interaction = discordBot.commands[commandId] ?: return@withContext
        context.getBean(interaction).onModalGlobalInteraction(event, fullCommandPath)
    }
}
