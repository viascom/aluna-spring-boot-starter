package io.viascom.discord.bot.starter.property

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.requests.GatewayIntent

class AlunaDiscordProperties {

    /**
     * Should Aluna auto create a shard manager
     */
    var enableJDA: Boolean = true

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
    var memberCachePolicy: MemberCachePolicyType = MemberCachePolicyType.ALL

    /**
     * Sets whether or not JDA should try to reconnect if a connection-error is encountered.
     */
    var autoReconnect: Boolean = true


    var defaultPermissions: ArrayList<Permission> = arrayListOf()

    /**
     * Publish jda events as Spring Boot events.
     */
    var publishEvents: Boolean = false

    var publishOnlyFirstEvent: Boolean = true
    var publishGatePingEvent: Boolean = false
    var publishGuildReadyEvent: Boolean = false
    var publishEventInterface: Boolean = false

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
}
