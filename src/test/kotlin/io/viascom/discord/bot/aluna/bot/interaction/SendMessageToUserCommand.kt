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

package io.viascom.discord.bot.aluna.bot.interaction

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.Interaction
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.util.ModalCreate
import io.viascom.discord.bot.aluna.util.getValueAsString
import io.viascom.discord.bot.aluna.util.textDisplay
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

@Interaction
class SendMessageToUserCommand : DiscordCommand("send-message-to-user", "Send a message to a user") {

    private lateinit var selectedUser: User
    private lateinit var selectedChannel: StandardGuildMessageChannel

    override fun execute(event: SlashCommandInteractionEvent) {
        val modal = ModalCreate("message", "Message") {
            textDisplay("Send message to user.")
            entitySelect("user_select", EntitySelectMenu.SelectTarget.USER, label = "Please select a user.")
            channelSelect("channel_select", label = "Please select a channel.")
            paragraphField("message", "Message")
        }

        event.replyModal(modal).queueAndRegisterInteraction(this, arrayListOf(EventRegisterType.MODAL))
    }

    override fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        event.deferEdit().queue()
        selectedUser = event.getValue("user_select")!!.asMentions.users.first()
        selectedChannel = event.getValue("channel_select")!!.asMentions.channels.first() as StandardGuildMessageChannel
        val message = event.getValueAsString("message")!!
        selectedChannel.sendMessage(selectedUser.asMention + " " + message).queue()
        return true
    }
}
