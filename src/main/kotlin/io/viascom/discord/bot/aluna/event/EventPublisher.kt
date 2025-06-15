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

package io.viascom.discord.bot.aluna.event

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.handler.AutoCompleteHandler
import io.viascom.discord.bot.aluna.bot.handler.DiscordCommandHandler
import io.viascom.discord.bot.aluna.bot.handler.DiscordMessageContextMenuHandler
import io.viascom.discord.bot.aluna.bot.handler.DiscordUserContextMenuHandler
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.AlunaThreadPool
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.PayloadApplicationEvent
import org.springframework.stereotype.Service
import kotlin.reflect.KClass


@Service
@ConditionalOnJdaEnabled
public class EventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val alunaProperties: AlunaProperties
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @JvmSynthetic
    internal val eventThreadPool = AlunaThreadPool.getDynamicThreadPool(0, alunaProperties.thread.eventThreadPool, java.time.Duration.ofMinutes(1), true, "Aluna-Event-Pool-%d")

    @JvmSynthetic
    internal suspend fun publishDiscordNodeReadyEvent(jdaEvent: ReadyEvent, shardManager: ShardManager) = withContext(AlunaDispatchers.Internal) {
        launch {
            logger.debug("Publishing DiscordNodeReadyEvent")
            val discordNodeReadyEvent = DiscordNodeReadyEvent(this, jdaEvent, shardManager)
            applicationEventPublisher.publishEvent(discordNodeReadyEvent)
        }
    }

    @JvmSynthetic
    internal suspend fun publishDiscordAllShardsReadyEvent(jdaEvent: ReadyEvent, shardManager: ShardManager) = withContext(AlunaDispatchers.Internal) {
        launch {
            logger.debug("Publishing DiscordAllShardsReadyEvent")
            val discordAlShardsReadyEvent = DiscordAllShardsReadyEvent(this, jdaEvent, shardManager)
            applicationEventPublisher.publishEvent(discordAlShardsReadyEvent)
        }
    }

    @JvmSynthetic
    internal suspend fun publishDiscordMainShardConnectedEvent(jdaEvent: ReadyEvent, shardManager: ShardManager) = withContext(AlunaDispatchers.Internal) {
        launch {
            logger.debug("Publishing DiscordMainShardConnectedEvent")
            val discordReadyEvent = DiscordMainShardConnectedEvent(this, jdaEvent, shardManager)
            applicationEventPublisher.publishEvent(discordReadyEvent)
        }
    }

    @JvmSynthetic
    internal suspend fun publishDiscordFirstShardConnectedEvent(jdaEvent: ReadyEvent, shardManager: ShardManager) = withContext(AlunaDispatchers.Internal) {
        launch {
            logger.debug("Publishing DiscordFirstShardConnectedEvent")
            val discordReadyEvent = DiscordFirstShardConnectedEvent(this, jdaEvent, shardManager)
            applicationEventPublisher.publishEvent(discordReadyEvent)
        }
    }

    @JvmSynthetic
    internal suspend fun publishDiscordSlashCommandInitializedEvent(
        newCommands: List<KClass<out CommandDataImpl>>,
        updatedCommands: List<KClass<out CommandDataImpl>>,
        removedCommands: List<String>
    ) = withContext(AlunaDispatchers.Internal) {
        launch {
            logger.debug("Publishing DiscordSlashCommandInitializedEvent")
            val discordSlashCommandInitializedEvent = DiscordSlashCommandInitializedEvent(this, newCommands, updatedCommands, removedCommands)
            applicationEventPublisher.publishEvent(discordSlashCommandInitializedEvent)
        }
    }

    @JvmSynthetic
    internal suspend fun publishDiscordAutoCompleteHandlerInitializedEvent(handlers: List<KClass<out AutoCompleteHandler>>) = withContext(AlunaDispatchers.Internal) {
        launch {
            logger.debug("Publishing DiscordAutoCompleteHandlerInitializedEvent")
            val discordAutoCompleteHandlerInitializedEvent = DiscordAutoCompleteHandlerInitializedEvent(this, handlers)
            applicationEventPublisher.publishEvent(discordAutoCompleteHandlerInitializedEvent)
        }
    }

    @JvmSynthetic
    internal suspend fun publishDiscordCommandEvent(user: User, channel: Channel, guild: Guild?, commandPath: String, commandHandler: DiscordCommandHandler) =
        withContext(AlunaDispatchers.Internal) {
            launch {
                logger.debug("Publishing DiscordCommandEvent")
                val discordCommandEvent = DiscordCommandEvent(this, user, channel, guild, commandPath, commandHandler)
                applicationEventPublisher.publishEvent(discordCommandEvent)
            }
        }

    @JvmSynthetic
    internal suspend fun publishDiscordMessageContextEvent(user: User, channel: Channel?, guild: Guild?, name: String, contextMenu: DiscordMessageContextMenuHandler) =
        withContext(AlunaDispatchers.Internal) {
            launch {
                logger.debug("Publishing DiscordMessageContextEvent")
                val discordMessageContextEvent = DiscordMessageContextEvent(this, user, channel, guild, name, contextMenu)
                applicationEventPublisher.publishEvent(discordMessageContextEvent)
            }
        }

    @JvmSynthetic
    internal suspend fun publishDiscordUserContextEvent(user: User, channel: Channel?, guild: Guild?, name: String, contextMenu: DiscordUserContextMenuHandler) =
        withContext(AlunaDispatchers.Internal) {
            launch {
                logger.debug("Publishing DiscordUserContextEvent")
                val discordUserContextEvent = DiscordUserContextEvent(this, user, channel, guild, name, contextMenu)
                applicationEventPublisher.publishEvent(discordUserContextEvent)
            }
        }

    @JvmSynthetic
    internal suspend fun publishDiscordEvent(event: GenericEvent) = withContext(AlunaDispatchers.Internal) {
        if (alunaProperties.discord.publishEvents) {
            try {
                var eventClass: Class<*>? = event::class.java

                if (eventClass?.simpleName == "GatewayPingEvent" && !alunaProperties.discord.publishGatePingEvent) {
                    return@withContext
                }
                if (eventClass?.simpleName == "GuildReadyEvent" && !alunaProperties.discord.publishGuildReadyEvent) {
                    return@withContext
                }

                while (eventClass != null) {
                    val workClass = eventClass
                    if (workClass.simpleName !in arrayListOf("Class", "Object", "HttpRequestEvent")) {
                        launch {
                            logger.debug("Publishing ${workClass.canonicalName}")
                            applicationEventPublisher.publishEvent(PayloadApplicationEvent<Any>(event.jda, event))
                        }
                    }

                    eventClass = if (alunaProperties.discord.publishOnlyFirstEvent) {
                        null
                    } else {
                        eventClass.superclass
                    }

                }
            } catch (e: Exception) {
                logger.debug("Could not publish event ${event::class.simpleName}\n" + e.printStackTrace())
            }
        }
    }

}
