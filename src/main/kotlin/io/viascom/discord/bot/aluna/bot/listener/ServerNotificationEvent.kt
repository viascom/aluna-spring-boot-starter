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

package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.event.CoroutineEventListener
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.SendServerNotificationCondition
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.TimestampFormat
import io.viascom.discord.bot.aluna.util.getGuildTextChannel
import io.viascom.discord.bot.aluna.util.toDiscordTimestamp
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import java.awt.Color

@Component
@ConditionalOnJdaEnabled
@Conditional(SendServerNotificationCondition::class)
public open class ServerNotificationEvent(
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties,
    private val additionalInformation: List<AdditionalServerJoinLeaveInformation>
) : CoroutineEventListener {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: GenericEvent) {
        when (event) {
            is GuildJoinEvent -> onGuildJoin(event)
            is GuildLeaveEvent -> onGuildLeave(event)
        }
    }

    private suspend fun onGuildJoin(event: GuildJoinEvent) = withContext(AlunaDispatchers.Interaction) {
        if (!alunaProperties.notification.serverJoin.enabled) {
            return@withContext
        }

        val server = event.guild
        val embedMessage = createServerMessage(true, server)

        val channel = shardManager.getGuildTextChannel(
            alunaProperties.notification.serverJoin.server.toString(),
            alunaProperties.notification.serverJoin.channel.toString()
        )

        if (channel == null) {
            logger.warn("Aluna was not able to send a GuildJoinEvent to the defined channel.")
            return@withContext
        }

        channel.sendMessageEmbeds(embedMessage.build()).queue { message ->
            if (alunaProperties.discord.gatewayIntents.any { it == GatewayIntent.GUILD_MEMBERS }) {
                //Update the message as soon as possible
                server.findMembers { it.user.isBot }
                    .onSuccess {
                        message.editMessageEmbeds(createServerMessage(true, server, true, it).build()).queue()
                    }
                    .onError {
                        message.editMessageEmbeds(createServerMessage(true, server, false).build()).queue()
                    }
            }

        }
    }

    private suspend fun onGuildLeave(event: GuildLeaveEvent) = withContext(AlunaDispatchers.Interaction) {
        if (!alunaProperties.notification.serverLeave.enabled) {
            return@withContext
        }

        val server = event.guild
        val embedMessage = createServerMessage(false, server)

        val channel = shardManager.getGuildTextChannel(
            alunaProperties.notification.serverLeave.server.toString(),
            alunaProperties.notification.serverLeave.channel.toString()
        )

        if (channel == null) {
            logger.warn("Aluna was not able to send a GuildLeaveEvent to the defined channel.")
            return@withContext
        }

        channel.sendMessageEmbeds(embedMessage.build()).queue { message ->
            if (alunaProperties.discord.gatewayIntents.any { it == GatewayIntent.GUILD_MEMBERS }) {
                //Update the message as soon as possible
                server.findMembers { it.user.isBot && it.user.id != server.jda.selfUser.id }
                    .onSuccess {
                        message.editMessageEmbeds(createServerMessage(false, server, true, it).build()).queue()
                    }
                    .onError {
                        message.editMessageEmbeds(createServerMessage(false, server, false).build()).queue()
                        logger.warn("Aluna was not able to load other bots: ${it.message}")
                    }
            }

        }
    }

    private fun createServerMessage(
        serverJoin: Boolean = true,
        server: Guild,
        membersSuccess: Boolean? = null,
        botList: List<Member>? = null
    ): EmbedBuilder {

        val title = if (serverJoin) {
            "\uD83D\uDFE2 New server **${server.name}** joined"
        } else {
            "\uD83D\uDD34 Server **${server.name}** left"
        }

        val color = if (serverJoin) {
            Color.GREEN
        } else {
            Color.RED
        }

        val embedMessage = EmbedBuilder()
            .setTitle(title)
            .setColor(color)
            .setDescription("")
            .setThumbnail(server.iconUrl)
            .addField("» Server", "Name: ${server.name}\nId: ${server.id}", false)

        if (!serverJoin) {
            embedMessage.addField("» Owner", "Name: ${server.owner?.effectiveName}\nId: ${server.ownerId}", false)
        } else {
            embedMessage.addField("» Owner", "Name: ${server.retrieveOwner().complete().effectiveName}\nId: ${server.ownerId}", false)
        }

        embedMessage.addField("» Locale", "Name: ${server.locale.languageName}", false)
            .addField("» Members", "Total: ${server.memberCount}", false)

        if (!serverJoin) {
            embedMessage.addField("» Joined", server.selfMember.timeJoined.toDiscordTimestamp(TimestampFormat.RELATIVE_TIME), false)
        }

        val otherBots = "» Other Bots"

        if (alunaProperties.discord.gatewayIntents.any { it == GatewayIntent.GUILD_MEMBERS }) {
            val botListText = botList?.joinToString(", ") { it.user.name }
            when {
                membersSuccess == true && botListText != null && botListText.length <= MessageEmbed.VALUE_MAX_LENGTH -> embedMessage.addField(otherBots, botListText, false)
                membersSuccess == true && botListText != null && botListText.length > MessageEmbed.VALUE_MAX_LENGTH -> embedMessage.addField(
                    otherBots,
                    "Server has ${botListText.length} bots",
                    false
                )

                membersSuccess == null -> embedMessage.addField(otherBots, "Loading...", false)
                membersSuccess == false -> embedMessage.addField(otherBots, "Could not load bots of this server", false)
            }
        }

        try {
            additionalInformation.flatMap {
                if (serverJoin) {
                    it.getAdditionalServerJoinInformation(server)
                } else {
                    it.getAdditionalServerLeaveInformation(server)
                }
            }.forEach {
                embedMessage.addField(it)
            }
        } catch (e: Exception) {
            logger.warn("Aluna was not able to get additional server information.\n" + e.printStackTrace())
        }

        return embedMessage
    }
}
