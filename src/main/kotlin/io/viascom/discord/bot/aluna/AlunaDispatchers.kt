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

package io.viascom.discord.bot.aluna

import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

object AlunaDispatchers {

    @JvmSynthetic
    internal lateinit var interactionScope: CoroutineScope

    @JvmSynthetic
    internal lateinit var eventScope: CoroutineScope

    @JvmSynthetic
    internal lateinit var detachedScope: CoroutineScope

    @OptIn(ExperimentalCoroutinesApi::class)
    @JvmSynthetic
    internal fun initScopes(interactionParallelism: Int = -1, eventParallelism: Int = -1, detachedParallelism: Int = -1) {
        val interactionScopeDispatcher = if (interactionParallelism == -1) Dispatchers.Default else Dispatchers.Default.limitedParallelism(interactionParallelism)
        interactionScope = getScope("Aluna-Interaction", interactionScopeDispatcher, cancelParent = true)
        val eventScopeDispatcher = if (eventParallelism == -1) Dispatchers.Default else Dispatchers.Default.limitedParallelism(eventParallelism)
        eventScope = getScope("Aluna-Event", eventScopeDispatcher, cancelParent = true)
        val detachedScopeDispatcher = if (detachedParallelism == -1) Dispatchers.VT else Dispatchers.VT.limitedParallelism(detachedParallelism)
        detachedScope = getScope("Aluna-Detached", detachedScopeDispatcher, cancelParent = false)
    }

    @get:JvmSynthetic
    internal val Internal: CoroutineContext
        get() = InternalScope.coroutineContext

    @get:JvmSynthetic
    internal val InternalScope: CoroutineScope
        get() = getScope("Aluna-Internal", Dispatchers.Default, cancelParent = true)

    val Interaction: CoroutineContext
        get() = InteractionScope.coroutineContext

    val InteractionScope: CoroutineScope
        get() = interactionScope

    val Event: CoroutineContext
        get() = EventScope.coroutineContext

    val EventScope: CoroutineScope
        get() = eventScope

    val Detached: CoroutineContext
        get() = DetachedScope.coroutineContext

    val DetachedScope: CoroutineScope
        get() = detachedScope

    val Dispatchers.VT: CoroutineDispatcher
        get() = if (isJava21()) {
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("AlunaVirtualDispatcher-worker-", 0L).factory()).asCoroutineDispatcher()
        } else {
            IO
        }

    private fun isJava21(): Boolean {
        return System.getProperty("java.version").startsWith("21")
    }

    private fun getScope(
        name: String = "Aluna",
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        parent: Job = SupervisorJob(),
        errorHandler: CoroutineExceptionHandler? = null,
        cancelParent: Boolean = false //TODO we should allow the developer to decide if the parent should be cancelled or not. As maybe we don't want to cancel the parent if the exception happens in a logger
    ): CoroutineScope {
        val exceptionHandler = errorHandler ?: CoroutineExceptionHandler { _, throwable ->
            LoggerFactory.getLogger(AlunaDispatchers::class.java).also {
                it.error("Uncaught exception from coroutine: ${throwable.message}", throwable)
            }
            if (throwable is Error) {
                if (cancelParent) {
                    parent.cancel()
                }
                throw throwable
            }
        }

        return CoroutineScope(dispatcher + parent + exceptionHandler + CoroutineName(name) + MDCContext())
    }

}
