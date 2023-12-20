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

import io.viascom.discord.bot.aluna.bot.handler.DiscordSubCommandHandler
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

/**
 * Coroutine discord sub command abstract base class
 */
abstract class CoroutineDiscordSubCommand(name: String, description: String) : DiscordSubCommandHandler(name, description), DiscordSubCommandElement {

    /**
     * Method to implement for command execution
     *
     * @param event The [SlashCommandInteractionEvent] that triggered this Command
     */
    protected abstract suspend fun execute(event: SlashCommandInteractionEvent)


    open suspend fun initCommandOptions() {}

    /**
     * On destroy gets called, when the object gets destroyed after the defined beanTimoutDelay.
     */
    open suspend fun onDestroy() {
    }

    //======= Button Interaction =======

    /**
     * This method gets triggered, as soon as a button event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForButtonEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [ButtonInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    open suspend fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        return false
    }

    /**
     * This method gets triggered, as soon as a button event observer duration timeout is reached.
     */
    open suspend fun onButtonInteractionTimeout() {
    }

    //======= Select Interaction =======

    /**
     * This method gets triggered, as soon as a select event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForSelectEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [StringSelectInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    open suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent): Boolean {
        return false
    }

    /**
     * This method gets triggered, as soon as a select event observer duration timeout is reached.
     */
    open suspend fun onStringSelectInteractionTimeout() {
    }

    /**
     * This method gets triggered, as soon as a select event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForSelectEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [EntitySelectInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    open suspend fun onEntitySelectInteraction(event: EntitySelectInteractionEvent): Boolean {
        return false
    }

    /**
     * This method gets triggered, as soon as a select event observer duration timeout is reached.
     */
    open suspend fun onEntitySelectInteractionTimeout() {
    }

    //======= Modal Interaction =======

    /**
     * This method gets triggered, as soon as a modal event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForModalEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [ModalInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    open suspend fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        return false
    }

    /**
     * This method gets triggered, as soon as a modal event observer duration timeout is reached.
     */
    open suspend fun onModalInteractionTimeout() {
    }

    //======= Auto Complete =======

    open suspend fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
    }

    override suspend fun runExecute(event: SlashCommandInteractionEvent) = execute(event)
    override suspend fun runOnDestroy() = onDestroy()
    override suspend fun runOnButtonInteraction(event: ButtonInteractionEvent) = onButtonInteraction(event)
    override suspend fun runOnButtonInteractionTimeout() = onButtonInteractionTimeout()
    override suspend fun runOnStringSelectInteraction(event: StringSelectInteractionEvent) = onStringSelectInteraction(event)
    override suspend fun runOnStringSelectInteractionTimeout() = onStringSelectInteractionTimeout()
    override suspend fun runOnEntitySelectInteraction(event: EntitySelectInteractionEvent) = onEntitySelectInteraction(event)
    override suspend fun runOnEntitySelectInteractionTimeout() = onEntitySelectInteractionTimeout()
    override suspend fun runOnModalInteraction(event: ModalInteractionEvent) = onModalInteraction(event)
    override suspend fun runOnModalInteractionTimeout() = onModalInteractionTimeout()
    override suspend fun runOnAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) = onAutoCompleteEvent(option, event)
    override suspend fun runInitCommandOptions() = initCommandOptions()
}
