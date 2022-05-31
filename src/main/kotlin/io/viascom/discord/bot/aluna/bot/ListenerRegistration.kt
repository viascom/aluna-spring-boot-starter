package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.bot.listener.EventWaiter
import io.viascom.discord.bot.aluna.bot.listener.GenericEventPublisher
import io.viascom.discord.bot.aluna.bot.listener.ServerNotificationEvent
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
            //Filter out static registered listeners
            it::class.java.canonicalName.startsWith("io.viascom.discord.bot.aluna.bot.listener") &&
                    it::class.java.canonicalName != ServerNotificationEvent::class.java.canonicalName
        }
        val internalListeners = listeners.filter {
            it::class.java.canonicalName.startsWith("io.viascom.discord.bot.aluna.bot.listener") &&
                    it::class.java.canonicalName != ServerNotificationEvent::class.java.canonicalName
        }
        logger.debug(
            "Register internal listeners: [${GenericEventPublisher::class.java.canonicalName}, ${EventWaiter::class.java.canonicalName}, " +
                    internalListeners.joinToString(", ") { it::class.java.canonicalName } + "]")
        if (listenersToRegister.isNotEmpty()) {
            logger.debug("Register listeners:\n" + listenersToRegister.joinToString("\n") { "- ${it::class.java.canonicalName}" })
            shardManager.addEventListener(*listenersToRegister.toTypedArray())
        }
    }

}
