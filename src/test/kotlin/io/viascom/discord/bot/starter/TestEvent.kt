package io.viascom.discord.bot.starter

import OnMessageReceivedEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class TestEvent(private val shardManager: ShardManager) : ApplicationListener<OnMessageReceivedEvent> {

    override fun onApplicationEvent(event: OnMessageReceivedEvent) {
        println("I got a message: ${event.event.message.contentRaw}\n" + shardManager.shards.size)
    }

}
