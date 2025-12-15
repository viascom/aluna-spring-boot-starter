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

package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.property.AlunaProperties
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Service

@Service
@ConditionalOnJdaEnabled
public class ShardManagerLoginRegistration(
    private val discordBot: DiscordBot,
    private val alunaProperties: AlunaProperties
) : SmartLifecycle {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private var running = false

    override fun start() {
        if (alunaProperties.discord.autoLoginOnStartup) {
            AlunaDispatchers.InternalScope.launch {
                logger.debug("AutoLoginOnStartup is enabled. Awaiting for shards to connect.")
                discordBot.login()
            }
        } else {
            logger.debug("AutoLoginOnStartup is disabled. Not awaiting for shards to connect.")
        }
        running = true
    }

    override fun isRunning(): Boolean = running
    override fun stop() {}
    override fun isAutoStartup(): Boolean = true

    override fun getPhase(): Int = 0

}
