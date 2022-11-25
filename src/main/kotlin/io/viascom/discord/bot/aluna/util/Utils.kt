/*
 * Copyright 2022 Viascom Ltd liab. Co
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

@file:JvmName("AlunaUtils")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.util

import io.viascom.discord.bot.aluna.model.CommandOption
import io.viascom.discord.bot.aluna.model.DiscordSticker
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.interactions.modals.ModalInteraction
import net.dv8tion.jda.api.requests.restaction.*
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import java.awt.Color
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Function

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

/**
 * Get the color as a hex representation
 *
 * @return Color as a hex representation
 */
fun Color.toHex(): String = String.format("#%02x%02x%02x", this.red, this.green, this.blue)

/**
 * Get the color as a Discord integer representation
 *
 * @return Color as a Discord integer representation
 */
fun Color.toDiscordColorInt(): Int = (this.red shl 16) + (this.green shl 8) + this.blue

/**
 * Remove all components
 */
fun WebhookMessageCreateAction<Message>.removeComponents() = this.setComponents(arrayListOf())

/**
 * Remove all components
 */
fun WebhookMessageEditAction<Message>.removeComponents() = this.setComponents(arrayListOf())

/**
 * Remove all components
 */
fun MessageCreateAction.removeComponents() = this.setComponents(arrayListOf())

/**
 * Remove all components
 */
fun MessageEditAction.removeComponents() = this.setComponents(arrayListOf())

/**
 * Remove all components
 */
fun MessageEditCallbackAction.removeComponents() = this.setComponents(arrayListOf())

/**
 * Remove all components
 */
fun ReplyCallbackAction.removeComponents() = this.setComponents(arrayListOf())
fun MessageCreateAction.setStickers(vararg stickers: DiscordSticker) = this.setStickers(stickers.map { it.toSticker() })
fun MessageCreateAction.setStickers(stickers: Collection<DiscordSticker>) = this.setStickers(stickers.map { it.toSticker() })

/**
 * Get guild text channel
 *
 * @param guildId guild id
 * @param channelId channel id
 * @return Guild Text Channel
 */
fun ShardManager.getGuildTextChannel(guildId: String, channelId: String): MessageChannel? = this.getGuildById(guildId)?.getTextChannelById(channelId)
fun ShardManager.getGuildVoiceChannel(guildId: String, channelId: String): VoiceChannel? = this.getGuildById(guildId)?.getVoiceChannelById(channelId)
fun ShardManager.getGuildMessage(guildId: String, channelId: String, messageId: String): Message? =
    this.getGuildTextChannel(guildId, channelId)?.retrieveMessageById(messageId)?.complete()

fun ShardManager.getPrivateChannelByUser(userId: String): MessageChannel? = this.retrieveUserById(userId).complete()?.openPrivateChannel()?.complete()
fun ShardManager.getPrivateMessageByUser(userId: String, messageId: String): Message? {
    return try {
        getPrivateChannelByUser(userId)?.retrieveMessageById(messageId)?.complete()
    } catch (e: Exception) {
        null
    }
}

fun ShardManager.getPrivateMessage(channelId: String, messageId: String): Message? {
    return try {
        getPrivateChannelById(channelId)?.retrieveMessageById(messageId)?.complete()
    } catch (e: Exception) {
        null
    }
}

fun ShardManager.getMemberById(guildId: String, userId: String): Member? = this.getGuildById(guildId)?.getMemberById(userId)

fun MessageEditData.toCreateData() = MessageCreateData.fromEditData(this)
fun MessageCreateData.toEditData() = MessageEditData.fromCreateData(this)

fun <T : Any> CommandDataImpl.addOption(option: CommandOption<in T>) {
    this.addOptions(option as OptionData)
}

fun <T : Any> CommandDataImpl.addOptions(vararg option: CommandOption<in T>) {
    option.forEach { this.addOption(it) }
}

/**
 * Bans the user specified by the provided {@link UserSnowflake} and deletes messages sent by the user based on the {@code deletionTimeframe}.
 *
 * @param  user
 *         The {@link UserSnowflake} for the user to ban.
 *         This can be a member or user instance or {@link User#fromId(long)}.
 * @param  deletionTimeframe
 *         The timeframe for the history of messages that will be deleted.
 * @param  reason
 *         The reason for this action which should be logged in the Guild's AuditLogs (up to 512 characters)
 * @return {@link net.dv8tion.jda.api.requests.restaction.AuditableRestAction AuditableRestAction}
 * @see Guild.ban ban
 */
