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

package io.viascom.discord.bot.aluna.event

import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.springframework.context.ApplicationEvent
import kotlin.reflect.KClass

/**
 * Discord slash command initialized event. This gets triggered, as soon as the slash commands are initialized.
 *
 * @property newCommands
 * @property updatedCommands
 * @property removedCommands
 *
 */
class DiscordSlashCommandInitializedEvent(
    source: Any,
    val newCommands: List<KClass<out CommandDataImpl>>,
    val updatedCommands: List<KClass<out CommandDataImpl>>,
    val removedCommands: List<String>
) : ApplicationEvent(source)
