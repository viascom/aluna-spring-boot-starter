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

import io.viascom.discord.bot.aluna.bot.event.CoroutineEventListener
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.EventPublisher
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.springframework.stereotype.Component
import java.util.concurrent.CountDownLatch

@Component
@ConditionalOnJdaEnabled
class ShardStartListener(val eventPublisher: EventPublisher) : CoroutineEventListener {

    var latch: CountDownLatch = CountDownLatch(1)
    private val initialLatchCount = latch.count
    private var mainShardLoaded = false

    override suspend fun onEvent(event: GenericEvent) {
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
