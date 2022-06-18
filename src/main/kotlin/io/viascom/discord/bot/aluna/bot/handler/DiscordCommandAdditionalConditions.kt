package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.DiscordContextMenu
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent

interface DiscordCommandAdditionalConditions {

    /**
     * Check for additional requirements.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param event Slash command event
     * @return AdditionalRequirements
     */
    fun checkForAdditionalCommandRequirements(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent): DiscordCommand.AdditionalRequirements

    /**
     * Check for additional requirements.
     * Make sure to not block the execution for to long as the interaction needs to be acknowledged in 3 seconds.
     *
     * @param contextMenu Discord context menu instance
     * @param event User context menu event
     * @return AdditionalRequirements
     */
    fun checkForAdditionalContextRequirements(contextMenu: DiscordContextMenu, event: UserContextInteractionEvent): DiscordCommand.AdditionalRequirements


    /**
     * Check for additional requirements.
     * Make sure to not block the execution for to long as the interaction needs to be acknowledged in 3 seconds.
     *
     * @param contextMenu Discord context menu instance
     * @param event Message context menu event
     * @return AdditionalRequirements
     */
    fun checkForAdditionalContextRequirements(contextMenu: DiscordContextMenu, event: MessageContextInteractionEvent): DiscordCommand.AdditionalRequirements
}
