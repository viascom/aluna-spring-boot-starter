package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.DiscordContextMenu
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent

class DefaultDiscordCommandAdditionalConditions : DiscordCommandAdditionalConditions {
    override fun checkForAdditionalCommandRequirements(
        discordCommand: DiscordCommand,
        event: SlashCommandInteractionEvent
    ): DiscordCommand.AdditionalRequirements {
        return DiscordCommand.AdditionalRequirements()
    }

    override fun checkForAdditionalContextRequirements(
        contextMenu: DiscordContextMenu,
        event: UserContextInteractionEvent
    ): DiscordCommand.AdditionalRequirements {
        return DiscordCommand.AdditionalRequirements()
    }

    override fun checkForAdditionalContextRequirements(
        contextMenu: DiscordContextMenu,
        event: MessageContextInteractionEvent
    ): DiscordCommand.AdditionalRequirements {
        return DiscordCommand.AdditionalRequirements()
    }

}
