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

package io.viascom.discord.bot.aluna.botlist

import io.viascom.discord.bot.aluna.bot.event.AlunaCoroutinesDispatcher
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.property.AlunaProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@ConditionalOnJdaEnabled
open class BotStatsHandler(
    private val senders: List<BotStatsSender>,
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties,
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    init {
        val enabledSenders = senders.filter { it.isEnabled() }
        val validSenders = enabledSenders.filter { it.isValid() }
        val invalidSenders = enabledSenders.filter { !it.isValid() }

        invalidSenders.forEach { sender ->
            logger.warn("BotStatsSender ${sender.getName()} is not valid and will therefore not be used:\n" + sender.getValidationErrors().joinToString("\n") { "- $it" })
        }

        if (alunaProperties.productionMode && validSenders.isNotEmpty()) {
            logger.info("Your bot stats are sent every 10 min to [${enabledSenders.joinToString(", ") { it.getName() }}]")
        }

        if (!alunaProperties.productionMode && validSenders.any { it.onProductionModeOnly() }) {
            logger.info(
                "Your bot stats will be sent every 10 min to [${validSenders.filter { it.onProductionModeOnly() }.joinToString(", ") { it.getName() }}] in production mode only."
            )
        }

        if (!alunaProperties.productionMode && validSenders.any { !it.onProductionModeOnly() }) {
            logger.info("Your bot stats are sent every 10 min to [${validSenders.filter { !it.onProductionModeOnly() }.joinToString(", ") { it.getName() }}]")
        }
    }

    @Scheduled(cron = "0 */10 * * * *", zone = "UTC") //Send updates every 10 minutes
    open fun sendStats() {
        runBlocking(AlunaCoroutinesDispatcher.IO) {
            senders.filter { it.isEnabled() && it.isValid() }.forEach { sender ->
                try {
                    if (alunaProperties.productionMode || !sender.onProductionModeOnly()) {
                        launch(AlunaCoroutinesDispatcher.IO) {
                            sender.sendStats(
                                shardManager.guilds.size,
                                shardManager.shardsTotal
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Was not able to send stats to ${sender::class.qualifiedName}: " + e.stackTraceToString())
                }
            }
        }

    }

}
