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
import io.viascom.discord.bot.aluna.bot.coQueue
import io.viascom.discord.bot.aluna.bot.command.DefaultHelpCommand
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordFirstShardConnectedEvent
import io.viascom.discord.bot.aluna.event.DiscordMainShardConnectedEvent
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.exception.AlunaInitializationException
import io.viascom.discord.bot.aluna.model.DevelopmentStatus
import io.viascom.discord.bot.aluna.property.AlunaDiscordProperties
import io.viascom.discord.bot.aluna.property.AlunaProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.interactions.commands.Command.Type
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.concurrent.CountDownLatch

@Service
@Order(100)
@ConditionalOnJdaEnabled
internal open class InteractionInitializer(
    private val commands: List<DiscordCommandHandler>,
    private val contextMenus: List<DiscordContextMenuHandler>,
    private val initializationCondition: InteractionInitializerCondition,
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val eventPublisher: EventPublisher,
    private val alunaProperties: AlunaProperties,
    private val applicationEventPublisher: ApplicationEventPublisher
) : ApplicationListener<DiscordMainShardConnectedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordMainShardConnectedEvent) {
        AlunaDispatchers.InternalScope.launch {
            if (discordBot.interactionsInitialized) {
                return@launch
            }

            //Check if initialization is necessary and if so, init slash commands otherwise call interaction loader
            if (initializationCondition.isInitializeNeeded()) {
                discordBot.interactionsInitialized = true
                initSlashCommands()
            } else {
                logger.debug("Initialization is not necessary. Call interaction loader")
                applicationEventPublisher.publishEvent(DiscordFirstShardConnectedEvent(event.source, event.jdaEvent, event.shardManager))
            }
        }
    }

    private suspend fun initSlashCommands() = withContext(AlunaDispatchers.Internal) {
        logger.debug("Check interactions to update")
        val deferredCurrentInteractions = async { shardManager.shards.first().retrieveCommands(true).complete() }

        //Get all interactions, filter not necessary interactions and call init methods
        val filteredCommands = getFilteredCommands()
        val filteredContext = getFilteredContext()

        //Check if the bot has its own help command and forgot to disable the default help command
        if (filteredCommands.count { it.name == "help" } > 1 && alunaProperties.command.helpCommand.enabled) {
            logger.warn("Found /help command in your commands, but help command is enabled in your configuration. Please disable help command in your configuration. Default help command will be disabled for this run.")
            filteredCommands.removeIf { it.name == "help" && it is DefaultHelpCommand }
        }

        val interactionToUpdate = arrayListOf<CommandDataImpl>()
        interactionToUpdate.addAll(filteredCommands.filter { (it.specificServers.isNullOrEmpty()) })
        interactionToUpdate.addAll(filteredContext.filter { (it.specificServers.isNullOrEmpty()) })

        if (interactionToUpdate.filter { it.type == Type.SLASH }.size > 100) {
            throw AlunaInitializationException("You have more than 100 global commands. This is not supported by Discord. Please reduce the number of commands.")
        }
        if (interactionToUpdate.filter { it.type == Type.USER }.size > 5) {
            throw AlunaInitializationException("You have more than 5 global user context menus. This is not supported by Discord. Please reduce the number of user context menus.")
        }
        if (interactionToUpdate.filter { it.type == Type.MESSAGE }.size > 5) {
            throw AlunaInitializationException("You have more than 5 global message context menus. This is not supported by Discord. Please reduce the number of message context menus.")
        }

        val currentInteractions = deferredCurrentInteractions.await()

        discordBot.discordRepresentations.clear()

        shardManager.shards.first().updateCommands()
            .addCommands(interactionToUpdate)
            .coQueue { updatedCommands ->
                discordBot.discordRepresentations.putAll(updatedCommands.associateBy { it.id })

                updatedCommands.filter { it.type == Type.SLASH }.filter { it.name in commands.map { it.name } }.forEach { interaction ->
                    try {
                        discordBot.commands.computeIfAbsent(interaction.id) { commands.first { it.name == interaction.name }.javaClass }

                        if (commands.first { it.name == interaction.name }.observeAutoComplete && interaction.id !in discordBot.commandsWithAutocomplete) {
                            discordBot.commandsWithAutocomplete.add(interaction.id)
                        }
                        if (commands.first { it.name == interaction.name }.handlePersistentInteractions && interaction.id !in discordBot.commandsWithPersistentInteractions) {
                            discordBot.commandsWithPersistentInteractions.add(interaction.id)
                        }
                    } catch (e: Exception) {
                        logger.error("Could not add command '${interaction.name}' to available commands")
                    }
                }

                updatedCommands.filter { it.type != Type.SLASH }.filter { it.name in contextMenus.map { it.name } }.forEach { interaction ->
                    try {
                        discordBot.contextMenus.computeIfAbsent(interaction.id) { contextMenus.first { it.name == interaction.name }.javaClass }
                    } catch (e: Exception) {
                        logger.error("Could not add context menu '${interaction.name}' to available commands")
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
                    logger.debug("All global interactions are up to date")
                }

                initServerSpecificCommands(filteredCommands, filteredContext)

                AlunaDispatchers.InternalScope.launch {
                    eventPublisher.publishDiscordSlashCommandInitializedEvent(
                        interactionToUpdate.filter { it.name in newCommands.map { it.name } }.map { it::class },
                        interactionToUpdate.filter { it.name in changedCommands.map { it.name } }.map { it::class },
                        removedCommands.map { it.name })
                }
            }
    }

    @JvmSynthetic
    internal fun getFilteredContext(): ArrayList<DiscordContextMenuHandler> {
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
        return filteredContext
    }

    @JvmSynthetic
    internal suspend fun getFilteredCommands(): ArrayList<DiscordCommandHandler> {
        val filteredCommands = commands
            .filter {
                when {
                    (alunaProperties.includeInDevelopmentInteractions) -> true
                    (alunaProperties.productionMode && it.interactionDevelopmentStatus == DevelopmentStatus.IN_DEVELOPMENT) -> false
                    (alunaProperties.command.systemCommand.enabled && alunaProperties.command.systemCommand.servers != null && it.name == "system-command") -> false
                    else -> true
                }
            }
            .map {
                try {
                    it.initCommandOptions()
                    it.prepareInteraction()
                    it.prepareLocalization()
                } catch (e: Exception) {
                    logger.warn("Was not able to initialize command ${it.name}\n${e.stackTraceToString()}")
                }
                it
            }.toCollection(arrayListOf())
        return filteredCommands
    }

    suspend fun initServerSpecificCommands(commands: ArrayList<DiscordCommandHandler>, context: ArrayList<DiscordContextMenuHandler>) = withContext(AlunaDispatchers.Internal) {
        if (!alunaProperties.command.enableServerSpecificCommands) return@withContext

        //Get all interactions, filter not needed interactions and call init methods
        val filteredCommands = commands.filter { it.specificServers?.isNotEmpty() == true }
        val filteredContext = context.filter { it.specificServers?.isNotEmpty() == true }

        if ((filteredCommands.isNotEmpty() || filteredContext.isNotEmpty()) && !alunaProperties.command.enableServerSpecificCommands) {
            logger.error("You have configured server specific commands, but you have not enabled them in your configuration. Please enable them in your configuration as they will otherwise be ignored.")
            return@withContext
        }

        //Handle specific server interactions
        val groupedByServer = hashMapOf<String, ArrayList<CommandDataImpl>>()
        filteredCommands.forEach { command ->
            command.specificServers!!.filter { isSpecificServerInShard(it) }.forEach { serverId ->
                groupedByServer.computeIfAbsent(serverId) { arrayListOf() }.add(command)
            }
        }
        filteredContext.forEach { command ->
            command.specificServers!!.filter { isSpecificServerInShard(it) }.forEach { serverId ->
                groupedByServer.computeIfAbsent(serverId) { arrayListOf() }.add(command)
            }
        }

        val updateLatch = CountDownLatch(groupedByServer.size)

        for ((serverId, relevantCommands) in groupedByServer) {

            if (relevantCommands.filter { it.type == Type.SLASH }.size > 100) {
                logger.error("You have more than 100 commands for a the server $serverId. This is not supported by Discord. Please reduce the number of commands.")
                return@withContext
            }
            if (relevantCommands.filter { it.type == Type.USER }.size > 5) {
                logger.error("You have more than 5 user context menus for a the server $serverId. This is not supported by Discord. Please reduce the number of user context menus.")
                return@withContext
            }
            if (relevantCommands.filter { it.type == Type.MESSAGE }.size > 5) {
                logger.error("You have more than 5 message context menus for a the server $serverId. This is not supported by Discord. Please reduce the number of message context menus.")
                return@withContext
            }

            val beforeUpdate = OffsetDateTime.now()
            val server = shardManager.getGuildById(serverId) ?: continue

            server.updateCommands().addCommands(relevantCommands).queue { updatedCommands ->

                discordBot.discordRepresentations.putAll(updatedCommands.associateBy { it.id })

                updatedCommands.filter { it.type == Type.SLASH }.filter { it.name in filteredCommands.map { it.name } }.forEach { interaction ->
                    try {
                        discordBot.commands.computeIfAbsent(interaction.id) { filteredCommands.first { it.name == interaction.name }.javaClass }

                        if (commands.first { it.name == interaction.name }.observeAutoComplete && interaction.id !in discordBot.commandsWithAutocomplete) {
                            discordBot.commandsWithAutocomplete.add(interaction.id)
                        }
                        if (commands.first { it.name == interaction.name }.handlePersistentInteractions && interaction.id !in discordBot.commandsWithPersistentInteractions) {
                            discordBot.commandsWithPersistentInteractions.add(interaction.id)
                        }
                    } catch (e: Exception) {
                        logger.error("Could not add command '${interaction.name}' on server '${serverId}' to available commands")
                    }
                }

                updatedCommands.filter { it.type != Type.SLASH }.filter { it.name in filteredContext.map { it.name } }.forEach { interaction ->
                    try {
                        discordBot.contextMenus.computeIfAbsent(interaction.id) { filteredContext.first { it.name == interaction.name }.javaClass }
                    } catch (e: Exception) {
                        logger.error("Could not add context menu '${interaction.name}' on server '${serverId}' to available commands")
                    }
                }

                updatedCommands.forEach { interaction ->
                    if (interaction.timeModified.isAfter(beforeUpdate)) {
                        if (interaction.type == Type.SLASH) {
                            printCommand(filteredCommands.first { it.name == interaction.name }, true, arrayListOf(serverId))
                        } else {
                            logger.debug("Update interaction '${interaction.name}' on server '${serverId}'")
                        }
                    }
                }

                updateLatch.countDown()
            }
        }

        async { updateLatch.await() }.await()

        shardManager.guilds.forEach { server ->
            launch {
                server.retrieveCommands().coQueue { commands ->
                    val outdatedCommands = commands.filterNot { it.id in discordBot.discordRepresentations.keys }
                    outdatedCommands.forEach {
                        launch {
                            server.deleteCommandById(it.id).queue()
                            logger.debug("Deleted outdated command '${it.name}' from server '${server.id}'")
                        }
                    }
                }
            }
        }


        //Check if we need /system-command
        if (alunaProperties.command.systemCommand.enabled && alunaProperties.command.systemCommand.servers != null) {
            alunaProperties.command.systemCommand.servers!!.forEach { serverId ->
                launch {
                    val server = shardManager.getGuildById(serverId)
                    val systemCommand = commands.firstOrNull { it.name == "system-command" } ?: throw IllegalArgumentException()

                    server!!.updateCommands().addCommands(systemCommand).coQueue { discordCommands ->
                        val discordCommand = discordCommands.first { it.name == systemCommand.name }
                        printCommand(systemCommand, true, arrayListOf(serverId))

                        discordBot.commands[discordCommand.id] = systemCommand.javaClass
                        discordBot.commandsWithAutocomplete.add(discordCommand.id)
                        discordBot.discordRepresentations[discordCommand.id] = discordCommand
                    }
                }
            }
        }
    }

    private fun isSpecificServerInShard(serverId: String): Boolean {
        return alunaProperties.discord.sharding.type == AlunaDiscordProperties.Sharding.Type.SINGLE ||
                (discordBot.getLowerBoundOfSnowflake() <= serverId.toLong() && discordBot.getUpperBoundOfSnowflake() >= serverId.toLong())
    }

    private fun printCommand(command: DiscordCommandHandler, isSpecific: Boolean = false, serverIds: ArrayList<String>? = null) {
        var commandText = ""
        commandText += "Add${if (isSpecific) " server specific" else ""} command '/${command.name}'${if (serverIds != null) " in servers ${serverIds.joinToString(", ")}" else ""}"
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
