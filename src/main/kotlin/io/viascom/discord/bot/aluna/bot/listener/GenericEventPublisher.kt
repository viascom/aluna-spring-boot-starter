package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.EventPublisher
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.springframework.stereotype.Service

@Service
@ConditionalOnJdaEnabled
class GenericEventPublisher(
    private val eventPublisher: EventPublisher
) : EventListener {
    override fun onEvent(event: GenericEvent) {
        eventPublisher.publishDiscordEvent(event)
    }
}
