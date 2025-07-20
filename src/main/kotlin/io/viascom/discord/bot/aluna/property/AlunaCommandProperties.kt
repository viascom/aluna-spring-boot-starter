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

package io.viascom.discord.bot.aluna.property

import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import org.springframework.boot.context.properties.NestedConfigurationProperty

public class AlunaCommandProperties {

    @NestedConfigurationProperty
    public var serverSpecific: ServerSpecificInteractionProperties = ServerSpecificInteractionProperties()

    /**
     * Print all defined arguments of a used command in the log
     */
    public var printArgs: Boolean = false

    @NestedConfigurationProperty
    public var systemCommand: SystemCommandProperties = SystemCommandProperties()

    @NestedConfigurationProperty
    public var helpCommand: HelpCommandProperties = HelpCommandProperties()
}

public class SystemCommandProperties {
    /**
     * Enable /system-command
     */
    public var enabled: Boolean = false

    /**
     * Represents where commands can be used.
     */
    public var contexts: List<InteractionContextType> = listOf(InteractionContextType.GUILD, InteractionContextType.BOT_DM)

    /**
     * Represents how an app was installed, or where a command can be used.
     */
    public var integrationTypes: List<IntegrationType> = listOf(IntegrationType.GUILD_INSTALL)

    /**
     * Server id on which this command can be used.
     * If set to 0 the command will be removed completely.
     * If set to null, the command can be used on every server and in DMs.
     */
    public var servers: ArrayList<String>? = null

    /**
     * Define which system command features should be enabled. If not defined, all implementations of SystemCommandDataProvider are available.
     * Functions: admin_search, extract_message, evaluate_kotlin, leave_server, purge_messages, send_message
     */
    public var enabledFunctions: ArrayList<String>? = null

    /**
     * Define which system command features are allowed for moderators. If not defined, Aluna will use what is defined in the feature or the default which is false
     */
    public var allowedForModeratorsFunctions: ArrayList<String>? = null

    /**
     * Enable kotlin script evaluation feature. If this is enabled, you need to run your application with a JDK.
     */
    public var enableKotlinScriptEvaluate: Boolean = false

    /**
     * Execute load additional data on system-command interaction
     */
    public var executeLoadAdditionalData: Boolean = false

    /**
     * Check additional conditions on system-command interaction
     */
    public var checkAdditionalConditions: Boolean = false

    @NestedConfigurationProperty
    public var releaseNotes: ReleaseNoteProperties = ReleaseNoteProperties()
}

public class ReleaseNoteProperties {
    /**
     * Channel where the bot should post release notes by default.
     */
    public var channel: String? = null

    public var newCommandEmote: String = "⌨️"
    public var newFeatureEmote: String = "\uD83E\uDDE9"
    public var bugFixEmote: String = "\uD83D\uDC1B"
    public var internalChangeEmote: String = "⚙️"
}

public class HelpCommandProperties {
    /**
     * Enable /help
     */
    public var enabled: Boolean = false

    /**
     * Represents where commands can be used.
     */
    public var contexts: List<InteractionContextType> = InteractionContextType.ALL.toList()

    /**
     * Represents how an app was installed, or where a command can be used.
     */
    public var integrationTypes: List<IntegrationType> = listOf(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)

    /**
     * Title used in the command
     */
    public var title: String = "Help"

    /**
     * Title used in the command
     */
    public var description: String = ""

    /**
     * Color of the embed
     */
    public var embedColor: String = "#03a66a"

    @NestedConfigurationProperty
    public var fields: ArrayList<HelpField> = arrayListOf()

    @NestedConfigurationProperty
    public var inviteButton: InviteButton = InviteButton()

    @NestedConfigurationProperty
    public var websiteButton: HelpButton = HelpButton().apply {
        label = "Visit our Website"
        emote = "🌐"
    }

    @NestedConfigurationProperty
    public var joinSupportServerButton: HelpButton = HelpButton().apply {
        label = "Join our Discord"
        emote = "👋"
    }

    @NestedConfigurationProperty
    public var supportButton: HelpButton = HelpButton().apply {
        label = "Support Us"
        emote = "❤️"
    }

    /**
     * Execute load additional data on help interaction
     */
    public var executeLoadAdditionalData: Boolean = false

    /**
     * Check additional conditions on help interaction
     */
    public var checkAdditionalConditions: Boolean = false
}

public class ServerSpecificInteractionProperties {
    /**
     * Enable logic to manage server-specific interactions
     */
    public var enabled: Boolean = false

    /**
     * Remove all server-specific interactions that are not available on the server anymore.
     * Using this on bigger bots can lead to longer startup times as Aluna has to check all the servers if their commands are available.
     */
    public var removeOutdatedInteractionsOnStartup: Boolean = false
}

public class InviteButton {
    public var enabled: Boolean = false
    public var label: String = "Invite Me"
    public var emote: String? = "📩"

    /**
     * If this is set, the bot will use this link instead of the default one based on your configuration.
     */
    public var link: String? = null
}

public class HelpButton {
    public var enabled: Boolean = false
    public var label: String = "Help"
    public var emote: String? = null
    public var link: String = ""
}

public class HelpField {
    public var name: String = ""
    public var value: String = ""
    public var inline: Boolean = false
}
