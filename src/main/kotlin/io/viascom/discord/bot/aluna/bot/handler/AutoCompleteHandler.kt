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

package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.InteractionScopedObject
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

/**
 * Global auto complete handler to provide choices for multiple commands.
 *
 * @property commands Commands which this auto complete handler is mapped to
 * @property option Name of the option or null if this auto complete handler should reply on all options
 */
public abstract class AutoCompleteHandler @JvmOverloads constructor(public val commands: ArrayList<Class<out DiscordCommandHandler>>, public val option: String? = null) :
    InteractionScopedObject {

    /**
     * @property command Command which this auto complete handler is mapped to
     * @property option Name of the option or null if this auto complete handler should reply on all options
     */
    public constructor(command: Class<out DiscordCommandHandler>, option: String? = null) : this(arrayListOf(command), option)

    @Autowired
    public lateinit var discordBot: DiscordBot

    @Autowired
    public lateinit var discordInteractionLoadAdditionalData: DiscordInteractionLoadAdditionalData

    public val logger: Logger = LoggerFactory.getLogger(javaClass)

    //This gets set by the CommandContext automatically
    override lateinit var uniqueId: String

    override var beanTimoutDelay: Duration = Duration.ofMinutes(5)
    override var beanUseAutoCompleteBean: Boolean = false
    override var beanRemoveObserverOnDestroy: Boolean = false
    override var beanResetObserverTimeoutOnBeanExtend: Boolean = true
    override var beanCallOnDestroy: Boolean = false
    override var freshInstance: Boolean = true

    /**
     * This method gets triggered, as soon as an auto complete event for the option is called.
     * Before calling this method, Aluna will execute discordCommandLoadAdditionalData.loadData()
     *
     * @param event
     */
    public abstract fun onRequest(event: CommandAutoCompleteInteractionEvent)

    @JvmSynthetic
    internal fun onRequestCall(event: CommandAutoCompleteInteractionEvent) {
        discordInteractionLoadAdditionalData.loadData(event)
        onRequest(event)
    }

    override suspend fun runOnDestroy() {
    }
}
