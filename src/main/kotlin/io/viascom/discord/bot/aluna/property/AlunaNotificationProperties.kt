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

package io.viascom.discord.bot.aluna.property

import org.springframework.boot.context.properties.NestedConfigurationProperty

class AlunaNotificationProperties {

    /**
     * Configuration for server join event notification
     */
    @NestedConfigurationProperty
    var serverJoin: Notification = Notification()

    /**
     * Configuration for server leave event notification
     */
    @NestedConfigurationProperty
    var serverLeave: Notification = Notification()

    /**
     * Configuration for bot ready event notification
     */
    @NestedConfigurationProperty
    var botReady: Notification = Notification()

}

class Notification {
    /**
     * Enable this notification
     */
    var enabled: Boolean = false

    /**
     * Server where it gets posted
     */
    var server: Long? = null

    /**
     * Channel where it gets posted
     */
    var channel: Long? = null
}
