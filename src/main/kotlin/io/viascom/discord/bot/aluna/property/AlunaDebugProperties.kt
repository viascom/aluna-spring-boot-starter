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

public class AlunaDebugProperties {

    /**
     * Show time elapsed for commands
     */
    public var useTimeMarks: Boolean = true

    /**
     * Show detailed time elapsed for commands
     */
    public var showDetailTimeMarks: ShowDetailTimeMarks = ShowDetailTimeMarks.ON_EXCEPTION

    public enum class ShowDetailTimeMarks {
        NONE, ALWAYS, ON_EXCEPTION, MDC_ONLY
    }

    /**
     * Show hash code for commands
     */
    public var showHashCode: Boolean = false

    /**
     * Enable Debug Configuration Log.
     * If enabled and not production mode, Aluna will print a configuration block in the log which contains some selected settings and an invitation link for the bot itself.
     */
    public var enableDebugConfigurationLog: Boolean = true

    /**
     * Hide the bot token in debug configuration log. Only set this to true if you want Aluna to print the token for debug purposes.
     * No matter what you select here, the Debug Configuration Log and its content is only printed in not production mode.
     */
    public var hideTokenInDebugConfigurationLog: Boolean = true

    /**
     * Defines if Aluna should list translations in the log on startup
     */
    public var showTranslationKeys: ShowTranslationKeys = ShowTranslationKeys.NONE

    public enum class ShowTranslationKeys {
        NONE, ALL, ONLY_MISSING
    }
}
