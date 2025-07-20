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

package io.viascom.discord.bot.aluna.bot.emoji

import io.viascom.discord.bot.aluna.model.ApplicationEmojiData

/**
 * Cache for application emojis
 *
 * This object stores application emojis and their loading status.
 * It is used by the ApplicationEmoji interface to retrieve emoji information.
 */
public object StaticEmojiCache {
    /**
     * Map of emoji name to emoji data
     */
    public val emojis: MutableMap<String, ApplicationEmojiData> = hashMapOf()

    /**
     * Whether emojis have been loaded
     */
    public var emojisLoaded: Boolean = false
}
