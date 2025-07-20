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

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(AlunaProperties.PREFIX)
public class AlunaProperties {

    public companion object {
        public const val PREFIX: String = "aluna"
    }

    /**
     * Discord settings
     */
    @NestedConfigurationProperty
    public var discord: AlunaDiscordProperties = AlunaDiscordProperties()

    /**
     * Notification settings
     */
    @NestedConfigurationProperty
    public var notification: AlunaNotificationProperties = AlunaNotificationProperties()

    /**
     * BotStats settings
     */
    @NestedConfigurationProperty
    public var botStats: AlunaBotStatsProperties = AlunaBotStatsProperties()

    /**
     * BotLists settings
     */
    @NestedConfigurationProperty
    public var thread: AlunaThreadProperties = AlunaThreadProperties()

    /**
     * Command settings
     */
    @NestedConfigurationProperty
    public var command: AlunaCommandProperties = AlunaCommandProperties()

    /**
     * Emoji settings
     */
    @NestedConfigurationProperty
    public var emoji: AlunaEmojiProperties = AlunaEmojiProperties()

    /**
     * Is in production mode
     */
    public var productionMode: Boolean = false

    /**
     * Owner ids. This is used by the DefaultOwnerIdProvider.
     */
    public var ownerIds: ArrayList<Long> = arrayListOf()

    /**
     * Moderator ids. This is used by the DefaultModeratorIdProvider.
     */
    public var modIds: ArrayList<Long> = arrayListOf()

    @NestedConfigurationProperty
    public var debug: AlunaDebugProperties = AlunaDebugProperties()

    @NestedConfigurationProperty
    public var translation: AlunaTranslationProperties = AlunaTranslationProperties()

    /**
     * Should Aluna register interactions in production mode which are in interactionDevelopmentStatus == IN_DEVELOPMENT
     */
    public var includeInDevelopmentInteractions: Boolean = false

    /**
     * Should Aluna enable its actuator health indicator
     *
     * Url: /actuator/health/aluna
     */
    public var enableActuatorHealthIndicator: Boolean = true

    public var nodeNumber: Int = 0
}
