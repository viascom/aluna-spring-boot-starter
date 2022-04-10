package io.viascom.discord.bot.starter.bot.handler

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

interface DiscordCommandLoadAdditionalData {

    fun loadData(command: DiscordCommand, event: SlashCommandInteractionEvent)
    fun loadData(command: DiscordCommand, event: CommandAutoCompleteInteractionEvent)

}
