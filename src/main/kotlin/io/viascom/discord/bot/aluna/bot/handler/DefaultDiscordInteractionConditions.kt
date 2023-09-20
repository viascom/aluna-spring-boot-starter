/*
 * Copyright 2023 Viascom Ltd liab. Co
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

import io.viascom.discord.bot.aluna.model.MissingPermissions
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

open class DefaultDiscordInteractionConditions : DiscordInteractionConditions {

    override fun checkForNeededUserPermissions(
        discordCommandHandler: DiscordCommandHandler,
        userPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): MissingPermissions = checkForNeededUserPermissionsGeneric(userPermissions, event)

    override fun checkForNeededBotPermissions(
        discordCommandHandler: DiscordCommandHandler,
        botPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): MissingPermissions = checkForNeededBotPermissionsGeneric(botPermissions, event)

    override fun checkForNeededUserPermissions(
        contextMenu: DiscordContextMenuHandler,
        userPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): MissingPermissions = checkForNeededUserPermissionsGeneric(userPermissions, event)

    override fun checkForNeededBotPermissions(
        contextMenu: DiscordContextMenuHandler,
        botPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): MissingPermissions = checkForNeededBotPermissionsGeneric(botPermissions, event)

    fun checkForNeededUserPermissionsGeneric(
        userPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): MissingPermissions {
        val missingPermissions = MissingPermissions()
        val server = event.guild ?: return missingPermissions
        val serverChannel = event.guildChannel
        val member = server.getMember(event.user)

        userPermissions.forEach { permission ->
            if (permission.isChannel) {
                if (!member!!.hasPermission(serverChannel, permission)) {
                    missingPermissions.textChannel.add(permission)
                }
            } else {
                if (!member!!.hasPermission(permission)) {
                    missingPermissions.guild.add(permission)
                }
            }
        }

        return missingPermissions
    }

    fun checkForNeededBotPermissionsGeneric(
        botPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): MissingPermissions {
        val missingPermissions = MissingPermissions()
        val server = event.guild ?: return missingPermissions
        val serverChannel = event.guildChannel
        val member = server.getMember(event.user)

        val selfMember = server.getMemberById(event.jda.selfUser.id)!!

        botPermissions.forEach { permission ->
            if (permission.isChannel) {
                if (permission.isVoice) {
                    val voiceChannel = member?.voiceState?.channel
                    if (voiceChannel == null) {
                        missingPermissions.notInVoice = true
                        return missingPermissions
                    }

                    if (!selfMember.hasPermission(voiceChannel, permission)) {
                        missingPermissions.voiceChannel.add(permission)
                    }
                }

                if (!selfMember.hasPermission(serverChannel, permission)) {
                    missingPermissions.textChannel.add(permission)
                }
            } else {
                if (!selfMember.hasPermission(permission)) {
                    missingPermissions.guild.add(permission)
                }
            }
        }

        return missingPermissions
    }
}
