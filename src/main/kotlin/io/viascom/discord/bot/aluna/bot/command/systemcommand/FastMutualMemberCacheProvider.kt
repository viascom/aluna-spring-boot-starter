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
import io.viascom.discord.bot.aluna.bot.handler.FastMutualGuildsCache
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnFastMutualGuildCacheEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.util.dangerButton
import io.viascom.discord.bot.aluna.util.getTypedOption
import io.viascom.discord.bot.aluna.util.round
import io.viascom.discord.bot.aluna.util.successButton
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component
import java.awt.Color

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
@ConditionalOnFastMutualGuildCacheEnabled
public class FastMutualMemberCacheProvider(
    private val fastMutualGuildsCache: FastMutualGuildsCache,
    private val shardManager: ShardManager
) : SystemCommandDataProvider(
    "fast_mutual_guilds_cache",
    "Get Fast Mutual Guilds Cache status",
    true,
    true,
    false,
    false
) {

    private var replyHook: InteractionHook? = null

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        val builder = EmbedBuilder()

        val userId = event.getTypedOption(command.argsOption)?.toLong() ?: event.user.idLong

        builder.setTitle("Fast Mutual Guild Cache")
            .setColor(Color.ORANGE)
            .setDescription("\uD83D\uDCE1 Loading...")

        replyHook = event.deferReply().complete()

        val (data1, _) = getWithTiming(userId) { fastMutualGuildsCache.getAsGuilds(userId) }
        val (data2, _) = getWithTiming(userId) { shardManager.getUserById(userId)?.let { shardManager.getMutualGuilds(it) } ?: emptySet() }

        val avg1 = calculateAverageExecutionTime(userId) { getWithTiming(it) { fastMutualGuildsCache.getAsGuilds(userId) } }
        val avg2 = calculateAverageExecutionTime(userId) { getWithTiming(it) { shardManager.getUserById(userId)?.let { shardManager.getMutualGuilds(it) } ?: emptySet() } }

        val avgId1 = calculateAverageExecutionTime(userId) { getWithTiming(it) { fastMutualGuildsCache[userId] } }
        val avgId2 = calculateAverageExecutionTime(userId) {
            getWithTiming(it) {
                shardManager.getUserById(userId)?.let { shardManager.getMutualGuilds(it) }?.mapNotNull { it.idLong } ?: emptySet()
            }
        }

        val integrityCheck = data1.size == data2.size && data2.all { it in data1 } && data1.all { it in data2 }

        replyHook!!.editOriginalEmbeds(builder.build()).queue {
            builder.setTitle("Fast Mutual Guild Cache")
                .setDescription(
                    "Checked user: ${shardManager.getUserById(userId)?.asMention ?: "n/a"} `${userId}`"
                )
                .addField(
                    "**Fast Mutual Guild Cache**", "❯ Entries: `${fastMutualGuildsCache.size}`\n" +
                            "❯ Load user:\n" +
                            "└❯ Ids: `${avgId1}ns`\n" +
                            "└❯ Guilds: `${avg1}ns`", true
                )
                .addField(
                    "**Shard Manager**", "❯ Entries: `${shardManager.guildCache.size()}`\n" +
                            "❯ Load user:\n" +
                            "└❯ Ids: `${avgId2}ns`\n" +
                            "└❯ Guilds: `${avg2}ns`", true
                )
                .addField("❯ Integrity:", if (integrityCheck) "✅ **Passed**" else "❌ Failed", false)
                .addField(
                    "❯ Load speed",
                    "└ Loading ids is **${(avgId2 / avgId1).round(1)}x** faster than using Shard Manager\n" +
                            "└ Loading Guilds is **${(avg2 / avg1).round(1)}x** faster than using Shard Manager", false
                )
                .addBlankField(false)
                .addField("Mutual Guilds:", data1.sortedBy { it.name }.joinToString("\n") { "- ${it.name}" }, false)
                .setColor(if (integrityCheck) Color.GREEN else Color.RED)
                .build()
            replyHook!!.editOriginalEmbeds(builder.build()).setComponents(
                ActionRow.of(
                    successButton("seedCache", "Re-Seed cache"),
                    dangerButton("clearCache", "Clear cache")
                )
            ).queueAndRegisterInteraction(replyHook!!, command)
        }


    }

    override fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        event.deferEdit().queue { hook ->
            when (event.componentId) {
                "seedCache" -> {
                    fastMutualGuildsCache.seedCache()
                    hook.sendMessage("Cache seeded!").setEphemeral(true).queue()
                }

                "clearCache" -> {
                    fastMutualGuildsCache.clear("Requested by user")
                    hook.sendMessage("Cache cleared!").setEphemeral(true).queue()
                }
            }
        }
        return true
    }

    private fun <T> getWithTiming(userId: Long, call: (Long) -> Collection<T>): Pair<Collection<T>, Long> {
        val startTime = System.nanoTime()
        val result = call(userId)
        val endTime = System.nanoTime()
        val timeTaken = endTime - startTime // time in nanoseconds
        return result to timeTaken
    }

    private fun calculateAverageExecutionTime(userId: Long, call: (Long) -> Pair<Collection<Any>, Long>): Double {
        var totalExecutionTime = 0L
        val iterations = 100

        repeat(iterations) {
            val (_, timeTaken) = call.invoke(userId)
            totalExecutionTime += timeTaken
        }

        return totalExecutionTime / iterations.toDouble() // Average time in nanoseconds
    }

}
