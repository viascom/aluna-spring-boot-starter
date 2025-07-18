/*
 * Copyright 2025 Viascom Ltd liab. Co
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
import net.dv8tion.jda.api.entities.emoji.EmojiUnion
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
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
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import java.awt.Color
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.function.Function

/**
 * Rounds the double value to the specified number of decimal places.
 *
 * @param decimals The number of decimal places to round to.
 * @return The rounded double value.
 */
public fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

/**
 * Get the color as a hex representation
 *
 * @return Color as a hex representation
 */
public fun Color.toHex(): String = String.format("#%02x%02x%02x", this.red, this.green, this.blue)

/**
 * Get the color as a Discord integer representation
 *
 * @return Color as a Discord integer representation
 */
public fun Color.toDiscordColorInt(): Int = (this.red shl 16) + (this.green shl 8) + this.blue

public fun <T : Any> CommandDataImpl.addOption(option: CommandOption<in T>) {
    this.addOptions(option as OptionData)
}

public fun <T : Any> CommandDataImpl.addOptions(vararg option: CommandOption<in T>) {
    option.forEach { this.addOption(it) }
}

public fun <T : Any> SubcommandData.addOption(option: CommandOption<in T>) {
    this.addOptions(option as OptionData)
}

public fun <T : Any> SubcommandData.addOptions(vararg option: CommandOption<in T>) {
    option.forEach { this.addOption(it) }
}

/**
 * Bans the user and deletes messages sent by the user if defined.
 *
 * @param  user The for the user to ban.
 *              This can be a member or user instance or {@link User#fromId(long)}.
 * @param  deletionTimeframe
 *         The timeframe for the history of messages that will be deleted.
 * @param  reason
 *         The reason for this action which should be logged in the Guild's AuditLogs (up to 512 characters)
 * @return {@link net.dv8tion.jda.api.requests.restaction.AuditableRestAction AuditableRestAction}
 * @see Guild.ban ban
 */
@JvmOverloads
public fun Guild.ban(user: UserSnowflake, deletionTimeframe: Duration = Duration.ZERO, reason: String? = null): AuditableRestAction<Void> {
    val action = this.ban(user, deletionTimeframe.seconds.toInt(), TimeUnit.SECONDS)
    return if (reason != null) action.reason(reason) else action
}

/**
 * Retrieves the value of a typed option.
 *
 * @param option The CommandOption to retrieve the value for.
 * @param default The default value to return if the option is not found. Default is `null`.
 * @return The value of the option, or the default value if the option is not found.
 * @throws IllegalArgumentException if the option type is OptionType.UNKNOWN, OptionType.SUB_COMMAND, or OptionType.SUB_COMMAND_GROUP.
 */
