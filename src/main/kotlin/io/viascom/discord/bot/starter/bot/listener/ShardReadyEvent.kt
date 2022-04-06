package io.viascom.discord.bot.starter.bot.listener

import io.viascom.discord.bot.starter.event.EventPublisher
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class ShardReadyEvent(private val discordReadyEventPublisher: EventPublisher) : ListenerAdapter() {

    override fun onReady(event: ReadyEvent) {
        super.onReady(event)

        //Publish DiscordReadyEvent as soon as all shards are connected
        if ((event.jda.shardInfo.shardId + 1) == (event.jda.shardInfo.shardTotal)) {
            discordReadyEventPublisher.publishDiscordReadyEvent(event)
        }
    }
}
