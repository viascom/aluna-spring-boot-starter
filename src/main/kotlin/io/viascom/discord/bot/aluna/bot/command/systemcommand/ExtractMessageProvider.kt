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
import io.viascom.discord.bot.aluna.bot.Command
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.Webhook
import io.viascom.discord.bot.aluna.util.getGuildMessage
import io.viascom.discord.bot.aluna.util.getMessage
import io.viascom.discord.bot.aluna.util.getOptionAsString
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.sharding.ShardManager

@Command
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class ExtractMessageProvider(
    private val shardManager: ShardManager,
    private val objectMapper: ObjectMapper
) : SystemCommandDataProvider(
    "extract_message",
    "Get Message as JSON",
    true,
    true,
    false,
    false
) {

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        val elements = event.getOptionAsString("args")?.split("/")

        if (elements == null) {
            event.reply("Please define a message link as arg to use this command.").setEphemeral(true).queue()
            return
        }

        val serverId = elements[4]
        val channelId = elements[5]
        val messageId = elements[6]

        val message = if (channelId == "@me") {
            try {
                event.user.getMessage(messageId)
            } catch (e: Exception) {
                null
            }
        } else {
            try {
                shardManager.getGuildMessage(serverId, channelId, messageId)
            } catch (e: Exception) {
                null
            }
        }

        if (message == null) {
            event.reply("Message not found").setEphemeral(true).queue()
            return
        }

        val webhook = Webhook.fromMessage(message)
        val webhookJson = objectMapper.writeValueAsString(webhook)

        event.reply("Message Json:").setEphemeral(true).addFile(webhookJson.toByteArray(), "message.json").queue()
    }
}
