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

package io.viascom.discord.bot.aluna.bot.command

import io.viascom.discord.bot.aluna.bot.CoroutineDiscordCommand
import io.viascom.discord.bot.aluna.bot.Interaction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnDefaultHelpCommandEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.util.linkButton
import io.viascom.discord.bot.aluna.util.setColor
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button


@Interaction
@ConditionalOnJdaEnabled
@ConditionalOnDefaultHelpCommandEnabled
public class DefaultHelpCommand : CoroutineDiscordCommand("help", "Shows information about the bot") {

    init {
        this.beanCallOnDestroy = false
    }

    override fun initCommandOptions() {
        setContexts(alunaProperties.command.helpCommand.contexts)
        setIntegrationTypes(alunaProperties.command.helpCommand.integrationTypes)
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val embed = EmbedBuilder()
            .setAuthor(alunaProperties.command.helpCommand.title, null, event.jda.selfUser.effectiveAvatarUrl)
            .setColor(alunaProperties.command.helpCommand.embedColor)
            .setThumbnail(event.jda.selfUser.effectiveAvatarUrl)
            .setDescription(alunaProperties.command.helpCommand.description)

        alunaProperties.command.helpCommand.fields.forEach {
            embed.addField(it.name, it.value, it.inline)
        }

        val buttons = arrayListOf<Button>()

        if (alunaProperties.command.helpCommand.inviteButton.enabled) {
            val inviteButton = alunaProperties.command.helpCommand.inviteButton
            val url = if (inviteButton.link == null) {
                var permission = 0L
                alunaProperties.discord.defaultPermissions.forEach { permission = permission or it.rawValue }
                "https://discord.com/oauth2/authorize?client_id=${alunaProperties.discord.applicationId}&scope=bot%20applications.commands&permissions=$permission"
            } else {
                inviteButton.link!!
            }

            buttons.add(linkButton(
                url,
                inviteButton.label,
                inviteButton.emote?.let { Emoji.fromFormatted(it) }
            ))
        }

        if (alunaProperties.command.helpCommand.websiteButton.enabled) {
            val websiteButton = alunaProperties.command.helpCommand.websiteButton
            buttons.add(linkButton(
                websiteButton.link,
                websiteButton.label,
                websiteButton.emote?.let { Emoji.fromFormatted(it) }
            ))
        }

        if (alunaProperties.command.helpCommand.joinSupportServerButton.enabled) {
            val joinSupportServerButton = alunaProperties.command.helpCommand.joinSupportServerButton
            buttons.add(linkButton(
                joinSupportServerButton.link,
                joinSupportServerButton.label,
                joinSupportServerButton.emote?.let { Emoji.fromFormatted(it) }
            ))
        }

        if (alunaProperties.command.helpCommand.supportButton.enabled) {
            val supportButton = alunaProperties.command.helpCommand.supportButton
            buttons.add(linkButton(
                supportButton.link,
                supportButton.label,
                supportButton.emote?.let { Emoji.fromFormatted(it) }
            ))
        }

        val actionRows = if (buttons.size > 2) {
            arrayListOf(ActionRow.of(buttons.take(2)), ActionRow.of(buttons.takeLast(buttons.size - 2)))
        } else {
            arrayListOf(ActionRow.of(buttons))
        }

        event.replyEmbeds(embed.build()).addComponents(actionRows).queue()
    }

}
