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

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.DiscordContextMenu
import io.viascom.discord.bot.aluna.model.MissingPermissions
import io.viascom.discord.bot.aluna.model.UseScope
import io.viascom.discord.bot.aluna.model.WrongUseScope
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

interface DiscordInteractionConditions {

    /**
     * Check the use scope for the command.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param subCommandUseScope Use scope of the sub commands
     * @param event Generic interaction event
     * @return WrongUseScope
     */
    fun checkUseScope(
        discordCommand: DiscordCommand,
        subCommandUseScope: HashMap<String, UseScope>,
        event: GenericCommandInteractionEvent
    ): WrongUseScope

    /**
     * Check if the user has all the needed permissions.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param userPermissions Needed Permissions
     * @param event Generic interaction event
     * @return MissingPermissions
     */
    fun checkForNeededUserPermissions(
        discordCommand: DiscordCommand,
        userPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): MissingPermissions

    /**
     * Check if the bot has all the needed permissions.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param botPermissions Needed Permissions
     * @param event Generic interaction event
     * @return MissingPermissions
     */
    fun checkForNeededBotPermissions(
        discordCommand: DiscordCommand,
        botPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): MissingPermissions

    /**
     * Check if the user has all the needed permissions.
     * Make sure to not block the execution for to long as the context menu needs to be acknowledged in 3 seconds.
     *
     * @param contextMenu Discord context menu instance
     * @param userPermissions Needed Permissions
     * @param event Generic interaction event
     * @return MissingPermissions
     */
    fun checkForNeededUserPermissions(
        contextMenu: DiscordContextMenu,
        userPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): MissingPermissions

    /**
     * Check if the bot has all the needed permissions.
     * Make sure to not block the execution for to long as the context menu needs to be acknowledged in 3 seconds.
     *
     * @param contextMenu Discord context menu instance
     * @param botPermissions Needed Permissions
     * @param event Generic interaction event
     * @return MissingPermissions
     */
    fun checkForNeededBotPermissions(
        contextMenu: DiscordContextMenu,
        botPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): MissingPermissions

}
