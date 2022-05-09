package io.viascom.discord.bot.aluna.bot.handler

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class DefaultDiscordCommandLoadAdditionalData : DiscordCommandLoadAdditionalData {
    override fun loadData(command: DiscordCommand, event: SlashCommandInteractionEvent) {
    }

    override fun loadData(command: DiscordCommand, event: CommandAutoCompleteInteractionEvent) {
    }
}
