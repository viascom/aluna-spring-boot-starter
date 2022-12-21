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

package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.configuration.condition.SendServerNotificationCondition
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.getGuildTextChannel
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import java.awt.Color

@Component
@Conditional(SendServerNotificationCondition::class)
open class ServerNotificationEvent(
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties
) : ListenerAdapter() {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onGuildJoin(event: GuildJoinEvent) {
        if (!alunaProperties.notification.serverJoin.enabled) {
            return
        }

        val server = event.guild
        val embedMessage = EmbedBuilder()
            .setTitle("\uD83D\uDFE2 New server **${server.name}** joined")
            .setColor(Color.GREEN)
            .setDescription("")
            .setThumbnail(server.iconUrl)
            .addField("» Server", "Name: ${server.name}\nId: ${server.id}", false)
            .addField("» Owner", "Name: ${server.owner?.effectiveName}\nId: ${server.ownerId}", false)
            .addField("» Locale", "Name: ${server.locale.languageName}", false)
            .addField("» Members", "Total: ${server.memberCount}", false)
        if (alunaProperties.discord.gatewayIntents.any { it == GatewayIntent.GUILD_MEMBERS }) {
            val otherBots = server.loadMembers().get().filter { it.user.isBot }.joinToString(", ") { it.user.asTag }
            if (otherBots.length > MessageEmbed.VALUE_MAX_LENGTH) {
                embedMessage.addField("» Other Bots", "Server has ${otherBots.length} bots", false)
            } else {
                embedMessage.addField("» Other Bots", otherBots, false)
            }
        }

        val channel = shardManager.getGuildTextChannel(
            alunaProperties.notification.serverJoin.server.toString(),
            alunaProperties.notification.serverJoin.channel.toString()
        )

        if (channel == null) {
            logger.warn("Aluna was not able to send a GuildJoinEvent to the defined channel.")
            return
        }

        channel.sendMessageEmbeds(embedMessage.build()).queue()
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        if (!alunaProperties.notification.serverLeave.enabled) {
            return
        }

        val server = event.guild
        val embedMessage = EmbedBuilder()
            .setTitle("\uD83D\uDD34 Server **${server.name}** left")
            .setColor(Color.RED)
            .setDescription("")
            .setThumbnail(server.iconUrl)
            .addField("» Server", "Name: ${server.name}\nId: ${server.id}", false)
            .addField("» Owner", "Name: ${server.owner?.effectiveName}\nId: ${server.ownerId}", false)
            .addField("» Locale", "Name: ${server.locale.languageName}", false)
            .addField("» Members", "Total: ${server.memberCount}", false)
        if (alunaProperties.discord.gatewayIntents.any { it == GatewayIntent.GUILD_MEMBERS }) {
            val otherBots = server.loadMembers().get().filter { it.user.isBot && it.user.id != server.jda.selfUser.id }.joinToString(", ") { it.user.asTag }
            if (otherBots.length > MessageEmbed.VALUE_MAX_LENGTH) {
                embedMessage.addField("» Other Bots", "Server has ${otherBots.length} bots", false)
            } else {
                embedMessage.addField("» Other Bots", otherBots, false)
            }
        }

        val channel = shardManager.getGuildTextChannel(
            alunaProperties.notification.serverLeave.server.toString(),
            alunaProperties.notification.serverLeave.channel.toString()
        )

        if (channel == null) {
            logger.warn("Aluna was not able to send a GuildLeaveEvent to the defined channel.")
            return
        }

        channel.sendMessageEmbeds(embedMessage.build()).queue()
    }
}
