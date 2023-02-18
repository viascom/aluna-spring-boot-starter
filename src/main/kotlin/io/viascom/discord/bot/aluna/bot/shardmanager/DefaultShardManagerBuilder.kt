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

import com.fasterxml.jackson.databind.ObjectMapper
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.event.CoroutineEventListener
import io.viascom.discord.bot.aluna.bot.listener.*
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.model.GatewayResponse
import io.viascom.discord.bot.aluna.property.AlunaDiscordProperties
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.AlunaThreadPool
import jakarta.annotation.Nonnull
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.ConcurrentSessionController
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


class DefaultShardManagerBuilder(
    private val interactionEventListener: InteractionEventListener,
    private val genericInteractionListener: GenericInteractionListener,
    private val interactionComponentEventListener: InteractionComponentEventListener,
    private val eventWaiter: EventWaiter,
    private val genericEventPublisher: GenericEventPublisher,
    private val eventPublisher: EventPublisher,
    private val alunaProperties: AlunaProperties,
    private val customizers: List<ShardManagerBuilderCustomizer>,
    private val objectMapper: ObjectMapper,
    private val discordBot: DiscordBot,
) : ShardManagerBuilder {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun build(): ShardManager {
        val shardManagerBuilder = DefaultShardManagerBuilder.createDefault(alunaProperties.discord.token)
            .addEventListeners(eventWaiter)
            .addEventListeners(genericEventPublisher)
            .addEventListeners(genericInteractionListener)
            .addEventListeners(interactionEventListener)
            .addEventListeners(interactionComponentEventListener)
            .setEventManagerProvider { discordBot.coroutineEventManager }
            .setCallbackPool(
                AlunaThreadPool.getDynamicThreadPool(
                    0,
                    alunaProperties.thread.jdaCallbackThreadPool,
                    java.time.Duration.ofMinutes(1),
                    true,
                    "Aluna-Callback-Pool-%d"
                )
            )
            .setStatus(OnlineStatus.DO_NOT_DISTURB)
            .setActivity(Activity.playing("loading..."))
            .setBulkDeleteSplittingEnabled(alunaProperties.discord.bulkDeleteSplitting)
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

        val recommendedShards = getRecommendedShards(alunaProperties.discord.token!!)
        discordBot.sessionStartLimits = recommendedShards?.sessionStartLimit

        val latchCount: Int
        if (alunaProperties.discord.sharding.type == AlunaDiscordProperties.Sharding.Type.SINGLE) {
            shardManagerBuilder.setShardsTotal(alunaProperties.discord.sharding.totalShards)

            if (alunaProperties.discord.sharding.totalShards == -1) {

                if (recommendedShards == null) {
                    exitProcess(1)
                }

                logger.info("Using from discord recommended amount of ${recommendedShards.shards} shards")
                discordBot.totalShards = recommendedShards.shards
                latchCount = recommendedShards.shards
            } else {
                logger.info("Using ${alunaProperties.discord.sharding.totalShards} shards as defined in the properties")
                discordBot.totalShards = alunaProperties.discord.sharding.totalShards
                latchCount = alunaProperties.discord.sharding.totalShards
            }
        } else {
            logger.info("Using shard range: ${alunaProperties.discord.sharding.fromShard} - ${alunaProperties.discord.sharding.fromShard + alunaProperties.discord.sharding.shardAmount - 1}")
            shardManagerBuilder.setShardsTotal(alunaProperties.discord.sharding.totalShards)
            shardManagerBuilder.setShards(alunaProperties.discord.sharding.fromShard, alunaProperties.discord.sharding.fromShard + alunaProperties.discord.sharding.shardAmount - 1)
            discordBot.totalShards = alunaProperties.discord.sharding.shardAmount
            latchCount = alunaProperties.discord.sharding.shardAmount
        }

        if (alunaProperties.discord.sharding.grouping.enabled) {
            val controller = ConcurrentSessionController()

            if (alunaProperties.discord.sharding.grouping.concurrency > 1) {
                logger.info("Set shard concurrency to: ${alunaProperties.discord.sharding.grouping.concurrency}")
                controller.setConcurrency(alunaProperties.discord.sharding.grouping.concurrency)

                if (alunaProperties.discord.sharding.grouping.concurrency != (recommendedShards?.sessionStartLimit?.maxConcurrency ?: 1)) {
                    logger.error(
                        "You defined a different shard concurrency than recommended by Discord! This will lead to IDENTIFY errors!\n" +
                                "Only change alunaProperties.discord.sharding.grouping.factor if really needed.\n" +
                                "Concurrency higher than 1 is only granted to bigger bots with at least 150'000 servers.\n\n" +
                                "Recommended by Discord: ${(recommendedShards?.sessionStartLimit?.maxConcurrency ?: 1)}\n" +
                                "Your definition: ${alunaProperties.discord.sharding.grouping.concurrency}"
                    )
                }
            }

            shardManagerBuilder.setSessionController(controller)
        }

        discordBot.sessionStartLimits?.let { it.remaining = it.remaining - discordBot.totalShards }

        if (alunaProperties.discord.chunkingFilter != null) {
            logger.debug("Set ChunkingFilter: [${alunaProperties.discord.chunkingFilter!!.name}]")
            shardManagerBuilder.setChunkingFilter(
                when (alunaProperties.discord.chunkingFilter!!) {
                    AlunaDiscordProperties.ChunkingFilter.ALL -> ChunkingFilter.ALL
                    AlunaDiscordProperties.ChunkingFilter.NONE -> ChunkingFilter.NONE
                }
            )
        }

        if (alunaProperties.discord.cacheFlagsDisabled.isNotEmpty()) {
            logger.debug("Disabled CacheFlags: [${alunaProperties.discord.cacheFlagsDisabled.joinToString(", ") { it.name }}]")

            val cacheFlags = if (alunaProperties.discord.cacheFlagsDisabled.contains(AlunaDiscordProperties.CacheFlag.ALL)) {
                CacheFlag.values().toList()
            } else {
                alunaProperties.discord.cacheFlagsDisabled.mapNotNull {
                    when (it) {
                        AlunaDiscordProperties.CacheFlag.ACTIVITY -> CacheFlag.ACTIVITY
                        AlunaDiscordProperties.CacheFlag.VOICE_STATE -> CacheFlag.VOICE_STATE
                        AlunaDiscordProperties.CacheFlag.EMOJI -> CacheFlag.EMOJI
                        AlunaDiscordProperties.CacheFlag.CLIENT_STATUS -> CacheFlag.CLIENT_STATUS
                        AlunaDiscordProperties.CacheFlag.MEMBER_OVERRIDES -> CacheFlag.MEMBER_OVERRIDES
                        AlunaDiscordProperties.CacheFlag.ROLE_TAGS -> CacheFlag.ROLE_TAGS
                        AlunaDiscordProperties.CacheFlag.ONLINE_STATUS -> CacheFlag.ONLINE_STATUS
                        AlunaDiscordProperties.CacheFlag.FORUM_TAGS -> CacheFlag.FORUM_TAGS
                        AlunaDiscordProperties.CacheFlag.SCHEDULED_EVENTS -> CacheFlag.SCHEDULED_EVENTS
                        AlunaDiscordProperties.CacheFlag.STICKER -> CacheFlag.STICKER
                        else -> null
                    }
                }
            }

            shardManagerBuilder.disableCache(cacheFlags)
        }

        if (alunaProperties.discord.cacheFlagsEnabled.isNotEmpty()) {
            logger.debug("Enable CacheFlags: [${alunaProperties.discord.cacheFlagsEnabled.joinToString(", ") { it.name }}]")
            val cacheFlags = if (alunaProperties.discord.cacheFlagsEnabled.contains(AlunaDiscordProperties.CacheFlag.ALL)) {
                CacheFlag.values().toList()
            } else {
                alunaProperties.discord.cacheFlagsEnabled.mapNotNull {
                    when (it) {
                        AlunaDiscordProperties.CacheFlag.ACTIVITY -> CacheFlag.ACTIVITY
                        AlunaDiscordProperties.CacheFlag.VOICE_STATE -> CacheFlag.VOICE_STATE
                        AlunaDiscordProperties.CacheFlag.EMOJI -> CacheFlag.EMOJI
                        AlunaDiscordProperties.CacheFlag.CLIENT_STATUS -> CacheFlag.CLIENT_STATUS
                        AlunaDiscordProperties.CacheFlag.MEMBER_OVERRIDES -> CacheFlag.MEMBER_OVERRIDES
                        AlunaDiscordProperties.CacheFlag.ROLE_TAGS -> CacheFlag.ROLE_TAGS
                        AlunaDiscordProperties.CacheFlag.ONLINE_STATUS -> CacheFlag.ONLINE_STATUS
                        AlunaDiscordProperties.CacheFlag.FORUM_TAGS -> CacheFlag.FORUM_TAGS
                        AlunaDiscordProperties.CacheFlag.SCHEDULED_EVENTS -> CacheFlag.SCHEDULED_EVENTS
                        AlunaDiscordProperties.CacheFlag.STICKER -> CacheFlag.STICKER
                        else -> null
                    }
                }
            }

            shardManagerBuilder.disableCache(cacheFlags)
        }

        if (alunaProperties.discord.gatewayIntents.isNotEmpty()) {
            val privilegedIntents = alunaProperties.discord.gatewayIntents
                .filter { it in arrayListOf(GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS) }
            logger.debug("Enable additional Intents: [${alunaProperties.discord.gatewayIntents.filter { it !in privilegedIntents }.joinToString(", ") { it.name }}]")
            if (privilegedIntents.isNotEmpty()) {
                logger.debug("Enable additional Privileged Intents: [${privilegedIntents.joinToString(", ") { it.name }}]")
            }

            alunaProperties.discord.gatewayIntents.forEach {
                shardManagerBuilder.enableIntents(it)
            }
        }

        when (alunaProperties.discord.shutdownHook) {
            AlunaDiscordProperties.ShutdownHook.JDA -> shardManagerBuilder.setEnableShutdownHook(true)
            AlunaDiscordProperties.ShutdownHook.ALUNA -> shardManagerBuilder.setEnableShutdownHook(false)
            AlunaDiscordProperties.ShutdownHook.NONE -> shardManagerBuilder.setEnableShutdownHook(false)
        }

        customizers.forEach {
            logger.debug("Run shardManagerBuilder customizer: [${it.javaClass.name}]")
            it.customize(shardManagerBuilder)
        }

        logger.info("Spawning {} shards...", latchCount)
        val start = System.currentTimeMillis()
        val shardStartListener = ShardStartListener(CountDownLatch(latchCount), eventPublisher)
        val shardReadyListener = ShardReadyEventListener(eventPublisher, discordBot)
        shardManagerBuilder.addEventListeners(shardStartListener)
        shardManagerBuilder.addEventListeners(shardReadyListener)

        val shardManager = shardManagerBuilder.build()

        val threadPool = AlunaThreadPool.getDynamicSingleThreadPool(10.seconds.toJavaDuration(), "Aluna-StartUp-Pool-%d")

        threadPool.submit {
            logger.debug("Awaiting for $latchCount shards to connect")
            try {
                shardStartListener.latch.await()
                val elapsed = System.currentTimeMillis() - start
                logger.debug("All shards are connected! Took ${TimeUnit.MILLISECONDS.toSeconds(elapsed)} seconds")
                shardManager.removeEventListener(shardStartListener)
                threadPool.shutdown()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        return shardManager
    }

    private fun getRecommendedShards(token: String): GatewayResponse? {
        val gatewayResponse = try {
            val request = Request.Builder()
                .get()
                .url("https://discord.com/api/gateway/bot")
                .addHeader("Authorization", "Bot $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = OkHttpClient().newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { objectMapper.readValue(it, GatewayResponse::class.java) } ?: throw IllegalAccessError("Response was null")
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Unable to fetch recommended shard count", e)
            null
        }

        gatewayResponse?.sessionStartLimit?.let { it.resetTimestamp = LocalDateTime.now().plusSeconds(it.resetAfter / 1000L) }

        return gatewayResponse
    }

    private class ShardStartListener(val latch: CountDownLatch, val eventPublisher: EventPublisher) : CoroutineEventListener {

        val initialLatchCount = latch.count
        var mainShardLoaded = false

        override suspend fun onEvent(@Nonnull event: GenericEvent) {
            if (event is ReadyEvent) {
                event.getJDA().shardManager ?: throw AssertionError()
                latch.countDown()

                //If main shard (0) is connected, trigger interaction update
                if (!mainShardLoaded && event.jda.shardInfo.shardId == 0) {
                    mainShardLoaded = true
                    eventPublisher.publishDiscordMainShardConnectedEvent(event, event.jda.shardManager!!)
                }

                //If first shard is connected.
                if (latch.count + 1 == initialLatchCount) {
                    eventPublisher.publishDiscordFirstShardConnectedEvent(event, event.jda.shardManager!!)
                }

                //Publish DiscordNodeReadyEvent as soon as all shards of this node are connected
                if (latch.count == 0L) {
                    eventPublisher.publishDiscordNodeReadyEvent(event, event.jda.shardManager!!)
                }
            }
        }
    }

}
