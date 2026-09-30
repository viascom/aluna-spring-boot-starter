/*
 * Copyright 2026 Viascom Ltd liab. Co
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

package io.viascom.discord.bot.aluna.util

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import org.junit.jupiter.api.Test

/**
 * Unit tests for [hasAccessibleChannelPermission].
 */
class PermissionUtilsTest {

    private fun channel(obfuscated: Boolean): GuildChannel = mockk { every { isObfuscated } returns obfuscated }

    private fun member(channel: GuildChannel, hasPermission: Boolean): Member = mockk {
        every { hasPermission(channel, Permission.MESSAGE_MANAGE) } returns hasPermission
    }

    @Test
    fun `obfuscated channel is never accessible even if JDA reports the permission`() {
        val channel = channel(obfuscated = true)

        expect(member(channel, hasPermission = true).hasAccessibleChannelPermission(channel, Permission.MESSAGE_MANAGE)).toEqual(false)
    }

    @Test
    fun `visible channel follows JDA permission check`() {
        val channel = channel(obfuscated = false)

        expect(member(channel, hasPermission = true).hasAccessibleChannelPermission(channel, Permission.MESSAGE_MANAGE)).toEqual(true)
        expect(member(channel, hasPermission = false).hasAccessibleChannelPermission(channel, Permission.MESSAGE_MANAGE)).toEqual(false)
    }
}
