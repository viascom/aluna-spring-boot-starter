package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.event.DiscordFirstShardReadyEvent
import io.viascom.discord.bot.aluna.translation.MessageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class OnReadEvent(
    private val messageService: MessageService
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onShardReadyEvent(event: DiscordFirstShardReadyEvent) {

    }

}
