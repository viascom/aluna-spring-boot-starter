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

import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.configuration.scope.InteractionScope
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.TimestampFormat
import io.viascom.discord.bot.aluna.util.secondsToTime
import io.viascom.discord.bot.aluna.util.toDiscordTimestamp
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.stereotype.Component
import java.awt.Color
import java.lang.management.ManagementFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.function.Consumer


@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class BotStatusProvider(
    private val alunaProperties: AlunaProperties,
    private val configurableListableBeanFactory: ConfigurableListableBeanFactory
) : SystemCommandDataProvider(
    "bot_status",
    "Get Bot status",
    true,
    true,
    false,
    false
) {
    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        val builder = EmbedBuilder()

        builder.setTitle("Bot Status")
            .setColor(Color.ORANGE)
            .setDescription("\uD83D\uDCE1 Get status...")

        val replyHook = event.deferReply().complete()

        val total = Runtime.getRuntime().totalMemory() / 1024 / 1024
        val used = total - Runtime.getRuntime().freeMemory() / 1024 / 1024
        val uptime = ManagementFactory.getRuntimeMXBean().uptime / 1000

        val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope

        replyHook.editOriginalEmbeds(builder.build()).queue {
            builder.setTitle("Bot Status")
                .setDescription(
                    """Start-time : ${
                        LocalDateTime.now(ZoneOffset.UTC).minusSeconds(uptime).toDiscordTimestamp(TimestampFormat.SHORT_DATE_TIME)
                    } *(${secondsToTime(uptime).replace("*", "")} ago)*
                       Memory     : $used MB / $total MB
                    """.trimIndent()
                )
                .addField(
                    "Interactions", """
                    Active : ${interactionScope.getInstanceCount()}
                    Active Timeouts : ${interactionScope.getTimeoutCount()}
                """.trimIndent(), false
                )
                .addField(
                    "sharding", """
                    Total : ${event.jda.shardInfo.shardTotal}
                """.trimIndent(), false
                )
                .setColor(Color.GREEN)
                .build()
            replyHook.editOriginalEmbeds(builder.build()).queue()
        }
    }


    fun formatShardStatuses(shards: Collection<JDA>): String? {
        val map: HashMap<JDA.Status, String> = HashMap()
        shards.forEach(Consumer { jda: JDA -> map[jda.status] = map.getOrDefault(jda.status, "") + " " + jda.shardInfo.shardId })
        val sb = StringBuilder("```diff")
        map.entries.forEach { entry ->
            sb.append("\n").append(if (entry.key === JDA.Status.CONNECTED) "+ " else "- ")
                .append(entry.key).append(":").append(entry.value)
        }
        return sb.append(" ```").toString()
    }
}
