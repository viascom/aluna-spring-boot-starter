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
import io.viascom.discord.bot.aluna.util.dangerButton
import io.viascom.discord.bot.aluna.util.primaryButton
import io.viascom.discord.bot.aluna.util.removeComponents
import io.viascom.discord.bot.aluna.util.sendOrEditMessage
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.time.LocalDateTime
import java.time.ZoneOffset

@Interaction
class EditOrCreateMessageCommand : DiscordCommand("edit-or-create-message", "Edit or Create Message") {

    private var messageIdToEdit: String? = null
    private var latestHook: InteractionHook? = null

    override fun execute(event: SlashCommandInteractionEvent) {
        val embed = EmbedBuilder()
            .setDescription("When you click the button below, Aluna will try to update the message above or send a new one if you delete it.")
            .build()

        event.replyEmbeds(embed)
            .setComponents(ActionRow.of(primaryButton("update-message", "Update Message"), dangerButton("remove", "Delete Message")))
            .queueAndRegisterInteraction(
                this,
                arrayListOf(EventRegisterType.BUTTON),
                true
            ) {
                latestHook = it
            }

        event.channel.sendMessage("This message will be updated or if you delete it, Aluna will send a new one.").queue {
            messageIdToEdit = it.id
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        event.deferEdit().queue()
        if (event.componentId == "update-message") {
            val newText = "This is the edited or new text | " + LocalDateTime.now(ZoneOffset.UTC).nano
            event.channel.sendOrEditMessage(messageIdToEdit, MessageCreateData.fromContent(newText)).queue {
                messageIdToEdit = it.id
                it.addReaction(Emoji.fromUnicode("\uD83C\uDF15")).queue()
            }
        } else {
            messageIdToEdit?.let { event.channel.deleteMessageById(it).queue() }
        }
        return true
    }

    override fun onButtonInteractionTimeout() {
        val embed = EmbedBuilder()
            .setDescription("The timeout for the button hook was reached. Please the command again.")
            .build()
        latestHook?.let { latestHook!!.editOriginalEmbeds(embed).removeComponents().queue() }
    }
}
