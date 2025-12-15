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
import io.viascom.discord.bot.aluna.bot.component.AlunaPaginator
import io.viascom.discord.bot.aluna.bot.listener.EventWaiter
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import org.springframework.stereotype.Component
import java.awt.Color

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
public class ShowAllSystemCommandsProvider(
    private val systemCommandDataProviders: List<SystemCommandDataProvider>,
    private val alunaProperties: AlunaProperties,
    private val eventWaiter: EventWaiter
) : SystemCommandDataProvider(
    "show_all_system_commands",
    "Show all system commands",
    true,
    true,
    false,
    true
) {

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        val interactionHook = hook ?: event.deferReply(true).complete()

        val sortedSystemCommands = systemCommandDataProviders
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.id })

        val fields = sortedSystemCommands.map { provider ->
            val modAllowed = isModAllowed(provider)
            val usageHint = if (provider.supportArgsAutoComplete) {
                "`/system-command command:${provider.id} args:`"
            } else {
                "`/system-command command:${provider.id}`"
            }

            MessageEmbed.Field(
                provider.id,
                "${provider.name}\n" +
                        "└ Use: $usageHint\n" +
                        "└ Mods: ${if (modAllowed) "Yes" else "No"}\n" +
                        "└ Ephemeral: ${if (provider.ephemeral) "Yes" else "No"}",
                false
            )
        }

        val paginator = AlunaPaginator(
            eventWaiter,
            elementsPerPage = 10,
            showBulkSkipButtons = true,
            bulkSkipNumber = 5,
            wrapPageEnds = true,
            columns = AlunaPaginator.Columns.ONE,
            color = { Color.MAGENTA },
            description = {
                "All system commands (providers) sorted alphabetically.\n" +
                        "Total: ${sortedSystemCommands.size}"
            },
            showButtons = true,
            showSelections = true,
            showFooter = true,
            onPageText = { page -> "Page ${page.number}/${page.total}" }
        )

        paginator.addElements(fields)
        paginator.display(interactionHook)
    }

    private fun isModAllowed(provider: SystemCommandDataProvider): Boolean {
        val override = alunaProperties.command.systemCommand.allowedForModeratorsFunctions
            ?.firstOrNull { it == provider.id }

        return when {
            override != null -> true
            else -> provider.allowMods
        }
    }
}
