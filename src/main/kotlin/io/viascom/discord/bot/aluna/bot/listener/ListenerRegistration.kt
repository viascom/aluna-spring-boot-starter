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

package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.event.CoroutineEventListener
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordNodeReadyEvent
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
@ConditionalOnJdaEnabled
class ListenerRegistration(private val coroutineListeners: List<CoroutineEventListener>, private val listeners: List<ListenerAdapter>, private val shardManager: ShardManager) :
    ApplicationListener<DiscordNodeReadyEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordNodeReadyEvent) {
        AlunaDispatchers.InternalScope.launch {
            val combinedListener = arrayListOf<Any>()
            combinedListener.addAll(listeners)
            combinedListener.addAll(coroutineListeners)

            val listenersToRegister = combinedListener.filterNot {
                //Filter out static registered listeners
                it::class.java.canonicalName.startsWith("io.viascom.discord.bot.aluna.bot.listener") && it::class.java.canonicalName != ServerNotificationEvent::class.java.canonicalName
            }
            val internalListeners = combinedListener.filter {
                it::class.java.canonicalName.startsWith("io.viascom.discord.bot.aluna.bot.listener") && it::class.java.canonicalName != ServerNotificationEvent::class.java.canonicalName
            }

            logger.debug("Register internal listeners: [" + internalListeners.joinToString(", ") { it::class.java.canonicalName } + "]")
            if (listenersToRegister.isNotEmpty()) {
                logger.debug("Register listeners:\n" + listenersToRegister.joinToString("\n") { "- ${it::class.java.canonicalName}" })
                shardManager.addEventListener(*listenersToRegister.toTypedArray())
            }
        }
    }

}
