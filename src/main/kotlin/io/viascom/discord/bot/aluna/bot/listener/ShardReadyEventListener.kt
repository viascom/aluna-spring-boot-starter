/*
 * Copyright 2024 Viascom Ltd liab. Co
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
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.EventPublisher
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
class ShardReadyEventListener(private val discordReadyEventPublisher: EventPublisher) : CoroutineEventListener {

    private var allShardsReady: Boolean = false

    override suspend fun onEvent(event: GenericEvent) {
        when (event) {
            is ReadyEvent -> onReady(event)
        }
    }

    private suspend fun onReady(event: ReadyEvent) = withContext(AlunaDispatchers.Internal) {
        //Publish DiscordAllShardsReadyEvent as soon as all shards are connected. If subset is used, this is only triggered on the last node.
        if ((event.jda.shardInfo.shardId + 1) == (event.jda.shardInfo.shardTotal) && !allShardsReady) {
            allShardsReady = true
            discordReadyEventPublisher.publishDiscordAllShardsReadyEvent(event, event.jda.shardManager!!)
            event.jda.shardManager!!.removeEventListener(this)
        }
    }


}
