package io.viascom.discord.bot.starter.util

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.ModalInteraction
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction
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

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

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
fun WebhookMessageUpdateAction<Message>.removeActionRows() = this.setActionRows(arrayListOf())

fun ShardManager.getServer(serverId: String): Guild? = this.getGuildById(serverId)
fun ShardManager.getServerTextChannel(serverId: String, channelId: String): MessageChannel? = this.getServer(serverId)?.getTextChannelById(channelId)
fun ShardManager.getServerVoiceChannel(serverId: String, channelId: String): VoiceChannel? = this.getServer(serverId)?.getVoiceChannelById(channelId)
fun ShardManager.getServerMessage(serverId: String, channelId: String, messageId: String): Message? =
    this.getServerTextChannel(serverId, channelId)?.retrieveMessageById(messageId)?.complete()

fun SlashCommandInteractionEvent.getOptionAsInt(name: String, default: Int? = null): Int? = this.getOption(name, default, OptionMapping::getAsInt)
fun SlashCommandInteractionEvent.getOptionAsString(name: String, default: String? = null): String? = this.getOption(name, default, OptionMapping::getAsString)
fun SlashCommandInteractionEvent.getOptionAsBoolean(name: String, default: Boolean? = null): Boolean? =
    this.getOption(name, default, OptionMapping::getAsBoolean)

fun SlashCommandInteractionEvent.getOptionAsMember(name: String, default: Member? = null): Member? = this.getOption(name, default, OptionMapping::getAsMember)
fun SlashCommandInteractionEvent.getOptionAsUser(name: String, default: User? = null): User? = this.getOption(name, default, OptionMapping::getAsUser)

fun CommandAutoCompleteInteractionEvent.getOptionAsInt(name: String, default: Int? = null): Int? = this.getOption(name, default, OptionMapping::getAsInt)
fun CommandAutoCompleteInteractionEvent.getOptionAsString(name: String, default: String? = null): String? =
    this.getOption(name, default, OptionMapping::getAsString)

fun CommandAutoCompleteInteractionEvent.getOptionAsBoolean(name: String, default: Boolean? = null): Boolean? =
    this.getOption(name, default, OptionMapping::getAsBoolean)

fun CommandAutoCompleteInteractionEvent.getOptionAsMember(name: String, default: Member? = null): Member? =
    this.getOption(name, default, OptionMapping::getAsMember)

fun CommandAutoCompleteInteractionEvent.getOptionAsUser(name: String, default: User? = null): User? = this.getOption(name, default, OptionMapping::getAsUser)

fun SelectMenuInteractionEvent.getSelection(): String = this.values.first()
fun SelectMenuInteractionEvent.getSelections(): List<String> = this.values

fun ModalInteraction.getValueAsString(name: String, default: String? = null): String? = this.getValue(name)?.asString ?: default

fun EmbedBuilder.setColor(red: Int, green: Int, blue: Int): EmbedBuilder = this.setColor(Color(red, green, blue))
fun createSelectOption(label: String, value: String, description: String? = null, emoji: Emoji? = null, isDefault: Boolean? = null): SelectOption {
    var option = SelectOption.of(label, value)
    description?.let { option = option.withDescription(description) }
    emoji?.let { option = option.withEmoji(emoji) }
    isDefault?.let { option = option.withDefault(isDefault) }

    return option
}

fun SelectMenu.Builder.addOption(
    label: String,
    value: String,
    description: String? = null,
    emoji: Emoji? = null,
    isDefault: Boolean? = null
): SelectMenu.Builder = this.addOptions(createSelectOption(label, value, description, emoji, isDefault))

fun createTextInput(
    id: String,
    label: String,
    style: TextInputStyle = TextInputStyle.SHORT,
    placeholder: String? = null,
    min: Int = -1,
    max: Int = -1
): TextInput {
    val builder = TextInput.create(id, label, style)
        .setPlaceholder(placeholder)

    if (min != -1) {
        builder.minLength = min
    }

    if (max != -1) {
        builder.maxLength = max
    }

    return builder.build()
}

fun createPrimaryButton(id: String, label: String? = null, emoji: Emoji? = null): Button = Button.of(ButtonStyle.PRIMARY, id, label, emoji)
fun createSecondaryButton(id: String, label: String? = null, emoji: Emoji? = null): Button = Button.of(ButtonStyle.SECONDARY, id, label, emoji)
fun createSuccessButton(id: String, label: String? = null, emoji: Emoji? = null): Button = Button.of(ButtonStyle.SUCCESS, id, label, emoji)
fun createDangerButton(id: String, label: String? = null, emoji: Emoji? = null): Button = Button.of(ButtonStyle.DANGER, id, label, emoji)

