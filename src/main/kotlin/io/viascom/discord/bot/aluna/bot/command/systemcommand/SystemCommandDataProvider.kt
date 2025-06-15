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

import io.viascom.discord.bot.aluna.bot.InteractionScopedObject
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.time.Duration

public abstract class SystemCommandDataProvider(
    override val id: String,
    override val name: String,
    override var ephemeral: Boolean = true,
    override var allowMods: Boolean = false,
    /**
     * Should Aluna call the onArgsAutoComplete method when the user focusing the args field.
     */
    override var supportArgsAutoComplete: Boolean = false,
    /**
     * Should Aluna keep the event open or not. If not, Aluna will acknowledge the event before calling execute() and hook is in this case provided.
     */
    override var autoAcknowledgeEvent: Boolean = true
) : SystemCommandDataProviderHandler(
    id,
    name,
    ephemeral,
    allowMods,
    supportArgsAutoComplete,
    autoAcknowledgeEvent
), InteractionScopedObject {

    override var uniqueId: String = ""
    override var beanTimoutDelay: Duration = Duration.ofMinutes(14)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = false
    override var beanResetObserverTimeoutOnBeanExtend: Boolean = true
    override var beanCallOnDestroy: Boolean = false
    override var freshInstance: Boolean = true

    public abstract fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand)

    override suspend fun runOnDestroy() {
    }

    public open fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        return true
    }

    public open fun onButtonInteractionTimeout() {}
    public open fun onStringSelectMenuInteraction(event: StringSelectInteractionEvent): Boolean {
        return true
    }

    public open fun onStringSelectInteractionTimeout() {}
    public open fun onEntitySelectInteraction(event: EntitySelectInteractionEvent): Boolean {
        return true
    }

    public open fun onEntitySelectInteractionTimeout() {}
    public open fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent, command: SystemCommand) {}


    public open fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        return true
    }

    public open fun onModalInteractionTimeout() {}

    override suspend fun runExecute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) = execute(event, hook, command)
    override suspend fun runOnButtonInteraction(event: ButtonInteractionEvent) = onButtonInteraction(event)
    override suspend fun runOnButtonInteractionTimeout() = onButtonInteractionTimeout()
    override suspend fun runOnStringSelectMenuInteraction(event: StringSelectInteractionEvent) = onStringSelectMenuInteraction(event)
    override suspend fun runOnStringSelectInteractionTimeout() = onStringSelectInteractionTimeout()
    override suspend fun runOnEntitySelectInteraction(event: EntitySelectInteractionEvent) = onEntitySelectInteraction(event)
    override suspend fun runOnEntitySelectInteractionTimeout() = onEntitySelectInteractionTimeout()
    override suspend fun runOnArgsAutoComplete(event: CommandAutoCompleteInteractionEvent, command: SystemCommand) = onArgsAutoComplete(event, command)
    override suspend fun runOnModalInteraction(event: ModalInteractionEvent) = onModalInteraction(event)
    override suspend fun runOnModalInteractionTimeout() = onModalInteractionTimeout()

}
