package io.viascom.discord.bot.starter.util

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction
import net.dv8tion.jda.api.sharding.ShardManager
import java.awt.Color
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

object Utils {
}

fun LocalDateTime.toDate(): Date = Date.from(this.atZone(ZoneId.systemDefault()).toInstant())

fun String.toUUID(): UUID = UUID.fromString(this)

fun String.hashMD5(): String = MessageDigest
    .getInstance("MD5")
    .digest(this.toByteArray())
    .fold("") { str, it -> str + "%02x".format(it) }

fun String.hashSHA256(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this.toByteArray())
    .fold("") { str, it -> str + "%02x".format(it) }

fun String.hashSHA1(): String = MessageDigest
    .getInstance("SHA-1")
    .digest(this.toByteArray())
    .fold("") { str, it -> str + "%02x".format(it) }

fun Color.toHex(): String = String.format("#%02x%02x%02x", this.red, this.green, this.blue)

fun MessageEditCallbackAction.removeActionRows() = this.setActionRows(arrayListOf())

fun ShardManager.getServer(serverId: String): Guild? = this.getGuildById(serverId)
fun ShardManager.getServerTextChannel(serverId: String, channelId: String): MessageChannel? = this.getServer(serverId)?.getTextChannelById(channelId)
fun ShardManager.getServerVoiceChannel(serverId: String, channelId: String): VoiceChannel? = this.getServer(serverId)?.getVoiceChannelById(channelId)
fun ShardManager.getServerMessage(serverId: String, channelId: String, messageId: String): Message? =
    this.getServerTextChannel(serverId, channelId)?.retrieveMessageById(messageId)?.complete()

fun SlashCommandInteractionEvent.getOptionAsInt(name: String, default: Int? = null): Int? = this.getOption(name, default, OptionMapping::getAsInt)
fun SlashCommandInteractionEvent.getOptionAsString(name: String, default: String? = null): String? = this.getOption(name, default, OptionMapping::getAsString)
fun SlashCommandInteractionEvent.getOptionAsBoolean(name: String, default: Boolean? = null): Boolean? = this.getOption(name, default, OptionMapping::getAsBoolean)
fun SlashCommandInteractionEvent.getOptionAsMember(name: String, default: Member? = null): Member? = this.getOption(name, default, OptionMapping::getAsMember)
fun SlashCommandInteractionEvent.getOptionAsUser(name: String, default: User? = null): User? = this.getOption(name, default, OptionMapping::getAsUser)
