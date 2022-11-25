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

@file:JvmName("AlunaMessageUtils")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.util

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import net.dv8tion.jda.api.requests.restaction.MessageEditAction
import net.dv8tion.jda.api.utils.messages.MessageCreateData

object MessageUtils {

    /**
     * Send or edit message
     *
     * @param message The message to edit of not null
     * @param channel The channel where the new message will be displayed
     * @param newMessage The new message data
     * @param onCreateAction Interceptor before the message is created and the MessageCreateAction gets completed
     * @return MessageEditAction
     */
    fun sendOrEditMessage(
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
    fun sendOrEditMessage(
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
fun MessageChannel.sendOrEditMessage(
    message: Message?,
    newMessage: MessageCreateData,
    onCreateAction: ((MessageCreateAction) -> MessageCreateAction)? = null
) = MessageUtils.sendOrEditMessage(message, this, newMessage, onCreateAction)

/**
 * Send or edit message
 *
 * @param messageId The id of the message to edit or send if not found or null
 * @param newMessage The new message data
 * @param onCreateAction Interceptor before the message is created and the MessageCreateAction gets completed
 * @return MessageEditAction
 */
fun MessageChannel.sendOrEditMessage(
    messageId: String?,
    newMessage: MessageCreateData,
    onCreateAction: ((MessageCreateAction) -> MessageCreateAction)? = null
) = MessageUtils.sendOrEditMessage(messageId, this, newMessage, onCreateAction)