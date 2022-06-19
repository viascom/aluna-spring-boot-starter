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

package io.viascom.discord.bot.aluna.event

import io.viascom.discord.bot.aluna.bot.*
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.entities.Channel
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import kotlin.reflect.KClass


@Service
@ConditionalOnJdaEnabled
class EventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val discordBot: DiscordBot,
    private val alunaProperties: AlunaProperties
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @JvmSynthetic
    internal fun publishDiscordReadyEvent(jdaEvent: ReadyEvent) {
        discordBot.asyncExecutor.execute {
            logger.debug("Publishing DiscordReadyEvent")
            val discordReadyEvent = DiscordReadyEvent(this, jdaEvent)
            applicationEventPublisher.publishEvent(discordReadyEvent)
        }
    }

    @JvmSynthetic
    internal fun publishDiscordFirstShardReadyEvent(jdaEvent: ReadyEvent) {
        discordBot.asyncExecutor.execute {
            logger.debug("Publishing DiscordFirstShardReadyEvent")
            val discordReadyEvent = DiscordFirstShardReadyEvent(this, jdaEvent)
            applicationEventPublisher.publishEvent(discordReadyEvent)
        }
    }

    @JvmSynthetic
    internal fun publishDiscordSlashCommandInitializedEvent(
        newCommands: List<KClass<out CommandDataImpl>>,
        updatedCommands: List<KClass<out CommandDataImpl>>,
        removedCommands: List<String>
    ) {
        discordBot.asyncExecutor.execute {
            logger.debug("Publishing DiscordSlashCommandInitializedEvent")
            val discordSlashCommandInitializedEvent = DiscordSlashCommandInitializedEvent(this, newCommands, updatedCommands, removedCommands)
            applicationEventPublisher.publishEvent(discordSlashCommandInitializedEvent)
        }
    }

    @JvmSynthetic
    internal fun publishDiscordAutoCompleteHandlerInitializedEvent(handlers: List<KClass<out AutoCompleteHandler>>) {
        discordBot.asyncExecutor.execute {
            logger.debug("Publishing DiscordAutoCompleteHandlerInitializedEvent")
            val discordAutoCompleteHandlerInitializedEvent = DiscordAutoCompleteHandlerInitializedEvent(this, handlers)
            applicationEventPublisher.publishEvent(discordAutoCompleteHandlerInitializedEvent)
        }
    }

    @JvmSynthetic
    internal fun publishDiscordCommandEvent(user: User, channel: Channel, server: Guild?, commandPath: String, command: DiscordCommand) {
        discordBot.asyncExecutor.execute {
            logger.debug("Publishing DiscordCommandEvent")
            val discordCommandEvent = DiscordCommandEvent(this, user, channel, server, commandPath, command)
            applicationEventPublisher.publishEvent(discordCommandEvent)
        }
    }

    @JvmSynthetic
    internal fun publishDiscordMessageContextEvent(user: User, channel: Channel?, server: Guild?, name: String, contextMenu: DiscordMessageContextMenu) {
        discordBot.asyncExecutor.execute {
            logger.debug("Publishing DiscordMessageContextEvent")
            val discordMessageContextEvent = DiscordMessageContextEvent(this, user, channel, server, name, contextMenu)
            applicationEventPublisher.publishEvent(discordMessageContextEvent)
        }
    }

    @JvmSynthetic
    internal fun publishDiscordUserContextEvent(user: User, channel: Channel?, server: Guild?, name: String, contextMenu: DiscordUserContextMenu) {
        discordBot.asyncExecutor.execute {
            logger.debug("Publishing DiscordUserContextEvent")
            val discordUserContextEvent = DiscordUserContextEvent(this, user, channel, server, name, contextMenu)
            applicationEventPublisher.publishEvent(discordUserContextEvent)
        }
    }

    @JvmSynthetic
    internal fun publishDiscordEvent(event: GenericEvent) {
        if (alunaProperties.discord.publishEvents) {
            discordBot.asyncExecutor.execute {
                try {
                    var eventClass: Class<*>? = event::class.java

                    if (eventClass?.simpleName == "GatewayPingEvent" && !alunaProperties.discord.publishGatePingEvent) {
                        return@execute
                    }
                    if (eventClass?.simpleName == "GuildReadyEvent" && !alunaProperties.discord.publishGuildReadyEvent) {
                        return@execute
                    }

                    while (eventClass != null) {
                        val workClass = eventClass
                        if (workClass.simpleName !in arrayListOf("Class", "Object", "HttpRequestEvent")) {
                            val specificEvent = Class.forName("On${workClass.simpleName}")
                            logger.debug("Publishing ${workClass.canonicalName}")
                            applicationEventPublisher.publishEvent(specificEvent.constructors.first().newInstance(this, event))
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

}
