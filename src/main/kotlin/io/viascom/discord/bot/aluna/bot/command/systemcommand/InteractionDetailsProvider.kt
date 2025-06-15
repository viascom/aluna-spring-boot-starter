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

package io.viascom.discord.bot.aluna.bot.command.systemcommand

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.InteractionId
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.util.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component
import java.awt.Color

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
public class InteractionDetailsProvider(
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val defaultSystemCommandEmojiProvider: DefaultSystemCommandEmojiProvider
) : SystemCommandDataProvider(
    "interaction_details",
    "Get details about an interaction",
    true,
    true,
    true,
    false
) {

    private var latestHook: InteractionHook? = null
    private var selectedInteractionId: InteractionId? = null
    private lateinit var selectedInteraction: Command

    private lateinit var embedBuilder: EmbedBuilder
    private var selectedPage: String = "overview"
    private var selectedOptionName: String? = null
    private var selectedSubcommandFullName: String? = null
    private var interactionName: String = ""

    private lateinit var command: SystemCommand

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        this.command = command
        embedBuilder = EmbedBuilder()

        val interactionInput = event.getTypedOption(command.argsOption, "")!!.lowercase()

        val interactions = hashMapOf<InteractionId, String>()
        interactions.putAll(discordBot.discordRepresentations.entries.filter { it.value.type == Command.Type.SLASH }.associate { Pair(it.key, "/${it.value.name}") })
        interactions.putAll(discordBot.discordRepresentations.entries.filter { it.value.type != Command.Type.SLASH }.associate { Pair(it.key, it.value.name) })

        if (interactions.none { it.key == interactionInput }) {
            event.reply("Could not find an interaction with the id `$interactionInput`").setEphemeral(true).queue()
            return
        }

        selectedInteractionId = interactionInput

        embedBuilder.setTitle("Interaction Details")
            .setColor(Color.ORANGE)
            .setDescription("\uD83D\uDCE1 Loading...")

        latestHook = event.deferReply().complete()

        selectedInteraction = discordBot.discordRepresentations.entries.first { it.key == interactionInput }.value

        //Set the first option if available
        selectedOptionName = selectedInteraction.options.firstOrNull()?.name

        //Set the first subcommand if available
        selectedSubcommandFullName =
            selectedInteraction.subcommands.firstOrNull()?.fullCommandName ?: selectedInteraction.subcommandGroups.firstOrNull()?.subcommands?.firstOrNull()?.fullCommandName

        interactionName = if (selectedInteraction.type == Command.Type.SLASH) "/${selectedInteraction.name}" else selectedInteraction.name

        embedBuilder.setTitle(null)
            .setColor(Color.GREEN)

        showOverview()
    }

    private fun showOverview() {
        embedBuilder.setDescription("## $interactionName\n${selectedInteraction.description}")

        embedBuilder.clearFields()
        embedBuilder.addField("Id", "`${selectedInteraction.id}`", true)
        embedBuilder.addField("Name", selectedInteraction.name, true)
        embedBuilder.addField("Type", "${selectedInteraction.type.name} (${selectedInteraction.type.id})", true)
        embedBuilder.addField("Created at", selectedInteraction.timeCreated.toDiscordTimestamp(), true)
        embedBuilder.addField("Modified at", selectedInteraction.timeModified.toDiscordTimestamp(), true)
        embedBuilder.addField("Version", "`${selectedInteraction.version}`", true)
        embedBuilder.addField(
            "Is NSFW",
            if (selectedInteraction.isNSFW) "${defaultSystemCommandEmojiProvider.tickEmoji().formatted} Yes" else "${defaultSystemCommandEmojiProvider.crossEmoji().formatted} No",
            true
        )

        if (selectedInteraction.type == Command.Type.SLASH) {
            embedBuilder.addField("Mention", "`${selectedInteraction.asMention}`", false)
        }

        val defaultPermissions = when (selectedInteraction.defaultPermissions) {
            DefaultMemberPermissions.ENABLED -> "${defaultSystemCommandEmojiProvider.tickEmoji().formatted} Everyone"
            DefaultMemberPermissions.DISABLED -> "${defaultSystemCommandEmojiProvider.crossEmoji().formatted} Administrators only"
            else -> "Only with:\n" + Permission.getPermissions(selectedInteraction.defaultPermissions.permissionsRaw ?: 0).joinToString("\n") { "└ ${it.name}" }
        }

        embedBuilder.addField("Default permissions", defaultPermissions, false)
        embedBuilder.addField("Contexts", selectedInteraction.contexts.joinToString("\n") { "└ ${it.name}" }, true)
        embedBuilder.addField("Integrations", selectedInteraction.integrationTypes.joinToString("\n") { "└ ${it.name}" }, true)

        if (selectedInteraction.options.isNotEmpty()) {
            val options = arrayListOf<String>()
            selectedInteraction.options.forEach { options.add(it.name) }
            embedBuilder.addFields(options.map { "└ $it" }.splitInFields("Options (${selectedInteraction.options.size})", false))
        }
        val totalSubcommands = selectedInteraction.subcommands.size + selectedInteraction.subcommandGroups.sumOf { it.subcommands.size }
        if (totalSubcommands > 0) {
            val subcommands = arrayListOf<String>()
            selectedInteraction.subcommands.forEach { subcommands.add(it.fullCommandName) }
            selectedInteraction.subcommandGroups.forEach { subcommands.addAll(it.subcommands.map { it.fullCommandName }) }

            embedBuilder.addFields(subcommands.map { "└ /$it" }.splitInFields("Subcommands (${totalSubcommands})", false))
        }

        val implementation = if (selectedInteraction.type == Command.Type.SLASH) {
            discordBot.commands.entries.firstOrNull { it.key == selectedInteraction.id }?.value
        } else {
            discordBot.contextMenus.entries.firstOrNull { it.key == selectedInteraction.id }?.value
        }

        if (implementation != null) {
            embedBuilder.addField("Implementation", "`${implementation.canonicalName}`", false)
        }

        latestHook!!.editOriginalEmbeds(embedBuilder.build()).setComponents(getActionRow())
            .queueAndRegisterInteraction(latestHook!!, command, type = arrayListOf(EventRegisterType.STRING_SELECT))
    }

    private fun showOptions() {
        val option = if (selectedSubcommandFullName != null) {
            (selectedInteraction.subcommands.firstOrNull { it.fullCommandName == selectedSubcommandFullName }?.options
                ?: selectedInteraction.subcommandGroups.flatMap { it.subcommands }.firstOrNull { it.fullCommandName == selectedSubcommandFullName }?.options
                ?: arrayListOf()).firstOrNull { it.name == selectedOptionName }
        } else {
            selectedInteraction.options.firstOrNull { it.name == selectedOptionName }
        }

        if (option == null) {
            embedBuilder.setDescription("## $interactionName - $selectedOptionName\nOption not found!")
            latestHook!!.editOriginalEmbeds(embedBuilder.build()).setComponents(getActionRow())
                .queueAndRegisterInteraction(latestHook!!, command, type = arrayListOf(EventRegisterType.STRING_SELECT))
            return
        }

        if (selectedSubcommandFullName != null) {
            embedBuilder.setDescription("## /$selectedSubcommandFullName - [${option.name}]\n${option.description}")
        } else {
            embedBuilder.setDescription("## $interactionName - [${option.name}]\n${option.description}")
        }

        embedBuilder.clearFields()

        embedBuilder.addField("Name", option.name, true)
        embedBuilder.addField("Type", "${option.type.name} (${option.type.key})", true)
        embedBuilder.addField(
            "Required",
            if (option.isRequired) "${defaultSystemCommandEmojiProvider.tickEmoji().formatted} Yes" else "${defaultSystemCommandEmojiProvider.crossEmoji().formatted} No",
            true
        )
        embedBuilder.addField(
            "AutoComplete",
            if (option.isAutoComplete) "${defaultSystemCommandEmojiProvider.tickEmoji().formatted} Yes" else "${defaultSystemCommandEmojiProvider.crossEmoji().formatted} No",
            true
        )

        if (option.maxValue != null) {
            embedBuilder.addField("Max value", option.maxValue.toString(), true)
        }
        if (option.minValue != null) {
            embedBuilder.addField("Min value", option.minValue.toString(), true)
        }
        if (option.maxLength != null) {
            embedBuilder.addField("Max length", option.maxLength.toString(), true)
        }
        if (option.minLength != null) {
            embedBuilder.addField("Min length", option.minLength.toString(), true)
        }
        if (option.type == OptionType.CHANNEL) {
            embedBuilder.addField("Channel types", option.channelTypes.joinToString("\n") { "└ ${it.name}" }, false)
        }
        if (option.choices.isNotEmpty()) {
            val choices = arrayListOf<String>()
            option.choices.forEach { choices.add(it.name) }
            embedBuilder.addFields(choices.map { "└ $it" }.splitInFields("Choices (${option.choices.size})", true))
        }

        latestHook!!.editOriginalEmbeds(embedBuilder.build()).setComponents(getActionRow())
            .queueAndRegisterInteraction(latestHook!!, command, type = arrayListOf(EventRegisterType.STRING_SELECT))
    }

    private fun showSubcommands() {
        val subcommand = (selectedInteraction.subcommands.firstOrNull { it.fullCommandName == selectedSubcommandFullName }
            ?: selectedInteraction.subcommandGroups.flatMap { it.subcommands }.firstOrNull { it.fullCommandName == selectedSubcommandFullName })

        if (subcommand == null) {
            embedBuilder.setDescription("## /$selectedSubcommandFullName\nSubcommand not found!")
            latestHook!!.editOriginalEmbeds(embedBuilder.build()).setComponents(getActionRow())
                .queueAndRegisterInteraction(latestHook!!, command, type = arrayListOf(EventRegisterType.STRING_SELECT))
            return
        }

        embedBuilder.setDescription("## /$selectedSubcommandFullName\n${subcommand.description}")
        embedBuilder.clearFields()

        embedBuilder.addField("Id", "`${subcommand.id}`", true)
        embedBuilder.addField("Name", subcommand.name, true)
        embedBuilder.addField("Created at", subcommand.timeCreated.toDiscordTimestamp(), true)
        embedBuilder.addField("Mention", "`${subcommand.asMention}`", false)

        if (subcommand.options.isNotEmpty()) {
            val options = arrayListOf<String>()
            subcommand.options.forEach { options.add(it.name) }
            embedBuilder.addFields(options.map { "└ $it" }.splitInFields("Options (${subcommand.options.size})", false))
        }

        latestHook!!.editOriginalEmbeds(embedBuilder.build()).setComponents(getActionRow())
            .queueAndRegisterInteraction(latestHook!!, command, type = arrayListOf(EventRegisterType.STRING_SELECT))
    }

    private fun getActionRow(): ArrayList<ActionRow> {
        val rows = arrayListOf<ActionRow>()

        val row1 = arrayListOf<ItemComponent>()
        val pageSelection = StringSelectMenu.create("page-selection")
        pageSelection.addOption("Overview", "overview")

        if (selectedInteraction.options.isNotEmpty()) {
            pageSelection.addOption("Options", "options")
        }
        if (selectedInteraction.subcommands.isNotEmpty() || selectedInteraction.subcommandGroups.isNotEmpty()) {
            pageSelection.addOption("Subcommands", "subcommands")
        }
        pageSelection.setDefaultValues(selectedPage)
        pageSelection.setMaxValues(1)
        row1.add(pageSelection.build())
        rows.add(ActionRow.of(row1))

        if (selectedPage == "options") {
            val row2 = arrayListOf<ItemComponent>()
            val optionSelection = StringSelectMenu.create("option-selection")
            selectedInteraction.options.forEach { option ->
                optionSelection.addOption(option.name, option.name)
            }
            optionSelection.setDefaultValues(selectedOptionName)
            optionSelection.setMaxValues(1)
            row2.add(optionSelection.build())
            rows.add(ActionRow.of(row2))
        }

        if (selectedPage == "subcommands") {
            val row2 = arrayListOf<ItemComponent>()
            val subcommandSelection = StringSelectMenu.create("subcommand-selection")

            selectedInteraction.subcommands.forEach { subcommand ->
                subcommandSelection.addOption(subcommand.fullCommandName, subcommand.fullCommandName)
            }
            if (selectedInteraction.subcommandGroups.isNotEmpty()) {
                selectedInteraction.subcommandGroups.forEach { subcommandGroup ->
                    subcommandGroup.subcommands.forEach { subcommand ->
                        subcommandSelection.addOption(subcommand.fullCommandName, subcommand.fullCommandName)
                    }
                }
            }
            subcommandSelection.setDefaultValues(selectedSubcommandFullName ?: "")
            subcommandSelection.setMaxValues(1)
            row2.add(subcommandSelection.build())
            rows.add(ActionRow.of(row2))

            val options = (selectedInteraction.subcommands.firstOrNull { it.fullCommandName == selectedSubcommandFullName }?.options
                ?: selectedInteraction.subcommandGroups.flatMap { it.subcommands }.firstOrNull { it.fullCommandName == selectedSubcommandFullName }?.options
                ?: arrayListOf())

            if (options.isNotEmpty()) {
                val row3 = arrayListOf<ItemComponent>()
                val optionSelection = StringSelectMenu.create("option-selection")
                optionSelection.addOption("Overview", "overview")

                options.forEach { option ->
                    optionSelection.addOption(option.name, option.name)
                }
                optionSelection.setDefaultValues(selectedOptionName ?: "overview")
                optionSelection.setMaxValues(1)
                row3.add(optionSelection.build())
                rows.add(ActionRow.of(row3))
            }
        }

        return rows
    }

    override fun onStringSelectMenuInteraction(event: StringSelectInteractionEvent): Boolean {
        latestHook = event.deferEdit().complete()

        when (event.interaction.componentId) {
            "page-selection" -> {
                selectedPage = event.getSelection()
                when (event.values.first()) {
                    "overview" -> showOverview()
                    "options" -> showOptions()
                    "subcommands" -> showSubcommands()
                }
            }

            "option-selection" -> {
                selectedOptionName = event.getSelection()
                if (selectedSubcommandFullName != null && selectedOptionName == "overview") {
                    showSubcommands()
                } else {
                    showOptions()
                }

            }

            "subcommand-selection" -> {
                selectedSubcommandFullName = event.getSelection()
                selectedOptionName = null
                showSubcommands()
            }
        }

        return true
    }

    override fun onStringSelectInteractionTimeout() {
        latestHook?.editOriginalEmbeds(embedBuilder.build())?.removeComponents()?.queue()
    }

    override fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent, command: SystemCommand) {
        val interactions = hashMapOf<InteractionId, String>()
        interactions.putAll(discordBot.discordRepresentations.entries.filter { it.value.type == Command.Type.SLASH }.associate { Pair(it.key, "/${it.value.name}") })
        interactions.putAll(discordBot.discordRepresentations.entries.filter { it.value.type != Command.Type.SLASH }.associate { Pair(it.key, it.value.name) })

        val filtered = interactions.filter { it.value.lowercase().contains(event.getTypedOption(command.argsOption, "")!!.lowercase()) }
        event.replyChoices(filtered.entries.take(25).map { Command.Choice(it.value, it.key) }).queue()
    }
}
