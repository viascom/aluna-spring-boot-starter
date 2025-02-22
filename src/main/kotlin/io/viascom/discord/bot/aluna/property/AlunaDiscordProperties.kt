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

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.requests.GatewayIntent
import java.time.Duration

class AlunaDiscordProperties {

    /**
     * Should Aluna auto create a shard manager
     */
    var enableJda: Boolean = true

    /**
     * Automatically start the login process on startup. If this is false, then you will need to manually call ShardManager.login() to start it.
     */
    var autoLoginOnStartup: Boolean = true

    /**
     * Discord Bot Token
     * Can be generated here: https://discord.com/developers/applications
     */
    var token: String? = null

    /**
     * Discord Bot Application-Id
     * Can be found here: https://discord.com/developers/applications
     */
    var applicationId: String? = null

    /**
     * Defines the support server which will be used for certain information..
     */
    var supportServer: String? = null

    /**
     * Sharding configuration
     */
    var sharding: Sharding = Sharding()

    /**
     * Intents which enable specific events from the discord gateway.
     *
     * <p>The way to use these, is very simple. Go through each intent in the following list and decide whether your bot
     * will need these events or not.
     *
     * <ol>
     *     <li><b>GUILD_MEMBERS</b> - This is a <b>privileged</b> gateway intent that is used to update user information and join/leaves (including kicks). This is required to cache all members of a guild (including chunking)</li>
     *     <li><b>GUILD_BANS</b> - This will only track guild bans and unbans</li>
     *     <li><b>GUILD_EMOJIS</b> - This will only track guild emojis create/modify/delete. Most bots don't need this since they just use the emojis id anyway.</li>
     *     <li><b>GUILD_WEBHOOKS</b> - This will only track guild webhook create/update/delete. Most bots don't need this since related events don't contain any useful information about webhook changes.</li>
     *     <li><b>GUILD_INVITES</b> - This will only track invite create/delete. Most bots don't make use of invites since they are added through OAuth2 authorization by administrators.</li>
     *     <li><b>GUILD_VOICE_STATES</b> - Required to properly get information of members in voice channels and cache them. <u>You cannot connect to a voice channel without this intent</u>.</li>
     *     <li><b>GUILD_PRESENCES</b> - This is a <b>privileged</b> gateway intent this is only used to track activity and online-status of a user.</li>
     *     <li><b>GUILD_MESSAGES</b> - This is a <b>privileged</b> gateway intent this is used to receive incoming messages in guilds (servers).</li>
     *     <li><b>GUILD_MESSAGE_REACTIONS</b> - This is used to track reactions on messages in guilds (servers).</li>
     *     <li><b>GUILD_MESSAGE_TYPING</b> - This is used to track when a user starts typing in guilds (servers). Almost no bot will have a use for this.</li>
     *     <li><b>DIRECT_MESSAGES</b> - This is used to receive incoming messages in private channels (DMs). You can still send private messages without this intent.</li>
     *     <li><b>DIRECT_MESSAGE_REACTIONS</b> - This is used to track reactions on messages in private channels (DMs).</li>
     *     <li><b>DIRECT_MESSAGE_TYPING</b> - This is used to track when a user starts typing in private channels (DMs). Almost no bot will have a use for this.</li>
     * </ol>
     *
     * If an intent is not specifically mentioned to be <b>privileged</b>, it is not required to be on the whitelist to use it (and its related events).
     * To get whitelisted you either need to contact discord support (for bots in more than 100 guilds)
     * or enable it in the developer dashboard of your application.
     *
     * <p>You must use [ChunkingFilter.NONE] if GUILD_MEMBERS is disabled.
     * To enable chunking the discord api requires the privileged GUILD_MEMBERS intent.
     */
    var gatewayIntents: ArrayList<GatewayIntent> = arrayListOf()

    /**
     * Configure the member caching policy.
     * This will decide whether to cache a member (and its respective user).
     * <br>If a guild is enabled for chunking and [MemberCachePolicyType.ALL] is set, all members will be cached for it.
     *
     * <p>You can use this to define a custom caching policy that will greatly improve memory usage.
     * <p>It is not possible to disable [GatewayIntent.GUILD_MEMBERS] when
     * using [MemberCachePolicy.ALL] as the members cannot be removed from cache by a leave event without this intent.
     */
    var memberCachePolicy: MemberCachePolicyType = MemberCachePolicyType.ONLINE

    /**
     * Flags used to disable cache services for JDA.
     */
    var cacheFlagsDisabled: ArrayList<CacheFlag> = arrayListOf()

