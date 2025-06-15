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

public abstract class SystemCommandDataProviderHandler(
    public open val id: String,
    public open val name: String,
    public open var ephemeral: Boolean = true,
    public open var allowMods: Boolean = false,
    /**
     * Should Aluna call the onArgsAutoComplete method when the user focusing the args field.
     */
    public open var supportArgsAutoComplete: Boolean = false,
    /**
     * Should Aluna keep the event open or not. If not, Aluna will acknowledge the event before calling execute() and hook is in this case provided.
     */
    public open var autoAcknowledgeEvent: Boolean = true
) : InteractionScopedObject {

    override var uniqueId: String = ""
    override var beanTimoutDelay: Duration = Duration.ofMinutes(14)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = false
    override var beanResetObserverTimeoutOnBeanExtend: Boolean = true
    override var beanCallOnDestroy: Boolean = false
    override var freshInstance: Boolean = true

    @JvmSynthetic
    internal abstract suspend fun runExecute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand)

    override suspend fun runOnDestroy() {
    }

    @JvmSynthetic
    internal abstract suspend fun runOnButtonInteraction(event: ButtonInteractionEvent): Boolean

    @JvmSynthetic
    internal abstract suspend fun runOnButtonInteractionTimeout()

    @JvmSynthetic
    internal abstract suspend fun runOnStringSelectMenuInteraction(event: StringSelectInteractionEvent): Boolean

    @JvmSynthetic
    internal abstract suspend fun runOnStringSelectInteractionTimeout()

    @JvmSynthetic
    internal abstract suspend fun runOnEntitySelectInteraction(event: EntitySelectInteractionEvent): Boolean

    @JvmSynthetic
    internal abstract suspend fun runOnEntitySelectInteractionTimeout()

    @JvmSynthetic
    internal abstract suspend fun runOnArgsAutoComplete(event: CommandAutoCompleteInteractionEvent, command: SystemCommand)

    @JvmSynthetic
    internal abstract suspend fun runOnModalInteraction(event: ModalInteractionEvent): Boolean

    @JvmSynthetic
    internal abstract suspend fun runOnModalInteractionTimeout()

}
