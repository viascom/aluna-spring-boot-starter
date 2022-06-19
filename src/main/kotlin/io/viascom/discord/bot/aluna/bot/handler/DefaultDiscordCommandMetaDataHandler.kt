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
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.util.StopWatch

class DefaultDiscordCommandMetaDataHandler : DiscordCommandMetaDataHandler {
    override fun onCommandExecution(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent) {
    }

    override fun onContextMenuExecution(contextMenu: DiscordContextMenu, event: GenericCommandInteractionEvent) {
    }

    override fun onExitCommand(discordCommand: DiscordCommand, stopWatch: StopWatch?, event: SlashCommandInteractionEvent) {
    }

    override fun onExitCommand(contextMenu: DiscordContextMenu, stopWatch: StopWatch?, event: GenericCommandInteractionEvent) {
    }

    override fun onGenericExecutionException(
        discordCommand: DiscordCommand,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        event: GenericCommandInteractionEvent
    ) {
        throw throwableOfExecution;
    }

    override fun onGenericExecutionException(
        contextMenu: DiscordContextMenu,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        event: GenericCommandInteractionEvent
    ) {
        throw throwableOfExecution;
    }

}
