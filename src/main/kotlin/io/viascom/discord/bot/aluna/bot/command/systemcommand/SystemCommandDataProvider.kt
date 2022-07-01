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

import io.viascom.discord.bot.aluna.bot.InteractionScopedObject
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.time.Duration

abstract class SystemCommandDataProvider(
    val id: String,
    val name: String,
    var ephemeral: Boolean = true,
    var allowMods: Boolean = false,
    /**
     * Should Aluna call the onArgsAutoComplete method when the user focusing the args field.
     */
    var supportArgsAutoComplete: Boolean = false,
    /**
     * Should Aluna keep the event open or not. If not, Aluna will acknowledge the event before calling execute() and hook is in this case null.
     */
    var autoAcknowledgeEvent: Boolean = true
) : InteractionScopedObject {

    override lateinit var uniqueId: String
    override var beanTimoutDelay: Duration = Duration.ofMinutes(14)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = false
    override var beanCallOnDestroy: Boolean = false

    abstract fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand)
    open fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        return true
    }

    open fun onButtonInteractionTimeout() {}
    open fun onSelectMenuInteraction(event: SelectMenuInteractionEvent): Boolean {
        return true
    }

    open fun onSelectMenuInteractionTimeout() {}
    open fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent, command: SystemCommand) {}


    open fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }
    open fun onModalInteractionTimeout(additionalData: HashMap<String, Any?>) {}
}
