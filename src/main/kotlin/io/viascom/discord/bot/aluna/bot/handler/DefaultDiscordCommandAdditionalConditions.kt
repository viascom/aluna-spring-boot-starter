package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class DefaultDiscordCommandAdditionalConditions : DiscordCommandAdditionalConditions {
    override fun checkForAdditionalRequirements(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent): DiscordCommand.AdditionalRequirements {
        return DiscordCommand.AdditionalRequirements()
    }
}
