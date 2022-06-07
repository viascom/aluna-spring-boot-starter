package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.event.EventPublisher
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true, havingValue = "true")
class GenericEventPublisher(
    private val eventPublisher: EventPublisher
) : EventListener {
    override fun onEvent(event: GenericEvent) {
        eventPublisher.publishDiscordEvent(event)
    }
}
