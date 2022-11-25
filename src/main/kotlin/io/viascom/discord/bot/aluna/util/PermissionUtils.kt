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

@file:JvmName("AlunaPermissionUtils")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.util

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel

fun Member.hasRole(roleId: String): Boolean = this.roles.any { it.id == roleId }

fun Guild.doesBotHavePermission(permission: Permission): Boolean = this.selfMember.hasPermission(permission)
fun Guild.doesBotHavePermission(permissions: ArrayList<Permission>, needsAll: Boolean = true): Boolean = if (needsAll) {
    this.selfMember.permissions.containsAll(permissions)
} else {
    this.selfMember.permissions.any { it in permissions }
}

fun Member.hasCategoryPermission(category: Category, permission: Permission): Boolean = this.getPermissions(category).contains(permission)
fun Member.hasCategoryPermission(category: Category, permissions: ArrayList<Permission>, needsAll: Boolean = true): Boolean = if (needsAll) {
    this.getPermissions(category).containsAll(permissions)
} else {
    this.getPermissions(category).any { it in permissions }
}

fun Member.hasChannelPermission(channel: GuildChannel, permission: Permission): Boolean = this.getPermissions(channel).contains(permission)
fun Member.hasChannelPermission(channel: GuildChannel, permissions: ArrayList<Permission>, needsAll: Boolean = true): Boolean = if (needsAll) {
    this.getPermissions(channel).containsAll(permissions)
} else {
    this.getPermissions(channel).any { it in permissions }
}

fun Member.hasPermissionOverride(channel: GuildChannel, permission: Permission): Boolean =
    channel.permissionContainer.permissionOverrides.any { it.allowed.contains(permission) && it.member?.id == this.id }

fun Member.hasPermissionOverrides(channel: GuildChannel, permissions: List<Permission>): Boolean =
    channel.permissionContainer.permissionOverrides.any { it.allowed.containsAll(permissions) && it.member?.id == this.id }

fun Role.hasCategoryPermission(category: Category, permission: Permission): Boolean = this.getPermissions(category).contains(permission)
fun Role.hasCategoryPermission(category: Category, permissions: ArrayList<Permission>, needsAll: Boolean = true): Boolean = if (needsAll) {
    this.getPermissions(category).containsAll(permissions)
} else {
    this.getPermissions(category).any { it in permissions }
}

fun Role.hasChannelPermission(channel: GuildChannel, permission: Permission): Boolean = this.getPermissions(channel).contains(permission)
fun Role.hasChannelPermission(channel: GuildChannel, permissions: ArrayList<Permission>, needsAll: Boolean = true): Boolean = if (needsAll) {
    this.getPermissions(channel).containsAll(permissions)
} else {
    this.getPermissions(channel).any { it in permissions }
}