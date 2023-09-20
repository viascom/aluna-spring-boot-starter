/*
 * Copyright 2023 Viascom Ltd liab. Co
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
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class AdminSearchServerPermissionsPage(
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : AdminSearchPageDataProvider(
    "PERMISSIONS",
    "Permissions",
    arrayListOf(AdminSearchDataProvider.AdminSearchType.SERVER)
) {

    override fun onServerRequest(discordServer: Guild, embedBuilder: EmbedBuilder) {
        embedBuilder.clearFields()

        embedBuilder.addField("Assigned Roles", discordServer.selfMember.roles.joinToString("\n") { "- ${it.name} (${it.id})" }, false)
        embedBuilder.addField("${systemCommandEmojiProvider.tickEmoji().formatted} Permissions", discordServer.selfMember.permissions.joinToString("\n") {
            if (it in alunaProperties.discord.defaultPermissions) {
                "- **${it.getName()}**"
            } else {
                "- ${it.getName()}"
            }
        }, true)

        val missingPermissions = alunaProperties.discord.defaultPermissions.filter { it !in discordServer.selfMember.permissions }

        embedBuilder.addField(
            "${systemCommandEmojiProvider.crossEmoji().formatted} Missing Permissions",
            if (missingPermissions.isNotEmpty()) {
                missingPermissions.joinToString("\n") {
                    "- **${it.getName()}**"
                }
            } else {
                "*nothing missing*"
            },
            true
        )
        embedBuilder.addBlankField(false)
        embedBuilder.addField(
            "${systemCommandEmojiProvider.tickEmoji().formatted} @everyone Permissions",
            discordServer.roles.first { it.isPublicRole }.permissions.joinToString("\n") {
                if (it in arrayListOf(Permission.USE_APPLICATION_COMMANDS, Permission.MESSAGE_EXT_EMOJI)) {
                    "- **${it.getName()}**"
                } else {
                    "- ${it.getName()}"
                }
            },
            true
        )

        val missingEveryonePermissions = arrayListOf(
            Permission.USE_APPLICATION_COMMANDS,
            Permission.MESSAGE_EXT_EMOJI
        ).filter { it !in discordServer.roles.first { it.isPublicRole }.permissions }

        embedBuilder.addField(
            "${systemCommandEmojiProvider.crossEmoji().formatted} Missing @everyone Permissions",
            if (missingEveryonePermissions.isNotEmpty()) {
                missingEveryonePermissions.joinToString("\n") {
                    "- **${it.getName()}**"
                }
            } else {
                "*nothing missing*"
            },
            true
        )
    }

}
