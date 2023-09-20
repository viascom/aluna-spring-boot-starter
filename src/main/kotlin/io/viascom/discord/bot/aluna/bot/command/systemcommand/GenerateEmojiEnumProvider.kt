/*
 * Copyright 2023 Viascom Ltd liab. Co
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
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.util.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.FileUpload
import org.springframework.stereotype.Component
import java.awt.Color

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class GenerateEmojiEnumProvider(
    private val shardManager: ShardManager
) : SystemCommandDataProvider(
    "generate_emoji_enum",
    "Generate Emoji-Enum",
    true,
    false,
    false,
    false
) {

    private lateinit var latestHook: InteractionHook
    private var latestEmbed: EmbedBuilder = EmbedBuilder()

    private var selectedServerIds = arrayListOf<String>()
    private var selectedType = "kotlin"

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        createOverviewMessage()
        event.replyEmbeds(latestEmbed.build()).setEphemeral(true).setComponents(getActionRow()).queueAndRegisterInteraction(
            command,
            arrayListOf(EventRegisterType.BUTTON, EventRegisterType.STRING_SELECT, EventRegisterType.MODAL),
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
            .setDescription("This command lets you create Emotji-Enums which extend the Aluna DiscordEmoji interface. To get started add the servers you want and hit generate.")
            .addField("Selected Servers", selectedServerIds.joinToString("\n") { "- ${shardManager.getGuildById(it)?.name ?: it}" }, false)
    }

    private fun createRemoveMessage() {
        latestEmbed.clearFields()
        createOverviewMessage()

        latestEmbed.addField("", "⬇️ Select a server you want to remove from the list ⬇️", false)
    }

    private fun getRemoveActionRow(): ArrayList<ActionRow> {
        val rows = arrayListOf<ActionRow>()
        val row1 = arrayListOf<ItemComponent>()
        val serverList = StringSelectMenu.create("serverList")

        selectedServerIds.mapNotNull { shardManager.getGuildById(it) }.forEach {
            serverList.addOption(it.name, it.id, it.id)
        }

        row1.add(serverList.build())
        rows.add(ActionRow.of(row1))

        val row2 = arrayListOf<ItemComponent>()
        row2.add(dangerButton("cancel-remove", "Cancel"))
        rows.add(ActionRow.of(row2))

        return rows
    }

    private fun getActionRow(): ArrayList<ActionRow> {
        val rows = arrayListOf<ActionRow>()

        val row1 = arrayListOf<ItemComponent>()
        row1.add(primaryButton("add", "Add Server"))
        row1.add(dangerButton("remove", "Remove Server", disabled = selectedServerIds.isEmpty()))
        rows.add(ActionRow.of(row1))

        val row2 = arrayListOf<ItemComponent>()
        row2.add(
            StringSelectMenu.create("type")
                .addOption("Kotlin", "kotlin", isDefault = selectedType == "kotlin")
                .addOption("Java", "java", isDefault = selectedType == "java")
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
            "add" -> {
                event.replyModal(
                    Modal.create("add_server", "Add Server")
                        .addTextField("serverId", "Server-ID")
                        .build()
                ).queue()
            }

            "remove" -> {
                event.deferEdit().queue()

                createRemoveMessage()
                latestHook.editOriginalEmbeds(latestEmbed.build()).setComponents(getRemoveActionRow()).queue()
            }

            "generate" -> {
                val file = generateFile()
                val name = when (selectedType) {
                    "text" -> "emojis.txt"
                    "kotlin" -> "MyEmotes.kt"
                    "java" -> "MyEmotes.java"
                    else -> "emojis.txt"
                }
                latestHook.editOriginalEmbeds().setContent("⬇️ Generated File: ⬇️").setComponents(arrayListOf())
                    .setFiles(FileUpload.fromData(file.encodeToByteArray(), name)).queue()
            }

            "cancel-remove" -> {
                event.deferEdit().queue()
                createOverviewMessage()
                latestHook.editOriginalEmbeds(latestEmbed.build()).setComponents(getActionRow()).queue()
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

        when (selectedType) {
            "text" -> {
                selectedServerIds.mapNotNull { shardManager.getGuildById(it) }.forEach { guild ->
                    content += "\n\n** ${guild.name} (${guild.id})**\n"
                    content += guild.emojis.sortedBy { it.name }.joinToString("\n") { "${it.asMention} `${it.asMention}`" }
                }
            }

            "kotlin" -> {
                content = """
                    import io.viascom.discord.bot.aluna.model.DiscordEmoji;
                    
                    /**
                     * My emojis
                     *
                     * @property id Id of the emoji
                     * @property emojiName Name of the emoji
                     * @property animated Is this emoji animated
                     */
                    enum class MyEmojis(override val id: String, override val emojiName: String, override val animated: Boolean = false) : DiscordEmoji {
                """.trimIndent()

                selectedServerIds.mapNotNull { shardManager.getGuildById(it) }.forEach { guild ->
                    content += "\n\n    //${guild.name} (${guild.id})\n"
                    content += guild.emojis
                        .sortedBy { it.name }
                        .joinToString("\n") { "    ${it.name.uppercase()}(\"${it.id}\", \"${it.name}\"" + if (it.isAnimated) ", true)," else ")," }
                }

                content = content.dropLast(1)
                content += "\n}"
            }

            "java" -> {
                content = """
                   import io.viascom.discord.bot.aluna.model.DiscordEmoji;
                   import org.jetbrains.annotations.NotNull;

                   /**
                    * My emojis
                    */
                   public enum MyEmojis implements DiscordEmoji {
                """.trimIndent()

                selectedServerIds.mapNotNull { shardManager.getGuildById(it) }.forEach { guild ->
                    content += "\n\n    //${guild.name} (${guild.id})\n"
                    content += guild.emojis
                        .sortedBy { it.name }
                        .joinToString("\n") { "    ${it.name.uppercase()}(\"${it.id}\", \"${it.name}\"" + if (it.isAnimated) ", true)," else ")," }
                }

                content = content.dropLast(1)
                content += ";\n\n"

                content += """
                    
                       final String id;
                       final String name;
                       final boolean animated;
                   
                       MyEmojis(String id, String name, boolean animated) {
                           this.id = id;
                           this.name = name;
                           this.animated = animated;
                       }
                   
                       MyEmojis(String id, String name) {
                           this.id = id;
                           this.name = name;
                           this.animated = false;
                       }

                       @Override
                       public boolean getAnimated() {
                           return animated;
                       }

                       @NotNull
                       @Override
                       public String getEmojiName() {
                           return name;
                       }

                       @NotNull
                       @Override
                       public String getId() {
                           return id;
                       }

                   }
                """.trimIndent()
            }
        }

        return content
    }
}
