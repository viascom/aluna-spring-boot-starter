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

package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.bot.handler.DiscordCommandHandler
import io.viascom.discord.bot.aluna.bot.handler.DiscordContextMenuHandler
import io.viascom.discord.bot.aluna.bot.handler.DiscordInteractionMetaDataHandler
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.util.concurrent.TimeUnit

@Service
class OnInteractionHandler : DiscordInteractionMetaDataHandler {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onCommandExecution(discordCommandHandler: DiscordCommandHandler, event: SlashCommandInteractionEvent) {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()


        httpClient.newCall(Request.Builder().get().url("http://httpbin.org/delay/10").build()).execute()
        logger.info("After rest :)")
    }

    override fun onContextMenuExecution(contextMenu: DiscordContextMenuHandler, event: GenericCommandInteractionEvent) {

    }

    override fun onExitInteraction(discordCommandHandler: DiscordCommandHandler, stopWatch: StopWatch?, event: SlashCommandInteractionEvent) {

    }

    override fun onExitInteraction(contextMenu: DiscordContextMenuHandler, stopWatch: StopWatch?, event: GenericCommandInteractionEvent) {

    }

    override fun onGenericExecutionException(
        discordCommandHandler: DiscordCommandHandler,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        event: GenericCommandInteractionEvent
    ) {
        throw throwableOfExecution
    }

    override fun onGenericExecutionException(
        contextMenu: DiscordContextMenuHandler,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        event: GenericCommandInteractionEvent
    ) {
        throw throwableOfExecution
    }
}