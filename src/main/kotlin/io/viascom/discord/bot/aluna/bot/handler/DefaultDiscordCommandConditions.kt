package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.bot.emotes.SystemEmoteLoader
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class DefaultDiscordCommandConditions(private val alunaProperties: AlunaProperties, private val systemEmoteLoader: SystemEmoteLoader) :
    DiscordCommandConditions {

    override fun checkUseScope(
        event: SlashCommandInteractionEvent,
        useScope: DiscordCommand.UseScope,
        subCommandUseScope: HashMap<String, DiscordCommand.UseScope>
    ): Boolean {
        val server = event.guild

        if (useScope != DiscordCommand.UseScope.GLOBAL && server == null) {
            event.deferReply(true).setContent("${systemEmoteLoader.getCross().asMention} This command can only be used on a server directly.").queue()
            return false
        }

        if (subCommandUseScope.getOrDefault(event.commandPath, DiscordCommand.UseScope.GLOBAL) != DiscordCommand.UseScope.GLOBAL && server == null) {
            event.deferReply(true).setContent("${systemEmoteLoader.getCross().asMention} This command can only be used on a server directly.").queue()
            return false
        }

        return true
    }

    override fun checkForNeededUserPermissions(event: SlashCommandInteractionEvent, userPermissions: ArrayList<Permission>): DiscordCommand.MissingPermissions {
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

        if (missingPermissions.hasMissingPermissions) {
            val textChannelPermissions = missingPermissions.textChannel.joinToString("\n") { "└ ${it.getName()}" }
            val voiceChannelPermissions = missingPermissions.voiceChannel.joinToString("\n") { "└ ${it.getName()}" }
            val serverPermissions = missingPermissions.server.joinToString("\n") { "└ ${it.getName()}" }
            event.deferReply(true).setContent(
                "${systemEmoteLoader.getCross().asMention} You are missing the following permission to execute this command:\n" +
                        (if (textChannelPermissions.isNotBlank()) textChannelPermissions + "\n" else "") +
                        (if (voiceChannelPermissions.isNotBlank()) voiceChannelPermissions + "\n" else "") +
                        (if (serverPermissions.isNotBlank()) serverPermissions + "\n" else "")
            ).queue()
        }

        return missingPermissions
    }

    override fun checkForNeededAleevaPermissions(
        event: SlashCommandInteractionEvent,
        botPermissions: ArrayList<Permission>
    ): DiscordCommand.MissingPermissions {
        val missingPermissions = DiscordCommand.MissingPermissions()
        val server = event.guild ?: return missingPermissions
        val serverChannel = event.guildChannel
        val member = server.getMember(event.user)

        val selfMember = server.getMemberById(alunaProperties.discord.applicationId!!)!!

        botPermissions.forEach { permission ->
            if (permission.isChannel) {
                if (permission.name.startsWith("VOICE")) {
                    val voiceChannel = member?.voiceState?.channel
                    if (voiceChannel == null) {
                        missingPermissions.notInVoice = true
                        event.deferReply(true)
                            .setContent("${systemEmoteLoader.getCross().asMention} You need to be in a voice channel yourself to execute this command").queue()
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

        if (missingPermissions.hasMissingPermissions) {
            event.deferReply(true).setContent("${systemEmoteLoader.getCross().asMention} I'm missing the following permission to execute this command:\n" +
                    missingPermissions.textChannel.joinToString("\n") { "└ ${it.getName()}" } + "\n" +
                    missingPermissions.voiceChannel.joinToString("\n") { "└ ${it.getName()}" } + "\n" +
                    missingPermissions.server.joinToString("\n") { "└ ${it.getName()}" }
            ).queue()
        }

        return missingPermissions
    }
}
