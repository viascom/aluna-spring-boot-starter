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

import io.viascom.discord.bot.aluna.bot.DiscordUserContextMenuHandler
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import org.springframework.context.ApplicationEvent

/**
 * Discord message context menu event which gets fired before the context menu is executed.
 * This event is fired asynchronously.
 *
 * @param user user of the context menu
 * @param channel channel of the context menu
 * @param guild server of the context menu if available
 * @param name name of the context menu§
 * @param contextMenu context menu itself
 */
class DiscordUserContextEvent(
    source: Any,
    val user: User,
    val channel: Channel?,
    val guild: Guild?,
    val name: String,
    val contextMenu: DiscordUserContextMenuHandler
) : ApplicationEvent(source)