@JvmOverloads
@Throws(IllegalArgumentException::class)
@Suppress("UNCHECKED_CAST")
public fun <T : Any> SlashCommandInteractionEvent.getTypedOption(option: CommandOption<in T>, default: T? = null): T? {
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

/**
 * Retrieves the value of a typed command option.
 *
 * @param option The command option to retrieve the value for.
 * @param mapper The function to apply to the OptionMapping in order to convert it to the desired type.
 * @param default The default value to return if the option is not found. Defaults to null.
 * @return The value of the command option, or the default value if not found.
 * @throws IllegalArgumentException if the option type is OptionType.UNKNOWN, OptionType.SUB_COMMAND, or OptionType.SUB_COMMAND_GROUP.
 */
@JvmOverloads
@Throws(IllegalArgumentException::class)
public fun <T : Any> SlashCommandInteractionEvent.getTypedOption(option: CommandOption<in T>, mapper: Function<in OptionMapping, out T?>, default: T? = null): T? {
    val optionData = (option as OptionData)
    return this.getOption(optionData.name)?.let { mapper.apply(it) } ?: default
}

/**
 * Retrieves the value of a typed command option.
 *
 * @param option The command option to retrieve the value for.
 * @param default The default value to return if the option is not present in the event. Defaults to null.
 * @return The value of the command option, converted to the specified type T, or the default value if not present.
 * @throws IllegalArgumentException If the option type is OptionType.UNKNOWN, OptionType.SUB_COMMAND, or OptionType.SUB_COMMAND_GROUP.
 */
@JvmOverloads
@Throws(IllegalArgumentException::class)
@Suppress("UNCHECKED_CAST")
public fun <T : Any> CommandAutoCompleteInteractionEvent.getTypedOption(option: CommandOption<in T>, default: T? = null): T? {
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

/**
 * Returns the typed value of the specified command option.
 *
 * @param option The command option to retrieve the value for.
 * @param mapper The function responsible for mapping the option mapping to the desired type.
 * @param default The default value to return if the option is not found or cannot be mapped.
 * @return The typed value of the specified command option, or the default value if not found or unable to map.
 * @throws IllegalArgumentException If the option type is OptionType.UNKNOWN, OptionType.SUB_COMMAND, or OptionType.SUB_COMMAND_GROUP.
 */
@JvmOverloads
@Throws(IllegalArgumentException::class)
public fun <T : Any> CommandAutoCompleteInteractionEvent.getTypedOption(
    option: CommandOption<in T>,
    mapper: Function<in OptionMapping, out T?>,
    default: T? = null
): T? {
    val optionData = (option as OptionData)
    return this.getOption(optionData.name)?.let { mapper.apply(it) } ?: default
}

/**
 * Retrieves the option with the specified [name] as an [Int] or the specified [default] value if the option is not present or cannot be cast to an [Int].
 * @param name The name of the option.
 * @param default The default value to use if the option is not present or cannot be cast to an [Int]. Default is `null`.
 * @return The option value as an [Int], or the specified default value if the option is not present or cannot be cast to an [Int].
 */
@JvmOverloads
public fun SlashCommandInteractionEvent.getOptionAsInt(name: String, default: Int? = null): Int? = this.getOption(name, default, OptionMapping::getAsInt)

/**
 * Retrieves the option with the specified [name] as a [String] or the specified [default] value if the option is not present or cannot be cast to a [String].
 * @param name The name of the option.
 * @param default The default value to use if the option is not present or cannot be cast to a [String]. Default is `null`.
 * @return The option value as a [String], or the specified default value if the option is not present or cannot be cast to a [String].
 */
@JvmOverloads
public fun SlashCommandInteractionEvent.getOptionAsString(name: String, default: String? = null): String? = this.getOption(name, default, OptionMapping::getAsString)


/**
 * Retrieves the option with the specified [name] as a [Boolean] or the specified [default] value if the option is not present or cannot be cast to a [Boolean].
 * @param name The name of the option.
 * @param default The default value to use if the option is not present or cannot be cast to a [Boolean]. Default is `null`.
 * @return The option value as a [Boolean], or the specified default value if the option is not present or cannot be cast to a [Boolean].
 */
@JvmOverloads
public fun SlashCommandInteractionEvent.getOptionAsBoolean(name: String, default: Boolean? = null): Boolean? =
    this.getOption(name, default, OptionMapping::getAsBoolean)

/**
 * Retrieves the option with the specified [name] as a [Member] or the specified [default] value if the option is not present or cannot be cast to a [Member].
 * @param name The name of the option.
 * @param default The default value to use if the option is not present or cannot be cast to a [Member]. Default is `null`.
 * @return The option value as a [Member], or the specified default value if the option is not present or cannot be cast to a [Member].
 */
@JvmOverloads
public fun SlashCommandInteractionEvent.getOptionAsMember(name: String, default: Member? = null): Member? = this.getOption(name, default, OptionMapping::getAsMember)

/**
 * Retrieves the option with the specified [name] as a `User` or the specified [default] value if the option is not present or cannot be cast to a `User`.
 * @param name The name of the option.
 * @param default The default value to use if the option is not present or cannot be cast to a `User`. Default is `null`.
 * @return The option value as a [User], or the specified default value if the option is not present or cannot be cast to a `User`.
 */
@JvmOverloads
public fun SlashCommandInteractionEvent.getOptionAsUser(name: String, default: User? = null): User? = this.getOption(name, default, OptionMapping::getAsUser)

/**
 * Retrieves the option with the specified [name] as a `GuildChannel` or the specified [default] value if the option is not present or cannot be cast to a `GuildChannel`.
 * @param name The name of the option.
 * @param default The default value to use if the option is not present or cannot be cast to a `GuildChannel`. Default is `null`.
 * @return The option value as a [GuildChannel], or the specified default value if the option is not present or cannot be cast to a `GuildChannel`.
 */
@JvmOverloads
public fun SlashCommandInteractionEvent.getOptionAsGuildChannel(name: String, default: GuildChannel? = null): GuildChannel? =
    this.getOption(name, default, OptionMapping::getAsChannel)

/**
 * Retrieves the value of a specified option as an Int.
 *
 * @param name The name of the option.
 * @param default The default value to return if the option is not present (default is null).
 * @return The value of the option as an Int, or the default value if the option is not present.
 */
@JvmOverloads
public fun CommandAutoCompleteInteractionEvent.getOptionAsInt(name: String, default: Int? = null): Int? = this.getOption(name, default, OptionMapping::getAsInt)

/**
 * Retrieves the value of a specified option as a String.
 *
 * @param name The name of the option.
 * @param default The default value to return if the option is not present (default is null).
 * @return The value of the option as a String, or the default value if the option is not present.
 */
@JvmOverloads
public fun CommandAutoCompleteInteractionEvent.getOptionAsString(name: String, default: String? = null): String? =
    this.getOption(name, default, OptionMapping::getAsString)

/**
 * Retrieves the value of a specified option as a Boolean.
 *
 * @param name The name of the option.
 * @param default The default value to return if the option is not present (default is null).
 * @return The value of the option as a Boolean, or the default value if the option is not present.
 */
@JvmOverloads
public fun CommandAutoCompleteInteractionEvent.getOptionAsBoolean(name: String, default: Boolean? = null): Boolean? =
    this.getOption(name, default, OptionMapping::getAsBoolean)

/**
 * Retrieves the value of a specified option as a Member.
 *
 * @param name The name of the option.
 * @param default The default value to return if the option is not present (default is null).
 * @return The value of the option as a Member, or the default value if the option is not present.
 */
@JvmOverloads
public fun CommandAutoCompleteInteractionEvent.getOptionAsMember(name: String, default: Member? = null): Member? =
    this.getOption(name, default, OptionMapping::getAsMember)

/**
 * Retrieves the value of a specified option as a User.
 *
 * @param name The name of the option.
 * @param default The default value to return if the option is not present (default is null).
 * @return The value of the option as a User, or the default value if the option is not present.
 */
@JvmOverloads
public fun CommandAutoCompleteInteractionEvent.getOptionAsUser(name: String, default: User? = null): User? = this.getOption(name, default, OptionMapping::getAsUser)

/**
 * Retrieves the value of a specified option as an Attachment.
 *
 * @param name The name of the option.
 * @param default The default value to return if the option is not present (default is null).
 * @return The value of the option as an Attachment, or the default value if the option is not present.
 */
@JvmOverloads
public fun CommandAutoCompleteInteractionEvent.getOptionAsAttachment(name: String, default: Attachment? = null): Attachment? =
    this.getOption(name, default, OptionMapping::getAsAttachment)

/**
 * Reply string choices
 *
 * @param choices HashMap of choices (Key , Value)
 * @return AutoCompleteCallbackAction
 */
public fun CommandAutoCompleteInteractionEvent.replyStringChoices(choices: Map<String, String>): AutoCompleteCallbackAction =
    this.replyChoices(choices.entries.map { Command.Choice(it.key, it.value) })

/**
 * Reply long choices
 *
 * @param choices HashMap of choices (Key , Value)
 * @return AutoCompleteCallbackAction
 */
public fun CommandAutoCompleteInteractionEvent.replyLongChoices(choices: Map<String, Long>): AutoCompleteCallbackAction =
    this.replyChoices(choices.entries.map { Command.Choice(it.key, it.value) })

/**
 * Reply double choices
 *
 * @param choices HashMap of choices (Key , Value)
 * @return AutoCompleteCallbackAction
 */
public fun CommandAutoCompleteInteractionEvent.replyDoubleChoices(choices: Map<String, Double>): AutoCompleteCallbackAction =
    this.replyChoices(choices.entries.map { Command.Choice(it.key, it.value) })

/**
 * Returns the first value of the [StringSelectInteractionEvent].
 *
 * @return The selected string.
 */
public fun StringSelectInteractionEvent.getSelection(): String = this.values.first()

/**
 * Returns all values of the [StringSelectInteractionEvent].
 *
 * @return A list of selected strings.
 */
public fun StringSelectInteractionEvent.getSelections(): List<String> = this.values

/**
 * Retrieves the user corresponding to the first value in the [EntitySelectInteractionEvent].
 *
 * @return The selected user, or null if not found.
 */
public fun EntitySelectInteractionEvent.getUserSelection(): User? = this.jda.shardManager?.getUserById(this.values.first().id)

/**
 * Retrieves all users corresponding to the values in the [EntitySelectInteractionEvent].
 *
 * @return A list of selected users, with nulls if not found.
 */
public fun EntitySelectInteractionEvent.getUserSelections(): List<User?> = this.values.map { this.jda.shardManager?.getUserById(it.id) }

/**
 * Retrieves the channel of type [T] corresponding to the first value in the [EntitySelectInteractionEvent].
 *
 * @return The selected channel, or null if not found.
 */
public inline fun <reified T : Channel> EntitySelectInteractionEvent.getChannelSelection(): T? = this.jda.shardManager?.getChannelById<T>(T::class.java, this.values.first().id)

/**
 * Retrieves all channels of type [T] corresponding to the values in the [EntitySelectInteractionEvent].
 *
 * @return A list of selected channels, with nulls if not found.
 */
public inline fun <reified T : Channel> EntitySelectInteractionEvent.getChannelSelections(): List<T?> =
    this.values.map { this.jda.shardManager?.getChannelById<T>(T::class.java, it.id) }

/**
 * Retrieves the category corresponding to the first value in the [EntitySelectInteractionEvent].
 *
 * @return The selected category, or null if not found.
 */
public fun EntitySelectInteractionEvent.getCategorySelection(): Category? = this.jda.shardManager?.getCategoryById(this.values.first().id)

/**
 * Retrieves all categories corresponding to the values in the [EntitySelectInteractionEvent].
 *
 * @return A list of selected categories, with nulls if not found.
 */
public fun EntitySelectInteractionEvent.getCategorySelections(): List<Category?> = this.values.map { this.jda.shardManager?.getCategoryById(it.id) }

/**
 * Retrieves the role corresponding to the first value in the [EntitySelectInteractionEvent].
 *
 * @return The selected role, or null if not found.
 */
public fun EntitySelectInteractionEvent.getRoleSelection(): Role? = this.jda.shardManager?.getRoleById(this.values.first().id)

/**
 * Retrieves all roles corresponding to the values in the [EntitySelectInteractionEvent].
 *
 * @return A list of selected roles, with nulls if not found.
 */
public fun EntitySelectInteractionEvent.getRoleSelections(): List<Role?> = this.values.map { this.jda.shardManager?.getRoleById(it.id) }

/**
 * Adds a text field to the [Modal.Builder].
 *
 * @param id Identifier for the text field.
 * @param label Label for the text field.
 * @param style Style for the text field, default is [TextInputStyle.SHORT].
 * @param placeholder Placeholder text for the text field, default is null.
 * @param min Minimum input length, default is -1.
 * @param max Maximum input length, default is -1.
 * @param value Initial value for the text field, default is null.
 * @param required Whether the text field is required, default is true.
 * @return The updated [Modal.Builder] with the added text field.
 */
@JvmOverloads
public fun Modal.Builder.addTextField(
    id: String,
    label: String,
    style: TextInputStyle = TextInputStyle.SHORT,
    placeholder: String? = null,
    min: Int = -1,
    max: Int = -1,
    value: String? = null,
    required: Boolean = true
): Modal.Builder = this.addActionRow(textInput(id, label, style, placeholder, min, max, value, required))

/**
 * Retrieves the value associated with the given [name] as a [String], or returns the provided [default] value if not found.
 *
 * @param name The name of the value to be retrieved.
 * @param default The default value to be returned if the name is not found. Defaults to [null].
 * @return The value associated with the given [name] as a [String], or the [default] value if not found.
 */
@JvmOverloads
public fun ModalInteraction.getValueAsString(name: String, default: String? = null): String? = this.getValue(name)?.asString ?: default

/**
 * Creates a [SelectOption] with the given parameters.
 *
 * @param label The display name of the select option.
 * @param value The value of the select option.
 * @param description An optional description for the select option.
 * @param emoji An optional [Emoji] to display with the select option.
 * @param isDefault An optional flag indicating if the select option is the default one.
 * @return The created [SelectOption] instance.
 */
@JvmOverloads
public fun selectOption(label: String, value: String, description: String? = null, emoji: Emoji? = null, isDefault: Boolean? = null): SelectOption {
    var option = SelectOption.of(label, value)
    description?.let { option = option.withDescription(description) }
    emoji?.let { option = option.withEmoji(emoji) }
    isDefault?.let { option = option.withDefault(isDefault) }

    return option
}

/**
 * Adds a select option to a [StringSelectMenu.Builder] instance.
 *
 * @param label The display name of the select option.
 * @param value The value of the select option.
 * @param description An optional description for the select option.
 * @param emoji An optional [Emoji] to display with the select option.
 * @param isDefault An optional flag indicating if the select option is the default one.
 * @return The modified [StringSelectMenu.Builder] instance with the added option.
 */
@JvmOverloads
public fun StringSelectMenu.Builder.addOption(
    label: String,
    value: String,
    description: String? = null,
    emoji: Emoji? = null,
    isDefault: Boolean? = null
): StringSelectMenu.Builder = this.addOptions(selectOption(label, value, description, emoji, isDefault))

/**
 * Creates a [TextInput] with the given parameters.
 *
 * @param id The unique identifier for the text input.
 * @param label The display name of the text input.
 * @param style The style of the text input, either [TextInputStyle.SHORT] or [TextInputStyle.PARAGRAPH].
 * @param placeholder An optional placeholder text to display when the input is empty.
 * @param min The minimum allowed input length, or -1 for no minimum (default).
 * @param max The maximum allowed input length, or -1 for no maximum (default).
 * @param value An optional initial value for the input.
 * @param required A flag indicating if the input is required (default is `true`).
 * @return The created [TextInput] instance.
 */
@JvmOverloads
public fun textInput(
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

/**
 * Creates a primary button with the specified properties.
 *
 * @param id The unique identifier for the button.
 * @param label The text displayed on the button, nullable.
 * @param emoji An optional emoji to be displayed on the button, nullable.
 * @param disabled Determines if the button should be disabled (default is false).
 * @return A primary-styled Button instance.
 */
@JvmOverloads
public fun primaryButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.PRIMARY, id, label, emoji).withDisabled(disabled)

/**
 * Creates a secondary button with the specified properties.
 *
 * @param id The unique identifier for the button.
 * @param label The text displayed on the button, nullable.
 * @param emoji An optional emoji to be displayed on the button, nullable.
 * @param disabled Determines if the button should be disabled (default is false).
 * @return A secondary-styled Button instance.
 */
@JvmOverloads
public fun secondaryButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.SECONDARY, id, label, emoji).withDisabled(disabled)

/**
 * Creates a success button with the specified properties.
 *
 * @param id The unique identifier for the button.
 * @param label The text displayed on the button, nullable.
 * @param emoji An optional emoji to be displayed on the button, nullable.
 * @param disabled Determines if the button should be disabled (default is false).
 * @return A success-styled Button instance.
 */
@JvmOverloads
public fun successButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.SUCCESS, id, label, emoji).withDisabled(disabled)

