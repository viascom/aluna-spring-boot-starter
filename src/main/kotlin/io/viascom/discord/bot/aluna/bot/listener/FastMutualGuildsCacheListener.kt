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
import io.viascom.discord.bot.aluna.bot.event.CoroutineEventListener
import io.viascom.discord.bot.aluna.bot.handler.FastMutualGuildsCache
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnFastMutualGuildCacheEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.property.AlunaProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent
import net.dv8tion.jda.api.events.session.SessionDisconnectEvent
import net.dv8tion.jda.api.events.session.SessionRecreateEvent
import net.dv8tion.jda.api.events.session.SessionResumeEvent
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue

@Component
@ConditionalOnJdaEnabled
@ConditionalOnFastMutualGuildCacheEnabled
open class FastMutualGuildsCacheListener(
    private val fastMutualGuildsCache: FastMutualGuildsCache,
    private val alunaProperties: AlunaProperties
) : CoroutineEventListener {

    private val joinEventsBuffer = ConcurrentLinkedQueue<Pair<Long, Long>>()
    private val removeEventsBuffer = ConcurrentLinkedQueue<Pair<Long, Long>>()

    private var isActive = true

    init {
        AlunaDispatchers.DetachedScope.launch {
            while (isActive) {
                flushBuffers()
                delay(alunaProperties.discord.fastMutualGuildCache.eventBuffer.toMillis())
            }
        }
    }

    private fun flushBuffers() {
        while (joinEventsBuffer.isNotEmpty()) {
            val (userId, guildId) = joinEventsBuffer.poll()
            fastMutualGuildsCache.add(userId, guildId)
        }

        while (removeEventsBuffer.isNotEmpty()) {
            val (userId, guildId) = removeEventsBuffer.poll()
            fastMutualGuildsCache.remove(userId, guildId)
        }
    }

    override suspend fun onEvent(event: GenericEvent) {
        val clearOnSessionDisconnect = alunaProperties.discord.fastMutualGuildCache.clearOnSessionDisconnect
        val seedOnSessionResume = alunaProperties.discord.fastMutualGuildCache.seedOnSessionResume

        when (event) {
            is GuildMemberJoinEvent -> joinEventsBuffer.add(event.member.user.idLong to event.guild.idLong)
            is GuildMemberRemoveEvent -> removeEventsBuffer.add(event.user.idLong to event.guild.idLong)
            is GuildMemberUpdateEvent -> fastMutualGuildsCache.add(event.member.user.idLong, event.guild.idLong)
            is GuildJoinEvent -> fastMutualGuildsCache.importGuild(event.guild.idLong)
            is GuildReadyEvent -> fastMutualGuildsCache.importGuild(event.guild.idLong)
            is GuildLeaveEvent -> fastMutualGuildsCache.removeGuild(event.guild.idLong)
            is SessionDisconnectEvent -> if (clearOnSessionDisconnect) fastMutualGuildsCache.clear("SessionDisconnectEvent")
            is SessionResumeEvent -> if (seedOnSessionResume) fastMutualGuildsCache.seedCache()
            is SessionRecreateEvent -> if (seedOnSessionResume) fastMutualGuildsCache.seedCache()
        }
    }

}
