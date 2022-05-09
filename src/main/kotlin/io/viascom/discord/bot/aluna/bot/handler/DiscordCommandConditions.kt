package io.viascom.discord.bot.aluna.bot.handler

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

interface DiscordCommandConditions {

    fun checkUseScope(
        event: SlashCommandInteractionEvent,
        useScope: DiscordCommand.UseScope,
        subCommandUseScope: HashMap<String, DiscordCommand.UseScope>
    ): Boolean

    fun checkForNeededUserPermissions(event: SlashCommandInteractionEvent, userPermissions: ArrayList<Permission>): DiscordCommand.MissingPermissions
    fun checkForNeededAleevaPermissions(event: SlashCommandInteractionEvent, botPermissions: ArrayList<Permission>): DiscordCommand.MissingPermissions

}
