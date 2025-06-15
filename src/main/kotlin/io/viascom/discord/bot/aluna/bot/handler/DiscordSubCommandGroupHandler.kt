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

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordSubCommandElement
import io.viascom.discord.bot.aluna.bot.InteractionScopedObject
import io.viascom.discord.bot.aluna.model.DevelopmentStatus
import io.viascom.discord.bot.aluna.util.InternalUtil
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData
import java.time.Duration
import kotlin.reflect.jvm.isAccessible

public abstract class DiscordSubCommandGroupHandler(name: String, description: String) : SubcommandGroupData(name, description), InteractionScopedObject, DiscordSubCommandElement {

    /**
     * This gets set by the CommandContext automatically and should not be changed
     */
    override lateinit var uniqueId: String

    override var beanTimoutDelay: Duration = Duration.ofMinutes(14)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = true
    override var beanResetObserverTimeoutOnBeanExtend: Boolean = true
    override var beanCallOnDestroy: Boolean = true
    override var freshInstance: Boolean = true

    public val subCommands: HashMap<String, DiscordSubCommandHandler> = hashMapOf()

    /**
     * Interaction development status
     */
    public var interactionDevelopmentStatus: DevelopmentStatus = DevelopmentStatus.LIVE

    @JvmSynthetic
    internal suspend fun initSubCommands() = withContext(AlunaDispatchers.Internal) {
        if (subCommands.isEmpty()) {
            InternalUtil.getSubCommandElements(this@DiscordSubCommandGroupHandler).forEach { field ->
                field.isAccessible = true
                registerSubCommands(field.getter.call(this@DiscordSubCommandGroupHandler) as DiscordSubCommandHandler)
            }
        }
    }

    public suspend fun registerSubCommand(subCommand: DiscordSubCommandHandler) {
        subCommands[subCommand.name] = subCommand
        subCommand.runInitCommandOptions()
        this.addSubcommands(subCommand)
    }

    public suspend fun registerSubCommands(vararg subCommands: DiscordSubCommandHandler) {
        subCommands.forEach {
            registerSubCommand(it)
        }
    }

    override suspend fun runOnDestroy() {
    }
}
