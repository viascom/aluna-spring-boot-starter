package io.viascom.discord.bot.starter.bot

import io.viascom.discord.bot.starter.bot.listener.GenericAutoCompleteListener
import io.viascom.discord.bot.starter.bot.listener.ShardReadyEvent
import io.viascom.discord.bot.starter.bot.listener.SlashCommandInteractionEventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
class ListenerRegistration(private val listeners: List<ListenerAdapter>, private val shardManager: ShardManager) :
    ApplicationListener<ApplicationStartedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ApplicationStartedEvent) {
        val listenersToRegister = listeners.filterNot {
            it::class.java.canonicalName in arrayListOf(
                ShardReadyEvent::class.java.canonicalName,
                SlashCommandInteractionEventListener::class.java.canonicalName,
                GenericAutoCompleteListener::class.java.canonicalName
            )
        }
        if(listenersToRegister.isNotEmpty()) {
            logger.debug("Register Listeners:\n" + listenersToRegister.joinToString("\n") { "- ${it::class.java.canonicalName}" })
            shardManager.addEventListener(*listenersToRegister.toTypedArray())
        }
    }

}
