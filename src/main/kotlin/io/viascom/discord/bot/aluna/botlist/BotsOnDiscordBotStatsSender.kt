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

package io.viascom.discord.bot.aluna.botlist

import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.sharding.ShardManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnJdaEnabled
public class BotsOnDiscordBotStatsSender(
    private val alunaProperties: AlunaProperties,
    private val shardManager: ShardManager
) : BotStatsSender {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onProductionModeOnly(): Boolean = true

    override fun isEnabled(): Boolean = alunaProperties.botStats.botsOnDiscord?.enabled == true

    override fun getName(): String = "bots.ondiscord.xyz"

    override fun isValid(): Boolean = alunaProperties.botStats.botsOnDiscord?.token != null

    override fun getValidationErrors(): List<String> =
        arrayListOf("Stats are not sent to bots.ondiscord.xyz because token (aluna.botStats.botsOnDiscord.token) is not set")

    override fun sendStats(totalServer: Int, totalShards: Int) {
        val botsOnDiscordToken = alunaProperties.botStats.botsOnDiscord?.token ?: ""

        logger.debug("Send stats to bots.ondiscord.xyz")

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://bots.ondiscord.xyz/bot-api/bots/${alunaProperties.discord.applicationId}/guilds").post(
                "{\"guildCount\": ${shardManager.guilds.size}}".toRequestBody("application/json".toMediaType())
            ).header("Authorization", botsOnDiscordToken).build()

        httpClient.newCall(request).execute().body?.close()
    }
}
