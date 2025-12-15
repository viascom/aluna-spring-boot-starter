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

package io.viascom.discord.bot.aluna.bot.command.systemcommand.adminsearch

import io.viascom.discord.bot.aluna.bot.command.systemcommand.AdminSearchDataProvider
import io.viascom.discord.bot.aluna.bot.command.systemcommand.SystemCommandEmojiProvider
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.TimestampFormat
import io.viascom.discord.bot.aluna.util.round
import io.viascom.discord.bot.aluna.util.toDiscordTimestamp
import io.viascom.discord.bot.aluna.util.toHex
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
public class AdminSearchOverviewPage(
    private val shardManager: ShardManager, private val alunaProperties: AlunaProperties, private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : AdminSearchPageDataProvider(
    "OVERVIEW", "Overview", arrayListOf(
        AdminSearchDataProvider.AdminSearchType.USER,
        AdminSearchDataProvider.AdminSearchType.SERVER,
        AdminSearchDataProvider.AdminSearchType.ROLE,
        AdminSearchDataProvider.AdminSearchType.CHANNEL,
        AdminSearchDataProvider.AdminSearchType.EMOTE,
        AdminSearchDataProvider.AdminSearchType.INTERACTION_COMMAND
    )
) {

    override fun onUserRequest(discordUser: User, embedBuilder: EmbedBuilder) {
        val mutualServers = shardManager.getMutualGuilds(discordUser)

        embedBuilder.addField("Discord-ID", discordUser.id, true).addField("Discord-Tag", discordUser.name, true)
            .addField("Discord-Mention", discordUser.asMention, true)
        if (alunaProperties.discord.gatewayIntents.any { it == GatewayIntent.GUILD_MEMBERS }) {
            val localeMap = discordUser.mutualGuilds.groupBy { it.locale }
            val probableLocales = localeMap.entries.sortedByDescending { it.value.size }.take(3)

            embedBuilder.addField(
                "Probable Locale",
                probableLocales.joinToString("\n") { "${it.key.languageName} - ${((0.0 + it.value.size) / localeMap.values.flatten().size * 100).round(1)}%" },
                true
            )
        }
        embedBuilder.addField(
            "Is App", (if (discordUser.isBot) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted, true
        ).addField("Flags", discordUser.flags.joinToString(", ") { it.getName() }, true)
            .addField("Time Created", discordUser.timeCreated.toDiscordTimestamp(TimestampFormat.SHORT_DATE_TIME), true).addField(
                "On Support Server",
                (if (mutualServers.any { it.id == alunaProperties.discord.supportServer }) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
                true
            ).addField("Avatar-URL", "[Link](${discordUser.effectiveAvatarUrl})", true)

        val profile = discordUser.retrieveProfile().complete()

        profile.bannerUrl?.let {
            embedBuilder.addField(
                "Banner-URL", "[Link](${discordUser.retrieveProfile().complete().bannerUrl})", true
            )
        }
        profile.accentColor?.let {
            embedBuilder.addField(
                "Accent Color", "`${discordUser.retrieveProfile().complete().accentColor?.toHex() ?: "n/a"}`", true
            )
        }

        embedBuilder.setImage(profile.bannerUrl)
    }

    override fun onServerRequest(discordServer: Guild, embedBuilder: EmbedBuilder) {
        embedBuilder.addField("ID", discordServer.id, true)
        embedBuilder.addField("Name", discordServer.name, true)
        embedBuilder.addField(
            "Owner",
            "${discordServer.owner?.asMention} | ${discordServer.owner?.effectiveName} (`${discordServer.ownerId}`)\n" + "Owner on Support Server: " + (if (discordServer.owner?.user?.mutualGuilds?.any { it.id == alunaProperties.discord.supportServer } == true) systemCommandEmojiProvider.tickEmoji().formatted + " Yes" else systemCommandEmojiProvider.crossEmoji().formatted + " No"),
            false)
        embedBuilder.addField("Locale", discordServer.locale.languageName, true)
        if (alunaProperties.discord.gatewayIntents.any { it == GatewayIntent.GUILD_MEMBERS }) {
            embedBuilder.addField("Members", discordServer.memberCount.toString(), true)
        }
        embedBuilder.addField("Channels", discordServer.channels.size.toString(), true)
        embedBuilder.addField("Roles", discordServer.roles.size.toString(), true)
        if (discordServer.vanityCode != null) {
            embedBuilder.addField("Vanity-Code", "${discordServer.vanityCode} | `${discordServer.vanityUrl}`", true)
        }
        embedBuilder.addField("Time Created", discordServer.timeCreated.toDiscordTimestamp(TimestampFormat.SHORT_DATE_TIME), true)
        embedBuilder.addField(
            "App join-time", discordServer.selfMember.timeJoined.toDiscordTimestamp(TimestampFormat.SHORT_DATE_TIME), true
        )
        embedBuilder.addField("In-Server-Name", discordServer.selfMember.effectiveName, true)
        embedBuilder.addField("Features", discordServer.features.joinToString(" | "), false)

        if (alunaProperties.discord.gatewayIntents.any { it == GatewayIntent.GUILD_MEMBERS }) {
            val otherBots = discordServer.loadMembers().get().filter { it.user.isBot }.joinToString(", ") { it.user.name }
            if (otherBots.length > MessageEmbed.VALUE_MAX_LENGTH) {
                embedBuilder.addField("Other Apps", "Server has ${otherBots.length} apps", false)
            } else {
                embedBuilder.addField("Other Apps", otherBots, false)
            }
        }
    }

    override fun onRoleRequest(discordRole: Role, embedBuilder: EmbedBuilder) {
        embedBuilder.addField("ID", "`${discordRole.id}`", true)
        embedBuilder.addField("Name", discordRole.name, true)
        embedBuilder.addField("Time Created", discordRole.timeCreated.toDiscordTimestamp(TimestampFormat.SHORT_DATE_TIME), true)
        embedBuilder.addField("Server", "${discordRole.guild.name} (`${discordRole.guild.id}`)", false)
        embedBuilder.addField(
            "Is below App",
            if (discordRole.positionRaw < discordRole.guild.selfMember.roles.maxByOrNull { it.positionRaw }!!.positionRaw) systemCommandEmojiProvider.tickEmoji().formatted else systemCommandEmojiProvider.crossEmoji().formatted,
            true
        )
        embedBuilder.addField(
            "App can interact",
            if (discordRole.guild.selfMember.roles.maxByOrNull { it.positionRaw }!!
                    .canInteract(discordRole)
            ) systemCommandEmojiProvider.tickEmoji().formatted else systemCommandEmojiProvider.crossEmoji().formatted,
            true
        )
        embedBuilder.addField(
            "Is Hoisted", (if (discordRole.isHoisted) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted, true
        )
        embedBuilder.addField(
            "Is Managed", (if (discordRole.isManaged) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted, true
        )
        embedBuilder.addField(
            "Is Mentionable",
            (if (discordRole.isMentionable) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
            true
        )
        embedBuilder.addField(
            "Has Admin permission",
            (if (discordRole.permissions.contains(Permission.ADMINISTRATOR)) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
            true
        )
        embedBuilder.addField(
            "Is from a App", (if (discordRole.tags.isBot) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted, true
        )
        discordRole.tags.botId?.let {
            embedBuilder.addField("Assigned App", "${shardManager.getUserById(it)?.name ?: "n/a"} (`${it}`)", true)
            embedBuilder.addBlankField(true)
        }
        embedBuilder.addField(
            "Is Boost Role", (if (discordRole.tags.isBoost) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted, true
        )
        embedBuilder.addField(
            "Is Integration Role",
            (if (discordRole.tags.isIntegration) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
            true
        )
        discordRole.tags.integrationId?.let {
            embedBuilder.addField(
                "Assigned Integration", it, true
            )
            embedBuilder.addBlankField(true)
        }

        val primaryHex = (if (discordRole.colors.primary != null) "`${discordRole.colors.primary!!.toHex()}`" else "n/a")
        val secondaryHex = (if (discordRole.colors.secondary != null) "`${discordRole.colors.secondary!!.toHex()}`" else "n/a")
        val tertiaryHex = (if (discordRole.colors.tertiary != null) "`${discordRole.colors.tertiary!!.toHex()}`" else "n/a")
        val type = when {
            discordRole.colors.isDefault -> "Default"
            discordRole.colors.isSolid -> "Solid"
            discordRole.colors.isGradient -> "Gradient"
            discordRole.colors.isHolographic -> "Holographic"
            else -> "Unknown"
        }
        embedBuilder.addField("Color", "Type: $type\nPrimary: $primaryHex\nSecondary: $secondaryHex\nTertiary: $tertiaryHex", true)

        val memberCount = discordRole.guild.members.count { it.roles.contains(discordRole) }
        embedBuilder.addField(
            "Member-Count", memberCount.toString() + " (${(memberCount.toDouble() / discordRole.guild.members.size * 100).round(2)}%)", false
        )
    }

    override fun onChannelRequest(discordChannel: Channel, embedBuilder: EmbedBuilder) {
        embedBuilder.addField("ID", discordChannel.id, true)
        embedBuilder.addField("Name", discordChannel.name, true)
        embedBuilder.addField(
            "Type", when (discordChannel.type) {
                ChannelType.TEXT -> systemCommandEmojiProvider.channelEmoji().formatted + " Text-Channel"
                ChannelType.PRIVATE -> systemCommandEmojiProvider.channelLockedEmoji().formatted + " Private-Channel"
                ChannelType.VOICE -> systemCommandEmojiProvider.voiceChannelEmoji().formatted + " Voice-Channel"
                ChannelType.GROUP -> systemCommandEmojiProvider.emptyEmoji().formatted + " Group"
                ChannelType.CATEGORY -> systemCommandEmojiProvider.categoryEmoji().formatted + " Category"
                ChannelType.NEWS -> systemCommandEmojiProvider.newsEmoji().formatted + " News"
                ChannelType.STAGE -> systemCommandEmojiProvider.stageChannelEmoji().formatted + " Stage"
                ChannelType.GUILD_NEWS_THREAD -> systemCommandEmojiProvider.newsEmoji().formatted + " News Thread"
                ChannelType.GUILD_PUBLIC_THREAD -> systemCommandEmojiProvider.threadChannelEmoji().formatted + " Public Thread"
                ChannelType.GUILD_PRIVATE_THREAD -> systemCommandEmojiProvider.threadChannelEmoji().formatted + " Private Thread"
                ChannelType.UNKNOWN -> "Unknown"
                else -> "Unknown"
            }, true
        )
        embedBuilder.addField("Time Created", discordChannel.timeCreated.toDiscordTimestamp(TimestampFormat.SHORT_DATE_TIME), true)
        if (discordChannel.type in ChannelType.entries.filter { it.isGuild }) {
            val channel = discordChannel as GuildChannel
            embedBuilder.addField("Server", "${channel.guild.name} (`${channel.guild.id}`)", false)

            //If message
            if (discordChannel.type in ChannelType.entries.filter { it.isMessage && !it.isThread }) {
                val textChannel = channel as TextChannel

                textChannel.parentCategory?.let {
                    embedBuilder.addField("Parent Category", "${it.name} (`${it.id}`)", false)
                }
                embedBuilder.addField(
                    "Can send Message",
                    (if (textChannel.canTalk()) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
                    true
                )
                embedBuilder.addField(
                    "Is Synced", (if (textChannel.isSynced) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted, true
                )
                embedBuilder.addField(
                    "Is NSFW", (if (textChannel.isNSFW) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted, true
                )

                embedBuilder.addField(
                    "Is Slowmode",
                    if (textChannel.slowmode != 0) systemCommandEmojiProvider.tickEmoji().formatted + " ${textChannel.slowmode}s" else systemCommandEmojiProvider.crossEmoji().formatted,
                    true
                )
                if (textChannel.threadChannels.isNotEmpty()) {
                    embedBuilder.addField(
                        "Threads (10 newest)",
                        textChannel.threadChannels.sortedByDescending { it.timeCreated }.take(10).joinToString("\n") { "└ ${it.name} (`${it.id}`)" },
                        false
                    )
                }
            }

            //If Voice
            if (discordChannel.type in ChannelType.entries.filter { it.isAudio }) {
                val voiceChannel = channel as VoiceChannel
                voiceChannel.parentCategory?.let {
                    embedBuilder.addField("Parent Category", "${it.name} (`${it.id}`)", false)
                }
                embedBuilder.addField(
                    "Is Synced",
                    (if (voiceChannel.isSynced) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
                    true
                )
                embedBuilder.addField(
                    "User-Limit",
                    if (voiceChannel.userLimit != 0) systemCommandEmojiProvider.tickEmoji().formatted + " ${voiceChannel.userLimit}" else systemCommandEmojiProvider.crossEmoji().formatted,
                    true
                )
            }

            //If Thread
            if (discordChannel.type in ChannelType.entries.filter { it.isThread }) {
                val threadChannel = channel as ThreadChannel

                embedBuilder.addField("Parent Channel", "${threadChannel.parentChannel.name} (`${threadChannel.parentChannel.id}`)", false)
                threadChannel.owner?.let { embedBuilder.addField("Owner", "${it.effectiveName} (`${it.id}`)", false) }
                embedBuilder.addField(
                    "Can send Message",
                    (if (threadChannel.canTalk()) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
                    true
                )
                embedBuilder.addField(
                    "Is Public",
                    (if (threadChannel.isPublic) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
                    true
                )
                embedBuilder.addField(
                    "Is Locked",
                    (if (threadChannel.isLocked) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
                    true
                )
                embedBuilder.addField(
                    "Is Slowmode",
                    if (threadChannel.slowmode != 0) systemCommandEmojiProvider.tickEmoji().formatted + " ${threadChannel.slowmode}s" else systemCommandEmojiProvider.crossEmoji().formatted,
                    true
                )
                if (discordChannel.type == ChannelType.GUILD_PRIVATE_THREAD) {
                    embedBuilder.addField(
                        "Is Invitable",
                        (if (threadChannel.isInvitable) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
                        true
                    )
                }
                embedBuilder.addField(
                    "Is Joined",
                    (if (threadChannel.isJoined) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
                    true
                )
                embedBuilder.addField(
                    "Is Archived",
                    (if (threadChannel.isArchived) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
                    true
                )
                embedBuilder.addField("Auto Archive Duration", "${threadChannel.autoArchiveDuration.minutes} min", true)
                embedBuilder.addBlankField(false)
                embedBuilder.addField("Message count", threadChannel.messageCount.toString(), true)
                embedBuilder.addField("Member count", threadChannel.memberCount.toString(), true)
            }
        }
    }

    override fun onEmoteRequest(discordEmote: RichCustomEmoji, embedBuilder: EmbedBuilder) {
        embedBuilder.addField("ID", discordEmote.id, true)
        embedBuilder.addField("Name", discordEmote.name, true)
        embedBuilder.addField("Mention", "`${discordEmote.formatted}`", true)
        embedBuilder.addField("Time Created", discordEmote.timeCreated.toDiscordTimestamp(TimestampFormat.SHORT_DATE_TIME), true)
        discordEmote.guild.let { embedBuilder.addField("Server", "${it.name} (`${it.id}`)", false) }
        embedBuilder.addField("Url", "`${discordEmote.imageUrl}`", false)
        embedBuilder.addField(
            "Is Animated", (if (discordEmote.isAnimated) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted, true
        )
        embedBuilder.addField(
            "Is Available", (if (discordEmote.isAvailable) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted, true
        )
        embedBuilder.addField(
            "Is Managed", (if (discordEmote.isManaged) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted, true
        )
    }

    override fun onInteractionCommandRequest(discordCommand: Command, embedBuilder: EmbedBuilder) {
        embedBuilder.addField("ID", discordCommand.id, true)
        embedBuilder.addField("Name", discordCommand.name, true)

        val type = when (discordCommand.type) {
            Command.Type.UNKNOWN -> "Unknown"
            Command.Type.SLASH -> "Slash-Command"
            Command.Type.USER -> "Context-Menu User"
            Command.Type.MESSAGE -> "Context-Menu Message"
        }
        embedBuilder.addField("Type", type, true)

        if (discordCommand.type == Command.Type.SLASH) {
            embedBuilder.addField("Description", discordCommand.description, false)

            if (discordCommand.subcommands.isEmpty() || discordCommand.subcommandGroups.isEmpty()) {
                embedBuilder.addField("Mention", discordCommand.asMention, true)
            }
        }

        embedBuilder.addField("Time Created", discordCommand.timeCreated.toDiscordTimestamp(TimestampFormat.SHORT_DATE_TIME), true)
        embedBuilder.addField("Time Modified", discordCommand.timeModified.toDiscordTimestamp(TimestampFormat.SHORT_DATE_TIME), true)
        embedBuilder.addField(
            "Is Guild Only",
            (if (discordCommand.contexts.contains(InteractionContextType.GUILD)) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted,
            true
        )
        embedBuilder.addField("Is NSFW", (if (discordCommand.isNSFW) systemCommandEmojiProvider.tickEmoji() else systemCommandEmojiProvider.crossEmoji()).formatted, true)

        if (discordCommand.type == Command.Type.SLASH) {
            var subCommands = ""
            discordCommand.subcommands.forEach {
                subCommands += "- ${it.name} (${it.asMention})\n"
            }
            discordCommand.subcommandGroups.forEach {
                subCommands += "\uD83D\uDCC1 ${it.name}\n"
                it.subcommands.forEach {
                    subCommands += "\u2063 └ ${it.name} (${it.asMention})\n"
                }
            }
            embedBuilder.addField("Sub-Commands", subCommands, false)
        }
    }
}
