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

package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.bot.handler.DiscordCommandHandler
import io.viascom.discord.bot.aluna.bot.handler.DiscordInteractionHandler
import io.viascom.discord.bot.aluna.bot.handler.DiscordInteractionLocalization
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction

public abstract class CoroutineDiscordCommand @JvmOverloads constructor(
    name: String,
    description: String,

    /**
     * Define a [LocalizationFunction] for this command. If set no null, Aluna will take the implementation of [DiscordInteractionLocalization].
     */
    override var localizations: LocalizationFunction? = null,

    /**
     * If enabled, Aluna will register an event listener for auto complete requests and link it to this command.
     *
     * If such an event gets triggered, the method [onAutoCompleteEvent] will be invoked.
     */
    override val observeAutoComplete: Boolean = false,

    /**
     * If enabled, Aluna will automatically forward the command execution as well as interaction events to the matching sub command.
     *
     * For this to work, you need to annotate your autowired [DiscordSubCommand] or [DiscordSubCommandGroupHandler] implementation with [@SubCommandElement][SubCommandElement]
     * or register them manually with [registerSubCommands] during [initSubCommands].
     *
     * The Top-Level command can not be used (limitation of Discord), but Aluna will nevertheless always call [execute] on the top-level command before executing the sub command method if you need to do some general stuff.
     */
    override val handleSubCommands: Boolean = false,

    /**
     * If enabled, Aluna will direct matching interactions to this command.
     * If a matching instance of this command (based on uniqueId or message) is found, the corresponding method is called. If not, a new instance gets created.
     */
    override val handlePersistentInteractions: Boolean = false
) : DiscordCommandHandler(name, description, localizations, observeAutoComplete, handleSubCommands, handlePersistentInteractions), SlashCommandData,
    InteractionScopedObject, DiscordInteractionHandler {

    /**
     * Method to implement for command execution
     *
     * @param event The [SlashCommandInteractionEvent] that triggered this Command
     */
    public abstract suspend fun execute(event: SlashCommandInteractionEvent)


    /**
     * On destroy gets called, when the object gets destroyed after the defined beanTimoutDelay.
     */
    public open suspend fun onDestroy() {
    }

    //======= Button Interaction =======

    /**
     * This method gets triggered, as soon as a button event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForButtonEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [ButtonInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    public open suspend fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        return false
    }

    /**
     * This method gets triggered, as soon as a button event observer duration timeout is reached.
     */
    public open suspend fun onButtonInteractionTimeout() {
    }

    //======= Select Interaction =======

    /**
     * This method gets triggered, as soon as a select event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForSelectEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [StringSelectInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    public open suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent): Boolean {
        return false
    }

    /**
     * This method gets triggered, as soon as a select event observer duration timeout is reached.
     */
    public open suspend fun onStringSelectInteractionTimeout() {
    }

    /**
     * This method gets triggered, as soon as a select event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForSelectEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [EntitySelectInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    public open suspend fun onEntitySelectInteraction(event: EntitySelectInteractionEvent): Boolean {
        return false
    }

    /**
     * This method gets triggered, as soon as a select event observer duration timeout is reached.
     */
    public open suspend fun onEntitySelectInteractionTimeout() {
    }

    //======= Modal Interaction =======

    /**
     * This method gets triggered, as soon as a modal event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForModalEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [ModalInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    public open suspend fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        return false
    }

    /**
     * This method gets triggered, as soon as a modal event observer duration timeout is reached.
     */
    public open suspend fun onModalInteractionTimeout() {
    }

    //======= Auto Complete =======

    public open suspend fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
    }

    override suspend fun runExecute(event: SlashCommandInteractionEvent) = execute(event)
    override suspend fun runOnDestroy(): Unit = onDestroy()
    override suspend fun runOnButtonInteraction(event: ButtonInteractionEvent) = onButtonInteraction(event)
    override suspend fun runOnButtonInteractionTimeout() = onButtonInteractionTimeout()
    override suspend fun runOnStringSelectInteraction(event: StringSelectInteractionEvent) = onStringSelectInteraction(event)
    override suspend fun runOnStringSelectInteractionTimeout() = onStringSelectInteractionTimeout()
    override suspend fun runOnEntitySelectInteraction(event: EntitySelectInteractionEvent) = onEntitySelectInteraction(event)
    override suspend fun runOnEntitySelectInteractionTimeout() = onEntitySelectInteractionTimeout()
    override suspend fun runOnModalInteraction(event: ModalInteractionEvent) = onModalInteraction(event)
    override suspend fun runOnModalInteractionTimeout() = onModalInteractionTimeout()
    override suspend fun runOnAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) = onAutoCompleteEvent(option, event)

}
