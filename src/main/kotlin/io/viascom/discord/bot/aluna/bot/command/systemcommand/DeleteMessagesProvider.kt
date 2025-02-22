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

package io.viascom.discord.bot.aluna.bot.command.systemcommand

import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.util.getValueAsString
import io.viascom.discord.bot.aluna.util.textInput
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class DeleteMessagesProvider(
    private val shardManager: ShardManager,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : SystemCommandDataProvider(
    "delete_message",
    "Delete Message",
    true,
    false,
    false,
    false
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        if (!event.isFromGuild) {
            event.deferReply()
                .setEphemeral(true)
                .setContent("${systemCommandEmojiProvider.crossEmoji().formatted} This command can only be used in servers directly.")
                .queue()
            return
        }

        event.replyModal(
            Modal.create("delete_message", "Delete Message")
                .addComponents(ActionRow.of(textInput("channel_id", "Channel ID (0 = current)", TextInputStyle.SHORT)))
                .addComponents(ActionRow.of(textInput("message_id", "Message ID", TextInputStyle.SHORT)))
                .build()
        ).queueAndRegisterInteraction(command)
    }

    override fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        event.deferReply(true).setEphemeral(true).queue { hook ->

            var channelId = event.getValueAsString("channel_id", "x")!!
            if (channelId == "0") channelId = event.channel.id
            val messageId = event.getValueAsString("message_id", "x")!!

            val channel = shardManager.getChannelById(GuildMessageChannel::class.java, channelId)
            if (channel == null) {
                hook.editOriginal("${systemCommandEmojiProvider.crossEmoji().formatted} Please specify a valid channel ID as argument for this command.").queue()
                return@queue
            }

            if (!event.guild!!.getMember(event.jda.selfUser)!!.hasPermission(channel, Permission.MESSAGE_MANAGE)) {
                hook.editOriginal("${systemCommandEmojiProvider.crossEmoji().formatted} This bot is not allowed to remove messages.").queue()
                return@queue
            }

            channel.retrieveMessageById(messageId).queue({
                it.delete().queue({
                    hook.editOriginal("${systemCommandEmojiProvider.tickEmoji().formatted} The message with the ID `$messageId` has been deleted.").queue()
                    logger.info("Message with ID $messageId in channel ${channel.name} (${channel.id}) has been deleted by ${event.user.name} (${event.user.id})")
                }, {
                    hook.editOriginal("${systemCommandEmojiProvider.crossEmoji().formatted} The message with the ID `$messageId` could not be deleted.").queue()
                })
            }, {
                hook.editOriginal("${systemCommandEmojiProvider.crossEmoji().formatted} The message with the ID `$messageId` could not be found in the specified channel.").queue()
            })
        }
        return true
    }
}
