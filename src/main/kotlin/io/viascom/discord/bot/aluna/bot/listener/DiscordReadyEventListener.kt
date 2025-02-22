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
import io.viascom.discord.bot.aluna.bot.handler.FastMutualGuildsCache
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordAllShardsReadyEvent
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.getGuildTextChannel
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Service
import java.awt.Color
import java.util.*

@Service
@Order(100)
@ConditionalOnJdaEnabled
internal open class DiscordReadyEventListener(
    private val discordBot: DiscordBot,
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties,
    private val taskScheduler: TaskScheduler,
    private val context: ConfigurableApplicationContext
) : ApplicationListener<DiscordAllShardsReadyEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordAllShardsReadyEvent) {
        AlunaDispatchers.InternalScope.launch {
            if (alunaProperties.discord.setStatusToOnlineWhenReady) {
                try {
                    //Set status to online and remove activity
                    shardManager.setStatus(OnlineStatus.ONLINE)
                    shardManager.setActivity(null)
                } catch (e: Exception) {
                    logger.error("Failed to set status to online and remove activity", e)
                }
            }

            if (alunaProperties.notification.botReady.enabled) {
                try {
                    val embedMessage = EmbedBuilder()
                        .setTitle("⚡ Bot Ready")
                        .setColor(Color.GREEN)
                        .setDescription("Bot is up and ready to answer interactions.")
                        .addField("» Client-Id", alunaProperties.discord.applicationId ?: "n/a", false)
                        .addField("» Total Interactions", (discordBot.commands.size + discordBot.contextMenus.size).toString(), true)
                        .addField("» Production Mode", alunaProperties.productionMode.toString(), true)

                    val channel = shardManager.getGuildTextChannel(
                        alunaProperties.notification.botReady.server.toString(),
                        alunaProperties.notification.botReady.channel.toString()
                    )

                    if (channel == null) {
                        logger.warn("Aluna was not able to send a DiscordAllShardsReadyEvent to the defined channel.")
                        return@launch
                    }

                    channel.sendMessageEmbeds(embedMessage.build()).queue()
                } catch (e: Exception) {
                    logger.error("Failed to send bot ready message", e)
                }
            }

            if (alunaProperties.discord.fastMutualGuildCache.enabled) {
                val fastMutualGuildsCache = context.getBean(FastMutualGuildsCache::class.java) as FastMutualGuildsCache

                try {
                    fastMutualGuildsCache.seedCache()
                } catch (e: Exception) {
                    logger.error("Failed to seed FastMutualGuildsCache", e)
                }
                try {
                    if (alunaProperties.discord.fastMutualGuildCache.reSeedCache) {
                        taskScheduler.schedule({
                            fastMutualGuildsCache.seedCache()
                        }, CronTrigger(alunaProperties.discord.fastMutualGuildCache.reSeedInterval, TimeZone.getTimeZone(TimeZone.getDefault().id)))
                    }
                } catch (e: Exception) {
                    logger.error("Failed to enable FastMutualGuildsCache re-seed scheduler", e)
                }
            }
        }
    }

}
