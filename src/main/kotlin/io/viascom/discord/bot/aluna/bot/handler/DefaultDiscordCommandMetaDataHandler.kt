package io.viascom.discord.bot.aluna.bot.handler

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.StopWatch

class DefaultDiscordCommandMetaDataHandler : DiscordCommandMetaDataHandler {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onCommandExecution(event: SlashCommandInteractionEvent, discordCommand: DiscordCommand) {
    }

    override fun onExitCommand(event: SlashCommandInteractionEvent, stopWatch: StopWatch?, discordCommand: DiscordCommand) {
    }

    override fun onGenericExecutionException(
        event: SlashCommandInteractionEvent,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        discordCommand: DiscordCommand
    ) {
        throw throwableOfExecution;
    }
}
