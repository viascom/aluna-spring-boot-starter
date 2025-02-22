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
import io.viascom.discord.bot.aluna.util.addTextField
import io.viascom.discord.bot.aluna.util.getChannelSelection
import io.viascom.discord.bot.aluna.util.getUserSelection
import io.viascom.discord.bot.aluna.util.removeComponents
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import java.awt.Color

@Interaction
class SendMessageToUserCommand : DiscordCommand("send-message-to-user", "Send a message to a user") {

    //This variable holds the latest embed and because Aluna will handle the spring scope, we always get the same instance if an interaction is used.
    private lateinit var latestEmbed: EmbedBuilder

    //This variable holds the latest interaction hook, so that we can for example remove the action rows after 14 min in the onDestroy method. 14 min is the default duration, Aluna will keep this bean and connected interaction waiters active.
    private lateinit var latestHook: InteractionHook

    private lateinit var selectedUser: User
    private lateinit var selectedChannel: StandardGuildMessageChannel

    override fun execute(event: SlashCommandInteractionEvent) {
        latestEmbed = EmbedBuilder()
            .setTitle("Send message to user")
            .setColor(Color.GREEN)
            .setDescription("Select a user")

        val userSelect = EntitySelectMenu.create("user_select", EntitySelectMenu.SelectTarget.USER).build()

        event.replyEmbeds(latestEmbed.build())
            .setEphemeral(true)
            .addComponents(ActionRow.of(userSelect))
            .queueAndRegisterInteraction(this, arrayListOf(EventRegisterType.ENTITY_SELECT)) {
                latestHook = it
            }
    }

    private fun showChannelSelect() {
        latestEmbed = EmbedBuilder()
            .setTitle("Send message to user")
            .setColor(Color.GREEN)
            .setDescription("Select a channel")

        val channelSelect = EntitySelectMenu.create("channel_select", EntitySelectMenu.SelectTarget.CHANNEL).setChannelTypes(ChannelType.TEXT).build()

        latestHook.editOriginalEmbeds(latestEmbed.build())
            .setComponents(ActionRow.of(channelSelect))
            .queueAndRegisterInteraction(latestHook, this, arrayListOf(EventRegisterType.ENTITY_SELECT))
    }

    private fun showMessageModal(event: EntitySelectInteractionEvent) {
        val modal = Modal.create("message", "Message")
        modal.addTextField("message", "Message", TextInputStyle.PARAGRAPH)
        event.replyModal(modal.build()).queueAndRegisterInteraction(this, arrayListOf(EventRegisterType.MODAL))
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent): Boolean {
        when (event.componentId) {
            "user_select" -> {
                event.deferEdit().queue()
                selectedUser = event.getUserSelection()!!
                showChannelSelect()
            }

            "channel_select" -> {
                val channel: StandardGuildChannel = event.getChannelSelection()!!
                if (channel.type == ChannelType.TEXT) {
                    selectedChannel = event.getChannelSelection()!!
                    showMessageModal(event)
                } else {
                    event.deferEdit().queue()
                    showChannelSelect()
                }
            }
        }

        return true
    }

    override fun onEntitySelectInteractionTimeout() {
        latestHook.editOriginalEmbeds(latestEmbed.build()).removeComponents().queue()
    }

    override fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        event.deferEdit().queue()
        selectedChannel.sendMessage(selectedUser.asMention + " " + event.values.first().asString).queue()

        //We can delete the ephemeral message as it is no longer needed
        latestHook.deleteOriginal().queue()
        return true
    }
}
