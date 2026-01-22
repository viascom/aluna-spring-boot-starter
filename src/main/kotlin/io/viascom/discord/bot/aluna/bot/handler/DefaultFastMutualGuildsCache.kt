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

package io.viascom.discord.bot.aluna.bot.handler

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.viascom.discord.bot.aluna.bot.GuildId
import io.viascom.discord.bot.aluna.bot.UserId
import io.viascom.discord.bot.aluna.property.AlunaProperties
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.LoggerFactory

/**
 * Default implementation of [FastMutualGuildsCache] using FastUtil primitive collections
 * for memory-efficient storage of user-to-guild mappings.
 */
public open class DefaultFastMutualGuildsCache(
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties
) : FastMutualGuildsCache {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val memberCache: Cache<UserId, LongOpenHashSet> = Caffeine.newBuilder().build()

    override val size: Int get() = memberCache.estimatedSize().toInt()

    override fun seedCache() {
        if (alunaProperties.discord.fastMutualGuildCache.invalidateBeforeSeedingCache) {
            clear("invalidateBeforeSeedingCache is enabled")
        }

        shardManager.guilds.parallelStream().forEach { guild ->
            val guildId = guild.idLong
            guild.members.forEach { member ->
                add(member.user.idLong, guildId)
            }
        }

        logger.debug("Cache populated with $size members")
    }

    override fun clear(reason: String) {
        memberCache.invalidateAll()
        logger.debug("Cache got cleared! - $reason")
    }

    override operator fun get(user: User): Collection<GuildId> = get(user.idLong)

    override operator fun get(member: Member): Collection<GuildId> = get(member.idLong)

    override operator fun get(userId: String): Collection<GuildId> = get(userId.toLong())

    override operator fun get(userId: Long): Collection<GuildId> {
        val guildIds = memberCache.getIfPresent(userId)
        if (guildIds != null) {
            // Unsynchronized read - FastUtil's LongOpenHashSet doesn't throw ConcurrentModificationException
            // and allows safe iteration during concurrent modification. Worst case is slightly stale data,
            // which is acceptable for a cache.
            return LongArrayList(guildIds)
        }

        if (alunaProperties.discord.fastMutualGuildCache.useShardManagerFallback) {
            val user = shardManager.getUserById(userId) ?: return emptyList()
            val guilds = shardManager.getMutualGuilds(user)
            val result = LongArrayList(guilds.size)
            guilds.forEach { guild ->
                val guildId = guild.idLong
                result.add(guildId)
                add(userId, guildId)
            }
            return result
        }

        return emptyList()
    }

    override fun getAsGuilds(user: User): Collection<Guild> = get(user).asGuilds()
    override fun getAsGuilds(member: Member): Collection<Guild> = get(member).asGuilds()
    override fun getAsGuilds(userId: String): Collection<Guild> = get(userId).asGuilds()
    override fun getAsGuilds(userId: Long): Collection<Guild> = get(userId).asGuilds()

    private fun Collection<GuildId>.asGuilds(): Collection<Guild> = mapNotNull { shardManager.getGuildById(it) }

    override fun add(userId: Long, guildId: Long) {
        val guildIds = memberCache.get(userId) { LongOpenHashSet(INITIAL_SET_CAPACITY) }!!
        synchronized(guildIds) {
            guildIds.add(guildId)
        }
    }

    override fun remove(userId: Long, guildId: Long) {
        memberCache.getIfPresent(userId)?.let { guildIds ->
            synchronized(guildIds) {
                guildIds.remove(guildId)
                if (guildIds.isEmpty()) {
                    memberCache.invalidate(userId)
                }
            }
        }
    }

    override fun importGuild(guildId: Long) {
        shardManager.getGuildById(guildId)?.let { guild ->
            guild.members.forEach { member ->
                add(member.user.idLong, guildId)
            }
        }
    }

    override fun removeGuild(guildId: Long) {
        val usersToInvalidate = mutableListOf<UserId>()

        memberCache.asMap().forEach { (userId, guildIds) ->
            synchronized(guildIds) {
                if (guildIds.remove(guildId) && guildIds.isEmpty()) {
                    usersToInvalidate.add(userId)
                }
            }
        }

        usersToInvalidate.forEach { memberCache.invalidate(it) }
    }

    private companion object {
        // Most users are in 1-4 guilds, so start small to save memory
        private const val INITIAL_SET_CAPACITY = 4
    }
}
