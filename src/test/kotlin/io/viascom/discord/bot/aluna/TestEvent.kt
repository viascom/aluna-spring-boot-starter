package io.viascom.discord.bot.aluna

import OnMessageReceivedEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.ApplicationListener

class TestEvent(private val shardManager: ShardManager) : ApplicationListener<OnMessageReceivedEvent> {

    override fun onApplicationEvent(event: OnMessageReceivedEvent) {
        println("I got a message: ${event.event.message.contentRaw}\n" + shardManager.shards.size)
    }

}
