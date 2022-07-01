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

package io.viascom.discord.bot.aluna.bot.shardmanager

import io.viascom.discord.bot.aluna.bot.listener.*
import io.viascom.discord.bot.aluna.property.AlunaDiscordProperties
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DefaultShardManagerBuilder(
    private val shardReadyEvent: ShardReadyEvent,
    private val interactionEventListener: InteractionEventListener,
    private val genericInteractionListener: GenericInteractionListener,
    private val eventWaiter: EventWaiter,
    private val genericEventPublisher: GenericEventPublisher,
    private val alunaProperties: AlunaProperties
) : ShardManagerBuilder {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun build(): ShardManager {
        val shardManagerBuilder = DefaultShardManagerBuilder.createDefault(alunaProperties.discord.token)
            .addEventListeners(eventWaiter)
            .addEventListeners(genericEventPublisher)
            .addEventListeners(genericInteractionListener)
            .addEventListeners(shardReadyEvent)
            .addEventListeners(interactionEventListener)
            .setStatus(OnlineStatus.DO_NOT_DISTURB)
            .setActivity(Activity.playing("loading..."))
            .setBulkDeleteSplittingEnabled(true)
            .setShardsTotal(alunaProperties.discord.totalShards)
            .setMemberCachePolicy(
                when (alunaProperties.discord.memberCachePolicy) {
                    AlunaDiscordProperties.MemberCachePolicyType.NONE -> MemberCachePolicy.NONE
                    AlunaDiscordProperties.MemberCachePolicyType.ALL -> MemberCachePolicy.ALL
                    AlunaDiscordProperties.MemberCachePolicyType.OWNER -> MemberCachePolicy.OWNER
                    AlunaDiscordProperties.MemberCachePolicyType.ONLINE -> MemberCachePolicy.ONLINE
                    AlunaDiscordProperties.MemberCachePolicyType.VOICE -> MemberCachePolicy.VOICE
                    AlunaDiscordProperties.MemberCachePolicyType.BOOSTER -> MemberCachePolicy.BOOSTER
                    AlunaDiscordProperties.MemberCachePolicyType.PENDING -> MemberCachePolicy.PENDING
                    AlunaDiscordProperties.MemberCachePolicyType.DEFAULT -> MemberCachePolicy.DEFAULT
                }
            )
            .setAutoReconnect(alunaProperties.discord.autoReconnect)

        if (alunaProperties.discord.chunkingFilter != null) {
            logger.debug("Set ChunkingFilter: [${alunaProperties.discord.chunkingFilter!!.name}]")
            shardManagerBuilder.setChunkingFilter(
                when (alunaProperties.discord.chunkingFilter!!) {
                    AlunaDiscordProperties.ChunkingFilter.ALL -> ChunkingFilter.ALL
                    AlunaDiscordProperties.ChunkingFilter.NONE -> ChunkingFilter.NONE
                }
            )
        }

        if (alunaProperties.discord.cacheFlags.isNotEmpty()) {
            logger.debug("Enable CacheFlags: [${alunaProperties.discord.cacheFlags.joinToString(", ") { it.name }}]")
            alunaProperties.discord.cacheFlags.forEach {
                shardManagerBuilder.enableCache(
                    when (it) {
                        AlunaDiscordProperties.CacheFlag.ACTIVITY -> CacheFlag.ACTIVITY
                        AlunaDiscordProperties.CacheFlag.VOICE_STATE -> CacheFlag.VOICE_STATE
                        AlunaDiscordProperties.CacheFlag.EMOJI -> CacheFlag.EMOJI
                        AlunaDiscordProperties.CacheFlag.CLIENT_STATUS -> CacheFlag.CLIENT_STATUS
                        AlunaDiscordProperties.CacheFlag.MEMBER_OVERRIDES -> CacheFlag.MEMBER_OVERRIDES
                        AlunaDiscordProperties.CacheFlag.ROLE_TAGS -> CacheFlag.ROLE_TAGS
                        AlunaDiscordProperties.CacheFlag.ONLINE_STATUS -> CacheFlag.ONLINE_STATUS
                    }
                )
            }
        }

        if (alunaProperties.discord.gatewayIntents.isNotEmpty()) {
            logger.debug("Enable Intents: [${alunaProperties.discord.gatewayIntents.joinToString(", ") { it.name }}]")
            alunaProperties.discord.gatewayIntents.forEach {
                shardManagerBuilder.enableIntents(it)
            }
        }

        return shardManagerBuilder.build()
    }

}
