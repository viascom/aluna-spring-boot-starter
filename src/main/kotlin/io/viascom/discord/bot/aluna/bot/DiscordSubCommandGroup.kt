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

package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.util.InternalUtil
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData
import java.time.Duration
import kotlin.reflect.jvm.isAccessible

abstract class DiscordSubCommandGroup(name: String, description: String) : SubcommandGroupData(name, description),
    InteractionScopedObject,
    DiscordSubCommandElement {

    override lateinit var uniqueId: String
    override var beanTimoutDelay: Duration = Duration.ofMinutes(14)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = true
    override var beanCallOnDestroy: Boolean = true

    val subCommands: HashMap<String, DiscordSubCommand> = hashMapOf()

    @JvmSynthetic
    internal fun initSubCommands() {
        if (subCommands.isEmpty()) {
            InternalUtil.getSubCommandElements(this).forEach { field ->
                field.isAccessible = true
                registerSubCommands(field.getter.call(this) as DiscordSubCommand)
            }
        }
    }

    fun registerSubCommand(subCommand: DiscordSubCommand) {
        subCommands[subCommand.name] = subCommand
        this.addSubcommands(subCommand)
    }

    fun registerSubCommands(vararg subCommands: DiscordSubCommand) {
        subCommands.forEach {
            registerSubCommand(it)
        }
    }

}