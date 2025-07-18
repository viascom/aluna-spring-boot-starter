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
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

public interface DiscordInteractionHandler {

    public var uniqueId: String
    public var author: User
    public var discordBot: DiscordBot

    public suspend fun handleOnButtonInteraction(event: ButtonInteractionEvent): Boolean
    public suspend fun handleOnButtonInteractionTimeout()

    public suspend fun handleOnStringSelectInteraction(event: StringSelectInteractionEvent): Boolean
    public suspend fun handleOnStringSelectInteractionTimeout()

    public suspend fun handleOnEntitySelectInteraction(event: EntitySelectInteractionEvent): Boolean
    public suspend fun handleOnEntitySelectInteractionTimeout()

    public suspend fun handleOnModalInteraction(event: ModalInteractionEvent): Boolean
    public suspend fun handleOnModalInteractionTimeout()
}
