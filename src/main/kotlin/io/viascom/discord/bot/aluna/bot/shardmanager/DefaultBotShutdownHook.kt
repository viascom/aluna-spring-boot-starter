/*
 * Copyright 2023 Viascom Ltd liab. Co
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

import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.sharding.ShardManager

class DefaultBotShutdownHook(
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties
) : BotShutdownHook() {

    override fun run() {
        // Remove all event listeners to make bot not process new events while restarting
        shardManager.shards.forEach { shard ->
            shard.audioManagers.forEach {
                it.closeAudioConnection()
            }

            shard.removeEventListener(*shard.registeredListeners.toTypedArray())
        }

        shardManager.shards.forEach { jda ->
            // Indicate on our presence that we are restarting
            jda.presence.setPresence(OnlineStatus.IDLE, Activity.playing(createActivityText("\uD83D\uDE34 Bot is restarting...", jda.shardInfo.shardId)))
        }
    }

    private fun createActivityText(activityText: String, shardId: Int) = "$activityText | Cluster ${alunaProperties.nodeNumber} [$shardId]"
}