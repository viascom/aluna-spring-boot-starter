package io.viascom.discord.bot.aluna.bot.handler

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.StopWatch

class DefaultDiscordCommandMetaDataHandler : DiscordCommandMetaDataHandler {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onCommandExecution(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent) {
    }

    override fun onExitCommand(discordCommand: DiscordCommand, stopWatch: StopWatch?, event: SlashCommandInteractionEvent) {
    }

    override fun onGenericExecutionException(
        discordCommand: DiscordCommand,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        event: SlashCommandInteractionEvent
    ) {
        throw throwableOfExecution;
    }
}
