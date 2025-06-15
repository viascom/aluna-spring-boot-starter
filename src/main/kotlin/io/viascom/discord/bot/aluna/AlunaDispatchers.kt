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

package io.viascom.discord.bot.aluna

import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

public object AlunaDispatchers {

    @JvmSynthetic
    internal lateinit var interactionScope: CoroutineScope
        private set

    @JvmSynthetic
    internal lateinit var eventScope: CoroutineScope
        private set

    @JvmSynthetic
    internal lateinit var detachedScope: CoroutineScope
        private set

    private var scopesInitialized = false

    @OptIn(ExperimentalCoroutinesApi::class)
    @JvmSynthetic
    internal fun initScopes(
        interactionParallelism: Int = -1,
        eventParallelism: Int = -1,
        detachedParallelism: Int = -1,
        cancelParent: Boolean = false
    ) {
        val interactionScopeDispatcher = if (interactionParallelism == -1) Dispatchers.Default else Dispatchers.Default.limitedParallelism(interactionParallelism)
        interactionScope = getScope("Aluna-Interaction", interactionScopeDispatcher, cancelParent = cancelParent)
        val eventScopeDispatcher = if (eventParallelism == -1) Dispatchers.Default else Dispatchers.Default.limitedParallelism(eventParallelism)
        eventScope = getScope("Aluna-Event", eventScopeDispatcher, cancelParent = cancelParent)
        val detachedScopeDispatcher = if (detachedParallelism == -1) Dispatchers.VT else Dispatchers.VT.limitedParallelism(detachedParallelism)
        detachedScope = getScope("Aluna-Detached", detachedScopeDispatcher, cancelParent = cancelParent)
        scopesInitialized = true
    }

    private fun ensureScopesInitialized() {
        if (!scopesInitialized) {
            throw IllegalStateException("Scopes are not initialized. Please call initScopes() before accessing them.")
        }
    }

    @get:JvmSynthetic
    internal val Internal: CoroutineContext
        get() = InternalScope.coroutineContext

    @get:JvmSynthetic
    internal val InternalScope: CoroutineScope
        get() = getScope("Aluna-Internal", Dispatchers.Default, cancelParent = true)

    public val Interaction: CoroutineContext
        get() {
            ensureScopesInitialized()
            return InteractionScope.coroutineContext
        }

    public val InteractionScope: CoroutineScope
        get() {
            ensureScopesInitialized()
            return interactionScope
        }

    public val Event: CoroutineContext
        get() {
            ensureScopesInitialized()
            return EventScope.coroutineContext
        }

    public val EventScope: CoroutineScope
        get() {
            ensureScopesInitialized()
            return eventScope
        }

    public val Detached: CoroutineContext
        get() {
            ensureScopesInitialized()
            return DetachedScope.coroutineContext
        }

    public val DetachedScope: CoroutineScope
        get() {
            ensureScopesInitialized()
            return detachedScope
        }

    @Suppress("Since15")
    private val Dispatchers.VT: CoroutineDispatcher
        get() = if (isJava21OrHigher()) {
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("AlunaVirtualDispatcher-worker-", 0L).factory()).asCoroutineDispatcher()
        } else {
            IO
        }

    private fun isJava21OrHigher(): Boolean {
        return try {
            (System.getProperty("java.version").split(".").firstOrNull()?.toInt() ?: 0) >= 21
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun getScope(
        name: String = "Aluna",
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        parent: Job = SupervisorJob(),
        errorHandler: CoroutineExceptionHandler? = null,
        cancelParent: Boolean = false
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
