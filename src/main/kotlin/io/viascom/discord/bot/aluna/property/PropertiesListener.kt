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

package io.viascom.discord.bot.aluna.property

import io.viascom.discord.bot.aluna.exception.AlunaPropertiesException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationContextInitializedEvent
import org.springframework.context.ApplicationListener


/**
 * Executes checks against the configuration present on startup to ensure that all needed parameters are set.
 */
class PropertiesListener : ApplicationListener<ApplicationContextInitializedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ApplicationContextInitializedEvent) {
        //Check if jda is disabled
        if (event.applicationContext.environment.getProperty("aluna.discord.enable-jda", Boolean::class.java) == false) {
            return
        }

        //check bot token
        val token = event.applicationContext.environment.getProperty("aluna.discord.token") ?: ""
        if (token.isEmpty()) {
            throw AlunaPropertiesException(
                "Aluna configuration is missing a needed parameter",
                "aluna.discord.token",
                "",
                "A valid discord token is needed"
            )
        }


        //Check owners if system-command is enabled
        val ownerIds = event.applicationContext.environment.getProperty("aluna.owner-ids", ArrayList::class.java) ?: arrayListOf<Long>()
        val systemCommand = event.applicationContext.environment.getProperty("aluna.command.system-command.enabled", Boolean::class.java) ?: false
        if (ownerIds.isEmpty() && systemCommand) {
            logger.info("/system-command is enabled but no owner-ids are defined! If you use the DefaultOwnerIdProvider, you may not be able to use this command.")
        }

        //Check notification
        checkNotification("aluna.notification.server-join", event)
        checkNotification("aluna.notification.server-leave", event)
        checkNotification("aluna.notification.bot-ready", event)


        //Check JDA Stuff
        val gatewayIntents = event.applicationContext.environment.getProperty("aluna.discord.gateway-intents", ArrayList::class.java) ?: arrayListOf<String>()
        val memberCachePolicy =
            event.applicationContext.environment.getProperty("aluna.discord.member-cache-policy", AlunaDiscordProperties.MemberCachePolicyType::class.java)
                ?: AlunaDiscordProperties.MemberCachePolicyType.DEFAULT
        val cacheFlags = event.applicationContext.environment.getProperty("aluna.discord.cache-flags", AlunaDiscordProperties.CacheFlags::class.java)
            ?: AlunaDiscordProperties.CacheFlags()
        val chunkingFilter =
            event.applicationContext.environment.getProperty("aluna.discord.chunking-filter", AlunaDiscordProperties.ChunkingFilter::class.java)

        //Check that GUILD_MEMBERS is active if memberCachePolicy is all
        if (!gatewayIntents.contains("GUILD_MEMBERS") && memberCachePolicy == AlunaDiscordProperties.MemberCachePolicyType.ALL) {
            throw AlunaPropertiesException(
                "Aluna configuration is not valid",
                "aluna.discord.member-cache-policy",
                memberCachePolicy.name,
                "Cannot use MemberCachePolicy.ALL without GatewayIntent.GUILD_MEMBERS enabled!"
            )
        }

        //Check that needed gatewayIntents for specific CacheFlags are enabled
        when {
            (cacheFlags.contains(AlunaDiscordProperties.CacheFlag.ACTIVITY) && !gatewayIntents.contains("GUILD_PRESENCES")) -> {
                throw AlunaPropertiesException(
                    "Aluna configuration is not valid",
                    "aluna.discord.cache-flags",
                    cacheFlags.joinToString(",") { it.name },
                    "CacheFlag.ACTIVITY requires GUILD_PRESENCES intent to be enabled."
                )
            }
            (cacheFlags.contains(AlunaDiscordProperties.CacheFlag.CLIENT_STATUS) && !gatewayIntents.contains("GUILD_PRESENCES")) -> {
                throw AlunaPropertiesException(
                    "Aluna configuration is not valid",
                    "aluna.discord.cache-flags",
                    cacheFlags.joinToString(",") { it.name },
                    "CacheFlag.CLIENT_STATUS requires GUILD_PRESENCES intent to be enabled."
                )
            }
            (cacheFlags.contains(AlunaDiscordProperties.CacheFlag.ONLINE_STATUS) && !gatewayIntents.contains("GUILD_PRESENCES")) -> {
                throw AlunaPropertiesException(
                    "Aluna configuration is not valid",
                    "aluna.discord.cache-flags",
                    cacheFlags.joinToString(",") { it.name },
                    "CacheFlag.ONLINE_STATUS requires GUILD_PRESENCES intent to be enabled."
                )
            }
        }

        //Check that gatewayIntents.GUILD_MEMBERS is enabled if chunking is not NONE
        if (chunkingFilter != null && chunkingFilter != AlunaDiscordProperties.ChunkingFilter.NONE && !gatewayIntents.contains("GUILD_MEMBERS")) {
            throw AlunaPropertiesException(
                "Aluna configuration is not valid",
                "aluna.discord.chunking-filter",
                chunkingFilter.name,
                "To use chunking, the GUILD_MEMBERS intent must be enabled! Otherwise you must use NONE!"
            )
        }


    }

    private fun checkNotification(base: String, event: ApplicationContextInitializedEvent) {
        val sendJoin = event.applicationContext.environment.getProperty("$base.enabled", Boolean::class.java) ?: false
        if (sendJoin) {
            val sendJoinServer = event.applicationContext.environment.getProperty("$base.server", Long::class.java) ?: 0L
            if (sendJoinServer == 0L) {
                throw AlunaPropertiesException(
                    "Aluna configuration is missing a needed parameter",
                    "$base.server",
                    "",
                    "$base.enable is enabled, $base.server has to be defined"
                )
            }

            val sendJoinChannel = event.applicationContext.environment.getProperty("$base.channel", Long::class.java) ?: 0L
            if (sendJoinChannel == 0L) {
                throw AlunaPropertiesException(
                    "Aluna configuration is missing a needed parameter",
                    "$base.channel",
                    "",
                    "$base.enable is enabled, $base.channel has to be defined"
                )
            }
        }
    }

}
