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

package io.viascom.discord.bot.aluna.property

import org.springframework.boot.context.properties.NestedConfigurationProperty

class AlunaCommandProperties {

    @NestedConfigurationProperty
    var systemCommand: SystemCommandProperties = SystemCommandProperties()

    @NestedConfigurationProperty
    var helpCommand: HelpCommandProperties = HelpCommandProperties()
}

class SystemCommandProperties {
    /**
     * Enable /system-command
     */
    var enabled: Boolean = false

    /**
     * Server id on which this command can be used.
     * If set to 0 the command will be removed completely.
     * If set to null, the command can be used on every server and in DMs.
     */
    var server: String? = null

    /**
     * Define which system command features should be enabled. If not defined, all implementations of SystemCommandDataProvider are available.
     * Functions: admin_search, extract_message, evaluate_kotlin, leave_server, purge_messages, send_message
     */
    var enabledFunctions: ArrayList<String>? = null

    /**
     * Define which system command features are allowed for moderators. If not defined, Aluna will use what is defined in the feature or the default which is false
     */
    var allowedForModeratorsFunctions: ArrayList<String>? = null

    /**
     * Enable kotlin script evaluation feature. If this is enabled, you need to run your application with a JDK.
     */
    var enableKotlinScriptEvaluate: Boolean = false

    /**
     * Execute load additional data on system-command interaction
     */
    var executeLoadAdditionalData: Boolean = false

    /**
     * Check additional conditions on system-command interaction
     */
    var checkAdditionalConditions: Boolean = false

    @NestedConfigurationProperty
    var releaseNotes: ReleaseNoteProperties = ReleaseNoteProperties()
}

class ReleaseNoteProperties {
    /**
     * Channel where the bot should post release notes by default.
     */
    var channel: String? = null

    var newCommandEmote: String = "⌨️"
    var newFeatureEmote: String = "\uD83E\uDDE9"
    var bugFixEmote: String = "\uD83D\uDC1B"
    var internalChangeEmote: String = "⚙️"
}

class HelpCommandProperties {
    /**
     * Enable /help
     */
    var enabled: Boolean = false

    /**
     * Title used in the command
     */
    var title: String = "Help"

    /**
     * Title used in the command
     */
    var description: String = ""

    /**
     * Color of the embed
     */
    var embedColor: String = "#03a66a"

    @NestedConfigurationProperty
    var fields: ArrayList<HelpField> = arrayListOf()

    @NestedConfigurationProperty
    var inviteButton: InviteButton = InviteButton()

    @NestedConfigurationProperty
    var websiteButton: HelpButton = HelpButton().apply {
        label = "Visit our Website"
        emote = "🌐"
    }

    @NestedConfigurationProperty
    var joinSupportServerButton: HelpButton = HelpButton().apply {
        label = "Join our Discord"
        emote = "👋"
    }

    @NestedConfigurationProperty
    var supportButton: HelpButton = HelpButton().apply {
        label = "Support Us"
        emote = "❤️"
    }

    /**
     * Execute load additional data on help interaction
     */
    var executeLoadAdditionalData: Boolean = false

    /**
     * Check additional conditions on help interaction
     */
    var checkAdditionalConditions: Boolean = false
}


class InviteButton {
    var enabled: Boolean = false
    var label: String = "Invite Me"
    var emote: String? = "📩"

    /**
     * If this is set, the bot will use this link instead of the default one based on your configuration.
     */
    var link: String? = null
}

class HelpButton {
    var enabled: Boolean = false
    var label: String = "Help"
    var emote: String? = null
    var link: String = ""
}

class HelpField {
    var name: String = ""
    var value: String = ""
    var inline: Boolean = false
}
