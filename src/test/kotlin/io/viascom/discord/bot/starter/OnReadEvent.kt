package io.viascom.discord.bot.starter

import io.viascom.discord.bot.starter.event.DiscordFirstShardReadyEvent
import io.viascom.discord.bot.starter.translation.MessageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.*

@Component
class OnReadEvent(
    private val messageService: MessageService
) {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onShardReadyEvent(event: DiscordFirstShardReadyEvent) {
        logger.info(messageService.get("command.key.description", Locale.ENGLISH))
        logger.info(messageService.get("command.key.description", Locale.GERMAN))
        logger.info(messageService.get("command.key.description", Locale.TAIWAN))
    }

}
