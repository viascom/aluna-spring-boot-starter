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
import io.viascom.discord.bot.aluna.bot.command.DefaultHelpCommand
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordMainShardConnectedEvent
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.model.DevelopmentStatus
import io.viascom.discord.bot.aluna.property.AlunaProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.interactions.commands.Command.Type
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

@Service
@Order(100)
@ConditionalOnJdaEnabled
internal open class InteractionInitializer(
    private val commands: List<DiscordCommandHandler>,
    private val contextMenus: List<DiscordContextMenuHandler>,
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val eventPublisher: EventPublisher,
    private val alunaProperties: AlunaProperties
) : ApplicationListener<DiscordMainShardConnectedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordMainShardConnectedEvent) {
        AlunaDispatchers.InternalScope.launch {
            if (discordBot.interactionsInitialized) {
                return@launch
            }
            discordBot.interactionsInitialized = true

            initSlashCommands()
        }
    }

    private suspend fun initSlashCommands() = withContext(AlunaDispatchers.Internal) {
        logger.debug("Check interactions to update")
        val deferredCurrentInteractions = async { shardManager.shards.first().retrieveCommands(true).complete() }

        //Get all interactions, filter not needed interactions and call init methods
        val filteredCommands = commands.filter {
            when {
                (alunaProperties.includeInDevelopmentInteractions) -> true
                (alunaProperties.productionMode && it.interactionDevelopmentStatus == DevelopmentStatus.IN_DEVELOPMENT) -> false
                (alunaProperties.command.systemCommand.enabled && alunaProperties.command.systemCommand.server != null && it.name == "system-command") -> false
                else -> true
            }
        }.map {
            try {
                it.initCommandOptions()
                it.prepareInteraction()
                it.prepareLocalization()
            } catch (e: Exception) {
                logger.warn("Was not able to initialize command ${it.name}\n${e.stackTraceToString()}")
            }
            it
        }.toCollection(arrayListOf())

        val filteredContext = contextMenus.filter {
            when {
                (alunaProperties.includeInDevelopmentInteractions) -> true
                (alunaProperties.productionMode && it.interactionDevelopmentStatus == DevelopmentStatus.IN_DEVELOPMENT) -> false
                else -> true
            }
        }.map {
            try {
                it.prepareInteraction()
                it.prepareLocalization()
            } catch (e: Exception) {
                logger.warn("Was not able to initialize context menu ${it.name}\n${e.stackTraceToString()}")
            }
            it
        }.toCollection(arrayListOf())


        //Check if bot has its own help command and forgot to disable default help command
        if (filteredCommands.count { it.name == "help" } > 1 && alunaProperties.command.helpCommand.enabled) {
            logger.warn("Found /help command in your commands, but help command is enabled in your configuration. Please disable help command in your configuration. Default help command will be disabled for this run.")
            filteredCommands.removeIf { it.name == "help" && it is DefaultHelpCommand }
        }

        val interactionToUpdate = arrayListOf<CommandDataImpl>()
        interactionToUpdate.addAll(filteredCommands)
        interactionToUpdate.addAll(filteredContext)

        val currentInteractions = deferredCurrentInteractions.await()

        shardManager.shards.first().updateCommands()
            .addCommands(interactionToUpdate)
            .queue { updatedCommands ->
                discordBot.discordRepresentations.clear()
                discordBot.discordRepresentations.putAll(updatedCommands.associateBy { it.name })

                updatedCommands.filter { it.type == Type.SLASH }.filter { it.name in commands.map { it.name } }.forEach { filteredCommand ->
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

                updatedCommands.filter { it.type != Type.SLASH }.filter { it.name in contextMenus.map { it.name } }.forEach { filteredCommand ->
                    try {
                        discordBot.contextMenus.computeIfAbsent(filteredCommand.id) { contextMenus.first { it.name == filteredCommand.name }.javaClass }
                    } catch (e: Exception) {
                        logger.error("Could not add context menu '${filteredCommand.name}' to available commands")
                    }
                }

                val changedAndNewCommands = updatedCommands.filter { it.id !in currentInteractions.map { it.id } }

                val newCommands = changedAndNewCommands.filter { it.name !in currentInteractions.map { it.name } }
                val changedCommands = changedAndNewCommands.filter { it.name in currentInteractions.map { it.name } }
                val removedCommands = currentInteractions.filter { it.id !in updatedCommands.map { it.id } }

                interactionToUpdate.filter { it.name in newCommands.map { it.name } }.forEach {
                    printCommand((it as DiscordCommandHandler))
                }
                changedCommands.forEach {
                    logger.debug("Update interaction '${it.name}'")
                }
                removedCommands.forEach {
                    logger.debug("Removed unneeded interaction '${it.name}'")
                }

                if (newCommands.isEmpty() && changedCommands.isEmpty() && removedCommands.isEmpty()) {
                    logger.debug("All interactions are up to date")
                }

                eventPublisher.publishDiscordSlashCommandInitializedEvent(
                    interactionToUpdate.filter { it.name in newCommands.map { it.name } }.map { it::class },
                    interactionToUpdate.filter { it.name in changedCommands.map { it.name } }.map { it::class },
                    removedCommands.map { it.name })
            }

        //Check if we need /system-command
        if (alunaProperties.command.systemCommand.enabled && alunaProperties.command.systemCommand.server != null) {

            val server = shardManager.getGuildById(alunaProperties.command.systemCommand.server!!)
            val systemCommand = commands.firstOrNull { it.name == "system-command" } ?: throw IllegalArgumentException()

            server!!.updateCommands().addCommands(systemCommand).queue { discordCommands ->
                val discordCommand = discordCommands.first { it.name == systemCommand.name }
                printCommand(systemCommand, true)
                discordBot.commands[discordCommand.id] = systemCommand.javaClass
                discordBot.commandsWithAutocomplete.add(discordCommand.id)
                discordBot.discordRepresentations[discordCommand.name] = discordCommand
            }
        }
    }

    private fun printCommand(command: DiscordCommandHandler, isSpecific: Boolean = false) {
        var commandText = ""
        commandText += "Add${if (isSpecific) " server specific" else ""} command '/${command.name}'"
        when {
            (command.subcommandGroups.isNotEmpty()) -> {
                commandText += "\n" + command.subcommandGroups.joinToString("\n") {
                    "\t--> ${it.name}\n" +
                            it.subcommands.joinToString("\n") {
                                "\t\t---> ${it.name}"
                            }
                }
            }

            (command.subcommands.isNotEmpty()) -> {
                commandText += "\n" + command.subcommands.joinToString("\n") {
                    "\t--> ${it.name}"
                }
            }
        }

        logger.debug(commandText)
    }

}
