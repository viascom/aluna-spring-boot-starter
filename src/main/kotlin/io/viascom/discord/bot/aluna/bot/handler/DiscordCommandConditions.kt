package io.viascom.discord.bot.aluna.bot.handler

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

interface DiscordCommandConditions {

    fun checkUseScope(
        event: SlashCommandInteractionEvent,
        useScope: DiscordCommand.UseScope,
        subCommandUseScope: HashMap<String, DiscordCommand.UseScope>,
        discordCommand: DiscordCommand
    ): DiscordCommand.WrongUseScope

    fun checkForNeededUserPermissions(
        event: SlashCommandInteractionEvent,
        userPermissions: ArrayList<Permission>,
        discordCommand: DiscordCommand
    ): DiscordCommand.MissingPermissions

    fun checkForNeededBotPermissions(
        event: SlashCommandInteractionEvent,
        botPermissions: ArrayList<Permission>,
        discordCommand: DiscordCommand
    ): DiscordCommand.MissingPermissions

    fun checkForAdditionalRequirements(event: SlashCommandInteractionEvent, discordCommand: DiscordCommand): DiscordCommand.AdditionalRequirements
}
