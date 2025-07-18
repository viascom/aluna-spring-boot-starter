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

@file:JvmName("AlunaOptionData")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.model

import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

public interface CommandOption<T>

public class OptionRange<T : Any>(public val min: T, public val max: T)

public class StringOption @JvmOverloads constructor(
    name: String,
    description: String,
    isRequired: Boolean = false,
    isAutoComplete: Boolean = false,
    choices: Map<String, String>? = null
) :
    OptionData(OptionType.STRING, name, description, isRequired, isAutoComplete), CommandOption<String?> {
    init {
        choices?.let { this.addChoices(it.entries.map { entry -> Choice(entry.key, entry.value) }) }
    }
}

public class IntegerOption @JvmOverloads constructor(
    name: String,
    description: String,
    isRequired: Boolean = false,
    isAutoComplete: Boolean = false,
    choices: Map<String, Int>? = null,
    requiredRange: OptionRange<Int>? = null
) :
    OptionData(OptionType.INTEGER, name, description, isRequired, isAutoComplete), CommandOption<Int?> {
    init {
        choices?.let { this.addChoices(it.entries.map { entry -> Choice(entry.key, entry.value.toLong()) }) }
        requiredRange?.let { this.setRequiredRange(requiredRange.min.toLong(), requiredRange.max.toLong()) }
    }
}

public class LongOption @JvmOverloads constructor(
    name: String,
    description: String,
    isRequired: Boolean = false,
    isAutoComplete: Boolean = false,
    choices: Map<String, Long>? = null,
    requiredRange: OptionRange<Long>? = null
) :
    OptionData(OptionType.INTEGER, name, description, isRequired, isAutoComplete), CommandOption<Long?> {
    init {
        choices?.let { this.addChoices(it.entries.map { entry -> Choice(entry.key, entry.value) }) }
        requiredRange?.let { this.setRequiredRange(requiredRange.min, requiredRange.max) }
    }
}

public class DoubleOption @JvmOverloads constructor(
    name: String,
    description: String,
    isRequired: Boolean = false,
    isAutoComplete: Boolean = false,
    choices: Map<String, Double>? = null,
    requiredRange: OptionRange<Double>? = null
) :
    OptionData(OptionType.NUMBER, name, description, isRequired, isAutoComplete), CommandOption<Double?> {
    init {
        choices?.let { this.addChoices(it.entries.map { entry -> Choice(entry.key, entry.value) }) }
        requiredRange?.let { this.setRequiredRange(requiredRange.min, requiredRange.max) }
    }
}
public typealias NumberOption = DoubleOption

public class BooleanOption @JvmOverloads constructor(name: String, description: String, isRequired: Boolean = false) :
    OptionData(OptionType.BOOLEAN, name, description, isRequired), CommandOption<Boolean?>

public class UserOption @JvmOverloads constructor(name: String, description: String, isRequired: Boolean = false) :
    OptionData(OptionType.USER, name, description, isRequired), CommandOption<User?>

public class RoleOption @JvmOverloads constructor(name: String, description: String, isRequired: Boolean = false) :
    OptionData(OptionType.ROLE, name, description, isRequired), CommandOption<Role?>

public class ChannelOption @JvmOverloads constructor(name: String, description: String, isRequired: Boolean = false, channelTypes: List<ChannelType>? = null) :
    OptionData(OptionType.CHANNEL, name, description, isRequired), CommandOption<Channel?> {
    init {
        if (channelTypes != null) {
            this.setChannelTypes(channelTypes)
        }
    }
}

public class MentionableOption @JvmOverloads constructor(name: String, description: String, isRequired: Boolean = false) :
    OptionData(OptionType.MENTIONABLE, name, description, isRequired), CommandOption<IMentionable?>

public class AttachmentOption @JvmOverloads constructor(name: String, description: String, isRequired: Boolean = false) :
    OptionData(OptionType.ATTACHMENT, name, description, isRequired), CommandOption<Attachment?>

public typealias FileOption = AttachmentOption
