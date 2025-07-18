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

package io.viascom.discord.bot.aluna.bot.command.systemcommand.adminsearch

import io.viascom.discord.bot.aluna.bot.command.systemcommand.AdminSearchDataProvider
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
public class AdminSearchServerEmotesPage : AdminSearchPageDataProvider(
    "EMOTES",
    "Emotes",
    arrayListOf(AdminSearchDataProvider.AdminSearchType.SERVER)
) {

    override fun onServerRequest(discordServer: Guild, embedBuilder: EmbedBuilder) {
        embedBuilder.clearFields()

        var text = ""
        var isFirst = true

        discordServer.retrieveEmojis().complete()

        discordServer.emojis.sortedBy { it.name }.forEach {
            val newElement = "${it.asMention}  `${it.asMention}`"
            if (text.length + newElement.length >= 1000) {
                embedBuilder.addField(if (isFirst) "Emotes (${discordServer.emojis.size})" else "", text, false)
                text = ""
                isFirst = false
            }
            text += "\n" + newElement
        }

        if (text.isNotEmpty()) {
            embedBuilder.addField(if (isFirst) "Emotes (${discordServer.emojis.size})" else "", text, false)
        }

    }

}
