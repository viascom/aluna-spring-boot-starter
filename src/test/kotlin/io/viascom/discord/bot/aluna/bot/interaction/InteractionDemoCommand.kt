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

package io.viascom.discord.bot.aluna.bot.interaction

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.Interaction
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.model.DevelopmentStatus
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.util.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import java.awt.Color

@Interaction
class InteractionDemoCommand : DiscordCommand("interaction-demo", "Demo of discord interactions") {

    init {
        //If we set the interactionDevelopmentStatus to IN_DEVELOPMENT, the command will not be shown if Aluna is set to production mode true
        this.interactionDevelopmentStatus = DevelopmentStatus.IN_DEVELOPMENT
    }

    //This variable holds the latest embed and because Aluna will handle the spring scope, we always get the same instance if an interaction is used.
    private lateinit var latestEmbed: EmbedBuilder

    //This variable holds the latest interaction hook, so that we can for example remove the action rows after 14 min in the onDestroy method. 14 min is the default duration, Aluna will keep this bean and connected interaction waiters active.
    private lateinit var latestHook: InteractionHook

    override fun execute(event: SlashCommandInteractionEvent) {
        latestEmbed = EmbedBuilder()
            .setTitle("Interaction Demo")
            .setColor(Color.GREEN)

        val row1 = ActionRow.of(
            createSuccessButton("set_text", "Set Text"),
            createSuccessButton("set_image", "Set Image"),
            createDangerButton("remove_image", "Remove Image")
        )

        val row2 = ActionRow.of(
            SelectMenu.create("color")
                .addOption("Green", Color.GREEN.rgb.toString())
                .addOption("Red", Color.RED.rgb.toString())
                .addOption("Yellow", Color.YELLOW.rgb.toString())
                .addOption("Blue", Color.BLUE.rgb.toString())
                .addOption("Magenta", Color.MAGENTA.rgb.toString())
                .build()
        )

        event.replyEmbeds(latestEmbed.build())
            .addActionRows(row1, row2)
            .queueAndRegisterInteraction(this, arrayListOf(EventRegisterType.BUTTON, EventRegisterType.SELECT, EventRegisterType.MODAL), persist = true) {
                latestHook = it
            }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        when (event.componentId) {
            "set_text" -> {
                val modal = Modal.create("set_text", "Set Image")
                    .addTextField("text", "Text", style = TextInputStyle.PARAGRAPH, value = latestEmbed.build().description, required = false)
                    .build()
                event.replyModal(modal).queue()
            }
            "set_image" -> {
                val modal = Modal.create("set_image", "Set Image")
                    .addTextField("url", "Url", value = latestEmbed.build().image?.url)
                    .build()
                event.replyModal(modal).queue()
            }
            "remove_image" -> {
                latestEmbed.setImage(null)
                event.editMessageEmbeds(latestEmbed.build()).queue { latestHook = it }
            }
        }

        return true
    }

    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        latestEmbed.setColor(event.getSelection().toInt())
        event.editMessageEmbeds(latestEmbed.build()).queue { latestHook = it }
        return true
    }

    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        when (event.modalId) {
            "set_text" -> {
                val text = event.getValueAsString("text", "")
                latestEmbed.setDescription(text)
                event.editMessageEmbeds(latestEmbed.build()).queue { latestHook = it }
            }
            "set_image" -> {
                val image = event.getValueAsString("url", "")
                latestEmbed.setImage(image)
                event.editMessageEmbeds(latestEmbed.build()).queue { latestHook = it }
            }
        }

        return true
    }

    override fun onDestroy() {
        latestHook.editOriginalEmbeds(latestEmbed.build()).removeActionRows().queue()
    }
}