    /**
     * Flags used to enable cache services for JDA.
     * <br>Check the flag descriptions to see which intents are required to use them.
     */
    var cacheFlagsEnabled: ArrayList<CacheFlag> = arrayListOf()

    /**
     * Filter function for member chunking of guilds.
     * <br>The filter decides based on the provided guild id whether chunking should be done
     * on guild initialization.
     *
     * <b>To use chunking, the [GatewayIntent.GUILD_MEMBERS] intent must be enabled!
     * Otherwise you <u>must</u> use [ChunkingFilter.NONE]!</b>
     */
    var chunkingFilter: ChunkingFilter? = null

    /**
     * Sets whether JDA should try to reconnect if a connection-error is encountered.
     */
    var autoReconnect: Boolean = true

    /**
     * If enabled, JDA will separate the bulk delete event into individual delete events, but this isn't as efficient as
     * handling a single event would be. It is recommended that BulkDelete Splitting be disabled and that the developer
     * should instead handle the {@link net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent MessageBulkDeleteEvent}.
     */
    var bulkDeleteSplitting: Boolean = true

    /**
     * Default permissions which are used for /system-command and invite generation
     */
    var defaultPermissions: ArrayList<Permission> = arrayListOf()

    /**
     * Publish jda events as Spring Boot events.
     */
    var publishEvents: Boolean = false

    /**
     * Publish only first event and don't publish parent events
     */
    var publishOnlyFirstEvent: Boolean = true

    /**
     * Publish gate ping event. This may cause a lot of events!
     */
    var publishGatePingEvent: Boolean = false

    /**
     * Publish guild ready event. This may cause a lot of events!
     */
    var publishGuildReadyEvent: Boolean = false

    /**
     * Publish Aluna discord interaction event. This event should not be used to answer the interaction!
     */
    var publishDiscordCommandEvent: Boolean = false

    /**
     * Publish Aluna discord context menu event. This event should not be used to answer the context menu!
     */
    var publishDiscordContextEvent: Boolean = false

    /**
     * Set status to online and remove activity as soon as Aluna is finished with the startup process and is connected to Discord.
     */
    var setStatusToOnlineWhenReady: Boolean = true

    var shutdownHook: ShutdownHook = ShutdownHook.JDA

    var fastMutualGuildCache: FastMutualGuildCacheProperties = FastMutualGuildCacheProperties()

    class Sharding {
        var type: Type = Type.SINGLE

        /**
         * This will set the total amount of shards the DefaultShardManager should use.
         * <p> If this is set to -1 JDA will automatically retrieve the recommended amount of shards from discord (default behavior).
         */
        var totalShards: Int = -1

        var fromShard: Int = 0
        var shardAmount: Int = 1

        var grouping: Grouping = Grouping()

        enum class Type {
            SINGLE, SUBSET
        }

        class Grouping {
            var enabled: Boolean = false

            /**
             * Only change this if really have to.
             * This will lead to IDENTIFY errors if you are not allowed to use a higher concurrency.
             * Which in the longer run will eat up your allowed session start limit.
             * Concurrency higher than 1 is only granted to bigger bots with at least 150'000 servers.
             */
            var concurrency: Int = 1
        }
    }

    enum class MemberCachePolicyType {
        NONE,
        ALL,
        OWNER,
        ONLINE,
        VOICE,
        BOOSTER,
        PENDING,
        DEFAULT
    }

    enum class CacheFlag {
        ALL,
        ACTIVITY,
        VOICE_STATE,
        EMOJI,
        CLIENT_STATUS,
        MEMBER_OVERRIDES,
        ROLE_TAGS,
        ONLINE_STATUS,
        FORUM_TAGS,
        SCHEDULED_EVENTS,
        STICKER
    }

    class CacheFlags : ArrayList<CacheFlag>()

    enum class ChunkingFilter {
        ALL, NONE
    }

    enum class ShutdownHook {
        JDA,
        ALUNA,
        NONE
    }

    class FastMutualGuildCacheProperties {
        /**
         * Should Aluna use and manage the FastMutualGuildCache?
         */
        var enabled: Boolean = false

        /**
         * Should Aluna use shardManager.getMutualGuilds(user) if the user is not found in the cache?
         */
        var useShardManagerFallback: Boolean = true

        var clearOnSessionDisconnect: Boolean = true

        var seedOnSessionResume: Boolean = true

        var eventBuffer: Duration = Duration.ofMillis(10)

        var invalidateBeforeSeedingCache: Boolean = true

        var reSeedCache: Boolean = false

        var reSeedInterval: String = "0 5,35 * * * *"
    }
}
