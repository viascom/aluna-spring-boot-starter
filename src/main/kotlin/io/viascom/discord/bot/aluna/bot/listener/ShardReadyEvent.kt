package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.EventPublisher
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
class ShardReadyEvent(private val discordReadyEventPublisher: EventPublisher) : ListenerAdapter() {

    override fun onReady(event: ReadyEvent) {
        super.onReady(event)

        //If first shard is connected, trigger command update
        if (event.jda.shardInfo.shardId == 0) {
            discordReadyEventPublisher.publishDiscordFirstShardReadyEvent(event)
        }

        //Publish DiscordReadyEvent as soon as all shards are connected
        if ((event.jda.shardInfo.shardId + 1) == (event.jda.shardInfo.shardTotal)) {
            discordReadyEventPublisher.publishDiscordReadyEvent(event)
        }
    }
}
