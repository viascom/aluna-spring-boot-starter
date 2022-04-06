package io.viascom.discord.bot.starter.bot.listener

import io.viascom.discord.bot.starter.event.EventPublisher
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.springframework.stereotype.Service

@Service
class GenericEventPublisher(
    private val eventPublisher: EventPublisher
) : EventListener {
    override fun onEvent(event: GenericEvent) {
        eventPublisher.publishDiscordEvent(event)
    }
}
