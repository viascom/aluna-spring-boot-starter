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

package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.InteractionScopedObject
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordReadyEvent
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.getGuildTextChannel
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import java.awt.Color

@Service
@ConditionalOnJdaEnabled
internal open class DiscordReadyEventListener(
    private val interactions: List<InteractionScopedObject>,
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val alunaProperties: AlunaProperties
) : ApplicationListener<DiscordReadyEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordReadyEvent) {
        if (alunaProperties.discord.setStatusToOnlineWhenReady) {
            //Set status to online and remove activity
            shardManager.setStatus(OnlineStatus.ONLINE)
            shardManager.setActivity(null)
        }

        discordBot.asyncExecutor.execute {
            if (alunaProperties.notification.botReady.enabled) {
                val embedMessage = EmbedBuilder()
                    .setTitle("⚡ Bot Ready")
                    .setColor(Color.GREEN)
                    .setDescription("Bot is up and ready to answer interactions.")
                    .addField("» Client-Id", alunaProperties.discord.applicationId ?: "n/a", false)
                    .addField("» Total Interactions", interactions.size.toString(), true)
                    .addField("» Production Mode", alunaProperties.productionMode.toString(), true)

                val channel = shardManager.getGuildTextChannel(
                    alunaProperties.notification.botReady.server.toString(),
                    alunaProperties.notification.botReady.channel.toString()
                )

                if (channel == null) {
                    logger.warn("Aluna was not able to send a DiscordReadyEvent to the defined channel.")
                    return@execute
                }

                channel.sendMessageEmbeds(embedMessage.build()).queue()
            }
        }
    }

}
