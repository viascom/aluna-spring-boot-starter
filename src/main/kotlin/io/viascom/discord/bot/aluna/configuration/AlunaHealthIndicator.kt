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

package io.viascom.discord.bot.aluna.configuration

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.listener.EventWaiter
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.scope.InteractionScope
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.property.AlunaDiscordProperties
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
import org.springframework.boot.SpringBootVersion
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.core.SpringVersion
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadPoolExecutor


@Component
@ConditionalOnWebApplication
@ConditionalOnClass(HealthIndicator::class)
@ConditionalOnJdaEnabled
@ConditionalOnProperty(name = ["enable-actuator-health-indicator"], prefix = "aluna", matchIfMissing = true)
class AlunaHealthIndicator(
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val eventWaiter: EventWaiter,
    private val eventPublisher: EventPublisher,
    private val alunaProperties: AlunaProperties,
    private val configurableListableBeanFactory: ConfigurableListableBeanFactory
) : HealthIndicator {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val alunaVersion: String
    private val jdaVersion: String

    init {
        logger.debug("Register AlunaHealthIndicator")

        val versions = this::class.java.classLoader.getResource("version.txt")?.readText()?.split("\n")
        val internalVersion = this::class.java.getPackage().implementationVersion
        alunaVersion = versions?.getOrElse(0) { _ -> internalVersion } ?: internalVersion
        jdaVersion = versions?.getOrElse(1) { _ -> "n/a" } ?: "n/a"
    }

    override fun health(): Health {
        val status = Health.unknown()

        when {
            (!discordBot.isLoggedIn) -> status.unknown()
            (shardManager.shards.any { it.status != JDA.Status.CONNECTED }) -> status.down()
            (shardManager.shards.all { it.status != JDA.Status.CONNECTED }) -> status.outOfService()
            else -> status.up()
        }

        val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope

        status.withDetail("loggedIn", discordBot.isLoggedIn)
        status.withDetail("autoLoginOnStartup", alunaProperties.discord.autoLoginOnStartup)
        status.withDetail("nodeNumber", alunaProperties.nodeNumber)
        status.withDetail("clientId", alunaProperties.discord.applicationId ?: "n/a")
        status.withDetail("supportServer", alunaProperties.discord.supportServer ?: "n/a")
        status.withDetail("commandsTotal", discordBot.commands.size)
        status.withDetail("commands", discordBot.commands.mapValues { it.value.name })
        status.withDetail("contextMenuTotal", discordBot.contextMenus.size)
        status.withDetail("contextMenus", discordBot.contextMenus.mapValues { it.value.name })
        status.withDetail("autoCompleteHandlers", discordBot.autoCompleteHandlers.mapValues { it.value.name })
        status.withDetail("commandsWithAutocomplete", discordBot.commandsWithAutocomplete)
        status.withDetail("commandsWithGlobalInteractions", discordBot.commandsWithPersistentInteractions)
        status.withDetail("interactionsInitialized", discordBot.interactionsInitialized)
        status.withDetail("productionMode", alunaProperties.productionMode)

        val threads = hashMapOf<String, Any>()
        threads["messagesToObserveTimeoutThreads"] = getThreadPoolDetail(discordBot.messagesToObserveScheduledThreadPool)
        threads["eventWaiterExecutorTimeoutThreads"] = getThreadPoolDetail(eventWaiter.scheduledThreadPool)
        threads["scopedObjectsTimeoutScheduler"] = getThreadPoolDetail(interactionScope.scopedObjectsTimeoutScheduler)
        threads["eventThreadPool"] = getThreadPoolDetail(eventPublisher.eventThreadPool)

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
        if (discordBot.isLoggedIn) {
            status.withDetail("serversTotal", shardManager.guilds.size)
            status.withDetail("averageGatewayPing", shardManager.averageGatewayPing)
        } else {
            status.withDetail("serversTotal", 0)
            status.withDetail("averageGatewayPing", 0)
        }
        status.withDetail("sharding", getSharding(shardManager, alunaProperties))
        status.withDetail("sessionStartLimit", discordBot.sessionStartLimits)

        status.withDetail("versions", Versions())

        return status.build()
    }

    private fun getThreadPoolDetail(executor: ThreadPoolExecutor): ThreadPoolDetail {
        return ThreadPoolDetail(
            executor.poolSize,
            executor.activeCount,
            executor.corePoolSize,
            executor.largestPoolSize,
            executor.maximumPoolSize,
            executor.taskCount,
            executor.completedTaskCount,
            executor.isTerminating,
            executor.isTerminated,
            executor.isShutdown
        )
    }

    class ThreadPoolDetail(
        val poolSize: Int,
        val activeCount: Int,
        val corePoolSize: Int,
        val largestPoolSize: Int,
        val maximumPoolSize: Int,
        val taskCount: Long,
        val completedTaskCount: Long,
        val isTerminating: Boolean,
        val isTerminated: Boolean,
        val isShutdown: Boolean
    )

    private fun getSharding(shardManager: ShardManager, alunaProperties: AlunaProperties): Sharding {
        val shards = arrayListOf<ShardDetail>()

        if (discordBot.isLoggedIn) {
            shardManager.shards.forEachIndexed { index, jda ->
                shards.add(ShardDetail(index, jda.status, jda.guilds.size))
            }
        }

        return Sharding(
            if (discordBot.isLoggedIn) shardManager.shardsTotal else 0,
            if (discordBot.isLoggedIn) shardManager.shardsQueued else 0,
            if (discordBot.isLoggedIn) shardManager.shardsRunning else 0,
            alunaProperties.discord.sharding.type,
            if (alunaProperties.discord.sharding.type == AlunaDiscordProperties.Sharding.Type.SINGLE) {
                0
            } else {
                alunaProperties.discord.sharding.fromShard
            },
            if (alunaProperties.discord.sharding.type == AlunaDiscordProperties.Sharding.Type.SINGLE) {
                if (discordBot.isLoggedIn) shardManager.shardsTotal - 1 else 0
            } else {
                alunaProperties.discord.sharding.fromShard + alunaProperties.discord.sharding.shardAmount
            },
            shards
        )
    }

    class Sharding(
        val total: Int,
        val queued: Int,
        val running: Int,
        val type: AlunaDiscordProperties.Sharding.Type,
        val from: Int,
        val to: Int,
        val shards: ArrayList<ShardDetail>
    )

    inner class Versions(
        val aluna: String = alunaVersion,
        val jda: String = jdaVersion,
        val spring: String = SpringVersion.getVersion() ?: "n/a",
        val springBoot: String = SpringBootVersion.getVersion() ?: "n/a",
    )

    class ShardDetail(
        val id: Int,
        val status: JDA.Status,
        val serverCount: Int
    )
}
