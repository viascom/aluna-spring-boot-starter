package io.viascom.discord.bot.aluna.property

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.requests.GatewayIntent

class AlunaDiscordProperties {

    /**
     * Should Aluna auto create a shard manager
     */
    var enableJda: Boolean = true

    /**
     * Discord Bot Token
     */
    var token: String? = null

    /**
     * Discord Bot Application-Id
     */
    var applicationId: String? = null

    /**
     * Gateway Intents
     */
    var gatewayIntents: ArrayList<GatewayIntent> = arrayListOf()

    /**
     * Configure the member caching policy.
     * This will decide whether to cache a member (and its respective user).
     * <br>All members are cached by default. If a guild is enabled for chunking, all members will be cached for it.
     *
     * <p>You can use this to define a custom caching policy that will greatly improve memory usage.
     * <p>It is not recommended to disable {@link GatewayIntent#GUILD_MEMBERS GatewayIntent.GUILD_MEMBERS} when
     * using {@link MemberCachePolicy#ALL MemberCachePolicy.ALL} as the members cannot be removed from cache by a leave event without this intent.
     *
     */
    var memberCachePolicy: MemberCachePolicyType = MemberCachePolicyType.ONLINE

    /**
     * Flags used to enable cache services for JDA.
     * <br>Check the flag descriptions to see which {@link net.dv8tion.jda.api.requests.GatewayIntent intents} are required to use them.
     *
     */
    var cacheFlags: ArrayList<CacheFlag> = arrayListOf()

    /**
     * Sets whether JDA should try to reconnect if a connection-error is encountered.
     */
    var autoReconnect: Boolean = true

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
     * Publish Aluna discord command event. This event should not be used to answer the command!
     */
    var publishDiscordCommandEvent: Boolean = false

    /**
     * Set status to online and remove activity as soon as Aluna is finished with the startup process and is connected to Discord.
     */
    var setStatusToOnlineWhenReady: Boolean = true

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
        ACTIVITY,
        VOICE_STATE,
        EMOTE,
        CLIENT_STATUS,
        MEMBER_OVERRIDES,
        ROLE_TAGS,
        ONLINE_STATUS
    }
}
