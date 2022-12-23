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

package io.viascom.discord.bot.aluna.model

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.DiscordInteractionHandler
import io.viascom.discord.bot.aluna.bot.event.AlunaCoroutinesDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class ObserveInteraction(
    val interaction: KClass<out DiscordInteractionHandler>,
    val uniqueId: String?,
    val startTime: LocalDateTime,
    val duration: Duration,
    val stayActive: Boolean = false,
    val additionalData: HashMap<String, Any?> = hashMapOf(),
    val authorIds: ArrayList<String>? = null,
    val interactionUserOnly: Boolean = false,
    var timeoutTask: ScheduledFuture<*>? = null
) {

    companion object {
        fun scheduleButtonTimeout(
            interaction: DiscordInteractionHandler,
            duration: Duration,
            messageId: String,
            discordBot: DiscordBot,
            logger: Logger,
            additionalData: HashMap<String, Any?>
        ): ScheduledFuture<*> {
            return discordBot.messagesToObserveScheduledThreadPool.schedule({
                runBlocking(AlunaCoroutinesDispatcher.Default) {
                    launch(AlunaCoroutinesDispatcher.Default) {
                        try {
                            interaction.onButtonInteractionTimeout(additionalData)
                        } catch (e: Exception) {
                            logger.debug("Could not run onButtonInteractionTimeout for interaction '${discordBot.getInteractionName(interaction)}'\n${e.stackTraceToString()}")
                        }
                    }
                    launch(AlunaCoroutinesDispatcher.Default) {
                        discordBot.removeMessageForButtonEvents(messageId)
                    }
                }
            }, duration.seconds, TimeUnit.SECONDS)
        }

        fun scheduleStringSelectTimeout(
            interaction: DiscordInteractionHandler,
            duration: Duration,
            messageId: String,
            discordBot: DiscordBot,
            logger: Logger,
            additionalData: HashMap<String, Any?>
        ): ScheduledFuture<*> {
            return discordBot.messagesToObserveScheduledThreadPool.schedule({
                runBlocking(AlunaCoroutinesDispatcher.Default) {
                    launch(AlunaCoroutinesDispatcher.Default) {
                        try {
                            interaction.onStringSelectInteractionTimeout(additionalData)
                        } catch (e: Exception) {
                            logger.debug("Could not run onStringSelectInteractionTimeout for interaction '${discordBot.getInteractionName(interaction)}'\n${e.stackTraceToString()}")
                        }
                    }
                    launch(AlunaCoroutinesDispatcher.Default) {
                        discordBot.removeMessageForStringSelectEvents(messageId)
                    }
                }
            }, duration.seconds, TimeUnit.SECONDS)
        }

        fun scheduleEntitySelectTimeout(
            interaction: DiscordInteractionHandler,
            duration: Duration,
            messageId: String,
            discordBot: DiscordBot,
            logger: Logger,
            additionalData: HashMap<String, Any?>
        ): ScheduledFuture<*> {
            return discordBot.messagesToObserveScheduledThreadPool.schedule({
                runBlocking(AlunaCoroutinesDispatcher.Default) {
                    launch(AlunaCoroutinesDispatcher.Default) {
                        try {
                            interaction.onEntitySelectInteractionTimeout(additionalData)
                        } catch (e: Exception) {
                            logger.debug("Could not run onEntitySelectInteractionTimeout for interaction '${discordBot.getInteractionName(interaction)}'\n${e.stackTraceToString()}")
                        }
                    }
                    launch(AlunaCoroutinesDispatcher.Default) {
                        discordBot.removeMessageForStringSelectEvents(messageId)
                    }
                }
            }, duration.seconds, TimeUnit.SECONDS)
        }

        fun scheduleModalTimeout(
            interaction: DiscordInteractionHandler,
            duration: Duration,
            authorId: String,
            discordBot: DiscordBot,
            logger: Logger,
            additionalData: HashMap<String, Any?>
        ): ScheduledFuture<*> {
            return discordBot.messagesToObserveScheduledThreadPool.schedule({
                runBlocking(AlunaCoroutinesDispatcher.Default) {
                    launch(AlunaCoroutinesDispatcher.Default) {
                        try {
                            interaction.onModalInteractionTimeout(additionalData)
                        } catch (e: Exception) {
                            logger.debug("Could not run onModalInteractionTimeout for interaction '${discordBot.getInteractionName(interaction)}'\n${e.stackTraceToString()}")
                        }
                    }
                    launch(AlunaCoroutinesDispatcher.Default) {
                        discordBot.removeMessageForModalEvents(authorId)
                    }
                }
            }, duration.seconds, TimeUnit.SECONDS)
        }
    }


}