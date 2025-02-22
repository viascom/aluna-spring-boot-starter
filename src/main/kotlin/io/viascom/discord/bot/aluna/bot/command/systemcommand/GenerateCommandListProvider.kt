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
import io.viascom.discord.bot.aluna.bot.handler.DiscordCommandHandler
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.DevelopmentStatus
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.util.*
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.FileUpload
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.awt.Color

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class GenerateCommandListProvider(
    private val shardManager: ShardManager,
    private val context: ConfigurableApplicationContext
) : SystemCommandDataProvider(
    "generate_command_list",
    "Generate Command List",
    true,
    false,
    false,
    false
) {

    private lateinit var latestHook: InteractionHook
    private var latestEmbed: EmbedBuilder = EmbedBuilder()

    private var selectedServerIds = arrayListOf<String>()
    private var selectedType = "html"

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        createOverviewMessage()
        event.replyEmbeds(latestEmbed.build()).setEphemeral(true).setComponents(getActionRow()).queueAndRegisterInteraction(
            command,
            arrayListOf(EventRegisterType.BUTTON, EventRegisterType.STRING_SELECT),
            true
        ) {
            latestHook = it
        }
    }

    private fun createOverviewMessage() {
        latestEmbed.clearFields()

        latestEmbed = EmbedBuilder()
            .setColor(Color.BLUE)
            .setTitle("Generate Emoji-Enum")
            .setDescription("This command lets you create a command list")
    }


    private fun getActionRow(): ArrayList<ActionRow> {
        val rows = arrayListOf<ActionRow>()

        val row2 = arrayListOf<ItemComponent>()
        row2.add(
            StringSelectMenu.create("type")
                .addOption("HTML", "html", isDefault = selectedType == "html")
                .addOption("Plain Text", "text", isDefault = selectedType == "text")
                .build()
        )
        rows.add(ActionRow.of(row2))

        val row3 = arrayListOf<ItemComponent>()
        row3.add(dangerButton("cancel", "Cancel"))
        row3.add(successButton("generate", "Generate File"))
        rows.add(ActionRow.of(row3))

        return rows
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        when (event.componentId) {
            "generate" -> {
                val file = generateFile()
                val name = when (selectedType) {
                    "text" -> "commands.txt"
                    "html" -> "commands.html"
                    else -> "commands.html"
                }
                latestHook.editOriginalEmbeds().setContent("⬇️ Generated File: ⬇️").setComponents(arrayListOf())
                    .setFiles(FileUpload.fromData(file.encodeToByteArray(), name)).queue()
            }

            "cancel" -> {
                event.deferEdit().queue()
                createOverviewMessage()
                latestHook.editOriginalEmbeds(latestEmbed.build()).setComponents(arrayListOf()).queue()
            }
        }

        return true
    }

    override fun onStringSelectMenuInteraction(event: StringSelectInteractionEvent): Boolean {
        when (event.componentId) {
            "type" -> {
                event.deferEdit().queue()
                selectedType = event.getSelection()

                createOverviewMessage()
                latestHook.editOriginalEmbeds(latestEmbed.build()).setComponents(getActionRow()).queue()
            }

            "serverList" -> {
                event.deferEdit().queue()
                selectedServerIds.remove(event.getSelection())

                createOverviewMessage()
                latestHook.editOriginalEmbeds(latestEmbed.build()).setComponents(getActionRow()).queue()
            }
        }
        return true
    }

    override fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        when (event.modalId) {
            "add_server" -> {
                event.deferEdit().queue()

                //Check server id
                val serverID = event.getValueAsString("serverId", "0")
                val newServer = try {
                    shardManager.getGuildById(serverID!!)
                } catch (e: Exception) {
                    null
                }
                if (newServer != null) {
                    if (!selectedServerIds.contains(newServer.id)) {
                        selectedServerIds.add(newServer.id)
                    }

                    createOverviewMessage()
                    latestHook.editOriginalEmbeds(latestEmbed.build()).setComponents(getActionRow()).queue()
                } else {
                    createOverviewMessage()

                    //Add error message
                    latestEmbed.setColor(Color.RED)
                    latestEmbed.addField("", "⛔ Server with id `$serverID` does not exist or the bot is not on it!", false)

                    latestHook.editOriginalEmbeds(latestEmbed.build()).setComponents(getActionRow()).queue()
                }
            }
        }
        return true
    }

    private fun generateFile(): String {
        var content = ""

        val commands = context.getBeansOfType(DiscordCommandHandler::class.java).values.filter {
            it.interactionDevelopmentStatus == DevelopmentStatus.LIVE
        }.sortedBy { it.name }

        when (selectedType) {
            "text" -> {
                content = commands.joinToString("\n") { command ->
                    "/${command.name} - ${command.description}" + command.subcommands.joinToString("\n") { subcommand ->
                        "  /${command.name} ${subcommand.name} - ${subcommand.description}"
                    } + command.subcommandGroups.joinToString("\n") { group ->
                        group.subcommands.joinToString("\n") { subcommand ->
                            "  /${command.name} ${group.name} ${subcommand.name} - ${subcommand.description}"
                        }
                    }
                }
            }

            "html" -> {
                content = """
                    <style>
                        :root {
                            --accent-color: #d63384; /* Adjust this to match your site's accent color */
                            --accent-color-soft: #d15995; /* Adjust this to match your site's accent color */
                            --background-color: #121212; /* Adjust for your site's background color */
                            --text-color: #ffffff;
                            --border-color: #333333;
                        }

                        .commands-container {
                            background-color: var(--background-color); /* Dark background */
                            color: var(--text-color); /* White text */
                            font-family: 'Arial', sans-serif;
                            border-radius: 8px;
                            padding: 20px;
                            margin: auto;
                            max-width: 800px;
                        }

                        h2 {
                            color: var(--accent-color); /* Vibrant orange accent for headings */
                            border-bottom: 2px solid var(--accent-color); /* Underline effect for the heading */
                            padding-bottom: 10px;
                            margin-bottom: 20px;
                        }

                        .command-group {
                            border-bottom: 1px solid #333;
                            padding-bottom: 15px;
                            /*margin-bottom: 15px;*/
                        }

                        .command {
                            padding-top: 15px;
                            padding-left: 15px;
                            padding-bottom: 5px;
                            border-left: 3px solid var(--accent-color);
                            margin-bottom: 10px;
                            transition: background-color 0.3s ease;
                            border-radius: 4px;
                            background-color: #262626;
                        }

                        .command-name {
                            color: var(--accent-color); /* Vibrant orange accent for command names */
                            font-weight: bold;
                        }

                        .command-description {
                            color: #c7c7c7; /* Light grey for descriptions */
                        }

                        .sub-command {
                            padding: 15px;
                            padding-left: 20px;
                            padding-bottom: 5px;
                            background-color: #262626;
                            margin-left: 20px;
                            margin-bottom: 10px;
                            border-radius: 4px;
                        }

                        .sub-command-name {
                            color: var(--accent-color-soft); /* Softer orange for subcommand names */
                            font-weight: bold;
                        }

                        .sub-command-description {
                            color: #c7c7c7;
                        }

                        .command-group:last-child {
                            border-bottom: none;
                        }

                        .command:hover, .sub-command:hover {
                            background-color: #333333;
                        }

                    </style>
                <div class="commands-container">
                    <h2>Slash Commands</h2>
                    """.trimIndent()
                commands.forEach { command ->
                    content += """
                        <div class="command-group">
                            <div class="command">
                                <div class="command-name">/${command.name}</div>
                                <p class="command-description">${command.description}</p>
                            </div>
                            """.trimIndent()
                    command.initCommandOptions()
                    runBlocking { runCatching { command.prepareInteraction() } }
                    command.subcommands.forEach {
                        content += """
                            <div class="sub-command">
                                <div class="sub-command-name">/${command.name} ${it.name}</div>
                                <p class="sub-command-description">${it.description}</p>
                            </div>
                                """.trimIndent()
                    }
                    command.subcommandGroups.forEach { group ->
                        group.subcommands.forEach {
                            content += """
                            <div class="sub-command">
                                <div class="sub-command-name">/${command.name} ${group.name} ${it.name}</div>
                                <p class="sub-command-description">${it.description}</p>
                            </div>
                                """.trimIndent()
                        }
                    }
                    content += """
                        </div>
                    """.trimIndent()
                }
                content += """
                </div>
                """.trimIndent()
            }
        }

        return content
    }
}
