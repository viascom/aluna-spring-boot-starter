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

import com.fasterxml.jackson.databind.ObjectMapper
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.Webhook
import io.viascom.discord.bot.aluna.util.getTypedOption
import io.viascom.discord.bot.aluna.util.getValueAsString
import io.viascom.discord.bot.aluna.util.modalTextField
import io.viascom.discord.bot.aluna.util.toDiscordEmbed
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.tree.ModalComponentTree
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.springframework.stereotype.Component
import java.awt.Color

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
public class MessageSizeProvider(
    private val shardManager: ShardManager,
    private val objectMapper: ObjectMapper
) : SystemCommandDataProvider(
    "calculate_message_size",
    "Calculate Message Size",
    true,
    true,
    false,
    false
) {

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        val url = event.getTypedOption(command.argsOption)

        if (url == null) {
            val modalComponents = ModalComponentTree.of(
                TextDisplay.of("Please enter the message link you want to calculate the size for."),
                modalTextField("message_url", "Message-Link")
            )
            val modal = Modal.create("extract_message", "Calculate Message Size")
                .addComponents(modalComponents)
                .build()
            event.replyModal(modal).queueAndRegisterInteraction(command)
        } else {
            handleMessageLink(url, event.user) { messageCreateData ->
                event.reply(messageCreateData).setEphemeral(true).queue()
            }
        }
    }

    private fun handleMessageLink(url: String, user: User, replyHandler: (MessageCreateData) -> Unit) {
        val webhook = Webhook.fromMessageLink(url, user, shardManager)
        if (webhook == null) {
            replyHandler.invoke(MessageCreateData.fromContent("Message not found"))
        } else {
            val embedMessage = "This message has a total size of **${webhook.getSize()}** characters."
                .toDiscordEmbed("Message Size")
                .setColor(Color.GREEN)
                .addField("Message", url, false)
                .addField("Embeds", webhook.embeds?.size?.toString() ?: "0", false)

            webhook.embeds?.forEachIndexed { index, embed ->
                embedMessage.addField(
                    "Embed ${index + 1}",
                    "**${embed.getSize()}** characters\n${embed.fields?.size ?: 0} fields",
                    true
                )
            }

            replyHandler.invoke(MessageCreateData.fromEmbeds(embedMessage.build()))
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        val url = event.getValueAsString("message_url")!!
        handleMessageLink(url, event.user) { messageCreateData ->
            event.reply(messageCreateData).setEphemeral(true).queue()
        }

        return true
    }
}
