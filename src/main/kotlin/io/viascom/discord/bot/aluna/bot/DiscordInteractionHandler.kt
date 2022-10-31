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

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

interface DiscordInteractionHandler {

    var uniqueId: String
    var author: User
    var discordBot: DiscordBot

    fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean
    fun onButtonInteractionTimeout(additionalData: HashMap<String, Any?>)

    fun onStringSelectInteraction(event: StringSelectInteractionEvent, additionalData: HashMap<String, Any?>): Boolean
    fun onStringSelectInteractionTimeout(additionalData: HashMap<String, Any?>)

    fun onEntitySelectInteraction(event: EntitySelectInteractionEvent, additionalData: HashMap<String, Any?>): Boolean
    fun onEntitySelectInteractionTimeout(additionalData: HashMap<String, Any?>)

    fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean
    fun onModalInteractionTimeout(additionalData: HashMap<String, Any?>)
}
