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

package io.viascom.discord.bot.aluna.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

public class GatewayResponse(
    public val shards: Int,
    @JsonProperty("session_start_limit")
    public val sessionStartLimit: SessionStartLimit
) {
    public class SessionStartLimit(
        public val total: Int,
        public var remaining: Int,

        @set:JsonProperty("reset_after")
        @get:JsonIgnore
        public var resetAfter: Int,

        @JsonProperty("max_concurrency")
        public val maxConcurrency: Int
    ) {

        public var resetTimestamp: LocalDateTime? = null

    }
}
