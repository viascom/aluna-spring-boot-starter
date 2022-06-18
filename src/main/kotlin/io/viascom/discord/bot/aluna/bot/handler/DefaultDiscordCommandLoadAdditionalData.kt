package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.DiscordContextMenu
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class DefaultDiscordCommandLoadAdditionalData : DiscordCommandLoadAdditionalData {
    override fun loadData(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent) {
    }

    override fun loadData(discordCommand: DiscordCommand, event: CommandAutoCompleteInteractionEvent) {
    }

    override fun loadData(event: CommandAutoCompleteInteractionEvent) {
    }

    override fun loadData(contextMenu: DiscordContextMenu, event: GenericCommandInteractionEvent) {
    }

}
