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

package io.viascom.discord.bot.aluna.configuration.listener

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.handler.InteractionInitializerCondition
import io.viascom.discord.bot.aluna.configuration.AlunaHealthIndicator
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordSlashCommandInitializedEvent
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.property.ModeratorIdProvider
import io.viascom.discord.bot.aluna.property.OwnerIdProvider
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Service

@Service
@ConditionalOnJdaEnabled
@ConditionalOnExpression("\${aluna.debug.enable-debug-configuration-log:true} && \${aluna.production-mode:false} == false")
class DebugInfoPrinter(
    private val discordBot: DiscordBot,
    private val alunaProperties: AlunaProperties,
    private val ownerIdProvider: OwnerIdProvider,
    private val moderatorIdProvider: ModeratorIdProvider,
    private val initializationCondition: InteractionInitializerCondition,
    private val context: ConfigurableApplicationContext
) : ApplicationListener<DiscordSlashCommandInitializedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordSlashCommandInitializedEvent) {
        AlunaDispatchers.InternalScope.launch {
            if (!alunaProperties.productionMode) {
                var permission = 0L
                alunaProperties.discord.defaultPermissions.forEach { permission = permission or it.rawValue }
                val token = if (!alunaProperties.debug.hideTokenInDebugConfigurationLog) {
                    "-> token:             ${alunaProperties.discord.token}\n"
                } else ""
                val inviteGuild = if (alunaProperties.discord.applicationId != null) {
                    "https://discord.com/oauth2/authorize?client_id=${alunaProperties.discord.applicationId}&integration_type=0&scope=bot%20applications.commands&permissions=$permission"
                } else {
                    "<Please add an applicationId to see this invite link!>"
                }
                val inviteUser = if (alunaProperties.discord.applicationId != null) {
                    "https://discord.com/oauth2/authorize?client_id=${alunaProperties.discord.applicationId}&integration_type=1&scope=applications.commands"
                } else {
                    "<Please add an applicationId to see this invite link!>"
                }
                val healthIndicator = try {
                    context.getBean(AlunaHealthIndicator::class.java)
                } catch (e: Throwable) {
                    null
                }
                val actuator = if (healthIndicator != null) {
                    val serverProperties = context.getBean(ServerProperties::class.java)
                    "-> healthIndicator:   http://localhost:${serverProperties.port}/actuator/health/aluna\n"
                } else ""

                val initializer = if (!initializationCondition.isInitializeNeeded()) {
                    "-> initialization:    skipped\n"
                } else ""

                logger.info(
                    """
                
                ###############################################
                                Configuration
                -> interaction:       ${discordBot.commands.size + discordBot.contextMenus.size}
                -> ownerIds:          ${ownerIdProvider.getOwnerIds().joinToString { it.toString() }.ifBlank { "<not defined>" }}
                -> modIds:            ${moderatorIdProvider.getModeratorIds().joinToString { it.toString() }.ifBlank { "<not defined>" }}
                -> applicationId:     ${alunaProperties.discord.applicationId ?: "<not defined>"}
                -> supportServer:     ${alunaProperties.discord.supportServer ?: "<not defined>"}
                -> invite (Guild):    $inviteGuild
                -> invite (User-App): $inviteUser
                """.trimIndent() + "\n" +
                            token +
                            actuator +
                            initializer +
                            "###############################################"

                )
            }
        }
    }

}
