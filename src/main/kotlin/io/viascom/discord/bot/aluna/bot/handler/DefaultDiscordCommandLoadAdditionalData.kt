package io.viascom.discord.bot.aluna.bot.handler

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class DefaultDiscordCommandLoadAdditionalData : DiscordCommandLoadAdditionalData {
    override fun loadData(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent) {
    }

    override fun loadData(discordCommand: DiscordCommand, event: CommandAutoCompleteInteractionEvent) {
    }
}
