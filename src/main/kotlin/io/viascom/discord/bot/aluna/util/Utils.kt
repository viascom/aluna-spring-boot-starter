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
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
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
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
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
import net.dv8tion.jda.api.sharding.ShardManager
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

fun ShardManager.getMemberById(guildId: String, userId: String): Member? = this.getGuildById(guildId)?.getMemberById(userId)

fun <T : Any> CommandDataImpl.addOption(option: CommandOption<in T>) {
    this.addOptions(option as OptionData)
}

fun <T : Any> CommandDataImpl.addOptions(vararg option: CommandOption<in T>) {
    option.forEach { this.addOption(it) }
}

fun <T : Any> SubcommandData.addOption(option: CommandOption<in T>) {
    this.addOptions(option as OptionData)
}

fun <T : Any> SubcommandData.addOptions(vararg option: CommandOption<in T>) {
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

fun User.tryToSendDM(message: String, then: Runnable) {
    try {
        this.openPrivateChannel().queue({ pc -> pc.sendMessage(message).queue({ then.run() }) { then.run() } }) { then.run() }
    } catch (ignore: Exception) {
    }
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

/**
 * Get the probable locale of a user based on the most common locale of the mutual servers.
 *
 * !! This will only work if your bot has access to mutualGuilds which is bound to the GUILD_MEMBERS intent !!
 *
 * @return probable Locale
 */
fun Member.probableLocale(): DiscordLocale? = user.probableLocale()

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

fun levenshteinDistance(str1: String, str2: String): Double {
    val len1 = str1.length
    val len2 = str2.length
    val dp = Array(len1 + 1) { IntArray(len2 + 1) }
    for (i in 0..len1) {
        dp[i][0] = i
    }
    for (j in 0..len2) {
        dp[0][j] = j
    }
    for (i in 1..len1) {
        for (j in 1..len2) {
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + if (str1[i - 1] == str2[j - 1]) 0 else 1
            )
        }
    }
    val distance = dp[len1][len2]
    return 1.0 - (distance.toDouble() / maxOf(str1.length, str2.length))
}