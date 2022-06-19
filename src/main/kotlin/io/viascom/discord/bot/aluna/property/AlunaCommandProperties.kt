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

import org.springframework.boot.context.properties.NestedConfigurationProperty

class AlunaCommandProperties {

    @NestedConfigurationProperty
    var systemCommand: SystemCommandProperties = SystemCommandProperties()
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
     * Defines the support server which will be used for certain information..
     */
    var supportServer: String? = null

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
}
