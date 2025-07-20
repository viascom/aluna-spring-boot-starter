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

package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.coQueue
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@ConditionalOnJdaEnabled
public class ServerSpecificInteractionManager(
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    public suspend fun removeAllOutdatedServerSpecificCommands(): Unit = withContext(AlunaDispatchers.Internal) {
        shardManager.guilds.forEach { server -> launch { removeOutdatedServerSpecificCommands(server) } }
    }

    public suspend fun removeOutdatedServerSpecificCommands(server: Guild): Unit = withContext(AlunaDispatchers.Internal) {
        server.retrieveCommands().coQueue { commands ->
            val outdatedCommands = commands.filterNot { it.id in discordBot.discordRepresentations.keys }
            outdatedCommands.forEach {
                server.deleteCommandById(it.id).queue()
                logger.debug("Deleted outdated interaction '${it.name}' from server '${server.id}'")
            }
        }
    }

}
