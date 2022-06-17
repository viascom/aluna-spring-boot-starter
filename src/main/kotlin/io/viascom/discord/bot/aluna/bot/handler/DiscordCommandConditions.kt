package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

interface DiscordCommandConditions {

    /**
     * Check the use scope f the command.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param useScope Use scope of the command
     * @param subCommandUseScope Use scope of the sub commands
     * @param event Slash command event
     * @return WrongUseScope
     */
    fun checkUseScope(
        discordCommand: DiscordCommand,
        useScope: DiscordCommand.UseScope,
        subCommandUseScope: HashMap<String, DiscordCommand.UseScope>,
        event: SlashCommandInteractionEvent
    ): DiscordCommand.WrongUseScope

    /**
     * Check if the user has all the needed permissions.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param userPermissions Needed Permissions
     * @param event Slash command event
     * @return MissingPermissions
     */
    fun checkForNeededUserPermissions(
        discordCommand: DiscordCommand,
        userPermissions: ArrayList<Permission>,
        event: SlashCommandInteractionEvent
    ): DiscordCommand.MissingPermissions

    /**
     * Check if the bot has all the needed permissions.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param botPermissions Needed Permissions
     * @param event Slash command event
     * @return MissingPermissions
     */
    fun checkForNeededBotPermissions(
        discordCommand: DiscordCommand,
        botPermissions: ArrayList<Permission>,
        event: SlashCommandInteractionEvent
    ): DiscordCommand.MissingPermissions

}
