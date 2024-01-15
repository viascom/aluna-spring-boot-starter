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

@file:JvmName("AlunaPermissionUtils")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.util

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel

/**
 * Checks whether the member has a specific role.
 *
 * @param roleId The ID of the role to check.
 * @return `true` if the member has the role, `false` otherwise.
 */
fun Member.hasRole(roleId: String): Boolean = this.roles.any { it.id == roleId }

/**
 * Checks if the bot has the given permission in the guild.
 *
 * @param permission The permission to check for.
 * @return `true` if the bot has the permission, `false` otherwise.
 */
fun Guild.doesBotHavePermission(permission: Permission): Boolean = this.selfMember.hasPermission(permission)

/**
 * Checks whether the bot has the specified permissions in the guild.
 *
 * @param permissions the list of permissions to check against
 * @param needsAll determines if the bot needs to have all the provided permissions (`true`) or at least one (`false`)
 *                 Default is `true`.
 * @return `true` if the bot has the specified permissions, `false` otherwise
 */
fun Guild.doesBotHavePermission(permissions: ArrayList<Permission>, needsAll: Boolean = true): Boolean = if (needsAll) {
    this.selfMember.permissions.containsAll(permissions)
} else {
    this.selfMember.permissions.any { it in permissions }
}

/**
 * Checks if the member has the specified permission for the given category.
 *
 * @param category The category to check the permission for.
 * @param permission The permission to check.
 * @return `true` if the member has the permission for the category, `false` otherwise.
 */
fun Member.hasCategoryPermission(category: Category, permission: Permission): Boolean = this.getPermissions(category).contains(permission)

/**
 * Checks if the member has the specified category permissions.
 *
 * @param category The category to check permissions against.
 * @param permissions The list of permissions to check for.
 * @param needsAll Specifies whether all permissions must be present (`true`) or at least one needs to be present (`false`).
 *                 Default is `true`.
 *
 * @return `true` if the member has the specified category permissions, otherwise `false`.
 */
fun Member.hasCategoryPermission(category: Category, permissions: ArrayList<Permission>, needsAll: Boolean = true): Boolean = if (needsAll) {
    this.getPermissions(category).containsAll(permissions)
} else {
    this.getPermissions(category).any { it in permissions }
}

/**
 * Checks if a member has a specific channel permission.
 *
 * @param channel The channel to check for permission.
 * @param permission The permission to check for.
 * @return `true` if the member has the specified permission for the channel, `false` otherwise.
 */
fun Member.hasChannelPermission(channel: GuildChannel, permission: Permission): Boolean = this.getPermissions(channel).contains(permission)

/**
 * Checks if the member has the specified channel permissions.
 *
 * @param channel The guild channel to check permissions for.
 * @param permissions The list of permissions to check.
 * @param needsAll Specifies whether all permissions must be present (`true`) or at least one needs to be present (`false`).
 *                 Default is `true`.
 * @return `true` if the member has the specified channel permissions, `false` otherwise.
 */
fun Member.hasChannelPermission(channel: GuildChannel, permissions: ArrayList<Permission>, needsAll: Boolean = true): Boolean = if (needsAll) {
    this.getPermissions(channel).containsAll(permissions)
} else {
    this.getPermissions(channel).any { it in permissions }
}

/**
 * Checks if the member has permission override for the given channel and permission.
 *
 * @param channel The guild channel to check permission override for.
 * @param permission The permission to check.
 * @return `true` if the member has permission override, otherwise `false`.
 */
fun Member.hasPermissionOverride(channel: GuildChannel, permission: Permission): Boolean =
    channel.permissionContainer.permissionOverrides.any { it.allowed.contains(permission) && it.member?.id == this.id }

/**
 * Checks if the member has permission overrides for a specific guild channel.
 *
 * @param channel The guild channel to check for permission overrides.
 * @param permissions The list of permissions to check for in the overrides.
 * @return `true` if the member has permission overrides that allow all the specified permissions
 *         in the given guild channel, `false` otherwise.
 */
fun Member.hasPermissionOverrides(channel: GuildChannel, permissions: List<Permission>): Boolean =
    channel.permissionContainer.permissionOverrides.any { it.allowed.containsAll(permissions) && it.member?.id == this.id }

/**
 * Checks if the Role has the specified permission for the given category.
 *
 * @param category The category to check the permission for.
 * @param permission The permission to check.
 * @return `true` if the Role has the permission for the category, `false` otherwise.
 */
fun Role.hasCategoryPermission(category: Category, permission: Permission): Boolean = this.getPermissions(category).contains(permission)

/**
 * Checks if the role has the specified permissions for a particular category.
 *
 * @param category The category to check permissions for.
 * @param permissions The list of permissions to check against.
 * @param needsAll Specifies whether all permissions must be present (`true`) or at least one needs to be present (`false`).
 *                 Default is `true`.
 *
 * @return `true` if the role has the required permissions, `false` otherwise.
 */
fun Role.hasCategoryPermission(category: Category, permissions: ArrayList<Permission>, needsAll: Boolean = true): Boolean = if (needsAll) {
    this.getPermissions(category).containsAll(permissions)
} else {
    this.getPermissions(category).any { it in permissions }
}

/**
 * Checks if the role has a specified permission for a given channel.
 *
 * @param channel The channel to check the role's permission against.
 * @param permission The permission to check.
 * @return `true` if the role has the specified permission for the channel, `false` otherwise.
 */
fun Role.hasChannelPermission(channel: GuildChannel, permission: Permission): Boolean = this.getPermissions(channel).contains(permission)

/**
 * Checks if the role has the specified channel permissions.
 *
 * @param channel The guild channel to check the permissions against.
 * @param permissions The list of permissions to check for.
 * @param needsAll Specifies whether all permissions must be present (`true`) or at least one needs to be present (`false`).
 *                 Default is `true`.
 * @return  `true` if the role has the specified permission for the channel, `false` otherwise.
 */
fun Role.hasChannelPermission(channel: GuildChannel, permissions: ArrayList<Permission>, needsAll: Boolean = true): Boolean = if (needsAll) {
    this.getPermissions(channel).containsAll(permissions)
} else {
    this.getPermissions(channel).any { it in permissions }
}
