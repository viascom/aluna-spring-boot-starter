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
import io.viascom.discord.bot.aluna.bot.command.systemcommand.SystemCommandEmojiProvider
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class AdminSearchServerSettingsPage(
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : AdminSearchPageDataProvider(
    "SETTINGS",
    "Settings",
    arrayListOf(AdminSearchDataProvider.AdminSearchType.SERVER)
) {

    override fun onServerRequest(discordServer: Guild, embedBuilder: EmbedBuilder) {
        embedBuilder.clearFields()

        if (discordServer.description != null && discordServer.description!!.isNotEmpty()) {
            embedBuilder.addField("Description", discordServer.description!!, true)
        }
        embedBuilder.addField("Emotes", "${discordServer.emojis.size} / ${discordServer.maxEmojis}", true)
        embedBuilder.addField("Members", "${discordServer.members.size} / ${discordServer.maxMembers}", true)
        embedBuilder.addField("Max Bitrate", "${discordServer.maxBitrate}", true)
        embedBuilder.addField("Max Filesize", "${discordServer.maxFileSize / 1000000} MB", true)
        embedBuilder.addField("Max Presences", "${discordServer.maxPresences}", true)
        embedBuilder.addField(
            "Default Notification Level", when (discordServer.defaultNotificationLevel) {
                Guild.NotificationLevel.ALL_MESSAGES -> "Every message sent in this guild will result in a message ping."
                Guild.NotificationLevel.MENTIONS_ONLY -> "Only messages that specifically mention will result in a ping."
                Guild.NotificationLevel.UNKNOWN -> "unknown"
            }, true
        )
        embedBuilder.addField(
            "Explicit Content Level", when (discordServer.explicitContentLevel) {
                Guild.ExplicitContentLevel.OFF -> systemCommandEmojiProvider.crossEmoji().formatted + " Don't scan any messages."
                Guild.ExplicitContentLevel.NO_ROLE -> "\uD83D\uDD75️ Scan messages from members without a role."
                Guild.ExplicitContentLevel.ALL -> "\uD83D\uDEC2 Scan messages sent by all members."
                Guild.ExplicitContentLevel.UNKNOWN -> "Unknown filter level!"
            }, true
        )
        embedBuilder.addField(
            "NSFW Level", when (discordServer.nsfwLevel) {
                Guild.NSFWLevel.DEFAULT -> "\uD83D\uDFE2 Discord has not rated this guild."
                Guild.NSFWLevel.EXPLICIT -> "\uD83D\uDD1E Is classified as a NSFW server"
                Guild.NSFWLevel.SAFE -> "\uD83D\uDFE2 Doesn't classify as a NSFW server"
                Guild.NSFWLevel.AGE_RESTRICTED -> "\uD83D\uDD1E Is classified as NSFW and has an age restriction in place"
                Guild.NSFWLevel.UNKNOWN -> "unknown"
            }, true
        )
        embedBuilder.addField(
            "Required MFA Level", when (discordServer.requiredMFALevel) {
                Guild.MFALevel.NONE -> systemCommandEmojiProvider.crossEmoji().formatted + " No"
                Guild.MFALevel.TWO_FACTOR_AUTH -> systemCommandEmojiProvider.tickEmoji().formatted + " Yes"
                else -> "unknown"
            }, true
        )
        embedBuilder.addField(
            "Verification Level", when (discordServer.verificationLevel) {
                Guild.VerificationLevel.NONE -> "⚫ None"
                Guild.VerificationLevel.LOW -> "\uD83D\uDFE2 Low"
                Guild.VerificationLevel.MEDIUM -> "\uD83D\uDFE1 Medium"
                Guild.VerificationLevel.HIGH -> "\uD83D\uDFE0 High"
                Guild.VerificationLevel.VERY_HIGH -> "\uD83D\uDD34 Very High"
                Guild.VerificationLevel.UNKNOWN -> "unknown"
            }, true
        )
        embedBuilder.addField(
            "Booster", when (discordServer.boostTier) {
                Guild.BoostTier.NONE -> systemCommandEmojiProvider.crossEmoji().formatted + " None"
                Guild.BoostTier.TIER_1 -> "1️⃣ Tier 1"
                Guild.BoostTier.TIER_2 -> "2️⃣ Tier 2"
                Guild.BoostTier.TIER_3 -> "3️⃣ Tier 3"
                Guild.BoostTier.UNKNOWN -> "unknown"
            } + " (${discordServer.boostCount})", true
        )
        embedBuilder.addField(
            "Boost Progress Bar",
            if (discordServer.isBoostProgressBarEnabled) systemCommandEmojiProvider.tickEmoji().formatted + " Yes" else systemCommandEmojiProvider.crossEmoji().formatted + " No",
            true
        )

        if (discordServer.defaultChannel != null) {
            embedBuilder.addField("Default Channel", "${discordServer.defaultChannel!!.name} (`${discordServer.defaultChannel!!.id}`)", false)
        }
        if (discordServer.communityUpdatesChannel != null) {
            embedBuilder.addField(
                "Community Updates Channel",
                "${discordServer.communityUpdatesChannel!!.name} (`${discordServer.communityUpdatesChannel!!.id}`)",
                false
            )
        }
        if (discordServer.rulesChannel != null) {
            embedBuilder.addField("Rules Channel", "${discordServer.rulesChannel!!.name} (`${discordServer.rulesChannel!!.id}`)", false)
        }
        if (discordServer.systemChannel != null) {
            embedBuilder.addField("System Channel", "${discordServer.systemChannel!!.name} (`${discordServer.systemChannel!!.id}`)", false)
        }
        if (discordServer.bannerUrl != null) {
            embedBuilder.addField("Splash-Url", "[Link](${discordServer.splashUrl})", true)
        }
        if (discordServer.bannerUrl != null) {
            embedBuilder.addField("Banner-Url", "[Link](${discordServer.bannerUrl})", true)
        }
    }

}
