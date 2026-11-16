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

package io.viascom.discord.bot.aluna.bot.shardmanager

import io.viascom.discord.bot.aluna.model.RecommendedShards

/**
 * Resolves the recommended shard count and session start limit used when building the shard manager.
 *
 * Provide a bean of this type to override how the recommended shards are resolved (for example to
 * query a gateway proxy instead of Discord). A default implementation querying the configured
 * gateway URL is registered when no custom bean is present.
 */
public fun interface RecommendedShardsResolver {

    /**
     * Resolve the recommended shards.
     *
     * Called inside the shard manager build on an internal dispatcher, so a blocking call is fine.
     *
     * @return the resolved shards, or `null` if resolution failed. When
     *   `aluna.discord.sharding.type = SINGLE` and `aluna.discord.sharding.total-shards = -1`, a
     *   `null` result aborts startup (unchanged behavior).
     */
    public fun resolve(): RecommendedShards?
}
