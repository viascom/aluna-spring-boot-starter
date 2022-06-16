package io.viascom.discord.bot.aluna.bot.handler

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

interface DiscordCommandAdditionalConditions {

    /**
     * Check for additional requirements.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param event Slash command event
     * @return AdditionalRequirements
     */
    fun checkForAdditionalRequirements(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent): DiscordCommand.AdditionalRequirements
}
