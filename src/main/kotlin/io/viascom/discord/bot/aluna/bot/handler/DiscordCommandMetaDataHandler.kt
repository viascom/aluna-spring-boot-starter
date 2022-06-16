package io.viascom.discord.bot.aluna.bot.handler

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.util.StopWatch

/**
 * Interface to implement if you want to execute actions on certain points during command execution..
 */
interface DiscordCommandMetaDataHandler {

    /**
     * Gets called asynchronously before the command is executed.
     *
     * @param discordCommand Discord command instance
     * @param event
     */
    fun onCommandExecution(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent)

    /**
     * Gets called asynchronously after the command is executed.
     * This gets also called if the command execution throws an exception.
     *
     * @param discordCommand Discord command instance
     * @param stopWatch StopWatch instance if enabled
     * @param event Slash command event
     */
    fun onExitCommand(discordCommand: DiscordCommand, stopWatch: StopWatch?, event: SlashCommandInteractionEvent)

    /**
     * Gets called if the command defined onExecutionException throws an exception.
     *
     * @param discordCommand Discord command instance
     * @param throwableOfExecution initial exception from the execution
     * @param exceptionOfSpecificHandler exception thrown by onExecutionException
     * @param event Slash command event
     */
    fun onGenericExecutionException(
        discordCommand: DiscordCommand,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        event: SlashCommandInteractionEvent
    )

}
