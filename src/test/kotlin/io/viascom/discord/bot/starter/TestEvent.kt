package io.viascom.discord.bot.starter

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component

@Component
class TestEvent(private val shardManager: ShardManager) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        //println("I got a message: ${event.message.contentRaw}\n" + shardManager.shards.size)
    }

}
