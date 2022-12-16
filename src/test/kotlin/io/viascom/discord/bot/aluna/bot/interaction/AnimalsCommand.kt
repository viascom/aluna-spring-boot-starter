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

package io.viascom.discord.bot.aluna.bot.interaction

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.Interaction
import io.viascom.discord.bot.aluna.bot.SubCommandElement
import io.viascom.discord.bot.aluna.bot.interaction.animal.BunnyCommand
import io.viascom.discord.bot.aluna.bot.interaction.animal.ForestSubCommandGroup
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.beans.factory.annotation.Autowired

@Interaction
class AnimalsCommand(
    @SubCommandElement
    private val forestSubCommandGroup: ForestSubCommandGroup,
) : DiscordCommand("animal", "Show images of animals", handleSubCommands = true) {

    @Autowired
    @SubCommandElement
    private lateinit var bunnyCommand: BunnyCommand

    override fun execute(event: SlashCommandInteractionEvent) {

    }

}