@JvmOverloads
fun Guild.ban(user: UserSnowflake, deletionTimeframe: Duration = Duration.ZERO, reason: String? = null): AuditableRestAction<Void> {
    val action = this.ban(user, deletionTimeframe.seconds.toInt(), TimeUnit.SECONDS)
    return if (reason != null) action.reason(reason) else action
}

/**
 * Puts this Member in time out in this {@link net.dv8tion.jda.api.entities.Guild Guild} for a specific amount of time.
 *
 * @param  duration
 *         The duration to put this Member in time out for
 * @return {@link net.dv8tion.jda.api.requests.restaction.AuditableRestAction AuditableRestAction}
 * @see Member.timeoutFor timeoutFor
 */
//This needs no @JvmOverloads as there is already a timeoutFor(duration: Duration) method
fun Member.timeoutFor(duration: Duration, reason: String? = null): AuditableRestAction<Void> {
    val action = this.timeoutFor(duration)
    return if (reason != null) action.reason(reason) else action
}

@JvmOverloads
@Suppress("UNCHECKED_CAST")
fun <T : Any> SlashCommandInteractionEvent.getTypedOption(option: CommandOption<in T>, default: T? = null): T? {
    val optionData = (option as OptionData)
    return when (optionData.type) {
        OptionType.STRING -> this.getOption(optionData.name, default, OptionMapping::getAsString)
        OptionType.INTEGER -> this.getOption(optionData.name, default, OptionMapping::getAsInt)
        OptionType.BOOLEAN -> this.getOption(optionData.name, default, OptionMapping::getAsBoolean)
        OptionType.USER -> this.getOption(optionData.name, default, OptionMapping::getAsUser)
        OptionType.CHANNEL -> this.getOption(optionData.name, default, OptionMapping::getAsChannel)
        OptionType.ROLE -> this.getOption(optionData.name, default, OptionMapping::getAsRole)
        OptionType.MENTIONABLE -> this.getOption(optionData.name, default, OptionMapping::getAsMentionable)
        OptionType.NUMBER -> this.getOption(optionData.name, default, OptionMapping::getAsDouble)
        OptionType.ATTACHMENT -> this.getOption(optionData.name, default, OptionMapping::getAsAttachment)
        OptionType.UNKNOWN -> throw IllegalArgumentException("Can't convert OptionType.UNKNOWN")
        OptionType.SUB_COMMAND -> throw IllegalArgumentException("Can't convert OptionType.SUB_COMMAND")
        OptionType.SUB_COMMAND_GROUP -> throw IllegalArgumentException("Can't convert OptionType.SUB_COMMAND_GROUP")
    } as T?
}

@JvmOverloads
fun <T : Any> SlashCommandInteractionEvent.getTypedOption(option: CommandOption<in T>, mapper: Function<in OptionMapping, out T?>, default: T? = null): T? {
    val optionData = (option as OptionData)
    return this.getOption(optionData.name)?.let { mapper.apply(it) } ?: default
}

@JvmOverloads
@Suppress("UNCHECKED_CAST")
fun <T : Any> CommandAutoCompleteInteractionEvent.getTypedOption(option: CommandOption<in T>, default: T? = null): T? {
    val optionData = (option as OptionData)
    return when (optionData.type) {
        OptionType.STRING -> this.getOption(optionData.name, default, OptionMapping::getAsString)
        OptionType.INTEGER -> this.getOption(optionData.name, default, OptionMapping::getAsInt)
        OptionType.BOOLEAN -> this.getOption(optionData.name, default, OptionMapping::getAsBoolean)
        OptionType.USER -> this.getOption(optionData.name, default, OptionMapping::getAsUser)
        OptionType.CHANNEL -> this.getOption(optionData.name, default, OptionMapping::getAsChannel)
        OptionType.ROLE -> this.getOption(optionData.name, default, OptionMapping::getAsRole)
        OptionType.MENTIONABLE -> this.getOption(optionData.name, default, OptionMapping::getAsMentionable)
        OptionType.NUMBER -> this.getOption(optionData.name, default, OptionMapping::getAsDouble)
        OptionType.ATTACHMENT -> this.getOption(optionData.name, default, OptionMapping::getAsAttachment)
        OptionType.UNKNOWN -> throw IllegalArgumentException("Can't convert OptionType.UNKNOWN")
        OptionType.SUB_COMMAND -> throw IllegalArgumentException("Can't convert OptionType.SUB_COMMAND")
        OptionType.SUB_COMMAND_GROUP -> throw IllegalArgumentException("Can't convert OptionType.SUB_COMMAND_GROUP")
    } as T?
}

