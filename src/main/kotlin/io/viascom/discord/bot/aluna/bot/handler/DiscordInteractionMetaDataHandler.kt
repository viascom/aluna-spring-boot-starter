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

import io.viascom.discord.bot.aluna.model.TimeMarkRecord
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

/**
 * Interface to implement if you want to execute actions on certain points during interaction execution.
 */
interface DiscordInteractionMetaDataHandler {

    /**
     * Gets called asynchronously before the command is executed.
     *
     * @param discordCommandHandler Discord command instance
     * @param event
     */
    fun onCommandExecution(discordCommandHandler: DiscordCommandHandler, event: SlashCommandInteractionEvent)

    /**
     * Gets called asynchronously before the context menu is executed.
     *
     * @param contextMenu Discord context menu instance
     * @param event
     */
    fun onContextMenuExecution(contextMenu: DiscordContextMenuHandler, event: GenericCommandInteractionEvent)

    /**
     * Gets called asynchronously after the command is executed.
     * This gets also called if the command execution throws an exception.
     *
     * @param discordCommandHandler Discord command instance
     * @param stopWatch StopWatch instance if enabled
     * @param event Slash command event
     */
    fun onExitInteraction(discordCommandHandler: DiscordCommandHandler, timeMarks: List<TimeMarkRecord>?, event: SlashCommandInteractionEvent)

    /**
     * Gets called asynchronously after the context menu is executed.
     * This gets also called if the context menu execution throws an exception.
     *
     * @param contextMenu Discord command instance
     * @param stopWatch StopWatch instance if enabled
     * @param event Slash command event
     */
    fun onExitInteraction(contextMenu: DiscordContextMenuHandler, timeMarks: List<TimeMarkRecord>?, event: GenericCommandInteractionEvent)

    /**
     * Gets called if the command defined onExecutionException throws an exception.
     *
     * @param discordCommandHandler Discord command instance
     * @param throwableOfExecution initial exception from the execution
     * @param exceptionOfSpecificHandler exception thrown by onExecutionException
     * @param event Slash command event
     */
    fun onGenericExecutionException(
        discordCommandHandler: DiscordCommandHandler,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        event: GenericCommandInteractionEvent
    )

    /**
     * Gets called if the command defined onExecutionException throws an exception.
     *
     * @param contextMenu Discord command instance
     * @param throwableOfExecution initial exception from the execution
     * @param exceptionOfSpecificHandler exception thrown by onExecutionException
     * @param event Slash command event
     */
    fun onGenericExecutionException(
        contextMenu: DiscordContextMenuHandler,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        event: GenericCommandInteractionEvent
    )
}
