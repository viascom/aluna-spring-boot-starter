package io.viascom.discord.bot.aluna.bot.shardmanager

import io.viascom.discord.bot.aluna.bot.listener.*
import io.viascom.discord.bot.aluna.property.AlunaDiscordProperties
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.LoggerFactory

class DefaultShardManagerBuilder(
    private val shardReadyEvent: ShardReadyEvent,
    private val slashCommandInteractionEventListener: SlashCommandInteractionEventListener,
    private val genericAutoCompleteListener: GenericInteractionListener,
    private val eventWaiter: EventWaiter,
    private val genericEventPublisher: GenericEventPublisher,
    private val alunaProperties: AlunaProperties
) : ShardManagerBuilder {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun build(): ShardManager {
        val shardManagerBuilder = DefaultShardManagerBuilder.createDefault(alunaProperties.discord.token)
            .addEventListeners(shardReadyEvent)
            .addEventListeners(slashCommandInteractionEventListener)
            .addEventListeners(genericAutoCompleteListener)
            .addEventListeners(eventWaiter)
            .addEventListeners(genericEventPublisher)
            .setStatus(OnlineStatus.DO_NOT_DISTURB)
            .setActivity(Activity.playing("loading..."))
            .setBulkDeleteSplittingEnabled(true)
            .setMemberCachePolicy(
                when (alunaProperties.discord.memberCachePolicy) {
                    AlunaDiscordProperties.MemberCachePolicyType.NONE -> MemberCachePolicy.NONE
                    AlunaDiscordProperties.MemberCachePolicyType.ALL -> MemberCachePolicy.ALL
                    AlunaDiscordProperties.MemberCachePolicyType.OWNER -> MemberCachePolicy.OWNER
                    AlunaDiscordProperties.MemberCachePolicyType.ONLINE -> MemberCachePolicy.ONLINE
                    AlunaDiscordProperties.MemberCachePolicyType.VOICE -> MemberCachePolicy.VOICE
                    AlunaDiscordProperties.MemberCachePolicyType.BOOSTER -> MemberCachePolicy.BOOSTER
                    AlunaDiscordProperties.MemberCachePolicyType.PENDING -> MemberCachePolicy.PENDING
                    AlunaDiscordProperties.MemberCachePolicyType.DEFAULT -> MemberCachePolicy.DEFAULT
                }
            )
            .setAutoReconnect(alunaProperties.discord.autoReconnect)

        alunaProperties.discord.cacheFlags.forEach {
            logger.debug("Enable CacheFlags: ${it.name}")
            shardManagerBuilder.enableCache(
                when (it) {
                    AlunaDiscordProperties.CacheFlag.ACTIVITY -> CacheFlag.ACTIVITY
                    AlunaDiscordProperties.CacheFlag.VOICE_STATE -> CacheFlag.VOICE_STATE
                    AlunaDiscordProperties.CacheFlag.EMOTE -> CacheFlag.EMOTE
                    AlunaDiscordProperties.CacheFlag.CLIENT_STATUS -> CacheFlag.CLIENT_STATUS
                    AlunaDiscordProperties.CacheFlag.MEMBER_OVERRIDES -> CacheFlag.MEMBER_OVERRIDES
                    AlunaDiscordProperties.CacheFlag.ROLE_TAGS -> CacheFlag.ROLE_TAGS
                    AlunaDiscordProperties.CacheFlag.ONLINE_STATUS -> CacheFlag.ONLINE_STATUS
                }
            )
        }

        alunaProperties.discord.gatewayIntents.forEach {
            logger.debug("Enable Intent: ${it.name}")
            shardManagerBuilder.enableIntents(it)
        }

        return shardManagerBuilder.build()
    }

}
