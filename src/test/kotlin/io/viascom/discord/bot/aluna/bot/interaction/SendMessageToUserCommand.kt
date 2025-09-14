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
import io.viascom.discord.bot.aluna.util.getValueAsString
import io.viascom.discord.bot.aluna.util.modalTextField
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.components.tree.ModalComponentTree
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.modals.Modal

@Interaction
class SendMessageToUserCommand : DiscordCommand("send-message-to-user", "Send a message to a user") {

    private lateinit var selectedUser: User
    private lateinit var selectedChannel: StandardGuildMessageChannel

    override fun execute(event: SlashCommandInteractionEvent) {
        val userSelect = EntitySelectMenu.create("user_select", EntitySelectMenu.SelectTarget.USER).build()
        val channelSelect = EntitySelectMenu.create("channel_select", EntitySelectMenu.SelectTarget.CHANNEL).setChannelTypes(ChannelType.TEXT).build()

        val modalComponents = ModalComponentTree.of(
            TextDisplay.of("Send message to user."),
            Label.of("Please select a user.", userSelect),
            Label.of("Please select a channel.", channelSelect),
            TextDisplay.of("Please enter the message you want to send."),
            modalTextField("message", "Message", TextInputStyle.PARAGRAPH)
        )

        val modal = Modal.create("message", "Message")
            .addComponents(modalComponents).build()


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
