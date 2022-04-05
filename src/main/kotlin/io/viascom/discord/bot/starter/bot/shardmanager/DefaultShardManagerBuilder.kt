package io.viascom.discord.bot.starter.bot.shardmanager

import io.viascom.discord.bot.starter.bot.listener.EventWaiter
import io.viascom.discord.bot.starter.bot.listener.GenericAutoCompleteListener
import io.viascom.discord.bot.starter.bot.listener.ShardReadyEvent
import io.viascom.discord.bot.starter.bot.listener.SlashCommandInteractionEventListener
import io.viascom.discord.bot.starter.property.AlunaProperties
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.LoggerFactory

class DefaultShardManagerBuilder(
    private val shardReadyEvent: ShardReadyEvent,
    private val slashCommandInteractionEventListener: SlashCommandInteractionEventListener,
    private val genericAutoCompleteListener: GenericAutoCompleteListener,
    private val eventWaiter: EventWaiter,
    private val alunaProperties: AlunaProperties
) : ShardManagerBuilder {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun build(): ShardManager {
        val shardManagerBuilder = DefaultShardManagerBuilder.createDefault(alunaProperties.discord.token)
            .addEventListeners(shardReadyEvent)
            .addEventListeners(slashCommandInteractionEventListener)
            .addEventListeners(genericAutoCompleteListener)
            .addEventListeners(eventWaiter)
            .setStatus(OnlineStatus.DO_NOT_DISTURB)
            .setActivity(Activity.playing("loading..."))
            .setBulkDeleteSplittingEnabled(true)
            .setMemberCachePolicy(alunaProperties.discord.memberCachePolicy)
            //.setChunkingFilter(ChunkingFilter.ALL)
            //.enableCache(CacheFlag.ACTIVITY)
            .setAutoReconnect(alunaProperties.discord.autoReconnect)

        alunaProperties.discord.gatewayIntents.forEach {
            logger.debug("Enable Intent: ${it.name}")
            shardManagerBuilder.enableIntents(it)
        }

        return shardManagerBuilder.build()
    }

}