@JvmOverloads
fun <T : Any> CommandAutoCompleteInteractionEvent.getTypedOption(
    option: CommandOption<in T>,
    mapper: Function<in OptionMapping, out T?>,
    default: T? = null
): T? {
    val optionData = (option as OptionData)
    return this.getOption(optionData.name)?.let { mapper.apply(it) } ?: default
}

@JvmOverloads
fun SlashCommandInteractionEvent.getOptionAsInt(name: String, default: Int? = null): Int? = this.getOption(name, default, OptionMapping::getAsInt)

@JvmOverloads
fun SlashCommandInteractionEvent.getOptionAsString(name: String, default: String? = null): String? = this.getOption(name, default, OptionMapping::getAsString)

@JvmOverloads
fun SlashCommandInteractionEvent.getOptionAsBoolean(name: String, default: Boolean? = null): Boolean? =
    this.getOption(name, default, OptionMapping::getAsBoolean)

@JvmOverloads
fun SlashCommandInteractionEvent.getOptionAsMember(name: String, default: Member? = null): Member? = this.getOption(name, default, OptionMapping::getAsMember)

@JvmOverloads
fun SlashCommandInteractionEvent.getOptionAsUser(name: String, default: User? = null): User? = this.getOption(name, default, OptionMapping::getAsUser)

@JvmOverloads
fun SlashCommandInteractionEvent.getOptionAsGuildChannel(name: String, default: GuildChannel? = null): GuildChannel? =
    this.getOption(name, default, OptionMapping::getAsChannel)

@JvmOverloads
fun CommandAutoCompleteInteractionEvent.getOptionAsInt(name: String, default: Int? = null): Int? = this.getOption(name, default, OptionMapping::getAsInt)

@JvmOverloads
fun CommandAutoCompleteInteractionEvent.getOptionAsString(name: String, default: String? = null): String? =
    this.getOption(name, default, OptionMapping::getAsString)

@JvmOverloads
fun CommandAutoCompleteInteractionEvent.getOptionAsBoolean(name: String, default: Boolean? = null): Boolean? =
    this.getOption(name, default, OptionMapping::getAsBoolean)

@JvmOverloads
fun CommandAutoCompleteInteractionEvent.getOptionAsMember(name: String, default: Member? = null): Member? =
    this.getOption(name, default, OptionMapping::getAsMember)

@JvmOverloads
fun CommandAutoCompleteInteractionEvent.getOptionAsUser(name: String, default: User? = null): User? = this.getOption(name, default, OptionMapping::getAsUser)

@JvmOverloads
fun CommandAutoCompleteInteractionEvent.getOptionAsAttachment(name: String, default: Attachment? = null): Attachment? =
    this.getOption(name, default, OptionMapping::getAsAttachment)

/**
 * Reply string choices
 *
 * @param choices HashMap of choices (Key , Value)
 * @return AutoCompleteCallbackAction
 */
fun CommandAutoCompleteInteractionEvent.replyStringChoices(choices: Map<String, String>): AutoCompleteCallbackAction =
    this.replyChoices(choices.entries.map { Command.Choice(it.key, it.value) })

/**
 * Reply long choices
 *
 * @param choices HashMap of choices (Key , Value)
 * @return AutoCompleteCallbackAction
 */
fun CommandAutoCompleteInteractionEvent.replyLongChoices(choices: Map<String, Long>): AutoCompleteCallbackAction =
    this.replyChoices(choices.entries.map { Command.Choice(it.key, it.value) })

/**
 * Reply double choices
 *
 * @param choices HashMap of choices (Key , Value)
 * @return AutoCompleteCallbackAction
 */
fun CommandAutoCompleteInteractionEvent.replyDoubleChoices(choices: Map<String, Double>): AutoCompleteCallbackAction =
    this.replyChoices(choices.entries.map { Command.Choice(it.key, it.value) })

