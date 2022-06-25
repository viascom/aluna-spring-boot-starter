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

package io.viascom.discord.bot.aluna.bot.command.systemcommand

import io.viascom.discord.bot.aluna.bot.Command
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.command.systemcommand.adminsearch.AdminSearchPageDataProvider
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.util.getSelection
import io.viascom.discord.bot.aluna.util.getTypedOption
import io.viascom.discord.bot.aluna.util.removeActionRows
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.sharding.ShardManager
import java.awt.Color

@Command
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class AdminSearchDataProvider(
    private val shardManager: ShardManager,
    private val adminSearchPageDataProviders: List<AdminSearchPageDataProvider>,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : SystemCommandDataProvider(
    "admin_search",
    "Admin Search",
    true,
    true,
    true
) {

    lateinit var lastHook: InteractionHook
    lateinit var lastEmbed: EmbedBuilder
    lateinit var selectedType: AdminSearchType

    lateinit var discordUser: User
    lateinit var discordServer: Guild
    lateinit var discordRole: Role
    lateinit var discordChannel: Channel
    lateinit var discordEmote: Emote

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        lastHook = hook!!

        val id = event.getTypedOption(command.argsOption, "")!!
        if (id.isEmpty()) {
            lastHook.editOriginal("${systemCommandEmojiProvider.crossEmoji().asMention} Please specify an ID as argument for this command").queue()
            return
        }

        lastEmbed = EmbedBuilder()
            .setColor(Color.MAGENTA)
            .setTitle("Admin Search")
            .setDescription("${systemCommandEmojiProvider.loadingEmoji().asMention} Searching for `${id}`...")
        lastHook.editOriginalEmbeds(lastEmbed.build()).complete()

        //======= User =======
        val optionalDiscordUser = checkForUser(id)
        if (optionalDiscordUser != null) {
            discordUser = optionalDiscordUser
            selectedType = AdminSearchType.USER
            generateDiscordUser(discordUser)
            lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(getDiscordMenu(AdminSearchType.USER))
                .queueAndRegisterInteraction(lastHook, command, arrayListOf(EventRegisterType.SELECT), true)
            return
        }

        //======= Server =======
        val optionalDiscordServer = checkForServer(id)
        if (optionalDiscordServer != null) {
            discordServer = optionalDiscordServer
            selectedType = AdminSearchType.SERVER
            generateDiscordServer(discordServer)
            lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(getDiscordMenu(AdminSearchType.SERVER))
                .queueAndRegisterInteraction(lastHook, command, arrayListOf(EventRegisterType.SELECT), true)
            return
        }

        //======= Role =======
        val optionalDiscordRole = checkForRole(id)
        if (optionalDiscordRole != null) {
            discordRole = optionalDiscordRole
            selectedType = AdminSearchType.ROLE
            generateDiscordRole(discordRole)
            lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(getDiscordMenu(AdminSearchType.ROLE))
                .queueAndRegisterInteraction(lastHook, command, arrayListOf(EventRegisterType.SELECT), true)
            return
        }

        //======= Channel =======
        val optionalDiscordChannel = checkForChannel(id)
        if (optionalDiscordChannel != null) {
            discordChannel = optionalDiscordChannel
            selectedType = AdminSearchType.CHANNEL
            generateDiscordChannel(discordChannel)
            lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(getDiscordMenu(AdminSearchType.CHANNEL))
                .queueAndRegisterInteraction(lastHook, command, arrayListOf(EventRegisterType.SELECT), true)
            return
        }

        //======= Emote =======
        val optionalDiscordEmote = checkForEmote(id)?.firstOrNull()
        if (optionalDiscordEmote != null) {
            discordEmote = optionalDiscordEmote
            selectedType = AdminSearchType.EMOTE
            generateDiscordEmote(discordEmote)
            lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(getDiscordMenu(AdminSearchType.EMOTE))
                .queueAndRegisterInteraction(lastHook, command, arrayListOf(EventRegisterType.SELECT), true)
            return
        }
    }

    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent): Boolean {
        lastHook = event.deferEdit().complete()
        val page = event.getSelection()

        when (selectedType) {
            AdminSearchType.USER -> generateDiscordUser(discordUser, page)
            AdminSearchType.SERVER -> generateDiscordServer(discordServer, page)
            AdminSearchType.ROLE -> generateDiscordRole(discordRole, page)
            AdminSearchType.CHANNEL -> generateDiscordChannel(discordChannel, page)
            AdminSearchType.EMOTE -> generateDiscordEmote(discordEmote, page)
        }

        val actionRows = getDiscordMenu(selectedType, page)
        lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(actionRows).queue()

        return true
    }

    override fun onSelectMenuInteractionTimeout() {
        lastHook.editOriginalEmbeds(lastEmbed.build()).removeActionRows().queue()
    }

    private fun checkForUser(id: String) = try {
        shardManager.retrieveUserById(id).complete()
    } catch (e: Exception) {
        try {
            shardManager.getUserByTag(id)
        } catch (e: Exception) {
            null
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
                shardManager.getChannelById(GuildChannel::class.java, id)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun checkForEmote(id: String): List<Emote>? {
        return if (id.isEmpty()) {
            null
        } else {
            try {
                shardManager.guilds.first().emoteCache
                shardManager.getEmoteById(id)?.let { arrayListOf(it) }
            } catch (e: Exception) {
                shardManager.getEmotesByName(id, false)
            }
        }
    }

    private fun generateDiscordUser(discordUser: User, page: String = "OVERVIEW") {
        lastEmbed.clearFields()
        lastEmbed.setDescription("Found Discord User **${discordUser.asTag}**\nwith ID: ``${discordUser.id}``")
        lastEmbed.setThumbnail(discordUser.avatarUrl)

        adminSearchPageDataProviders.firstOrNull { it.supportedTypes.contains(AdminSearchType.USER) && it.pageId == page }
            ?.onUserRequest(discordUser, lastEmbed)
    }

    private fun generateDiscordServer(discordServer: Guild, page: String = "OVERVIEW") {
        lastEmbed.clearFields()
        lastEmbed.setDescription("Found Discord Server **${discordServer.name}**\nwith ID: ``${discordServer.id}``")
        lastEmbed.setThumbnail(discordServer.iconUrl)

        adminSearchPageDataProviders.firstOrNull { it.supportedTypes.contains(AdminSearchType.SERVER) && it.pageId == page }
            ?.onServerRequest(discordServer, lastEmbed)
    }

    private fun generateDiscordRole(discordRole: Role, page: String = "OVERVIEW") {
        lastEmbed.clearFields()
        lastEmbed.setDescription("Found Discord Role **${discordRole.name}**\nwith ID: ``${discordRole.id}``")
        discordRole.icon?.let { lastEmbed.setThumbnail(it.iconUrl) }

        adminSearchPageDataProviders.firstOrNull { it.supportedTypes.contains(AdminSearchType.ROLE) && it.pageId == page }
            ?.onRoleRequest(discordRole, lastEmbed)
    }

    private fun generateDiscordChannel(discordChannel: Channel, page: String = "OVERVIEW") {
        lastEmbed.clearFields()
        lastEmbed.setDescription("Found Discord Channel **${discordChannel.name}**\nwith ID: ``${discordChannel.id}``")

        adminSearchPageDataProviders.firstOrNull { it.supportedTypes.contains(AdminSearchType.CHANNEL) && it.pageId == page }
            ?.onChannelRequest(discordChannel, lastEmbed)
    }

    private fun generateDiscordEmote(discordEmote: Emote, page: String = "OVERVIEW") {
        lastEmbed.clearFields()
        lastEmbed.setDescription("Found Discord Emote **${discordEmote.name}**\nwith ID: ``${discordEmote.id}``")
        lastEmbed.setThumbnail(discordEmote.imageUrl)

        adminSearchPageDataProviders.firstOrNull { it.supportedTypes.contains(AdminSearchType.EMOTE) && it.pageId == page }
            ?.onEmoteRequest(discordEmote, lastEmbed)
    }

    private fun getDiscordMenu(type: AdminSearchType, page: String = "OVERVIEW"): ActionRow {
        val menu = SelectMenu.create("menu:type")
            .setRequiredRange(1, 1)

        adminSearchPageDataProviders.filter { it.supportedTypes.contains(type) }.forEach {
            menu.addOptions(SelectOption.of(it.pageName, it.pageId).withDefault(it.pageId == page))
        }
        return ActionRow.of(menu.build())
    }

    override fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent, command: SystemCommand) {
        val arg = event.getTypedOption(command.argsOption, "")!!

        val user = checkForUser(arg)
        if (user != null) {
            event.replyChoices(net.dv8tion.jda.api.interactions.commands.Command.Choice(user.asTag + " (User)", arg)).queue()
            return
        }

        val server = checkForServer(arg)
        if (server != null) {
            event.replyChoices(net.dv8tion.jda.api.interactions.commands.Command.Choice(server.name + " (Server)", arg)).queue()
            return
        }

        val role = checkForRole(arg)
        if (role != null) {
            event.replyChoices(net.dv8tion.jda.api.interactions.commands.Command.Choice(role.name + " (Role)", arg)).queue()
            return
        }

        val channel = checkForChannel(arg)
        if (channel != null) {
            event.replyChoices(net.dv8tion.jda.api.interactions.commands.Command.Choice(channel.name + " (Channel)", arg)).queue()
            return
        }

        val emotes = checkForEmote(arg)
        if (emotes != null && emotes.isNotEmpty()) {
            event.replyChoices(emotes.map { net.dv8tion.jda.api.interactions.commands.Command.Choice(it.name + " (Emote) (${it.guild?.name ?: ""})", it.id) })
                .queue()
            return
        }

        val possibleServers = shardManager.guilds.filter { it.name.lowercase().contains(arg.lowercase()) }.take(10).map {
            net.dv8tion.jda.api.interactions.commands.Command.Choice(it.name, it.id)
        }
        val possibleUsers = shardManager.userCache.filter { it.asTag.lowercase().contains(arg.lowercase()) }.take(15).map {
            net.dv8tion.jda.api.interactions.commands.Command.Choice(it.asTag, it.id)
        }

        if (possibleServers.isNotEmpty() || possibleUsers.isNotEmpty()) {
            val list = arrayListOf<net.dv8tion.jda.api.interactions.commands.Command.Choice>()
            list.addAll(possibleServers)
            list.addAll(possibleUsers)
            event.replyChoices(list).queue()
            return
        }

        event.replyChoices().queue()
    }

    enum class AdminSearchType {
        USER, SERVER, ROLE, CHANNEL, EMOTE
    }
}
