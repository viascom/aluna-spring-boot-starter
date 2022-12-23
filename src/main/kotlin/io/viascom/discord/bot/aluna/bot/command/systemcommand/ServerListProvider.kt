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

import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.component.AlunaPaginator
import io.viascom.discord.bot.aluna.bot.listener.EventWaiter
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.util.TimestampFormat
import io.viascom.discord.bot.aluna.util.getTypedOption
import io.viascom.discord.bot.aluna.util.splitListInFields
import io.viascom.discord.bot.aluna.util.toDiscordTimestamp
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component
import java.awt.Color

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class ServerListProvider(
    private val shardManager: ShardManager,
    private val eventWaiter: EventWaiter
) : SystemCommandDataProvider(
    "server_list",
    "Show servers",
    true,
    false,
    true
) {

    private lateinit var lastHook: InteractionHook

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        lastHook = hook!!

        val servers = shardManager.guilds

        val sorting = event.getTypedOption(command.argsOption, "name")!!
        val sortedServers: List<String>
        val sortingText: String

        when (sorting) {
            "name_asc" -> {
                sortedServers = servers.sortedBy { it.name }.map { "${it.name} (`${it.id}`)" }
                sortingText = "Name *(ascending)*"
            }

            "name_desc" -> {
                sortedServers = servers.sortedByDescending { it.name }.map { "${it.name} (`${it.id}`)" }
                sortingText = "Name *(descending)*"
            }

            "id" -> {
                sortedServers = servers.sortedBy { it.id }.map { "${it.name} (`${it.id}`)" }
                sortingText = "ID"
            }

            "members_asc" -> {
                sortedServers = servers.sortedBy { it.memberCount }.map { "**${it.memberCount}** - ${it.name} (`${it.id}`)" }
                sortingText = "Member Count *(ascending)*"
            }

            "members_desc" -> {
                sortedServers = servers.sortedByDescending { it.memberCount }.map { "**${it.memberCount}** - ${it.name} (`${it.id}`)" }
                sortingText = "Member Count *(descending)*"
            }

            "join-time_asc" -> {
                sortedServers = servers.sortedBy { it.selfMember.timeJoined }
                    .map { "${it.selfMember.timeJoined.toDiscordTimestamp(TimestampFormat.SHORT_DATE_TIME)} - ${it.name} (`${it.id}`)" }
                sortingText = "Bot Join Time *(ascending)*"
            }

            "join-time_desc" -> {
                sortedServers = servers.sortedByDescending { it.selfMember.timeJoined }
                    .map { "${it.selfMember.timeJoined.toDiscordTimestamp(TimestampFormat.SHORT_DATE_TIME)} - ${it.name} (`${it.id}`)" }
                sortingText = "Bot Join Time *(descending)*"
            }

            else -> {
                sortedServers = servers.sortedBy { it.name }.map { "${it.name} (`${it.id}`)" }
                sortingText = "Name"
            }
        }


        val paginator = AlunaPaginator(
            eventWaiter,
            elementsPerPage = 5,
            showBulkSkipButtons = true,
            bulkSkipNumber = 5,
            wrapPageEnds = true,
            columns = AlunaPaginator.Columns.ONE,
            color = { Color.MAGENTA },
            description = {
                "All servers sorted by $sortingText.\n" +
                        "Total servers: ${shardManager.guilds.size}"
            }
        )

        val serverFields = splitListInFields(sortedServers, "", false)
        paginator.addElements(serverFields)

        paginator.display(lastHook)
    }


    override fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent, command: SystemCommand) {
        val options = arrayListOf(
            Command.Choice("Sort by name (ASC)", "name_asc"),
            Command.Choice("Sort by name (DESC)", "name_desc"),
            Command.Choice("Sort by id", "id"),
            Command.Choice("Sort by member-Count (ASC)", "members_asc"),
            Command.Choice("Sort by member-Count (DESC)", "members_desc"),
            Command.Choice("Sort by bot join time (ASC)", "join-time_asc"),
            Command.Choice("Sort by bot join time (DESC)", "join-time_desc")
        )

        event.replyChoices(options).queue()
    }
}
