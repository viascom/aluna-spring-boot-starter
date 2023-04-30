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

package io.viascom.discord.bot.aluna.bot

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction

abstract class DiscordMessageContext(name: String, localizations: LocalizationFunction? = null) : DiscordMessageContextMenuHandler(name, localizations) {

    /**
     * The main body method of a [DiscordContextMenuHandler].
     * <br></br>This is the "response" for a successful
     * [#run(DiscordContextMenu)][DiscordContextMenuHandler.execute].
     *
     * @param event The [MessageContextInteractionEvent] that triggered this Command
     */

    protected abstract fun execute(event: MessageContextInteractionEvent)

    /**
     * On destroy gets called, when the object gets destroyed after the defined beanTimoutDelay.
     */
    open fun onDestroy() {
    }

    //======= Button Interaction =======

    /**
     * This method gets triggered, as soon as a button event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForButtonEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [ButtonInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    open fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: java.util.HashMap<String, Any?>): Boolean {
        return false
    }

    /**
     * This method gets triggered, as soon as a button event observer duration timeout is reached.
     */
    open fun onButtonInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    //======= Select Interaction =======

    /**
     * This method gets triggered, as soon as a select event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForSelectEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [StringSelectInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    open fun onStringSelectInteraction(event: StringSelectInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return false
    }

    /**
     * This method gets triggered, as soon as a select event observer duration timeout is reached.
     */
    open fun onStringSelectInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    /**
     * This method gets triggered, as soon as a select event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForSelectEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [EntitySelectInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    open fun onEntitySelectInteraction(event: EntitySelectInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return false
    }

    /**
     * This method gets triggered, as soon as a select event observer duration timeout is reached.
     */
    open fun onEntitySelectInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    //======= Modal Interaction =======

    /**
     * This method gets triggered, as soon as a modal event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForModalEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [ModalInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    open fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return false
    }

    /**
     * This method gets triggered, as soon as a modal event observer duration timeout is reached.
     */
    open fun onModalInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    override suspend fun runExecute(event: MessageContextInteractionEvent) = execute(event)
    override suspend fun runOnDestroy() = onDestroy()
    override suspend fun runOnButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>) = onButtonInteraction(event, additionalData)
    override suspend fun runOnButtonInteractionTimeout(additionalData: HashMap<String, Any?>) = onButtonInteractionTimeout(additionalData)
    override suspend fun runOnStringSelectInteraction(event: StringSelectInteractionEvent, additionalData: HashMap<String, Any?>) = onStringSelectInteraction(event, additionalData)
    override suspend fun runOnStringSelectInteractionTimeout(additionalData: HashMap<String, Any?>) = onStringSelectInteractionTimeout(additionalData)
    override suspend fun runOnEntitySelectInteraction(event: EntitySelectInteractionEvent, additionalData: HashMap<String, Any?>) = onEntitySelectInteraction(event, additionalData)
    override suspend fun runOnEntitySelectInteractionTimeout(additionalData: HashMap<String, Any?>) = onEntitySelectInteractionTimeout(additionalData)
    override suspend fun runOnModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>) = onModalInteraction(event, additionalData)
    override suspend fun runOnModalInteractionTimeout(additionalData: HashMap<String, Any?>) = onModalInteractionTimeout(additionalData)

}