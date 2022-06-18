package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.DiscordContextMenu
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.util.StopWatch

class DefaultDiscordCommandMetaDataHandler : DiscordCommandMetaDataHandler {
    override fun onCommandExecution(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent) {
    }

    override fun onContextMenuExecution(contextMenu: DiscordContextMenu, event: GenericCommandInteractionEvent) {
    }

    override fun onExitCommand(discordCommand: DiscordCommand, stopWatch: StopWatch?, event: SlashCommandInteractionEvent) {
    }

    override fun onExitCommand(contextMenu: DiscordContextMenu, stopWatch: StopWatch?, event: GenericCommandInteractionEvent) {
    }

    override fun onGenericExecutionException(
        discordCommand: DiscordCommand,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        event: GenericCommandInteractionEvent
    ) {
        throw throwableOfExecution;
    }

    override fun onGenericExecutionException(
        contextMenu: DiscordContextMenu,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        event: GenericCommandInteractionEvent
    ) {
        throw throwableOfExecution;
    }

}
