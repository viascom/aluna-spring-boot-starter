package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.bot.AutoCompleteHandler
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordSlashCommandInitializedEvent
import io.viascom.discord.bot.aluna.event.EventPublisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
@ConditionalOnJdaEnabled
internal open class AutoCompleteInteractionInitializer(
    private val autoCompleteHandlers: List<AutoCompleteHandler>,
    private val discordBot: DiscordBot,
    private val eventPublisher: EventPublisher
) : ApplicationListener<DiscordSlashCommandInitializedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordSlashCommandInitializedEvent) {
        initAutoCompleteHandlers()
    }

    private fun initAutoCompleteHandlers() {
        logger.debug("Register AutoCompleteHandlers")

        autoCompleteHandlers.forEach { handler ->
            val command = discordBot.commands.entries.firstOrNull { entry -> handler.command.isAssignableFrom(entry.value) }
            if (command == null) {
                logger.warn("Could not register '${handler::class.java.canonicalName}'. No registered command for '${handler.command.canonicalName}' found.")
                return
            }

            discordBot.autoCompleteHandlers[Pair(command.key, handler.option)] = handler::class.java
            logger.debug("\t--> ${handler::class.simpleName} for ${command.value.simpleName} (${handler.option ?: "<all>"})")
        }

        eventPublisher.publishDiscordAutoCompleteHandlerInitializedEvent(autoCompleteHandlers.map { it::class })
    }

}
