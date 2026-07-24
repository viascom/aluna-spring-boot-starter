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

package io.viascom.discord.bot.aluna.bot.shardmanager

import io.viascom.discord.bot.aluna.model.GatewayResponse
import io.viascom.discord.bot.aluna.model.RecommendedShards
import io.viascom.discord.bot.aluna.property.AlunaProperties
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Default [RecommendedShardsResolver]: queries the configured gateway endpoint
 * (`aluna.discord.gateway-bot-url`, Discord by default) with the bot token.
 */
public open class DefaultRecommendedShardsResolver(
    private val alunaProperties: AlunaProperties,
    private val objectMapper: ObjectMapper,
    private val httpClient: OkHttpClient = OkHttpClient(),
) : RecommendedShardsResolver {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun resolve(): RecommendedShards? {
        val token = alunaProperties.discord.token ?: run {
            logger.error("Unable to fetch recommended shard count: no discord token configured")
            return null
        }

        val gatewayResponse = try {
            val request = Request.Builder()
                .get()
                .url(alunaProperties.discord.gatewayBotUrl)
                .addHeader("Authorization", "Bot $token")
                .addHeader("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    objectMapper.readValue(response.body.string(), GatewayResponse::class.java)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Unable to fetch recommended shard count", e)
            null
        }

        gatewayResponse?.sessionStartLimit?.let {
            it.resetTimestamp = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(it.resetAfter / 1000L)
        }

        return gatewayResponse?.let { RecommendedShards(it.shards, it.sessionStartLimit) }
    }
}
