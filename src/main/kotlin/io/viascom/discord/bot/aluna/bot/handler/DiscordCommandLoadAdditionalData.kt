package io.viascom.discord.bot.aluna.bot.handler

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

interface DiscordCommandLoadAdditionalData {

    fun loadData(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent)
    fun loadData(discordCommand: DiscordCommand, event: CommandAutoCompleteInteractionEvent)

}
