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

package io.viascom.discord.bot.aluna.configuration

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.listener.EventWaiter
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.scope.InteractionScope
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.stereotype.Component

@Component
@ConditionalOnWebApplication
@ConditionalOnClass(HealthIndicator::class)
@ConditionalOnJdaEnabled
@ConditionalOnProperty(name = ["enable-actuator-health-indicator"], prefix = "aluna", matchIfMissing = true)
class AlunaHealthIndicator(
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val eventWaiter: EventWaiter,
    private val alunaProperties: AlunaProperties,
    private val configurableListableBeanFactory: ConfigurableListableBeanFactory
) : HealthIndicator {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    init {
        logger.debug("Register AlunaHealthIndicator")
    }

    override fun health(): Health {
        val status = Health.unknown()

        if (shardManager.shards.any { it.status != JDA.Status.CONNECTED }) {
            status.down()
        } else {
            status.up()
        }

        if (shardManager.shards.all { it.status != JDA.Status.CONNECTED }) {
            status.outOfService()
        }

        val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope

        shardManager.shards.first().status
        status.withDetail("clientId", alunaProperties.discord.applicationId)
        status.withDetail("commandsTotal", discordBot.commands.size)
        status.withDetail("commands", discordBot.commands.mapValues { it.value.name })
        status.withDetail("contextMenuTotal", discordBot.contextMenus.size)
        status.withDetail("contextMenus", discordBot.contextMenus.mapValues { it.value.name })
        status.withDetail("productionMode", alunaProperties.productionMode)

        val threads = hashMapOf<String, Any>()
        threads["messagesToObserveTimeoutThreads"] = discordBot.messagesToObserveScheduledThreadPool.activeCount
        threads["eventWaiterExecutorTimeoutThreads"] = eventWaiter.scheduledThreadPool.activeCount
        threads["scopedObjectsTimeoutScheduler"] = interactionScope.scopedObjectsTimeoutScheduler.activeCount

        status.withDetail("threads", threads)
        status.withDetail("currentActiveInteractions", interactionScope.getInstanceCount())
        status.withDetail("currentActiveInteractionTimeouts", interactionScope.getTimeoutCount())

        val interactionObserver = hashMapOf<String, Any>()
        interactionObserver["buttons"] = discordBot.messagesToObserveButton.size +
                eventWaiter.waitingEvents.entries.filter { it.key == ButtonInteractionEvent::class.java }.count { it.value.isNotEmpty() }
        interactionObserver["string_select"] = discordBot.messagesToObserveStringSelect.size +
                eventWaiter.waitingEvents.entries.filter { it.key == StringSelectInteractionEvent::class.java }.count { it.value.isNotEmpty() }
        interactionObserver["entity_select"] = discordBot.messagesToObserveEntitySelect.size +
                eventWaiter.waitingEvents.entries.filter { it.key == EntitySelectInteractionEvent::class.java }.count { it.value.isNotEmpty() }
        interactionObserver["modal"] = discordBot.messagesToObserveModal.size +
                eventWaiter.waitingEvents.entries.filter { it.key == ModalInteractionEvent::class.java }.count { it.value.isNotEmpty() }

        status.withDetail("interactionObserver", interactionObserver)
        status.withDetail("serversTotal", shardManager.guilds.size)
        status.withDetail("shardsTotal", shardManager.shardsTotal)

        val shards = arrayListOf<ShardDetail>()

        shardManager.shards.forEachIndexed { index, jda ->
            shards.add(ShardDetail(index, jda.status, jda.guilds.size))
        }

        status.withDetail("shards", shards)

        return status.build()
    }

    class ShardDetail(
        val id: Int,
        val status: JDA.Status,
        val serverCount: Int
    )
}
