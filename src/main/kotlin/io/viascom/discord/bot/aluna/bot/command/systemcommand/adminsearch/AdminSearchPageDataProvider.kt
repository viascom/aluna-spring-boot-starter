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
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji
import net.dv8tion.jda.api.interactions.commands.Command

abstract class AdminSearchPageDataProvider(
    val pageId: String,
    val pageName: String,
    val supportedTypes: ArrayList<AdminSearchDataProvider.AdminSearchType>
) {

    open fun onUserRequest(discordUser: User, embedBuilder: EmbedBuilder) {}
    open fun onServerRequest(discordServer: Guild, embedBuilder: EmbedBuilder) {}
    open fun onRoleRequest(discordRole: Role, embedBuilder: EmbedBuilder) {}
    open fun onChannelRequest(discordChannel: Channel, embedBuilder: EmbedBuilder) {}
    open fun onEmoteRequest(discordEmote: RichCustomEmoji, embedBuilder: EmbedBuilder) {}
    open fun onInteractionCommandRequest(discordCommand: Command, embedBuilder: EmbedBuilder) {}
}
