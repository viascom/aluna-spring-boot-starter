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

import io.viascom.discord.bot.aluna.bot.Interaction
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.util.getTypedOption
import io.viascom.discord.bot.aluna.util.getValueAsString
import io.viascom.discord.bot.aluna.util.textInput
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.sharding.ShardManager

@Interaction
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class PurgeMessagesProvider(
    private val shardManager: ShardManager,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : SystemCommandDataProvider(
    "purge_messages",
    "Purge Messages",
    true,
    false,
    true,
    false
) {

    private lateinit var selectedChannel: GuildMessageChannel

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        var id = event.getTypedOption(command.argsOption, "")!!

        if (!event.isFromGuild && id.isEmpty()) {
            event.deferReply()
                .setContent("${systemCommandEmojiProvider.crossEmoji().formatted} This command can only be used in servers directly or a channelId is needed.")
                .queue()
            return
        }

        if (id.isEmpty()) {
            id = event.channel.id
        }

        val channel = shardManager.getChannelById(GuildMessageChannel::class.java, id)
        if (channel == null) {
            event.deferReply()
                .setContent("${systemCommandEmojiProvider.crossEmoji().formatted} Please specify a valid channel ID as argument for this command.")
                .queue()
            return
        }

        if (!event.guild!!.getMember(event.jda.selfUser)!!.hasPermission(channel, Permission.MESSAGE_MANAGE)) {
            event.deferReply().setContent("${systemCommandEmojiProvider.crossEmoji().formatted} This bot is not allowed to remove messages.")
                .queue()
            return
        }

        selectedChannel = channel

        //Show modal
        val amount: TextInput = textInput("amount", "Amount of messages", TextInputStyle.SHORT)

        val modal: Modal = Modal.create("purge", "Purge")
            .addActionRows(ActionRow.of(amount))
            .build()

        event.replyModal(modal).queueAndRegisterInteraction(command)
    }

    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        event.deferReply(true).queue {
            it.editOriginal(
                "${systemCommandEmojiProvider.tickEmoji().formatted} Removing ${
                    event.getValueAsString(
                        "amount",
                        "0"
                    )
                } messages from ${selectedChannel.asMention}..."
            ).queue()

            val amount = event.getValueAsString("amount", "0")!!.toInt()

            selectedChannel.iterableHistory
                .takeAsync(amount)
                .thenAccept(selectedChannel::purgeMessages)
        }
        return true
    }

    override fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent, command: SystemCommand) {
        val input = event.getTypedOption(command.argsOption, "")!!

        if (input == "") {
            event.replyChoices().queue()
            return
        }

        val possibleChannel = shardManager.getChannelById(GuildMessageChannel::class.java, input)

        if (possibleChannel != null) {
            event.replyChoices(Command.Choice(possibleChannel.name, possibleChannel.id)).queue()
        } else {
            event.replyChoices().queue()
        }
    }
}
