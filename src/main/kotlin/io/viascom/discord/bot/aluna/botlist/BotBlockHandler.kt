/*
 * Copyright 2024 Viascom Ltd liab. Co
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

package io.viascom.discord.bot.aluna.botlist

import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnBotBlockEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordAllShardsReadyEvent
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.sharding.ShardManager
import org.botblock.javabotblockapi.core.BotBlockAPI
import org.botblock.javabotblockapi.jda.PostAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
@ConditionalOnJdaEnabled
@ConditionalOnBotBlockEnabled
class BotBlockHandler(
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties,
) : ApplicationListener<DiscordAllShardsReadyEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordAllShardsReadyEvent) {
        val delay = alunaProperties.botStats.botBlock.updateDelay.toMinutes().toInt()

        val api = BotBlockAPI.Builder()
            .setUpdateDelay(delay)
            .setAuthTokens(alunaProperties.botStats.botBlock.tokens)
            .build()

        if (alunaProperties.productionMode) {
            val postAction = PostAction(shardManager)
            logger.info("Your bot stats are sent every $delay min to [${alunaProperties.botStats.botBlock.tokens.keys.joinToString(",") { it }}]")
            postAction.enableAutoPost(shardManager, api)
        } else {
            logger.info("Your bot stats will be sent every $delay min to [${alunaProperties.botStats.botBlock.tokens.keys.joinToString(",") { it }}] if in production mode.")
        }
    }

}
