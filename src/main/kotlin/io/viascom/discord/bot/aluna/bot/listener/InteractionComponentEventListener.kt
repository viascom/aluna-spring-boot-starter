/*
 * Copyright 2023 Viascom Ltd liab. Co
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
import io.viascom.discord.bot.aluna.bot.event.CoroutineEventListener
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.scope.DiscordContext
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.NanoId
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
open class InteractionComponentEventListener(
    private val discordBot: DiscordBot,
    private val alunaProperties: AlunaProperties,
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

    private fun onButtonInteraction(event: ButtonInteractionEvent) {
        val id = event.componentId
        if (!id.startsWith("/")) {
            return
        }

        val commandId = id.substring(1).split(":")[0]
        if (!discordBot.commandsWithPersistentInteractions.contains(commandId)) {
            return
        }
        val uniqueId = id.substring(1).split(":").getOrNull(1) ?: NanoId.generate()
        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            uniqueId,
            event.messageId
        )
        val interaction = discordBot.commands[commandId] ?: return
        context.getBean(interaction).onButtonGlobalInteraction(event)
    }

    private fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        val id = event.componentId
        if (!id.startsWith("/")) {
            return
        }

        val commandId = id.substring(1).split(":")[0]
        if (!discordBot.commandsWithPersistentInteractions.contains(commandId)) {
            return
        }
        val uniqueId = id.substring(1).split(":").getOrNull(1) ?: NanoId.generate()
        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            uniqueId,
            event.messageId
        )
        val interaction = discordBot.commands[commandId] ?: return
        context.getBean(interaction).onStringSelectGlobalInteraction(event)
    }

    private fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        val id = event.componentId
        if (!id.startsWith("/")) {
            return
        }

        val commandId = id.substring(1).split(":")[0]
        if (!discordBot.commandsWithPersistentInteractions.contains(commandId)) {
            return
        }
        val uniqueId = id.substring(1).split(":").getOrNull(1) ?: NanoId.generate()
        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            uniqueId,
            event.messageId
        )
        val interaction = discordBot.commands[commandId] ?: return
        context.getBean(interaction).onEntitySelectGlobalInteraction(event)
    }

    private fun onModalInteraction(event: ModalInteractionEvent) {
        val id = event.modalId
        if (!id.startsWith("/")) {
            return
        }

        val commandId = id.substring(1).split(":")[0]
        if (!discordBot.commandsWithPersistentInteractions.contains(commandId)) {
            return
        }
        val uniqueId = id.substring(1).split(":").getOrNull(1) ?: NanoId.generate()
        DiscordContext.setDiscordState(
            event.user.id,
            event.guild?.id,
            DiscordContext.Type.INTERACTION,
            uniqueId
        )
        val interaction = discordBot.commands[commandId] ?: return
        context.getBean(interaction).onModalGlobalInteraction(event)
    }
}