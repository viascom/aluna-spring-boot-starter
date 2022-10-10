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
import io.viascom.discord.bot.aluna.bot.component.AlunaPaginator
import io.viascom.discord.bot.aluna.bot.listener.EventWaiter
import io.viascom.discord.bot.aluna.model.UseScope
import net.dv8tion.jda.api.entities.MessageEmbed.Field
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

@Interaction
class ServerMembersCommand(
    private val eventWaiter: EventWaiter
) : DiscordCommand("server-members", "Show server members") {

    init {
        this.useScope = UseScope.GUILD_ONLY
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        val hook = event.deferReply().complete()

        val paginator = AlunaPaginator(
            eventWaiter,
            elementsPerPage = 5,
            showBulkSkipButtons = true,
            bulkSkipNumber = 5,
            showSelections = true,
            columns = AlunaPaginator.Columns.TWO
        )

        paginator.addElements(event.guild!!.members.map { Field(it.effectiveName, it.id, true) })
        paginator.display(hook)
    }
}