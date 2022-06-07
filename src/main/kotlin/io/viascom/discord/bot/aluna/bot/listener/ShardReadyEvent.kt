package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.event.EventPublisher
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true, havingValue = "true")
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