/**
 * Creates a danger button with the specified properties.
 *
 * @param id The unique identifier for the button.
 * @param label The text displayed on the button, nullable.
 * @param emoji An optional emoji to be displayed on the button, nullable.
 * @param disabled Determines if the button should be disabled (default is false).
 * @return A danger-styled Button instance.
 */
@JvmOverloads
public fun dangerButton(id: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.DANGER, id, label, emoji).withDisabled(disabled)

/**
 * Creates a link button with the specified properties.
 *
 * @param url The URL to be opened when the button is clicked.
 * @param label The text displayed on the button, nullable.
 * @param emoji An optional emoji to be displayed on the button, nullable.
 * @param disabled Determines if the button should be disabled (default is false).
 * @return A link-styled Button instance.
 */
@JvmOverloads
public fun linkButton(url: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.LINK, url, label, emoji).withDisabled(disabled)

/**
 * Creates a premium button with the specified properties.
 *
 * @param url The URL to be associated with the button.
 * @param label The optional label to be displayed on the button.
 * @param emoji The optional emoji to be displayed on the button.
 * @param disabled Whether the button should be disabled or not.
 * @return A Button object with the specified properties.
 */
public fun premiumButton(url: String, label: String? = null, emoji: Emoji? = null, disabled: Boolean = false): Button =
    Button.of(ButtonStyle.PREMIUM, url, label, emoji).withDisabled(disabled)

