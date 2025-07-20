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

package io.viascom.discord.bot.aluna.bot.interaction

import io.viascom.discord.bot.aluna.bot.DiscordUserContextMenu
import io.viascom.discord.bot.aluna.bot.Interaction
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.util.addTextField
import io.viascom.discord.bot.aluna.util.getValueAsString
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.modals.Modal

@Interaction
class SayHiUserMenuHandler : DiscordUserContextMenu(
    "Say Hi"
) {
    init {
        //Make this context menu only usable in guilds as it makes no sense in dm with the bot
        setContexts(InteractionContextType.GUILD)
    }

    private lateinit var target: User

    override fun execute(event: UserContextInteractionEvent) {
        //Save the target (in this case the user) the user clicked on
        target = event.target

        //Create a new modal with one text input
        val modal: Modal = Modal.create("say_hi", "Say hi")
            .addTextField("message", "Message", TextInputStyle.PARAGRAPH, placeholder = "You can use @user to mention ${target.name}")
            .build()

        //Show the modal to the user and register the interaction to Aluna
        event.replyModal(modal).queueAndRegisterInteraction(this)
    }

    override fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        //Acknowledge the event
        event.deferEdit().queue()

        //Send a new message in the same channel
        event.channel.sendMessage(event.getValueAsString("message")!!.replace("@user", target.asMention)).queue()

        //Let Aluna know that the event got handled and can be removed from the observer
        return true
    }
}
