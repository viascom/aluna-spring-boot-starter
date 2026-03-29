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
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
public class AdminSearchChannelPermissionsPage(
    private val alunaProperties: AlunaProperties,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : AdminSearchPageDataProvider(
    "PERMISSIONS",
    "Permissions",
    arrayListOf(AdminSearchDataProvider.AdminSearchType.CHANNEL)
) {

    private val permissionCategories: LinkedHashMap<String, List<Permission>> = linkedMapOf(
        "General" to listOf(
            Permission.MANAGE_CHANNEL, Permission.VIEW_CHANNEL, Permission.MANAGE_PERMISSIONS,
            Permission.MANAGE_WEBHOOKS, Permission.MANAGE_EVENTS, Permission.USE_EMBEDDED_ACTIVITIES,
            Permission.CREATE_INSTANT_INVITE
        ),
        "Text" to listOf(
            Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY, Permission.MESSAGE_MENTION_EVERYONE,
            Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_EXT_STICKER, Permission.MESSAGE_ATTACH_VOICE_MESSAGE,
            Permission.MESSAGE_SEND_POLLS, Permission.USE_EXTERNAL_APPLICATIONS, Permission.PIN_MESSAGES,
            Permission.BYPASS_SLOWMODE, Permission.MESSAGE_MANAGE, Permission.USE_APPLICATION_COMMANDS,
            Permission.MESSAGE_TTS
        ),
        "Threads" to listOf(
            Permission.MANAGE_THREADS, Permission.CREATE_PUBLIC_THREADS,
            Permission.CREATE_PRIVATE_THREADS, Permission.MESSAGE_SEND_IN_THREADS
        ),
        "Voice" to listOf(
            Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VOICE_STREAM,
            Permission.PRIORITY_SPEAKER, Permission.VOICE_MUTE_OTHERS, Permission.VOICE_DEAF_OTHERS,
            Permission.VOICE_MOVE_OTHERS, Permission.VOICE_USE_VAD, Permission.VOICE_USE_SOUNDBOARD,
            Permission.VOICE_USE_EXTERNAL_SOUNDS, Permission.VOICE_SET_STATUS
        ),
        "Stage" to listOf(
            Permission.REQUEST_TO_SPEAK
        )
    )

    override fun onChannelRequest(discordChannel: Channel, embedBuilder: EmbedBuilder) {
        val guildChannel = discordChannel as? GuildChannel ?: return
        val selfMember = guildChannel.guild.selfMember
        val effectivePermissions = selfMember.getPermissions(guildChannel)

        // Collect all permissions affected by channel overrides for the bot
        val overriddenPermissions = guildChannel.permissionContainer.permissionOverrides
            .filter { override ->
                override.member?.id == selfMember.id ||
                    selfMember.roles.any { it.id == override.role?.id } ||
                    override.role?.isPublicRole == true
            }
            .flatMap { it.allowed + it.denied }
            .toSet()

        embedBuilder.clearFields()

        for ((categoryName, permissions) in permissionCategories) {
            val lines = permissions.joinToString("\n") { permission ->
                val hasPermission = permission in effectivePermissions
                val isDefault = permission in alunaProperties.discord.defaultPermissions
                val isOverridden = permission in overriddenPermissions

                val emoji = if (hasPermission) {
                    systemCommandEmojiProvider.tickEmoji().formatted
                } else {
                    systemCommandEmojiProvider.crossEmoji().formatted
                }

                val name = if (isDefault) "**${permission.getName()}**" else permission.getName()
                val overrideIndicator = if (isOverridden) " ⤴" else ""

                "$emoji $name$overrideIndicator"
            }
            embedBuilder.addField(categoryName, lines, false)
        }
    }
}
