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

package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordSlashCommandInitializedEvent
import io.viascom.discord.bot.aluna.event.EventPublisher
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
@ConditionalOnJdaEnabled
internal open class AutoCompleteInteractionInitializer(
    private val autoCompleteHandlers: List<AutoCompleteHandler>,
    private val discordBot: DiscordBot,
    private val eventPublisher: EventPublisher
) : ApplicationListener<DiscordSlashCommandInitializedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordSlashCommandInitializedEvent) {
        AlunaDispatchers.InternalScope.launch {
            initAutoCompleteHandlers()
        }
    }

    private fun initAutoCompleteHandlers() {
        logger.debug("Register AutoCompleteHandlers")

        autoCompleteHandlers.forEach { handler ->
            handler.commands.forEach { command ->
                val commandElement = discordBot.commands.entries.firstOrNull { entry -> command.isAssignableFrom(entry.value) }
                if (commandElement == null) {
                    logger.warn("Could not register '${handler::class.java.canonicalName}'. No registered command for '${command.canonicalName}' found.")
                    return
                }

                discordBot.autoCompleteHandlers[Pair(commandElement.key, handler.option)] = handler::class.java
                logger.debug("\t--> ${handler::class.simpleName} for ${commandElement.value.simpleName} (${handler.option ?: "<all>"})")
            }
        }

        eventPublisher.publishDiscordAutoCompleteHandlerInitializedEvent(autoCompleteHandlers.map { it::class })
    }

}
