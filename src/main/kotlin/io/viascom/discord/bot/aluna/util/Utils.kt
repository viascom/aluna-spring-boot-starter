package io.viascom.discord.bot.aluna.util

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.ModalInteraction
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction
import net.dv8tion.jda.api.sharding.ShardManager
import java.awt.Color
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

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
fun MessageAction.removeActionRows() = this.setActionRows(arrayListOf())

fun ShardManager.getServer(serverId: String): Guild? = this.getGuildById(serverId)
fun ShardManager.getServerTextChannel(serverId: String, channelId: String): MessageChannel? = this.getServer(serverId)?.getTextChannelById(channelId)
fun ShardManager.getServerVoiceChannel(serverId: String, channelId: String): VoiceChannel? = this.getServer(serverId)?.getVoiceChannelById(channelId)
fun ShardManager.getServerMessage(serverId: String, channelId: String, messageId: String): Message? =
    this.getServerTextChannel(serverId, channelId)?.retrieveMessageById(messageId)?.complete()

fun ShardManager.getPrivateChannelByUser(userId: String): MessageChannel? = this.retrieveUserById(userId).complete()?.openPrivateChannel()?.complete()
fun ShardManager.getPrivateChannel(channelId: String): MessageChannel? = this.getPrivateChannelById(channelId)
fun ShardManager.getPrivateMessageByUser(userId: String, messageId: String): Message? {
    return try {
        getPrivateChannelByUser(userId)?.retrieveMessageById(messageId)?.complete()
    } catch (e: Exception) {
        null
    }
}

fun ShardManager.getPrivateMessage(channelId: String, messageId: String): Message? {
    return try {
        getPrivateChannel(channelId)?.retrieveMessageById(messageId)?.complete()
    } catch (e: Exception) {
        null
    }
}

fun SlashCommandInteractionEvent.getOptionAsInt(name: String, default: Int? = null): Int? = this.getOption(name, default, OptionMapping::getAsInt)
fun SlashCommandInteractionEvent.getOptionAsString(name: String, default: String? = null): String? = this.getOption(name, default, OptionMapping::getAsString)
fun SlashCommandInteractionEvent.getOptionAsBoolean(name: String, default: Boolean? = null): Boolean? =
    this.getOption(name, default, OptionMapping::getAsBoolean)

fun SlashCommandInteractionEvent.getOptionAsMember(name: String, default: Member? = null): Member? = this.getOption(name, default, OptionMapping::getAsMember)
fun SlashCommandInteractionEvent.getOptionAsUser(name: String, default: User? = null): User? = this.getOption(name, default, OptionMapping::getAsUser)
fun SlashCommandInteractionEvent.getOptionAsGuildChannel(name: String, default: GuildChannel? = null): GuildChannel? =
    this.getOption(name, default, OptionMapping::getAsGuildChannel)

fun CommandAutoCompleteInteractionEvent.getOptionAsInt(name: String, default: Int? = null): Int? = this.getOption(name, default, OptionMapping::getAsInt)
fun CommandAutoCompleteInteractionEvent.getOptionAsString(name: String, default: String? = null): String? =
    this.getOption(name, default, OptionMapping::getAsString)

fun CommandAutoCompleteInteractionEvent.getOptionAsBoolean(name: String, default: Boolean? = null): Boolean? =
    this.getOption(name, default, OptionMapping::getAsBoolean)

fun CommandAutoCompleteInteractionEvent.getOptionAsMember(name: String, default: Member? = null): Member? =
    this.getOption(name, default, OptionMapping::getAsMember)

fun CommandAutoCompleteInteractionEvent.getOptionAsUser(name: String, default: User? = null): User? = this.getOption(name, default, OptionMapping::getAsUser)

fun CommandAutoCompleteInteractionEvent.replyStringChoices(choices: Map<String, String>): AutoCompleteCallbackAction =
    this.replyChoices(choices.entries.map { Command.Choice(it.key, it.value) })

fun CommandAutoCompleteInteractionEvent.replyLongChoices(choices: Map<String, Long>): AutoCompleteCallbackAction =
    this.replyChoices(choices.entries.map { Command.Choice(it.key, it.value) })

fun CommandAutoCompleteInteractionEvent.replyDoubleChoices(choices: Map<String, Double>): AutoCompleteCallbackAction =
    this.replyChoices(choices.entries.map { Command.Choice(it.key, it.value) })

fun SelectMenuInteractionEvent.getSelection(): String = this.values.first()
fun SelectMenuInteractionEvent.getSelections(): List<String> = this.values

fun Modal.Builder.addTextField(
    id: String,
    label: String,
    style: TextInputStyle = TextInputStyle.SHORT,
    placeholder: String? = null,
    min: Int = -1,
    max: Int = -1,
    value: String? = null,
    required: Boolean = true
): Modal.Builder = this.addActionRow(createTextInput(id, label, style, placeholder, min, max, value, required))

fun ModalInteraction.getValueAsString(name: String, default: String? = null): String? = this.getValue(name)?.asString ?: default

/**
 * Get the probable locale of a user based on the most common locale of the mutual servers.
 *
 * !! This will only work if your bot has access to mutualGuilds which is bound to the GUILD_MEMBERS intent !!
 *
 * @return probable Locale
 */
fun User.probableLocale(): Locale? = mutualGuilds.groupBy { it.locale }.maxByOrNull { it.value.size }?.value?.firstOrNull()?.locale

fun User.getMessage(messageId: String): Message? = try {
    this.openPrivateChannel().complete().retrieveMessageById(messageId).complete()
} catch (e: Exception) {
    null
}

/**
 * Get the probable locale of a user based on the most common locale of the mutual servers.
 *
 * !! This will only work if your bot has access to mutualGuilds which is bound to the GUILD_MEMBERS intent !!
 *
 * @return probable Locale
 */
fun Member.probableLocale(): Locale? = user.probableLocale()

fun EmbedBuilder.setColor(red: Int, green: Int, blue: Int): EmbedBuilder = this.setColor(Color(red, green, blue))
fun EmbedBuilder.setColor(hexColor: String): EmbedBuilder = this.setColor(Color.getColor(hexColor))
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
    max: Int = -1,
    value: String? = null,
    required: Boolean = true
): TextInput {
    val builder = TextInput.create(id, label, style)
        .setPlaceholder(placeholder)

    if (min != -1) {
        builder.minLength = min
    }

    if (max != -1) {
        builder.maxLength = max
    }

    if (value != null && value.isNotBlank()) {
        builder.value = value
    }

    builder.isRequired = required

    return builder.build()
}

fun createPrimaryButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.PRIMARY, id, label, emoji).withDisabled(disabled)

fun createSecondaryButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.SECONDARY, id, label, emoji).withDisabled(disabled)

fun createSuccessButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.SUCCESS, id, label, emoji).withDisabled(disabled)

fun createDangerButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.DANGER, id, label, emoji).withDisabled(disabled)

fun createLinkButton(url: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.LINK, url, label, emoji).withDisabled(disabled)
