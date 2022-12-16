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

import com.fasterxml.jackson.databind.ObjectMapper
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
class TopGGBotListSender(
    private val alunaProperties: AlunaProperties,
    private val shardManager: ShardManager,
    private val objectMapper: ObjectMapper
) : BotListSender {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onProductionModeOnly(): Boolean = true

    override fun isEnabled(): Boolean = alunaProperties.botList.topggToken?.enabled == true

    override fun getName(): String = "top.gg"
    override fun isValid(): Boolean = alunaProperties.botList.topggToken?.token != null

    override fun getValidationErrors(): List<String> =
        arrayListOf("Stats are not sent to top.gg because token (aluna.botList.topggToken.token) is not set")

    override fun sendStats(totalServer: Int, totalShards: Int) {
        val topGGToken = alunaProperties.botList.topggToken?.token ?: ""

        logger.debug("Send stats to top.gg")

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        val request =
            Request.Builder().url("https://top.gg/api/bots/${alunaProperties.discord.applicationId}/stats").post(
                objectMapper.writeValueAsBytes(TopGGData(shardManager.shards.map { it.guilds.size }))
                    .toRequestBody("application/json".toMediaType())
            ).header("Authorization", topGGToken).build()

        httpClient.newCall(request).execute().body?.close()
    }

    class TopGGData(
        val shards: List<Int>
    )
}
