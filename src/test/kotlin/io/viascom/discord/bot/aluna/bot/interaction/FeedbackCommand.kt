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
import io.viascom.discord.bot.aluna.util.addTextField
import io.viascom.discord.bot.aluna.util.getValueAsString
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

@Interaction
class FeedbackCommand : DiscordCommand("feedback", "Add some feedback") {

    override fun execute(event: SlashCommandInteractionEvent) {
        //Create a new modal with two text inputs, one for the title and one for the content
        val modal: Modal = Modal.create("feedback", "Add your Feedback")
            .addTextField("title", "Title", TextInputStyle.SHORT)
            .addTextField("text", "Text", TextInputStyle.PARAGRAPH)
            .build()

        //Show the modal to the user and register the interaction to Aluna
        event.replyModal(modal).queueAndRegisterInteraction(this)
    }

    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        //Create a new embed builder with the content of the modal
        val embed = EmbedBuilder()
            .setTitle("Feedback from ${event.user.asTag}")
            .setDescription(
                """**Title:** ${event.getValueAsString("title", "*n/a*")}
                   **Text:**
                   ${event.getValueAsString("text", "*n/a*")}"""
            )
            .setColor(0xFF00000)

        //Send the modal to the user. (In a productive environment this could be sent to a dedicated channel or saved into a database)
        event.replyEmbeds(embed.build()).queue()

        //Let Aluna know that the event got handled and can be removed from the observer
        return true
    }
}
