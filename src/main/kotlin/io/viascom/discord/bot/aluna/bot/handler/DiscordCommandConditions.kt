package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.DiscordContextMenu
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

interface DiscordCommandConditions {

    /**
     * Check the use scope for the command.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param useScope Use scope of the command
     * @param subCommandUseScope Use scope of the sub commands
     * @param event Generic interaction event
     * @return WrongUseScope
     */
    fun checkUseScope(
        discordCommand: DiscordCommand,
        useScope: DiscordCommand.UseScope,
        subCommandUseScope: HashMap<String, DiscordCommand.UseScope>,
        event: GenericCommandInteractionEvent
    ): DiscordCommand.WrongUseScope

    /**
     * Check if the user has all the needed permissions.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param userPermissions Needed Permissions
     * @param event Generic interaction event
     * @return MissingPermissions
     */
    fun checkForNeededUserPermissions(
        discordCommand: DiscordCommand,
        userPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): DiscordCommand.MissingPermissions

    /**
     * Check if the bot has all the needed permissions.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param botPermissions Needed Permissions
     * @param event Generic interaction event
     * @return MissingPermissions
     */
    fun checkForNeededBotPermissions(
        discordCommand: DiscordCommand,
        botPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): DiscordCommand.MissingPermissions


    /**
     * Check the use scope for the context menu.
     * Make sure to not block the execution for to long as the context menu needs to be acknowledged in 3 seconds.
     *
     * @param contextMenu Discord context menu instance
     * @param useScope Use scope of the command
     * @param subCommandUseScope Use scope of the sub commands
     * @param event Generic interaction event
     * @return WrongUseScope
     */
    fun checkUseScope(
        contextMenu: DiscordContextMenu,
        useScope: DiscordCommand.UseScope,
        subCommandUseScope: HashMap<String, DiscordCommand.UseScope>,
        event: GenericCommandInteractionEvent
    ): DiscordCommand.WrongUseScope

    /**
     * Check if the user has all the needed permissions.
     * Make sure to not block the execution for to long as the context menu needs to be acknowledged in 3 seconds.
     *
     * @param contextMenu Discord context menu instance
     * @param userPermissions Needed Permissions
     * @param event Generic interaction event
     * @return MissingPermissions
     */
    fun checkForNeededUserPermissions(
        contextMenu: DiscordContextMenu,
        userPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): DiscordCommand.MissingPermissions

    /**
     * Check if the bot has all the needed permissions.
     * Make sure to not block the execution for to long as the context menu needs to be acknowledged in 3 seconds.
     *
     * @param contextMenu Discord context menu instance
     * @param botPermissions Needed Permissions
     * @param event Generic interaction event
     * @return MissingPermissions
     */
    fun checkForNeededBotPermissions(
        contextMenu: DiscordContextMenu,
        botPermissions: ArrayList<Permission>,
        event: GenericCommandInteractionEvent
    ): DiscordCommand.MissingPermissions

}
