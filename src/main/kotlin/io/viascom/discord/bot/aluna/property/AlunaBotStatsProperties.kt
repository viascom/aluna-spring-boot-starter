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

import java.time.Duration

public class AlunaBotStatsProperties {

    /**
     * Send stats over BotBlock Api: https://botblock.org/
     */
    public var botBlock: BotBlock = BotBlock()

    /**
     * bots.ondiscord.xyz
     */
    public var botsOnDiscord: Configuration? = null

    /**
     * discord.bots.gg
     */
    public var discordBots: Configuration? = null

    /**
     * discordbotlist.com
     */
    public var discordBotList: Configuration? = null

    /**
     * Top.gg
     */
    public var topgg: Configuration? = null

    public class Configuration {
        public var enabled: Boolean = false
        public var token: String? = null
    }

    public class BotBlock {
        public var enabled: Boolean = false
        public var updateDelay: Duration = Duration.ofMinutes(30)

        /**
         * site with their tokens to send data to.
         * Full list of sites: https://botblock.org/lists
         */
        public var tokens: HashMap<String, String> = hashMapOf()
    }
}
