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

package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.DiscordContextMenu
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordFirstShardReadyEvent
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.model.DevelopmentStatus
import io.viascom.discord.bot.aluna.model.UseScope
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
@ConditionalOnJdaEnabled
internal open class InteractionInitializer(
    private val commands: List<DiscordCommand>,
    private val contextMenus: List<DiscordContextMenu>,
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val eventPublisher: EventPublisher,
    private val alunaProperties: AlunaProperties
) : ApplicationListener<DiscordFirstShardReadyEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordFirstShardReadyEvent) {
        initSlashCommands()
    }

    private fun initSlashCommands() {
        logger.debug("Check interactions to update")

        //Get all interactions, filter not needed interactions and call init methods
        val filteredCommands = commands.filter {
            when {
                (alunaProperties.includeInDevelopmentInteractions) -> true
                (alunaProperties.productionMode && it.interactionDevelopmentStatus == DevelopmentStatus.IN_DEVELOPMENT) -> false
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

        //Create global interactions
        //shardManager.shards.first().retrieveCommands(true).queue { currentCommands ->
        shardManager.shards.first().retrieveCommands().queue { currentCommands ->
            val commandDataList = arrayListOf<CommandDataImpl>()

            commandDataList.addAll(filteredCommands)
            commandDataList.addAll(filteredContext)


            val commandsToRemove = currentCommands.filter { command ->
                commandDataList.filter {
                    if (it.type == Command.Type.SLASH) {
                        (it as DiscordCommand).useScope in arrayListOf(UseScope.GLOBAL, UseScope.GUILD_ONLY)
                    } else {
                        true
                    }
                }.none { command.name == it.name && command.type == it.type }
            }

            commandsToRemove.forEach {
                logger.debug("Removed unneeded interaction '${it.name}'")
                shardManager.shards.first().deleteCommandById(it.id).queue()
            }

            val commandsToUpdate = currentCommands.filter { it.name !in commandsToRemove.map { it.name } }.filter { command ->
                commandDataList.filter {
                    if (it.type == Command.Type.SLASH) {
                        (it as DiscordCommand).useScope in arrayListOf(UseScope.GLOBAL, UseScope.GUILD_ONLY)
                    } else {
                        (it as DiscordContextMenu).useScope in arrayListOf(UseScope.GLOBAL, UseScope.GUILD_ONLY)
                    }
                }.none { compareCommands(it, command) }
            }

            commandsToUpdate.forEach { discordCommand ->
                logger.debug("Update interaction '${discordCommand.name}'")
                val editCommand = shardManager.shards.first().editCommandById(discordCommand.id)
                editCommand.clearOptions()
                editCommand.apply(commandDataList.first { it.name == discordCommand.name && it.type == discordCommand.type }).queue()
            }

            val commandsToAdd = commandDataList
                .filter {
                    if (it.type == Command.Type.SLASH) {
                        (it as DiscordCommand).useScope in arrayListOf(UseScope.GLOBAL, UseScope.GUILD_ONLY)
                    } else {
                        true
                    }
                }
                .filter { commandData ->
                    commandData.name !in commandsToUpdate.map { it.name }
                }
                .filter { commandData ->
                    commandData.name !in commandsToRemove.map { it.name }
                }
                .filter { commandData ->
                    commandData.name !in currentCommands.map { it.name }
                }

            commandsToAdd.forEach { discordCommand ->
                shardManager.shards.first().upsertCommand(discordCommand).queue { command ->
                    if (discordCommand.type == Command.Type.SLASH) {
                        printCommand((discordCommand as DiscordCommand))
                        discordBot.commands[command.id] = discordCommand.javaClass
                    }
                    if (discordCommand.type != Command.Type.SLASH) {
                        logger.debug("Register context menu '${(discordCommand as DiscordContextMenu).name}'")
                        discordBot.contextMenus[command.id] = discordCommand.javaClass
                    }
                    if (discordCommand.type == Command.Type.SLASH && (discordCommand as DiscordCommand).observeAutoComplete && command.name !in discordBot.commandsWithAutocomplete) {
                        discordBot.commandsWithAutocomplete.add(command.id)
                    }
                }
            }

            //shardManager.shards.first().retrieveCommands(true).queue {
            shardManager.shards.first().retrieveCommands().queue { command ->
                command.filter { it.type == Command.Type.SLASH }.filter { it.name in commands.map { it.name } }.forEach { filteredCommand ->
                    try {
                        discordBot.commands.computeIfAbsent(filteredCommand.id) { commands.first { it.name == filteredCommand.name }.javaClass }
                        if (commands.first { it.name == filteredCommand.name }.observeAutoComplete && filteredCommand.id !in discordBot.commandsWithAutocomplete) {
                            discordBot.commandsWithAutocomplete.add(filteredCommand.id)
                        }
                    } catch (e: Exception) {
                        logger.error("Could not add command '${filteredCommand.name}' to available commands")
                    }
                }

                command.filter { it.type != Command.Type.SLASH }.filter { it.name in contextMenus.map { it.name } }.forEach { filteredCommand ->
                    try {
                        discordBot.contextMenus.computeIfAbsent(filteredCommand.id) { contextMenus.first { it.name == filteredCommand.name }.javaClass }
                    } catch (e: Exception) {
                        logger.error("Could not add context menu '${filteredCommand.name}' to available commands")
                    }
                }

                eventPublisher.publishDiscordSlashCommandInitializedEvent(
                    commandsToAdd.filter { it.type == Command.Type.SLASH }.map { it::class },
                    commandsToUpdate.filter { it.type == Command.Type.SLASH }
                        .map { discordCommand -> commandDataList.first { it.name == discordCommand.name } }
                        .map { it::class },
                    commandsToRemove.map { it.name })
            }
        }

        //Register internal commands
        if (filteredCommands.any { it.name == "system-command" }) {
            val command = filteredCommands.first { it.name == "system-command" }
            val server = command.specificServer?.let { shardManager.getGuildById(it) }
            val serverCommands = server?.retrieveCommands()?.complete()

            if (!command.alunaProperties.command.systemCommand.enabled) {
                if (serverCommands != null && serverCommands.any { it.name == "system-command" }) {
                    val serverCommand = serverCommands.first { it.name == "system-command" }
                    logger.debug("Removed unneeded specific command '/${serverCommand.name}'")
                    server.deleteCommandById(serverCommand.id).queue()
                }
            } else {

                //Check if system command should be global
                if (server == null) {
                    //val serverCommand = shardManager.shards.first().retrieveCommands(true).complete().firstOrNull { it.name == command.name }
                    val serverCommand = shardManager.shards.first().retrieveCommands().complete().firstOrNull { it.name == command.name }
                    if (serverCommand != null && !compareCommands(command, serverCommand)) {
                        shardManager.shards.first().upsertCommand(command).queue {
                            printCommand(command)
                            discordBot.commands[command.uniqueId] = command.javaClass
                            discordBot.commandsWithAutocomplete.add(command.uniqueId)
                        }
                    }
                } else {
                    var upsert = serverCommands != null && serverCommands.none { it.name == "system-command" }

                    if (!upsert) {
                        val serverCommand = serverCommands?.firstOrNull { it.name == "system-command" }
                        if (serverCommand == null) {
                            upsert = true
                        } else {
                            upsert = !compareCommands(command, serverCommand)
                        }
                    }

                    if (upsert) {
                        server.upsertCommand(command).queue { discordCommand ->
                            printCommand(command, true)
                            discordBot.commands[discordCommand.id] = command.javaClass
                            discordBot.commandsWithAutocomplete.add(discordCommand.id)
                        }
                    }
                }
            }


        }

        /* This does currently not work for 100% as if a command is changed to another server, Aluna has no idea
        where to remove the old command as this information can only be obtained by checking every server individually.


        //Create per guild Commands
        val commandDataList = arrayListOf<CommandDataImpl>()
        commandDataList.addAll(filteredCommands)

        val specificCommands = commandDataList
            .filter { it.type == Command.Type.SLASH }
            .filter { (it as DiscordCommand).useScope in arrayListOf(DiscordCommand.UseScope.GUILD_SPECIFIC) }
            .filter { (it as DiscordCommand).specificServer != null }

        val serverCommands = specificCommands
            .distinctBy { (it as DiscordCommand).specificServer }
            .associate {
                Pair((it as DiscordCommand).specificServer, shardManager.getServer(it.specificServer!!)?.retrieveCommands()?.complete() ?: arrayListOf())
            }

        val commandsToRemove = arrayListOf<Command>()
        serverCommands.forEach { commands ->
            if (specificCommands.none { (it as DiscordCommand).specificServer == commands.key }) {
                commandsToRemove.addAll(commands.value)
                return@forEach
            }

            serverCommands[commands.key]?.forEach { command ->
                if (commandDataList.none { compareCommands(it, command) }) {
                    commandsToRemove.addAll(commands.value)
                }
            }
        }

        commandsToRemove.forEach {
            logger.debug("Removed unneeded specific command '/${it.name}'")
            shardManager.shards.first().deleteCommandById(it.id).queue()
        }

        var commandsToUpdateOrAdd: HashMap<String, ArrayList<CommandData>> = hashMapOf()
        specificCommands.forEach { command ->
            val commandDatas = serverCommands[(command as DiscordCommand).specificServer!!]

            if (commandDatas?.isNotEmpty() == true) {
                commandDatas.forEach { commandData ->
                    if (!compareCommands(command, commandData)) {
                        if (commandsToUpdateOrAdd.containsKey(command.specificServer!!)) {
                            commandsToUpdateOrAdd[command.specificServer!!]!!.add(command)
                        } else {
                            commandsToUpdateOrAdd[command.specificServer!!] = arrayListOf(command)
                        }
                    }
                }
            } else {
                if (commandsToUpdateOrAdd.containsKey(command.specificServer!!)) {
                    commandsToUpdateOrAdd[command.specificServer!!]!!.add(command)
                } else {
                    commandsToUpdateOrAdd[command.specificServer!!] = arrayListOf(command)
                }
            }
        }

        commandsToUpdateOrAdd.forEach {
            val server = shardManager.getGuildById(it.key)
            it.value.forEach {
                server?.upsertCommand(it)?.queue { discordCommand ->
                    printCommand((it as DiscordCommand), true)
                    discordBot.commands[it.name] = it.javaClass
                }
            }
        }*/
    }

    private fun printCommand(command: DiscordCommand, isSpecific: Boolean = false) {
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

    private fun compareCommands(commandData: CommandDataImpl, command: Command): Boolean {
        return commandData.name == command.name &&
                commandData.description == command.description &&
                commandData.options.map { Command.Option(it.toData()) } == command.options &&
                commandData.subcommandGroups.map { Command.SubcommandGroup(it.toData()) } == command.subcommandGroups &&
                commandData.subcommands.map { Command.Subcommand(it.toData()) } == command.subcommands &&
                commandData.defaultPermissions.permissionsRaw == command.defaultPermissions.permissionsRaw &&
                commandData.isGuildOnly == command.isGuildOnly
        //commandData.nameLocalizations.toMap() == command.nameLocalizations.toMap() &&
        //commandData.descriptionLocalizations.toMap() == command.descriptionLocalizations.toMap()
    }

}