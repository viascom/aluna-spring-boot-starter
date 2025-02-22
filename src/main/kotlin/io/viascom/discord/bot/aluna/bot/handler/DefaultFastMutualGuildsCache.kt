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
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.LoggerFactory

open class DefaultFastMutualGuildsCache(
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties
) : FastMutualGuildsCache {

    private var logger = LoggerFactory.getLogger(this::class.java)!!

    private val memberCache: Cache<UserId, MutableSet<GuildId>> = Caffeine.newBuilder().build()

    override val size: Int get() = memberCache.asMap().size

    override fun seedCache() {
        if (alunaProperties.discord.fastMutualGuildCache.invalidateBeforeSeedingCache) {
            clear("invalidateBeforeSeedingCache is enabled")
        }

        shardManager.guilds.parallelStream().forEach { guild ->
            guild.members.forEach { member ->
                add(member.user.idLong, guild.idLong)
            }
        }

        logger.debug("Cache populated with $size members")
    }

    override fun clear(reason: String) {
        memberCache.invalidateAll()

        logger.debug("Cache got cleared! - $reason")
    }

    override operator fun get(user: User): Collection<GuildId> {
        return get(user.idLong)
    }

    override operator fun get(member: Member): Collection<GuildId> {
        return get(member.idLong)
    }

    override operator fun get(userId: String): Collection<GuildId> {
        return get(userId.toLong())
    }

    override operator fun get(userId: Long): Collection<GuildId> {
        return memberCache.getIfPresent(userId) ?: run {
            if (alunaProperties.discord.fastMutualGuildCache.useShardManagerFallback) {
                val guilds = shardManager.getMutualGuilds(shardManager.getUserById(userId)).map { it.idLong }
                guilds.parallelStream().forEach {
                    add(userId, it)
                }
                guilds
            } else {
                emptySet()
            }
        }
    }

    override fun getAsGuilds(user: User): Collection<Guild> = get(user).asGuilds()
    override fun getAsGuilds(member: Member): Collection<Guild> = get(member).asGuilds()
    override fun getAsGuilds(userId: String): Collection<Guild> = get(userId).asGuilds()
    override fun getAsGuilds(userId: Long): Collection<Guild> = get(userId).asGuilds()

    private fun Collection<GuildId>.asGuilds(): Collection<Guild> = this.mapNotNull { shardManager.getGuildById(it) }

    override fun add(userId: Long, guildId: Long) {
        val guildIds = memberCache.get(userId) { mutableSetOf() }.apply { add(guildId) }
        memberCache.put(userId, guildIds)
    }

    override fun remove(userId: Long, guildId: Long) {
        memberCache.getIfPresent(userId)?.let { guildIds ->
            guildIds.remove(guildId)
            if (guildIds.isEmpty()) {
                memberCache.invalidate(userId)
            } else {
                memberCache.put(userId, guildIds)
            }
        }
    }

    override fun importGuild(guildId: Long) {
        shardManager.getGuildById(guildId)?.let { guild ->
            guild.members.forEach { member ->
                add(member.user.idLong, guild.idLong)
            }
        }
    }

    override fun removeGuild(guildId: Long) {
        memberCache.asMap().filter { guildId in it.value }.forEach { (userId, _) ->
            remove(userId, guildId)
        }
    }
}
