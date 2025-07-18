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

package io.viascom.discord.bot.aluna.bot.command

import io.viascom.discord.bot.aluna.bot.CoroutineDiscordCommand
import io.viascom.discord.bot.aluna.bot.Interaction
import io.viascom.discord.bot.aluna.bot.command.systemcommand.SystemCommandDataProviderHandler
import io.viascom.discord.bot.aluna.bot.command.systemcommand.SystemCommandEmojiProvider
import io.viascom.discord.bot.aluna.configuration.Experimental
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.StringOption
import io.viascom.discord.bot.aluna.property.ModeratorIdProvider
import io.viascom.discord.bot.aluna.util.getTypedOption
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command

@Interaction
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
public class SystemCommand(
    private val dataProviders: List<SystemCommandDataProviderHandler>,
    private val moderatorIdProvider: ModeratorIdProvider,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : CoroutineDiscordCommand(
    "system-command",
    "Runs a system command.",
    observeAutoComplete = true
) {

    init {
        this.beanCallOnDestroy = false
    }

    private var selectedProvider: SystemCommandDataProviderHandler? = null

    private var commandOption = StringOption("command", "System command to execute", isRequired = true, isAutoComplete = true)
    public var argsOption: StringOption = StringOption("args", "Arguments", isRequired = false, isAutoComplete = true)

    override fun initCommandOptions() {
        @OptIn(Experimental::class)
        specificServers = alunaProperties.command.systemCommand.servers
        setContexts(alunaProperties.command.systemCommand.contexts)
        setIntegrationTypes(alunaProperties.command.systemCommand.integrationTypes)
        addOptions(commandOption, argsOption)
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        if (event.user.idLong !in ownerIdProvider.getOwnerIds() && event.user.idLong !in moderatorIdProvider.getModeratorIds()) {
            event.deferReply(true).setContent("${systemCommandEmojiProvider.crossEmoji().formatted} This command is to powerful for you.").queue()
            return
        }

        selectedProvider = dataProviders.filter {
            it.id in (alunaProperties.command.systemCommand.enabledFunctions ?: arrayListOf()) || alunaProperties.command.systemCommand.enabledFunctions == null
        }.firstOrNull { it.id == event.getTypedOption(commandOption, "") }

        if (selectedProvider == null) {
            event.reply("Command not found!").setEphemeral(true).queue()
            return
        }

        //Check if it is an owner or (mod and mod is allowed)
        if (event.user.idLong !in ownerIdProvider.getOwnerIds() && !(isModAllowed(selectedProvider!!) &&
                    event.user.idLong in moderatorIdProvider.getModeratorIdsForCommandPath("system-command/${selectedProvider!!.id}"))
        ) {
            event.deferReply(true).setContent("${systemCommandEmojiProvider.crossEmoji().formatted} This command is to powerful for you.").queue()
            return
        }

        val ephemeral = if (event.user.idLong in moderatorIdProvider.getModeratorIdsForCommandPath("system-command/${selectedProvider!!.id}")) {
            false
        } else {
            selectedProvider!!.ephemeral
        }

        if (!selectedProvider!!.autoAcknowledgeEvent) {
            selectedProvider!!.runExecute(event, null, this)
        } else {
            val hook = event.deferReply(ephemeral).complete()
            selectedProvider!!.runExecute(event, hook, this)
        }
    }

    override suspend fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        return selectedProvider?.runOnButtonInteraction(event) ?: true
    }

    override suspend fun onButtonInteractionTimeout() {
        selectedProvider?.runOnButtonInteractionTimeout()
    }

    override suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent): Boolean {
        return selectedProvider?.runOnStringSelectMenuInteraction(event) ?: true
    }

    override suspend fun onStringSelectInteractionTimeout() {
        selectedProvider?.runOnStringSelectInteractionTimeout()
    }

    override suspend fun onEntitySelectInteraction(event: EntitySelectInteractionEvent): Boolean {
        return selectedProvider?.runOnEntitySelectInteraction(event) ?: true
    }

    override suspend fun onEntitySelectInteractionTimeout() {
        selectedProvider?.runOnEntitySelectInteractionTimeout()
    }

    override suspend fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        return selectedProvider?.runOnModalInteraction(event) ?: true
    }

    override suspend fun onModalInteractionTimeout() {
        selectedProvider?.runOnModalInteractionTimeout()
    }

    override suspend fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
        super.onAutoCompleteEvent(option, event)

        if (option == "command") {
            val input = event.getTypedOption(commandOption, "")!!

            val filteredDataProviders = dataProviders.filter {
                it.id in (alunaProperties.command.systemCommand.enabledFunctions ?: arrayListOf()) ||
                        alunaProperties.command.systemCommand.enabledFunctions == null
            }

            val options = if (input.isEmpty()) {
                filteredDataProviders.filter {
                    event.user.idLong in ownerIdProvider.getOwnerIds() || (isModAllowed(it) && event.user.idLong in moderatorIdProvider.getModeratorIdsForCommandPath(
                        "system-command/${it.id}"
                    ))
                }
            } else {
                filteredDataProviders.filter {
                    event.user.idLong in ownerIdProvider.getOwnerIds() || (isModAllowed(it) && event.user.idLong in moderatorIdProvider.getModeratorIdsForCommandPath(
                        "system-command/${it.id}"
                    ))
                }.filter { it.name.lowercase().contains(input.lowercase()) || it.id.lowercase().contains(input.lowercase()) }
            }.take(25).sortedBy { it.name }.map {
                Command.Choice(it.name, it.id)
            }

            event.replyChoices(options).queue()
            return
        }

        if (option == "args") {
            val possibleProvider = dataProviders.firstOrNull { it.id == event.getTypedOption(commandOption, "")!! && it.supportArgsAutoComplete }
            if (possibleProvider != null) {
                possibleProvider.runOnArgsAutoComplete(event, this)
            } else {
                event.replyChoices().queue()
            }
        }
    }

    private fun isModAllowed(selectedProvider: SystemCommandDataProviderHandler): Boolean {
        val propertiesOverride = alunaProperties.command.systemCommand.allowedForModeratorsFunctions?.firstOrNull { it == selectedProvider.id }

        return when {
            (propertiesOverride != null) -> true
            else -> selectedProvider.allowMods
        }


    }
}
