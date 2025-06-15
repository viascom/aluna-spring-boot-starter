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

@file:JvmName("AlunaMessageUtils")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.util

import io.viascom.discord.bot.aluna.model.DiscordSticker
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import net.dv8tion.jda.api.requests.restaction.MessageEditAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import java.awt.Color
import java.nio.charset.Charset

public object MessageUtils {

    /**
     * Send or edit message
     *
     * @param message The message to edit of not null
     * @param channel The channel where the new message will be displayed
     * @param newMessage The new message data
     * @param onCreateAction Interceptor before the message is created and the MessageCreateAction gets completed
     * @return MessageEditAction
     */
    public fun sendOrEditMessage(
        message: Message?,
        channel: MessageChannel,
        newMessage: MessageCreateData,
        onCreateAction: ((MessageCreateAction) -> MessageCreateAction)? = null
    ): MessageEditAction = message?.editMessage(newMessage.toEditData()) ?: run {
        var createAction = channel.sendMessage(newMessage)
        if (onCreateAction != null) {
            createAction = onCreateAction.invoke(createAction)
        }
        createAction.complete().editMessage(newMessage.toEditData())
    }

    /**
     * Send or edit message
     *
     * @param messageId The id of the message to edit or send if not found or null
     * @param channel The channel where the new message will be displayed
     * @param newMessage The new message data
     * @param onCreateAction Interceptor before the message is created and the MessageCreateAction gets completed
     * @return MessageEditAction
     */
    public fun sendOrEditMessage(
        messageId: String?,
        channel: MessageChannel,
        newMessage: MessageCreateData,
        onCreateAction: ((MessageCreateAction) -> MessageCreateAction)? = null
    ): MessageEditAction {
        val message = messageId?.let {
            try {
                channel.retrieveMessageById(messageId).complete()
            } catch (e: ErrorResponseException) {
                if (e.errorCode == 10008) { //10008: Unknown Message
                    null
                } else {
                    throw e
                }
            }
        }
        return sendOrEditMessage(message, channel, newMessage, onCreateAction)
    }
}

/**
 * Send or edit message
 *
 * @param message The message to edit of not null
 * @param newMessage The new message data
 * @param onCreateAction Interceptor before the message is created and the MessageCreateAction gets completed
 * @return MessageEditAction
 */
public fun MessageChannel.sendOrEditMessage(
    message: Message?,
    newMessage: MessageCreateData,
    onCreateAction: ((MessageCreateAction) -> MessageCreateAction)? = null
): MessageEditAction = MessageUtils.sendOrEditMessage(message, this, newMessage, onCreateAction)

/**
 * Send or edit message
 *
 * @param messageId The id of the message to edit or send if not found or null
 * @param newMessage The new message data
 * @param onCreateAction Interceptor before the message is created and the MessageCreateAction gets completed
 * @return MessageEditAction
 */
public fun MessageChannel.sendOrEditMessage(
    messageId: String?,
    newMessage: MessageCreateData,
    onCreateAction: ((MessageCreateAction) -> MessageCreateAction)? = null
): MessageEditAction = MessageUtils.sendOrEditMessage(messageId, this, newMessage, onCreateAction)

/**
 * Split list of elements in multiple fields to keep the embed valid
 *
 * @param values List of elements
 * @param name Title for the first field
 * @param nameOnEachLine If true, the name will be added on each line
 * @param inline Define if the fields are inline
 * @return List of Fields
 */
@Throws(IllegalArgumentException::class)
public fun splitListInFields(values: List<String>, name: String = "", nameOnEachLine: Boolean = false, inline: Boolean = false): List<MessageEmbed.Field> {
    require(name.length <= MessageEmbed.TITLE_MAX_LENGTH) { "Name cannot be longer than " + MessageEmbed.TITLE_MAX_LENGTH + " characters." }

    val fields = arrayListOf<MessageEmbed.Field>()
    var list = ""
    var tempName = name

    values.forEach { element ->
        if (list.length + element.length > MessageEmbed.VALUE_MAX_LENGTH) {
            fields.add(MessageEmbed.Field(tempName, list, inline))
            list = ""
            if (!nameOnEachLine) {
                tempName = ""
            }
        }

        list += "\n" + element
    }

    if (list.isNotEmpty()) {
        fields.add(MessageEmbed.Field(tempName, list, inline))
    }

    return fields
}

/**
 * Split list of elements in multiple fields to keep the embed valid
 *
 * @param name Title for the first field
 * @param inline Define if the fields are inline
 * @return List of Fields
 */
@Throws(IllegalArgumentException::class)
public fun List<String>.splitInFields(name: String = "", inline: Boolean = false): List<MessageEmbed.Field> = splitListInFields(this, name, inline)

/**
 * Replace mentions to be displayed with id instead of the resolved version of discord.
 *
 * @param input String with mentions to replace
 * @return String with replaced mentions
 */
public fun demention(input: String): String {
    return input.replace("<@", "<@\u200B")
}

/**
 * Remove all components
 */
public fun WebhookMessageCreateAction<Message>.removeComponents(): WebhookMessageCreateAction<Message> = this.setComponents(arrayListOf())

/**
 * Remove all components
 */
