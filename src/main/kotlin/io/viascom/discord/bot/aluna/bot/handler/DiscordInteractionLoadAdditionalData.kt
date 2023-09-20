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

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

/**
 * Interface to implement if you need to load additional data before the interaction gets executed.
 */
interface DiscordInteractionLoadAdditionalData {

    /**
     * This method get called before the command is executed.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommandHandler Discord command handler instance
     * @param event Slash command event
     */
    fun loadData(discordCommandHandler: DiscordCommandHandler, event: SlashCommandInteractionEvent)

    /**
     * This method get called before the additional requirements are checked.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommandHandler Discord command handler instance
     * @param event Slash command event
     */
    fun loadDataBeforeAdditionalRequirements(discordCommandHandler: DiscordCommandHandler, event: SlashCommandInteractionEvent)

    /**
     * This method get called before the command is executed.
     * Make sure to not block the execution for to long as the auto complete interaction needs to be acknowledged in 3 seconds.
     *
     * @param discordCommandHandler Discord command handler instance
     * @param event Auto complete interaction event
     */
    fun loadData(discordCommandHandler: DiscordCommandHandler, event: CommandAutoCompleteInteractionEvent)

    /**
     * This method get called before the additional requirements are checked.
     * Make sure to not block the execution for to long as the auto complete interaction needs to be acknowledged in 3 seconds.
     *
     * @param discordCommandHandler Discord command handler instance
     * @param event Slash command event
     */
    fun loadDataBeforeAdditionalRequirements(discordCommandHandler: DiscordCommandHandler, event: CommandAutoCompleteInteractionEvent)

    /**
     * This method get called before the auto-complete is executed.
     * Make sure to not block the execution for to long as the auto complete interaction needs to be acknowledged in 3 seconds.
     *
     * @param event Auto complete interaction event
     */
    fun loadData(event: CommandAutoCompleteInteractionEvent)

    /**
     * This method get called before the context menu is executed.
     * Make sure to not block the execution for to long as the interaction needs to be acknowledged in 3 seconds.
     *
     * @param contextMenu Discord context menu instance
     * @param event Discord context menu event
     */
    fun loadData(contextMenu: DiscordContextMenuHandler, event: GenericCommandInteractionEvent)

    /**
     * This method get called before the additional requirements are checked.
     * Make sure to not block the execution for to long as the interaction needs to be acknowledged in 3 seconds.
     *
     * @param discordContextMenuHandler Discord context menu instance
     * @param event Slash command event
     */
    fun loadDataBeforeAdditionalRequirements(discordContextMenuHandler: DiscordContextMenuHandler, event: GenericCommandInteractionEvent)

}
