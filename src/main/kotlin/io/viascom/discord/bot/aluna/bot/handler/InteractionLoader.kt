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

package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordFirstShardConnectedEvent
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.property.AlunaProperties
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.interactions.commands.Command.*
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

@Service
@Order(100)
@ConditionalOnJdaEnabled
internal open class InteractionLoader(
    private val commands: List<DiscordCommand>,
    private val contextMenus: List<DiscordContextMenuHandler>,
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val eventPublisher: EventPublisher,
    private val alunaProperties: AlunaProperties
) : ApplicationListener<DiscordFirstShardConnectedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordFirstShardConnectedEvent) {
        AlunaDispatchers.InternalScope.launch {
            //No need to load interactions if it is the main shard
            if (alunaProperties.discord.sharding.fromShard == 0) {
                return@launch
            }

            logger.debug("Load interactions")

            shardManager.shards.first().retrieveCommands(true).queue { currentCommands ->
                discordBot.discordRepresentations.clear()
                discordBot.discordRepresentations.putAll(currentCommands.associateBy { it.name })

                currentCommands.filter { it.type == Type.SLASH }.filter { it.name in commands.map { it.name } }.forEach { filteredCommand ->
                    try {
                        discordBot.commands.computeIfAbsent(filteredCommand.id) { commands.first { it.name == filteredCommand.name }.javaClass }
                        if (commands.first { it.name == filteredCommand.name }.observeAutoComplete && filteredCommand.id !in discordBot.commandsWithAutocomplete) {
                            discordBot.commandsWithAutocomplete.add(filteredCommand.id)
                        }
                        if (commands.first { it.name == filteredCommand.name }.handlePersistentInteractions && filteredCommand.id !in discordBot.commandsWithPersistentInteractions) {
                            discordBot.commandsWithPersistentInteractions.add(filteredCommand.id)
                        }
                    } catch (e: Exception) {
                        logger.error("Could not add command '${filteredCommand.name}' to available commands")
                    }
                }

                currentCommands.filter { it.type != Type.SLASH }.filter { it.name in contextMenus.map { it.name } }.forEach { filteredCommand ->
                    try {
                        discordBot.contextMenus.computeIfAbsent(filteredCommand.id) { contextMenus.first { it.name == filteredCommand.name }.javaClass }
                    } catch (e: Exception) {
                        logger.error("Could not add context menu '${filteredCommand.name}' to available commands")
                    }
                }

                eventPublisher.publishDiscordSlashCommandInitializedEvent(arrayListOf(), arrayListOf(), arrayListOf())
            }
        }
    }
}
