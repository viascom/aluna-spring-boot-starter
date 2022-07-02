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

import org.springframework.boot.convert.DurationUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

class AlunaTranslationProperties {

    /**
     * Enable Translation
     */
    var enabled: Boolean = false

    /**
     * Translation path
     *
     * Format: `file:/` or `classpath:`
     *
     * If not set, Aluna will fall back to `classpath:i18n/messages`
     */
    var basePath: String? = null

    /**
     * Use en_GB for en in production
     */
    var useEnGbForEnInProduction: Boolean = false

    /**
     * Set the default charset to use for parsing properties files.
     * Used if no file-specific charset is specified for a file.
     */
    var defaultEncoding: String = "UTF-8"

    /**
     * Duration to cache loaded properties files.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    var cacheDuration: Duration = Duration.ofSeconds(60)

    /**
     * Set whether to use the message code as default message instead of throwing a NoSuchMessageException.
     * Useful for development and debugging.
     */
    var useCodeAsDefaultMessage: Boolean = true

    /**
     * Set whether to fall back to the system Locale if no files for a specific Locale have been found.
     * If this is turned off, the only fallback will be the default file (e.g. "messages.properties" for basename "messages").
     */
    var fallbackToSystemLocale: Boolean = false
}
