/*
 * Copyright 2023 Viascom Ltd liab. Co
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
/**
 * This class is based on CoroutineEventListener.kt from https://github.com/MinnDevelopment/jda-ktx at commit a5d840c under the Apache License, Version 2.0
 */

package io.viascom.discord.bot.aluna.bot.event

import net.dv8tion.jda.api.events.GenericEvent

/**
 * Identical to [EventListener][net.dv8tion.jda.api.hooks.EventListener] but uses suspending function.
 */
fun interface CoroutineEventListener {
    /**
     * The timeout [kotlin.time.Duration] to use, or [EventTimeout.Inherit] to use event manager default.
     *
     * This timeout decides how long a listener function is allowed to run, not when to unregister it.
     */
    val timeout: EventTimeout get() = EventTimeout.Inherit

    suspend fun onEvent(event: GenericEvent)

    /**
     * Unregisters this listener
     */
    fun cancel() {}
}
