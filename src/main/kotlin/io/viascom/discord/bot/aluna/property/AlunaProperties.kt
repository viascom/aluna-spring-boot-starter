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

package io.viascom.discord.bot.aluna.property

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(AlunaProperties.PREFIX)
class AlunaProperties {

    companion object {
        const val PREFIX = "aluna"
    }

    /**
     * Discord settings
     */
    @NestedConfigurationProperty
    var discord: AlunaDiscordProperties = AlunaDiscordProperties()

    /**
     * Notification settings
     */
    @NestedConfigurationProperty
    var notification: AlunaNotificationProperties = AlunaNotificationProperties()

    /**
     * BotLists settings
     */
    @NestedConfigurationProperty
    var botList: AlunaBotListProperties = AlunaBotListProperties()

    /**
     * BotLists settings
     */
    @NestedConfigurationProperty
    var thread: AlunaThreadProperties = AlunaThreadProperties()

    /**
     * Command settings
     */
    @NestedConfigurationProperty
    var command: AlunaCommandProperties = AlunaCommandProperties()

    /**
     * Is in production mode
     */
    var productionMode: Boolean = false

    /**
     * Owner ids. This is used by the DefaultOwnerIdProvider.
     */
    var ownerIds: ArrayList<Long> = arrayListOf()

    /**
     * Moderator ids. This is used by the DefaultModeratorIdProvider.
     */
    var modIds: ArrayList<Long> = arrayListOf()

    @NestedConfigurationProperty
    var debug: AlunaDebugProperties = AlunaDebugProperties()

    @NestedConfigurationProperty
    var translation: AlunaTranslationProperties = AlunaTranslationProperties()

    /**
     * Should Aluna register commands in production mode which are in commandDevelopmentStatus == IN_DEVELOPMENT
     */
    var includeInDevelopmentCommands: Boolean = false
}
