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

import io.viascom.discord.bot.aluna.bot.GuildId
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User

public interface FastMutualGuildsCache {

    public val size: Int

    public fun seedCache()

    public fun clear(reason: String)

    public operator fun get(user: User): Collection<GuildId>

    public operator fun get(member: Member): Collection<GuildId>

    public operator fun get(userId: String): Collection<GuildId>

    public operator fun get(userId: Long): Collection<GuildId>

    public fun getAsGuilds(user: User): Collection<Guild>
    public fun getAsGuilds(member: Member): Collection<Guild>
    public fun getAsGuilds(userId: String): Collection<Guild>
    public fun getAsGuilds(userId: Long): Collection<Guild>

    public fun add(userId: Long, guildId: Long)
    public fun remove(userId: Long, guildId: Long)
    public fun importGuild(guildId: Long)
    public fun removeGuild(guildId: Long)
}