public fun WebhookMessageEditAction<Message>.removeComponents(): WebhookMessageEditAction<Message> = this.setComponents(arrayListOf())

/**
 * Remove all components
 */
public fun MessageCreateAction.removeComponents(): MessageCreateAction = this.setComponents(arrayListOf())

/**
 * Remove all components
 */
public fun MessageEditAction.removeComponents(): MessageEditAction = this.setComponents(arrayListOf())

/**
 * Remove all components
 */
public fun MessageEditCallbackAction.removeComponents(): MessageEditCallbackAction = this.setComponents(arrayListOf())

/**
 * Remove all components
 */
public fun ReplyCallbackAction.removeComponents(): ReplyCallbackAction = this.setComponents(arrayListOf())

/**
 * Sets the stickers for this message create action.
 *
 * @param stickers The Discord stickers to be set.
 */
public fun MessageCreateAction.setStickers(vararg stickers: DiscordSticker): MessageCreateAction = this.setStickers(stickers.map { it.toSticker() })

/**
 * Sets the stickers for the message create action.
 *
 * @param stickers The collection of DiscordStickers to set.
 */
public fun MessageCreateAction.setStickers(stickers: Collection<DiscordSticker>): MessageCreateAction = this.setStickers(stickers.map { it.toSticker() })

/**
 * Converts a MessageEditData object to a MessageCreateData object.
 *
 * @return The converted MessageCreateData object.
 */
public fun MessageEditData.toCreateData(): MessageCreateData = MessageCreateData.fromEditData(this)

/**
 * Converts a MessageCreateData object to a MessageEditData object.
 *
 * @return The converted MessageEditData object.
 */
public fun MessageCreateData.toEditData(): MessageEditData = MessageEditData.fromCreateData(this)

/**
 * Adds Fields to the embed.
 *
 * @param fields Fields to add
 * @return the builder after the field has been added
 */
public fun EmbedBuilder.addFields(vararg fields: MessageEmbed.Field): EmbedBuilder {
    fields.forEach { this.addField(it) }
    return this
}

/**
 * Adds Fields to the embed.
 *
 * @param fields Fields to add
 * @return the builder after the field has been added
 */
public fun EmbedBuilder.addFields(fields: Collection<MessageEmbed.Field>): EmbedBuilder {
    fields.forEach { this.addField(it) }
    return this
}

/**
 * Sets the color of the embed using RGB values.
 *
 * @param red The red component of the color, a value between 0 and 255.
 * @param green The green component of the color, a value between 0 and 255.
 * @param blue The blue component of the color, a value between 0 and 255.
 * @return The updated EmbedBuilder object.
 */
public fun EmbedBuilder.setColor(red: Int, green: Int, blue: Int): EmbedBuilder = this.setColor(Color(red, green, blue))

/**
 * Sets the color of the embed using a hexadecimal color string.
 *
 * @param hexColor The hexadecimal color string, e.g., "#FF0000" for red.
 * @return The updated EmbedBuilder object.
 */
public fun EmbedBuilder.setColor(hexColor: String): EmbedBuilder = this.setColor(Color.decode(hexColor))

/**
 * Converts the constructed EmbedBuilder object into a JSON byte array.
 *
 * @return A byte array containing the JSON representation of the embed.
 */
public fun EmbedBuilder.toJson(): ByteArray = this.build().toData().toJson()

/**
 * Converts the constructed EmbedBuilder object into a JSON string.
 *
 * @return A string containing the JSON representation of the embed.
 */
public fun EmbedBuilder.toJsonString(): String = this.toJson().toString(Charset.defaultCharset())

/**
 * Converts the MessageEmbed object into a JSON byte array.
 *
 * @return A byte array containing the JSON representation of the MessageEmbed.
 */
public fun MessageEmbed.toJson(): ByteArray = this.toData().toJson()

/**
 * Converts the MessageEmbed object into a JSON string.
 *
 * @return A string containing the JSON representation of the MessageEmbed.
 */
public fun MessageEmbed.toJsonString(): String = this.toJson().toString(Charset.defaultCharset())


/**
 * Retrieves a message with the given message ID in the user's private channel.
 *
 * @param messageId The ID of the message to retrieve.
 * @return The retrieved message, or null if an exception occurred.
 */
public fun User.getMessage(messageId: String): Message? = try {
    this.openPrivateChannel().complete().retrieveMessageById(messageId).complete()
} catch (e: Exception) {
    null
}

/**
 * Converts a string into a Discord embed using the specified title.
 *
 * @param title The title of the embed. Defaults to null if not provided.
 * @return An EmbedBuilder object representing the Discord embed with the specified title and content.
 */
public fun String.toDiscordEmbed(title: String? = null): EmbedBuilder = EmbedBuilder().setTitle(title).setDescription(this).setColor("#2c2d31")

/**
 * Create a new {@link FileUpload} for a string.
 * <br>This is used to upload data to discord for various purposes.
 *
 * @param  data
 *         The string to upload
 * @param  name
 *         The representative name to use for the file
 *
 * @throws IllegalArgumentException
 *         If null is provided or the name is empty
 *
 * @return {@link FileUpload}
 */
public fun FileUpload.fromString(data: String, name: String): FileUpload = FileUpload.fromData(data.toByteArray(), name)