fun StringSelectInteractionEvent.getSelection(): String = this.values.first()
fun StringSelectInteractionEvent.getSelections(): List<String> = this.values

fun EntitySelectInteractionEvent.getUserSelection(): User? = this.jda.shardManager?.getUserById(this.values.first().id)
fun EntitySelectInteractionEvent.getUserSelections(): List<User?> = this.values.map { this.jda.shardManager?.getUserById(it.id) }


inline fun <reified T : Channel> EntitySelectInteractionEvent.getChannelSelection(): T? =
    this.jda.shardManager?.getChannelById(T::class.java, this.values.first().id)

inline fun <reified T : Channel> EntitySelectInteractionEvent.getChannelSelections(): List<T?> =
    this.values.map { this.jda.shardManager?.getChannelById(T::class.java, it.id) }


fun EntitySelectInteractionEvent.getCategorySelection(): Category? = this.jda.shardManager?.getCategoryById(this.values.first().id)
fun EntitySelectInteractionEvent.getCategorySelections(): List<Category?> = this.values.map { this.jda.shardManager?.getCategoryById(it.id) }


fun EntitySelectInteractionEvent.getRoleSelection(): Role? = this.jda.shardManager?.getRoleById(this.values.first().id)
fun EntitySelectInteractionEvent.getRoleSelections(): List<Role?> = this.values.map { this.jda.shardManager?.getRoleById(it.id) }


@JvmOverloads
fun Modal.Builder.addTextField(
    id: String,
    label: String,
    style: TextInputStyle = TextInputStyle.SHORT,
    placeholder: String? = null,
    min: Int = -1,
    max: Int = -1,
    value: String? = null,
    required: Boolean = true
): Modal.Builder = this.addActionRow(textInput(id, label, style, placeholder, min, max, value, required))

@JvmOverloads
fun ModalInteraction.getValueAsString(name: String, default: String? = null): String? = this.getValue(name)?.asString ?: default

/**
 * Get the probable locale of a user based on the most common locale of the mutual servers.
 *
 * !! This will only work if your bot has access to mutualGuilds which is bound to the GUILD_MEMBERS intent !!
 *
 * @return probable Locale
 */
fun User.probableLocale(): DiscordLocale? = mutualGuilds.groupBy { it.locale }.maxByOrNull { it.value.size }?.value?.firstOrNull()?.locale

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
fun Member.probableLocale(): DiscordLocale? = user.probableLocale()

fun EmbedBuilder.setColor(red: Int, green: Int, blue: Int): EmbedBuilder = this.setColor(Color(red, green, blue))
fun EmbedBuilder.setColor(hexColor: String): EmbedBuilder = this.setColor(Color.getColor(hexColor))

@JvmOverloads
fun selectOption(label: String, value: String, description: String? = null, emoji: Emoji? = null, isDefault: Boolean? = null): SelectOption {
    var option = SelectOption.of(label, value)
    description?.let { option = option.withDescription(description) }
    emoji?.let { option = option.withEmoji(emoji) }
    isDefault?.let { option = option.withDefault(isDefault) }

    return option
}

@JvmOverloads
fun StringSelectMenu.Builder.addOption(
    label: String,
    value: String,
    description: String? = null,
    emoji: Emoji? = null,
    isDefault: Boolean? = null
): StringSelectMenu.Builder = this.addOptions(selectOption(label, value, description, emoji, isDefault))

@JvmOverloads
fun textInput(
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

    if (!value.isNullOrBlank()) {
        builder.value = value
    }

    builder.isRequired = required

    return builder.build()
}

@JvmOverloads
fun primaryButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.PRIMARY, id, label, emoji).withDisabled(disabled)

@JvmOverloads
fun secondaryButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.SECONDARY, id, label, emoji).withDisabled(disabled)

@JvmOverloads
fun successButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.SUCCESS, id, label, emoji).withDisabled(disabled)

@JvmOverloads
fun dangerButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.DANGER, id, label, emoji).withDisabled(disabled)

@JvmOverloads
fun linkButton(url: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.LINK, url, label, emoji).withDisabled(disabled)

fun String.toEmoji() = Emoji.fromFormatted(this)

fun DiscordLocale.toLocale() = Locale.forLanguageTag(this.locale)