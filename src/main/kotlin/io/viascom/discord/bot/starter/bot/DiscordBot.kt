package io.viascom.discord.bot.starter.bot

import io.viascom.discord.bot.starter.bot.handler.DiscordCommand
import io.viascom.discord.bot.starter.util.AlunaThreadPool
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Service

@Service
open class DiscordBot {

    var shardManager: ShardManager? = null
        private set

    val commands = hashMapOf<String, Class<DiscordCommand>>()
    val commandsWithAutoComplete = hashMapOf<String, Class<DiscordCommand>>()
    val commandExecutor = AlunaThreadPool.getDynamicThreadPool(100, 30, "Aluna-Command-%d")
    val asyncExecutor = AlunaThreadPool.getDynamicThreadPool(100, 10, "Aluna-Async-%d")

}
