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
import io.viascom.discord.bot.aluna.model.AdditionalRequirements
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent

interface DiscordCommandAdditionalConditions {

    /**
     * Check for additional requirements.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param event Slash command event
     * @return AdditionalRequirements
     */
    fun checkForAdditionalCommandRequirements(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent): AdditionalRequirements

    /**
     * Check for additional requirements.
     * Make sure to not block the execution for to long as the interaction needs to be acknowledged in 3 seconds.
     *
     * @param contextMenu Discord context menu instance
     * @param event User context menu event
     * @return AdditionalRequirements
     */
    fun checkForAdditionalContextRequirements(contextMenu: DiscordContextMenu, event: UserContextInteractionEvent): AdditionalRequirements


    /**
     * Check for additional requirements.
     * Make sure to not block the execution for to long as the interaction needs to be acknowledged in 3 seconds.
     *
     * @param contextMenu Discord context menu instance
     * @param event Message context menu event
     * @return AdditionalRequirements
     */
    fun checkForAdditionalContextRequirements(contextMenu: DiscordContextMenu, event: MessageContextInteractionEvent): AdditionalRequirements
}
