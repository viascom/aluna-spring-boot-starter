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

package io.viascom.discord.bot.aluna.configuration.listener

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordAllShardsReadyEvent
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
@ConditionalOnJdaEnabled
class SessionStartLimitListener(
    private val discordBot: DiscordBot,
) : ApplicationListener<DiscordAllShardsReadyEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordAllShardsReadyEvent) {
        AlunaDispatchers.InternalScope.launch {
            //Show session info, if remaining is below 300
            if (discordBot.sessionStartLimits?.remaining != null && discordBot.sessionStartLimits!!.remaining < 300) {
                val resetDate = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(discordBot.sessionStartLimits!!.resetAfter / 1000L)
                logger.warn(
                    """
                    
                ###############################################
                             Session Start Limit
                
                You have less than 300 session starts remaining until $resetDate. 
                If you exceed this, your token will be reset!
                
                -> Remaining:       ${discordBot.sessionStartLimits!!.remaining}
                -> Total:           ${discordBot.sessionStartLimits!!.total}
                -> Reset After:     ${discordBot.sessionStartLimits!!.resetAfter} ($resetDate)
                -> Max Concurrency: ${discordBot.sessionStartLimits!!.maxConcurrency}
                ###############################################
                """.trimIndent()
                )
            }
        }
    }

}