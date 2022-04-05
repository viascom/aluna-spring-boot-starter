package io.viascom.discord.bot.starter.event

import net.dv8tion.jda.api.events.ReadyEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service


@Service
class DiscordReadyEventPublisher(private val applicationEventPublisher: ApplicationEventPublisher) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun publish(jdaEvent: ReadyEvent) {
        logger.debug("Publishing DiscordReadyEvent.")
        val discordReadyEvent = DiscordReadyEvent(this, jdaEvent)
        applicationEventPublisher.publishEvent(discordReadyEvent)
    }

}
