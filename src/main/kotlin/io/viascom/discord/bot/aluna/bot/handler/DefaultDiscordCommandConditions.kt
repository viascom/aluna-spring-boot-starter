package io.viascom.discord.bot.aluna.bot.handler

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class DefaultDiscordCommandConditions : DiscordCommandConditions {

    override fun checkUseScope(
        event: SlashCommandInteractionEvent,
        useScope: DiscordCommand.UseScope,
        subCommandUseScope: HashMap<String, DiscordCommand.UseScope>,
        discordCommand: DiscordCommand
    ): DiscordCommand.WrongUseScope {
        val wrongUseScope = DiscordCommand.WrongUseScope()

        val server = event.guild

        if (useScope != DiscordCommand.UseScope.GLOBAL && server == null) {
            wrongUseScope.serverOnly = true
        }

        if (subCommandUseScope.getOrDefault(event.commandPath, DiscordCommand.UseScope.GLOBAL) != DiscordCommand.UseScope.GLOBAL && server == null) {
            wrongUseScope.subCommandServerOnly = true
        }

        return wrongUseScope
    }

    override fun checkForNeededUserPermissions(
        event: SlashCommandInteractionEvent,
        userPermissions: ArrayList<Permission>,
        discordCommand: DiscordCommand
    ): DiscordCommand.MissingPermissions {
        val missingPermissions = DiscordCommand.MissingPermissions()
        val server = event.guild ?: return missingPermissions
        val serverChannel = event.guildChannel
        val member = server.getMember(event.user)

        userPermissions.forEach { permission ->
            if (permission.isChannel) {
                if (!member!!.hasPermission(serverChannel, permission)) {
                    missingPermissions.textChannel.add(permission)
                }
            } else {
                if (!member!!.hasPermission(permission)) {
                    missingPermissions.server.add(permission)
                }
            }
        }

        return missingPermissions
    }

    override fun checkForNeededBotPermissions(
        event: SlashCommandInteractionEvent,
        botPermissions: ArrayList<Permission>,
        discordCommand: DiscordCommand
    ): DiscordCommand.MissingPermissions {
        val missingPermissions = DiscordCommand.MissingPermissions()
        val server = event.guild ?: return missingPermissions
        val serverChannel = event.guildChannel
        val member = server.getMember(event.user)

        val selfMember = server.getMemberById(event.jda.selfUser.id)!!

        botPermissions.forEach { permission ->
            if (permission.isChannel) {
                if (permission.name.startsWith("VOICE")) {
                    val voiceChannel = member?.voiceState?.channel
                    if (voiceChannel == null) {
                        missingPermissions.notInVoice = true
                        return missingPermissions
                    }

                    if (!selfMember.hasPermission(voiceChannel, permission)) {
                        missingPermissions.voiceChannel.add(permission)
                    }
                }

                if (!selfMember.hasPermission(serverChannel, permission)) {
                    missingPermissions.textChannel.add(permission)
                }
            } else {
                if (!selfMember.hasPermission(permission)) {
                    missingPermissions.server.add(permission)
                }
            }
        }

        return missingPermissions
    }

    override fun checkForAdditionalRequirements(event: SlashCommandInteractionEvent, discordCommand: DiscordCommand): DiscordCommand.AdditionalRequirements {
        return DiscordCommand.AdditionalRequirements()
    }
}
