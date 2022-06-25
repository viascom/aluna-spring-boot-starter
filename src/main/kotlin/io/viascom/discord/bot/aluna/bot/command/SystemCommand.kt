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

package io.viascom.discord.bot.aluna.bot.command

import io.viascom.discord.bot.aluna.bot.Command
import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.command.systemcommand.SystemCommandDataProvider
import io.viascom.discord.bot.aluna.bot.command.systemcommand.SystemCommandEmojiProvider
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.StringOption
import io.viascom.discord.bot.aluna.property.ModeratorIdProvider
import io.viascom.discord.bot.aluna.util.addOption
import io.viascom.discord.bot.aluna.util.getTypedOption
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent

@Command
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class SystemCommand(
    private val dataProviders: List<SystemCommandDataProvider>,
    private val moderatorIdProvider: ModeratorIdProvider,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : DiscordCommand(
    "system-command",
    "Runs a system command.",
    observeAutoComplete = true
) {

    init {
        this.beanCallOnDestroy = false
    }

    private var selectedProvider: SystemCommandDataProvider? = null

    var commandOption = StringOption("command", "System command to execute", isRequired = true, isAutoComplete = true)
    var argsOption = StringOption("args", "Arguments", isRequired = false, isAutoComplete = true)

    override fun initCommandOptions() {
        specificServer = alunaProperties.command.systemCommand.server
        addOption(commandOption)
        addOption(argsOption)
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        if (event.user.idLong !in ownerIdProvider.getOwnerIds() && event.user.idLong !in moderatorIdProvider.getModeratorIds()) {
            event.deferReply(true).setContent("${systemCommandEmojiProvider.crossEmoji().asMention} This command is to powerful for you.").queue()
            return
        }

        selectedProvider = dataProviders.filter {
            it.id in (alunaProperties.command.systemCommand.enabledFunctions ?: arrayListOf()) || alunaProperties.command.systemCommand.enabledFunctions == null
        }
            .firstOrNull { it.id == event.getTypedOption(commandOption, "") }

        if (selectedProvider == null) {
            event.reply("Command not found!").setEphemeral(true).queue()
            return
        }

        //Check if it is an owner or (mod and mod is allowed)
        if (event.user.idLong !in ownerIdProvider.getOwnerIds() && !(isModAllowed(selectedProvider!!) && event.user.idLong in moderatorIdProvider.getModeratorIdsForCommandPath(
                "system-command/${selectedProvider!!.id}"
            ))
        ) {
            event.deferReply(true).setContent("${systemCommandEmojiProvider.crossEmoji().asMention} This command is to powerful for you.").queue()
            return
        }

        val ephemeral = if (event.user.idLong in moderatorIdProvider.getModeratorIdsForCommandPath("system-command/${selectedProvider!!.id}")) {
            false
        } else {
            selectedProvider!!.ephemeral
        }

        if (!selectedProvider!!.autoAcknowledgeEvent) {
            selectedProvider!!.execute(event, null, this)
        } else {
            val hook = event.deferReply(ephemeral).complete()
            selectedProvider!!.execute(event, hook, this)
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return selectedProvider?.onButtonInteraction(event) ?: true
    }

    override fun onButtonInteractionTimeout(additionalData: HashMap<String, Any?>) {
        selectedProvider?.onButtonInteractionTimeout()
    }

    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return selectedProvider?.onSelectMenuInteraction(event) ?: true
    }

    override fun onSelectMenuInteractionTimeout(additionalData: HashMap<String, Any?>) {
        selectedProvider?.onSelectMenuInteractionTimeout()
    }

    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return selectedProvider?.onModalInteraction(event, additionalData) ?: true
    }

    override fun onModalInteractionTimeout(additionalData: HashMap<String, Any?>) {
        selectedProvider?.onModalInteractionTimeout(additionalData)
    }

    override fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
        super.onAutoCompleteEvent(option, event)

        if (option == "command") {
            val input = event.getTypedOption(commandOption, "")!!

            val filteredDataProviders = dataProviders.filter {
                it.id in (alunaProperties.command.systemCommand.enabledFunctions
                    ?: arrayListOf()) || alunaProperties.command.systemCommand.enabledFunctions == null
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
                }
                    .filter { it.name.lowercase().contains(input.lowercase()) || it.id.lowercase().contains(input.lowercase()) }
            }.take(25).sortedBy { it.name }.map {
                net.dv8tion.jda.api.interactions.commands.Command.Choice(it.name, it.id)
            }

            event.replyChoices(options).queue()
            return
        }

        if (option == "args") {
            val possibleProvider = dataProviders.firstOrNull { it.id == event.getTypedOption(commandOption, "")!! && it.supportArgsAutoComplete }
            if (possibleProvider != null) {
                possibleProvider.onArgsAutoComplete(event, this)
            } else {
                event.replyChoices().queue()
            }
        }
    }

    private fun isModAllowed(selectedProvider: SystemCommandDataProvider): Boolean {
        val propertiesOverride = alunaProperties.command.systemCommand.allowedForModeratorsFunctions?.firstOrNull { it == selectedProvider.id }

        return when {
            (propertiesOverride != null) -> true
            else -> selectedProvider.allowMods
        }


    }
}
