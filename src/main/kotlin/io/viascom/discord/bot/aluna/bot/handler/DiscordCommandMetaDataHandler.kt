package io.viascom.discord.bot.aluna.bot.handler

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.util.StopWatch

interface DiscordCommandMetaDataHandler {

    fun onCommandExecution(event: SlashCommandInteractionEvent, discordCommand: DiscordCommand)

    fun onExitCommand(event: SlashCommandInteractionEvent, stopWatch: StopWatch?, discordCommand: DiscordCommand)

    fun onGenericExecutionException(
        event: SlashCommandInteractionEvent,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        discordCommand: DiscordCommand
    )

}