/**
 * Converts a formatted string into an Emoji instance.
 *
 * @return An Emoji instance based on the formatted string.
 */
public fun String.toEmoji(): EmojiUnion {
    return Emoji.fromFormatted(this)
}

/**
 * Computes the normalized Levenshtein distance between two strings.
 *
 * The function calculates the minimum number of single-character edits
 * (insertions, deletions, or substitutions) required to transform one string
 * into the other. The result is normalized to a value between 0 and 1, where
 * 0 represents no similarity and 1 represents a perfect match.
 *
 * Example usage:
 * <pre>
 * val similarity = levenshteinDistance("kitten", "sitting")
 * println(similarity) // Output: 0.5714285714285714
 * <pre>
 *
 * @param str1 The first input string.
 * @param str2 The second input string.
 * @return The normalized Levenshtein distance (a Double value between 0 and 1).
 * */
public fun levenshteinDistance(str1: String, str2: String): Double {
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

public fun secondsToTime(inputSeconds: Long, noTimeText: String = "**No time**"): String {
    var timeseconds = inputSeconds
    val builder = StringBuilder()
    val years = (timeseconds / (60 * 60 * 24 * 365)).toInt()
    if (years > 0) {
        builder.append("**").append(years).append("** ").append(pluralise(years, "year", "years")).append(", ")
        timeseconds %= (60 * 60 * 24 * 365)
    }
    val weeks = (timeseconds / (60 * 60 * 24 * 7)).toInt()
    if (weeks > 0) {
        builder.append("**").append(weeks).append("** ").append(pluralise(weeks, "week", "weeks")).append(", ")
        timeseconds %= (60 * 60 * 24 * 7)
    }
    val days = (timeseconds / (60 * 60 * 24)).toInt()
    if (days > 0) {
        builder.append("**").append(days).append("** ").append(pluralise(days, "day", "days")).append(", ")
        timeseconds %= (60 * 60 * 24)
    }
    val hours = (timeseconds / (60 * 60)).toInt()
    if (hours > 0) {
        builder.append("**").append(hours).append("** ").append(pluralise(hours, "hour", "hours")).append(", ")
        timeseconds %= (60 * 60)
    }
    val minutes = (timeseconds / 60).toInt()
    if (minutes > 0) {
        builder.append("**").append(minutes).append("** ").append(pluralise(minutes, "minute", "minutes")).append(", ")
        timeseconds %= 60
    }
    if (timeseconds > 0) {
        builder.append("**").append(timeseconds).append("** ").append(pluralise(timeseconds, "second", "seconds"))
    }
    var str = builder.toString()
    if (str.endsWith(", ")) str = str.substring(0, str.length - 2)
    if (str == "") str = noTimeText
    return str
}

/**
 * Pluralizes the given word based on the count.
 *
 * @param x The count of the word.
 * @param singular The singular form of the word.
 * @param plural The plural form of the word.
 * @return Returns the pluralized word based on the count.
 */
public fun pluralise(x: Int, singular: String?, plural: String?): String? {
    return if (x == 1) singular else plural
}

/**
 * Pluralizes the given word based on the count.
 *
 * @param x The count of the word.
 * @param singular The singular form of the word.
 * @param plural The plural form of the word.
 * @return Returns the pluralized word based on the count.
 */
public fun pluralise(x: Long, singular: String?, plural: String?): String? {
    return if (x == 1L) singular else plural
}

/**
 * Escapes Markdown links in a given input string.
 *
 * @param input The input string containing Markdown links to be escaped.
 * @return The input string with escaped Markdown links.
 */
public fun escapeMarkdownLinks(input: String): String {
    var result = input

    // Escape inline links: [link text](URL)
    result = result.replace(Regex("""\[(.*?)\]\((.*?)\)""")) { "\\${it.value}" }

    // Escape reference-style links: [link text][id] and [id]: URL
    result = result.replace(Regex("""\[(.*?)\]\[(.*?)\]""")) { "\\${it.value}" }
    result = result.replace(Regex("""\[(.*?)\]: (.*?)""")) { "\\${it.value}" }

    return result
}
