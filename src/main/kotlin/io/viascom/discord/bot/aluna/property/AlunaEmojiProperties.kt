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

/**
 * Properties for emoji management
 */
public class AlunaEmojiProperties {

    /**
     * Whether to enable emoji management
     */
    public var enabled: Boolean = false

    /**
     * Whether to update emojis on change
     */
    public var updateOnChange: Boolean = false

    /**
     * Whether to automatically upload missing emojis to Discord
     */
    public var uploadMissingEmojis: Boolean = false

    /**
     * Whether to automatically delete emojis from Discord that are not in the local database
     */
    public var deleteMissingEmojis: Boolean = false

    /**
     * Fallback string to use if an emoji is not found
     */
    public var fallbackEmoji: String = ""
}
