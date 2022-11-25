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

@file:JvmName("AlunaChannelUtils")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.util

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel

/**
 * Sort text channels by comparator
 *
 * @param comparator The comparator to use for sorting
 * @param failure Failure consumer
 * @param success Success consumer
 */
fun Category.sortTextChannelsBy(
    comparator: Comparator<GuildChannel>,
    failure: (exception: Throwable) -> Unit = {},
    success: (Void) -> Unit
) = this.guild.modifyTextChannelPositions(this).sortOrder(comparator).queue(success, failure)

/**
 * Sort voice channels by comparator
 *
 * @param comparator The comparator to use for sorting
 * @param failure Failure consumer
 * @param success Success consumer
 */
fun Category.sortVoiceChannelsBy(
    comparator: Comparator<GuildChannel>,
    failure: (exception: Throwable) -> Unit = {},
    success: (Void) -> Unit
) = this.guild.modifyVoiceChannelPositions(this).sortOrder(comparator).queue(success, failure)

/**
 * Sort categories by comparator
 *
 * @param comparator The comparator to use for sorting
 * @param failure Failure consumer
 * @param success Success consumer
 */
fun Guild.sortCategoriesBy(
    comparator: Comparator<GuildChannel>,
    failure: (exception: Throwable) -> Unit = {},
    success: (Void) -> Unit
) = this.modifyCategoryPositions().sortOrder(comparator).queue(success, failure)

/**
 * Get channels where both the bot and member have view permissions
 *
 * @param member Member which should have view permissions
 * @param type Type of channels to include
 * @return list of channels where the bot and the member have view permissions
 */
fun Guild.getChannelsWithBotAndMember(member: Member, type: ArrayList<ChannelType>): List<GuildChannel> {
    val aleevaChannels = this.getChannels(false).filter { it.type in type }
    return aleevaChannels
        .filter { member.hasChannelPermission(it, Permission.VIEW_CHANNEL) }
        .sortedBy { (it as StandardGuildChannel).position }
}