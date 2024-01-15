/*
 * Copyright 2024 Viascom Ltd liab. Co
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

@file:JvmName("AlunaMemberUtils")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.util

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import java.time.Duration

/**
 * Get the probable locale of a user based on the most common locale of the mutual servers.
 *
 * !! This will only work if your bot has access to mutualGuilds which is bound to the GUILD_MEMBERS intent !!
 *
 * @return probable Locale
 */
fun User.probableLocale(): DiscordLocale? = mutualGuilds.groupBy { it.locale }.maxByOrNull { it.value.size }?.value?.firstOrNull()?.locale

/**
 * Get the probable locale of a user based on the most common locale of the mutual servers.
 *
 * !! This will only work if your bot has access to mutualGuilds which is bound to the GUILD_MEMBERS intent !!
 *
 * @return probable Locale
 */
fun Member.probableLocale(): DiscordLocale? = user.probableLocale()

/**
 * Returns if this member have administrator permission in this guild
 *
 * @return Is administrator
 */
fun Member.isAdministrator(): Boolean = this.hasPermission(Permission.ADMINISTRATOR)

/**
 * Returns if this member have administrator permission or is the owner of this guild
 *
 * @return Is administrator or owner
 */
fun Member.isAdministratorOrOwner(): Boolean = this.hasPermission(Permission.ADMINISTRATOR) || this.isOwner

/**
 * Returns if this member is rejoining the guild
 *
 * The rejoin flag is not entirely reliable, since Discord has only started tracking rejoins recently. Members who rejoined years ago will not have this flag set.
 *
 * @return is rejoining
 */
fun Member.isRejoining(): Boolean = this.flags.contains(Member.MemberFlag.DID_REJOIN)

/**
 * Return if this member has completed the onboarding process
 *
 * @return is onboarding completed
 */
fun Member.hasCompletedOnboarding(): Boolean = this.flags.contains(Member.MemberFlag.COMPLETED_ONBOARDING)

/**
 * Return if this member has started the onboarding process
 *
 * @return is onboarding started
 */
fun Member.hasStartedOnboarding(): Boolean = this.flags.contains(Member.MemberFlag.STARTED_ONBOARDING)

/**
 * Retrieves the Name displayed in the official Discord Client.
 * The returned name is escaped and will not contain any markdown.
 *
 * @return The guild nickname of this Member or the {@link User#getEffectiveName() effective user name} if no guild nickname is present. The returned name is escaped and will not contain any markdown.
 */
fun Member.effectiveNameEscaped(): String = MarkdownSanitizer.sanitize(escapeMarkdownLinks(this.effectiveName), MarkdownSanitizer.SanitizationStrategy.ESCAPE)

/**
 * Puts this Member in time out in this {@link net.dv8tion.jda.api.entities.Guild Guild} for a specific amount of time.
 *
 * @param  duration
 *         The duration to put this Member in time out for
 * @return {@link net.dv8tion.jda.api.requests.restaction.AuditableRestAction AuditableRestAction}
 * @see Member.timeoutFor timeoutFor
 */
//This needs no @JvmOverloads as there is already a timeoutFor(duration: Duration) method
fun Member.timeoutFor(duration: Duration, reason: String? = null): AuditableRestAction<Void> {
    val action = this.timeoutFor(duration)
    return if (reason != null) action.reason(reason) else action
}

/**
 * Tries to send a direct message (DM) to the user.
 * Exceptions are ignored.
 *
 * @param message the message to send
 * @param then the runnable to be executed after the DM is sent
 */
fun User.tryToSendDM(message: String, then: Runnable) {
    try {
        this.openPrivateChannel().queue({ pc -> pc.sendMessage(message).queue({ then.run() }) { then.run() } }) { then.run() }
    } catch (ignore: Exception) {
    }
}

/**
 * Retrieves a Member by their ID from the given guild.
 *
 * @param guildId The ID of the guild to search in.
 * @param userId The ID of the member to retrieve.
 * @return The Member if found, or null if the guild or member was not found.
 */
fun ShardManager.getMemberById(guildId: String, userId: String): Member? = this.getGuildById(guildId)?.getMemberById(userId)
