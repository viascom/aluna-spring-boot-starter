package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.event.DiscordCommandEvent
import io.viascom.discord.bot.aluna.translation.MessageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class OnReadEvent(
    private val messageService: MessageService
) : ApplicationListener<DiscordCommandEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordCommandEvent) {
        logger.info("Command `/${event.commandPath}` got called")
    }

}
