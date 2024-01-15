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

package io.viascom.discord.bot.aluna.configuration.scope

import org.springframework.context.annotation.Scope

/**
 * InteractionScoped is an Aluna specific scope which should be used for all DiscordCommand's as well as DiscordContextMenu's
 *
 * Based on DiscordContext, this scope will create or reuse existing instances. If no DiscordContext is provided for the current thread, This scope will always provide a new instance.
 * Instances are destroyed after a defined amount of time. For instances created with the type INTERACTION will aso trigger the onDestroy method.
 * The amount of time can be defined per interaction and is by default 14 min for interaction and 5 min for auto complete events.
 * After the onDestroy method trigger, all observers and event listeners for this instance are also removed if not disabled in the per interaction configuration.
 *
 * If DiscordContext provides a uniqueId, like it is the case for Button and Select observer, the same instance is returned.
 *
 * If DiscordContext.Type is set to AUTO_COMPLETE, the same instance for this DiscordContext.id (`user.id + ":" + server?.id`) is returned. This means during multiple onAutoCompleteEvent per interaction, the data is persistent.
 *
 * If DiscordContext.Type is set to INTERACTION, a new instance will be created if no auto complete instance exists or this feature is disabled.
 *
 * @author itsmefox
 * @since 0.0.10
 */
@Scope("interaction")
annotation class InteractionScoped
