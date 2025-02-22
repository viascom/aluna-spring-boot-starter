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
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.command.systemcommand.adminsearch.AdminSearchArgsProvider
import io.viascom.discord.bot.aluna.bot.command.systemcommand.adminsearch.AdminSearchPageDataProvider
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.util.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component
import java.awt.Color

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class AdminSearchDataProvider(
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val adminSearchPageDataProviders: List<AdminSearchPageDataProvider>,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider,
    private val adminSearchArgsProviders: List<AdminSearchArgsProvider>
) : SystemCommandDataProvider(
    "admin_search",
    "Admin Search",
    true,
    true,
    true,
    false
) {

    private lateinit var lastHook: InteractionHook
    private lateinit var lastEmbed: EmbedBuilder
    private lateinit var selectedType: AdminSearchType
    private lateinit var systemCommand: SystemCommand

    private lateinit var discordUser: User
    private lateinit var discordServer: Guild
    private lateinit var discordRole: Role
    private lateinit var discordChannel: Channel
    private lateinit var discordEmote: RichCustomEmoji
    private lateinit var discordInteractionCommand: Command

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        systemCommand = command
        val id = event.getTypedOption(command.argsOption, "")!!

        if (id.isEmpty()) {
            val idModal = Modal.create("admin_search_id_modal", "Admin Search")
                .addTextField("id", "ID to search")
                .build()
            event.replyModal(idModal).queueAndRegisterInteraction(command, arrayListOf(EventRegisterType.MODAL))
            return
        }

        lastHook = event.deferReply().complete()
        handleSearch(id)
    }

    private fun handleSearch(id: String) {
        if (id.isEmpty()) {
            lastHook.editOriginal("${systemCommandEmojiProvider.crossEmoji().formatted} Please specify an ID as argument for this command").queue()
            return
        }

        lastEmbed = EmbedBuilder()
            .setColor(Color.MAGENTA)
            .setTitle("Admin Search")
            .setDescription("${systemCommandEmojiProvider.loadingEmoji().formatted} Searching for `${id}`...")
        lastHook.editOriginalEmbeds(lastEmbed.build()).queue()

        //======= User =======
        val optionalDiscordUser = checkForUser(id)
        if (optionalDiscordUser.isNotEmpty()) {
            discordUser = optionalDiscordUser.first()
            selectedType = AdminSearchType.USER
            generateDiscordUser(discordUser)
            lastHook.editOriginalEmbeds(lastEmbed.build()).setComponents(getDiscordMenu(AdminSearchType.USER))
                .queueAndRegisterInteraction(lastHook, systemCommand, arrayListOf(EventRegisterType.STRING_SELECT), true)
            return
        }

        //======= Server =======
        val optionalDiscordServer = checkForServer(id)
        if (optionalDiscordServer != null) {
            discordServer = optionalDiscordServer
            selectedType = AdminSearchType.SERVER
            generateDiscordServer(discordServer)
            lastHook.editOriginalEmbeds(lastEmbed.build()).setComponents(getDiscordMenu(AdminSearchType.SERVER))
                .queueAndRegisterInteraction(lastHook, systemCommand, arrayListOf(EventRegisterType.STRING_SELECT), true)
            return
        }

        //======= Role =======
        val optionalDiscordRole = checkForRole(id)
        if (optionalDiscordRole != null) {
            discordRole = optionalDiscordRole
            selectedType = AdminSearchType.ROLE
            generateDiscordRole(discordRole)
            lastHook.editOriginalEmbeds(lastEmbed.build()).setComponents(getDiscordMenu(AdminSearchType.ROLE))
                .queueAndRegisterInteraction(lastHook, systemCommand, arrayListOf(EventRegisterType.STRING_SELECT), true)
            return
        }

        //======= Channel =======
        val optionalDiscordChannel = checkForChannel(id)
        if (optionalDiscordChannel != null) {
            discordChannel = optionalDiscordChannel
            selectedType = AdminSearchType.CHANNEL
            generateDiscordChannel(discordChannel)
            lastHook.editOriginalEmbeds(lastEmbed.build()).setComponents(getDiscordMenu(AdminSearchType.CHANNEL))
                .queueAndRegisterInteraction(lastHook, systemCommand, arrayListOf(EventRegisterType.STRING_SELECT), true)
            return
        }

        //======= Emote =======
        val optionalDiscordEmote = checkForEmoji(id)?.firstOrNull()
        if (optionalDiscordEmote != null) {
            discordEmote = optionalDiscordEmote
            selectedType = AdminSearchType.EMOTE
            generateDiscordEmote(discordEmote)
            lastHook.editOriginalEmbeds(lastEmbed.build()).setComponents(getDiscordMenu(AdminSearchType.EMOTE))
                .queueAndRegisterInteraction(lastHook, systemCommand, arrayListOf(EventRegisterType.STRING_SELECT), true)
            return
        }

        //======= Interaction =======
        val optionalDiscordInteraction = checkForInteractionCommand(id)?.firstOrNull()
        if (optionalDiscordInteraction != null) {
            discordInteractionCommand = optionalDiscordInteraction
            selectedType = AdminSearchType.INTERACTION_COMMAND
            generateDiscordInteractionCommand(discordInteractionCommand)
            lastHook.editOriginalEmbeds(lastEmbed.build()).setComponents(getDiscordMenu(AdminSearchType.INTERACTION_COMMAND))
                .queueAndRegisterInteraction(lastHook, systemCommand, arrayListOf(EventRegisterType.STRING_SELECT), true)
            return
        }

        //======= FOUND NOTHING =======
        lastEmbed.clearFields()
        lastEmbed.setColor(Color.RED)
        lastEmbed.setDescription("${systemCommandEmojiProvider.crossEmoji().formatted} Could not find an object with id: **${id}**")
        lastHook.editOriginalEmbeds(lastEmbed.build()).queue()
    }

    override fun onStringSelectMenuInteraction(event: StringSelectInteractionEvent): Boolean {
        lastHook = event.deferEdit().complete()
        val page = event.getSelection()

        when (selectedType) {
            AdminSearchType.USER -> generateDiscordUser(discordUser, page)
            AdminSearchType.SERVER -> generateDiscordServer(discordServer, page)
            AdminSearchType.ROLE -> generateDiscordRole(discordRole, page)
            AdminSearchType.CHANNEL -> generateDiscordChannel(discordChannel, page)
            AdminSearchType.EMOTE -> generateDiscordEmote(discordEmote, page)
            AdminSearchType.INTERACTION_COMMAND -> generateDiscordInteractionCommand(discordInteractionCommand, page)
        }

        val actionRows = getDiscordMenu(selectedType, page)
        lastHook.editOriginalEmbeds(lastEmbed.build()).setComponents(actionRows).queue()

        return true
    }

    override fun onStringSelectInteractionTimeout() {
        lastHook.editOriginalEmbeds(lastEmbed.build()).removeComponents().queue()
    }

    override fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        lastHook = event.deferReply().complete()
        handleSearch(event.interaction.getValue("id")?.asString ?: "")
        return true
    }

    private fun checkForUser(id: String) = try {
        arrayListOf(shardManager.retrieveUserById(id).complete())
    } catch (e: Exception) {
        try {
            arrayListOf(shardManager.getUserByTag(id))
        } catch (e: Exception) {
            shardManager.userCache.filter { it.name.lowercase().contains(id.lowercase()) }
        }
    }

    private fun checkForServer(id: String): Guild? {
        return if (id.isEmpty()) {
            null
        } else {
            try {
                shardManager.getGuildById(id)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun checkForRole(id: String): Role? {
        return if (id.isEmpty()) {
            null
        } else {
            try {
                shardManager.getRoleById(id)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun checkForChannel(id: String): GuildChannel? {
        return if (id.isEmpty()) {
            null
        } else {
            try {
                shardManager.getChannelById<GuildChannel>(GuildChannel::class.java, id) as GuildChannel
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun checkForEmoji(id: String): List<RichCustomEmoji>? {
        return if (id.isEmpty()) {
            null
        } else {
            try {
                shardManager.getEmojiById(id)?.let { arrayListOf(it) }
            } catch (e: Exception) {
                shardManager.emojis.filter { it.name.lowercase().contains(id) }
            }
        }
    }

    private fun checkForInteractionCommand(id: String): List<Command>? {
        return if (id.isEmpty()) {
            null
        } else {
            val commands = discordBot.discordRepresentations.values.flatMap { command ->
                val elements = arrayListOf<Pair<Command, String>>()
                elements.add(Pair(command, command.fullCommandName))
                elements.addAll(command.subcommands.map { Pair(command, it.fullCommandName) })
                elements.addAll(command.subcommandGroups.flatMap { group -> group.subcommands.map { Pair(command, it.fullCommandName) } })
                elements
            }

            discordBot.discordRepresentations.values.firstOrNull { it.id == id }?.let { arrayListOf(it) } ?: commands.filter {
                it.second.lowercase().contains(id.lowercase())
            }.map { it.first }
        }
    }

    private fun generateDiscordUser(discordUser: User, page: String = "OVERVIEW") {
        lastEmbed.clearFields()
        lastEmbed.setDescription("Found Discord User **${discordUser.name}**\nwith ID: ``${discordUser.id}``")
        lastEmbed.setThumbnail(discordUser.avatarUrl)
        lastEmbed.setFooter(null)
        lastEmbed.setImage(null)

        adminSearchPageDataProviders.firstOrNull { it.supportedTypes.contains(AdminSearchType.USER) && it.pageId == page }?.onUserRequest(discordUser, lastEmbed)
    }

    private fun generateDiscordServer(discordServer: Guild, page: String = "OVERVIEW") {
        lastEmbed.clearFields()
        lastEmbed.setDescription("Found Discord Server **${discordServer.name}**\nwith ID: ``${discordServer.id}``")
        lastEmbed.setThumbnail(discordServer.iconUrl)
        lastEmbed.setFooter(null)
        lastEmbed.setImage(null)

        adminSearchPageDataProviders.firstOrNull { it.supportedTypes.contains(AdminSearchType.SERVER) && it.pageId == page }?.onServerRequest(discordServer, lastEmbed)
    }

    private fun generateDiscordRole(discordRole: Role, page: String = "OVERVIEW") {
        lastEmbed.clearFields()
        lastEmbed.setDescription("Found Discord Role **${discordRole.name}**\nwith ID: ``${discordRole.id}``")
        lastEmbed.setFooter(null)
        lastEmbed.setImage(null)
        discordRole.icon?.let { lastEmbed.setThumbnail(it.iconUrl) }

        adminSearchPageDataProviders.firstOrNull { it.supportedTypes.contains(AdminSearchType.ROLE) && it.pageId == page }?.onRoleRequest(discordRole, lastEmbed)
    }

    private fun generateDiscordChannel(discordChannel: Channel, page: String = "OVERVIEW") {
        lastEmbed.clearFields()
        lastEmbed.setDescription("Found Discord Channel **${discordChannel.name}**\nwith ID: ``${discordChannel.id}``")
        lastEmbed.setFooter(null)
        lastEmbed.setImage(null)

        adminSearchPageDataProviders.firstOrNull { it.supportedTypes.contains(AdminSearchType.CHANNEL) && it.pageId == page }?.onChannelRequest(discordChannel, lastEmbed)
    }

    private fun generateDiscordEmote(discordEmote: RichCustomEmoji, page: String = "OVERVIEW") {
        lastEmbed.clearFields()
        lastEmbed.setDescription("Found Discord Emote **${discordEmote.name}**\nwith ID: ``${discordEmote.id}``")
        lastEmbed.setThumbnail(discordEmote.imageUrl)
        lastEmbed.setFooter(null)
        lastEmbed.setImage(null)

        adminSearchPageDataProviders.firstOrNull { it.supportedTypes.contains(AdminSearchType.EMOTE) && it.pageId == page }?.onEmoteRequest(discordEmote, lastEmbed)
    }

    private fun generateDiscordInteractionCommand(discordInteraction: Command, page: String = "OVERVIEW") {
        lastEmbed.clearFields()
        lastEmbed.setDescription("Found Discord Interaction **${discordInteraction.name}**\nwith ID: ``${discordInteraction.id}``")
        lastEmbed.setFooter(null)
        lastEmbed.setImage(null)
        lastEmbed.clearFields()

        adminSearchPageDataProviders.firstOrNull { it.supportedTypes.contains(AdminSearchType.INTERACTION_COMMAND) && it.pageId == page }
            ?.onInteractionCommandRequest(discordInteraction, lastEmbed)
    }

    private fun getDiscordMenu(type: AdminSearchType, page: String = "OVERVIEW"): ActionRow {
        val menu = StringSelectMenu.create("menu:type").setRequiredRange(1, 1)

        val pages = adminSearchPageDataProviders.filter { it.supportedTypes.contains(type) }.sortedBy { it.pageName }.toCollection(arrayListOf())
        val overviewPage = pages.first { it.pageId == "OVERVIEW" }
        pages.remove(overviewPage)
        pages.add(0, overviewPage)
        pages.forEach {
            menu.addOptions(SelectOption.of(it.pageName, it.pageId).withDefault(it.pageId == page))
        }
        return ActionRow.of(menu.build())
    }

    override fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent, command: SystemCommand) {
        val arg = event.getTypedOption(command.argsOption, "")!!

        val responses = hashMapOf<Command.Choice, Double>()

        val users = checkForUser(arg)
        if (users.isNotEmpty()) {
            users.forEach {
                responses[Command.Choice(it.name + " (User)", it.id)] = levenshteinDistance(it.name, arg)
            }
        }

        val server = checkForServer(arg)
        if (server != null) {
            responses[Command.Choice(server.name + " (Server)", server.id)] = 1.0
        }

        val role = checkForRole(arg)
        if (role != null) {
            responses[Command.Choice(role.name + " (Role)", role.id)] = 1.0
        }

        val channel = checkForChannel(arg)
        if (channel != null) {
            responses[Command.Choice(channel.name + " (Channel)", channel.id)] = 1.0
        }

        val emojis = checkForEmoji(arg)
        if (!emojis.isNullOrEmpty()) {
            emojis.forEach {
                responses[Command.Choice(it.name + " (Emote) (${it.guild.name})", it.id)] = levenshteinDistance(it.name, arg)
            }
        }

        val interactionsCommand = checkForInteractionCommand(arg)
        if (!interactionsCommand.isNullOrEmpty()) {
            interactionsCommand.forEach {
                responses[Command.Choice(it.name + " (Interaction)", it.id)] = levenshteinDistance(it.name, arg)
            }
        }

        val customArgsProvider = adminSearchArgsProviders.flatMap { it.onArgsRequest(arg).entries }
        if (customArgsProvider.isNotEmpty()) {
            customArgsProvider.forEach {
                responses[it.key] = it.value
            }
        }

        if (responses.isNotEmpty()) {
            event.replyChoices(responses.entries.sortedByDescending { it.value }.map { it.key }.take(25)).queue()
            return
        }

        val possibleServers = shardManager.guilds.filter { it.name.lowercase().contains(arg.lowercase()) }.take(10).map {
            Command.Choice(it.name, it.id)
        }
        val possibleUsers = shardManager.userCache.filter { it.name.lowercase().contains(arg.lowercase()) }.take(15).map {
            Command.Choice(it.name, it.id)
        }

        if (possibleServers.isNotEmpty() || possibleUsers.isNotEmpty()) {
            val list = arrayListOf<Command.Choice>()
            list.addAll(possibleServers)
            list.addAll(possibleUsers)
            event.replyChoices(list.sortedByDescending { it.asString }).queue()
            return
        }

        event.replyChoices().queue()
    }

    enum class AdminSearchType {
        USER, SERVER, ROLE, CHANNEL, EMOTE, INTERACTION_COMMAND
    }
}
