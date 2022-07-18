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

package io.viascom.discord.bot.aluna.bot.command.systemcommand

import com.fasterxml.jackson.databind.ObjectMapper
import io.viascom.discord.bot.aluna.bot.Interaction
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.Webhook
import io.viascom.discord.bot.aluna.util.getGuildTextChannel
import io.viascom.discord.bot.aluna.util.getPrivateChannelByUser
import io.viascom.discord.bot.aluna.util.getValueAsString
import io.viascom.discord.bot.aluna.util.textInput
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.sharding.ShardManager

@Interaction
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class SendMessageProvider(
    private val shardManager: ShardManager,
    private val objectMapper: ObjectMapper
) : SystemCommandDataProvider(
    "send_message",
    "Send Message",
    true,
    false,
    false,
    false
) {

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        //Show modal
        val serverId: TextInput = textInput("serverId", "Server ID (0 = current or if DM)", TextInputStyle.SHORT)
        val channelId: TextInput = textInput("channelId", "Channel ID (0 = current, @<id> = for DM)", TextInputStyle.SHORT)
        val message: TextInput = textInput("message", "Message", TextInputStyle.PARAGRAPH)

        val modal: Modal = Modal.create("send_message", "Send Message")
            .addActionRows(ActionRow.of(serverId), ActionRow.of(channelId), ActionRow.of(message))
            .build()

        event.replyModal(modal).queueAndRegisterInteraction(command)
    }

    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        var serverId = event.getValueAsString("serverId", "0")!!
        var channelId = event.getValueAsString("channelId", "0")!!
        var isDM = false
        val message = event.getValueAsString("message")!!
        val messageObj = if (message.startsWith("{")) {
            objectMapper.readValue(message, Webhook::class.java).toMessage()
        } else {
            Webhook(message).toMessage()
        }

        //Set correct values for current if in DM
        if (!event.isFromGuild && serverId == "0" && channelId == "0") {
            channelId = "@${event.user.id}"
        }

        //Set serverId to current server if channel is not set to dm
        if (serverId == "0" && !channelId.startsWith("@")) {
            serverId = event.guild?.id ?: "0"
        }

        //Set channelId to current channel or dm channel
        if (channelId.startsWith("@")) {
            channelId = channelId.substring(1)
            serverId = "0"
            isDM = true
        }
        if (channelId == "0") {
            channelId = event.channel.id
        }

        when {
            (isDM) -> {
                val dmChannel = shardManager.getPrivateChannelByUser(channelId)
                if (dmChannel == null) {
                    event.deferReply(true).queue {
                        it.editOriginal("Could not find user: $channelId").queue()
                    }
                    return true
                }

                dmChannel.sendMessage(messageObj).queue()
                event.deferReply(true).queue {
                    it.editOriginal("Message Sent!").queue()
                }
                return true
            }
            (serverId != "0" && channelId != "0") -> {
                val channel = shardManager.getGuildTextChannel(serverId, channelId)
                if (channel == null) {
                    event.deferReply(true).queue {
                        it.editOriginal("Could not find channel $channelId on $serverId!").queue()
                    }
                    return true
                }

                channel.sendMessage(messageObj).queue()
                event.deferReply(true).queue {
                    it.editOriginal("Message Sent!").queue()
                }
                return true
            }
            else -> {
                event.deferReply(true).queue {
                    it.editOriginal("Could not find channel $channelId on $serverId!").queue()
                }
                return true
            }
        }
    }
}
