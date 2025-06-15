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

package io.viascom.discord.bot.aluna.property

import io.viascom.discord.bot.aluna.exception.AlunaPropertiesException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationContextInitializedEvent
import org.springframework.context.ApplicationListener

/**
 * Executes checks against the configuration present on startup to ensure that all needed parameters are set.
 */
public class PropertiesListener : ApplicationListener<ApplicationContextInitializedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val enableJdaPath = "aluna.discord.enable-jda"
    private val tokenPath = "aluna.discord.token"
    private val applicationIdPath = "aluna.discord.application-id"
    private val cacheFlagsPath = "aluna.discord.cache-flags"
    private val ownerIdsPath = "aluna.owner-ids"
    private val systemCommandEnabledPath = "aluna.command.system-command.enabled"
    private val helpCommandEnabledPath = "aluna.command.help-command.enabled"
    private val helpCommandInviteEnabledPath = "aluna.command.help-command.invite-button.enabled"
    private val helpCommandInviteLinkPath = "aluna.command.help-command.invite-button.link"
    private val serverJoinPath = "aluna.notification.server-join"
    private val serverLeavePath = "aluna.notification.server-leave"
    private val botReadyPath = "aluna.notification.bot-ready"
    private val gatewayIntentsPath = "aluna.discord.gateway-intents"
    private val memberCachePolicyPath = "aluna.discord.member-cache-policy"
    private val chunkingFilterPath = "aluna.discord.chunking-filter"
    private val discordShardingPath = "aluna.discord.sharding"

    override fun onApplicationEvent(event: ApplicationContextInitializedEvent) {

        //Check if jda is disabled
        if (event.applicationContext.environment.getProperty(enableJdaPath, Boolean::class.java) == false) {
            return
        }

        //check bot token
        val token = event.applicationContext.environment.getProperty(tokenPath) ?: ""
        if (token.isEmpty()) {
            throw AlunaPropertiesException(
                "Aluna configuration is missing a needed parameter",
                tokenPath,
                "",
                "A valid discord token is needed"
            )
        }


        //Check owners if system-command is enabled
        val ownerIds = event.applicationContext.environment.getProperty(ownerIdsPath, ArrayList::class.java) ?: arrayListOf<Long>()
        val systemCommand = event.applicationContext.environment.getProperty(systemCommandEnabledPath, Boolean::class.java) ?: false
        if (ownerIds.isEmpty() && systemCommand) {
            logger.info("/system-command is enabled but no owner-ids are defined! If you use the DefaultOwnerIdProvider, you may not be able to use this command.")
        }

        //Check if help-command is enabled and invite is enabled and that application id is set if link is null
        val helpCommand = event.applicationContext.environment.getProperty(helpCommandEnabledPath, Boolean::class.java) ?: false
        val helpCommandInviteEnabled = event.applicationContext.environment.getProperty(helpCommandInviteEnabledPath, Boolean::class.java) ?: false
        val helpCommandInviteLink = event.applicationContext.environment.getProperty(helpCommandInviteLinkPath) ?: ""
        if (helpCommand && helpCommandInviteEnabled && helpCommandInviteLink.isEmpty()) {
            val applicationId = event.applicationContext.environment.getProperty(applicationIdPath) ?: ""
            if (applicationId.isEmpty()) {
                throw AlunaPropertiesException(
                    "Aluna configuration is missing a needed parameter",
                    applicationIdPath,
                    "",
                    "If help-command.invite-button.enabled is true, you need to set set aluna.discord.application-id to generate a link automatically or a valid invite link."
                )
            }
        }

        //Check notification
        checkNotification(serverJoinPath, event)
        checkNotification(serverLeavePath, event)
        checkNotification(botReadyPath, event)


        //Check JDA Stuff
        val gatewayIntents = event.applicationContext.environment.getProperty(gatewayIntentsPath, ArrayList::class.java) ?: arrayListOf<String>()
        val memberCachePolicy =
            event.applicationContext.environment.getProperty(memberCachePolicyPath, AlunaDiscordProperties.MemberCachePolicyType::class.java)
                ?: AlunaDiscordProperties.MemberCachePolicyType.DEFAULT
        val cacheFlags = event.applicationContext.environment.getProperty(cacheFlagsPath, AlunaDiscordProperties.CacheFlags::class.java)
            ?: AlunaDiscordProperties.CacheFlags()
        val chunkingFilter =
            event.applicationContext.environment.getProperty(chunkingFilterPath, AlunaDiscordProperties.ChunkingFilter::class.java)

        //Check that GUILD_MEMBERS is active if memberCachePolicy is all
        if (!gatewayIntents.contains("GUILD_MEMBERS") && memberCachePolicy == AlunaDiscordProperties.MemberCachePolicyType.ALL) {
            throw AlunaPropertiesException(
                "Aluna configuration is not valid",
                memberCachePolicyPath,
                memberCachePolicy.name,
                "Cannot use MemberCachePolicy.ALL without GatewayIntent.GUILD_MEMBERS enabled!"
            )
        }

        //Check that needed gatewayIntents for specific CacheFlags are enabled
        when {
            (cacheFlags.contains(AlunaDiscordProperties.CacheFlag.ACTIVITY) && !gatewayIntents.contains("GUILD_PRESENCES")) -> {
                throw AlunaPropertiesException(
                    "Aluna configuration is not valid",
                    cacheFlagsPath,
                    cacheFlags.joinToString(",") { it.name },
                    "CacheFlag.ACTIVITY requires GUILD_PRESENCES intent to be enabled."
                )
            }

            (cacheFlags.contains(AlunaDiscordProperties.CacheFlag.CLIENT_STATUS) && !gatewayIntents.contains("GUILD_PRESENCES")) -> {
                throw AlunaPropertiesException(
                    "Aluna configuration is not valid",
                    cacheFlagsPath,
                    cacheFlags.joinToString(",") { it.name },
                    "CacheFlag.CLIENT_STATUS requires GUILD_PRESENCES intent to be enabled."
                )
            }

            (cacheFlags.contains(AlunaDiscordProperties.CacheFlag.ONLINE_STATUS) && !gatewayIntents.contains("GUILD_PRESENCES")) -> {
                throw AlunaPropertiesException(
                    "Aluna configuration is not valid",
                    cacheFlagsPath,
                    cacheFlags.joinToString(",") { it.name },
                    "CacheFlag.ONLINE_STATUS requires GUILD_PRESENCES intent to be enabled."
                )
            }
        }

        //Check that gatewayIntents.GUILD_MEMBERS is enabled if chunking is not NONE
        if (chunkingFilter != null && chunkingFilter != AlunaDiscordProperties.ChunkingFilter.NONE && !gatewayIntents.contains("GUILD_MEMBERS")) {
            throw AlunaPropertiesException(
                "Aluna configuration is not valid",
                chunkingFilterPath,
                chunkingFilter.name,
                "To use chunking, the GUILD_MEMBERS intent must be enabled! Otherwise you must use NONE!"
            )
        }


        //Check if
        val shardingType = event.applicationContext.environment.getProperty("$discordShardingPath.type", AlunaDiscordProperties.Sharding.Type::class.java)
            ?: AlunaDiscordProperties.Sharding.Type.SINGLE
        if (shardingType == AlunaDiscordProperties.Sharding.Type.SUBSET) {
            val totalShards = event.applicationContext.environment.getProperty("$discordShardingPath.total-shards", Int::class.java) ?: -1
            val fromShard = event.applicationContext.environment.getProperty("$discordShardingPath.from-shard", Int::class.java) ?: 0
            val shardAmount = event.applicationContext.environment.getProperty("$discordShardingPath.shard-amount", Int::class.java) ?: 1

            when {
                totalShards < 1 -> throw AlunaPropertiesException(
                    "Aluna configuration is not valid",
                    "$discordShardingPath.total-shards",
                    totalShards.toString(),
                    "Total has to be 1 or higher!"
                )

                fromShard < 0 -> throw AlunaPropertiesException(
                    "Aluna configuration is not valid",
                    "$discordShardingPath.from-shard",
                    fromShard.toString(),
                    "Has to be 0 or higher!"
                )

                shardAmount < 1 -> throw AlunaPropertiesException(
                    "Aluna configuration is not valid",
                    "$discordShardingPath.shard-amount",
                    shardAmount.toString(),
                    "Amount has to be 1 or higher!"
                )
            }
        }
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
