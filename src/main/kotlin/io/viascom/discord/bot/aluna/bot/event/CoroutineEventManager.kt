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
/**
 * This class is based on CoroutineEventManager.kt from https://github.com/MinnDevelopment/jda-ktx at commit 3ffa0c7 under the Apache License, Version 2.0
 */

package io.viascom.discord.bot.aluna.bot.event

import io.viascom.discord.bot.aluna.AlunaDispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.scheduling.*
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.IEventManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ThreadPoolExecutor
import kotlin.time.Duration

/**
 * EventManager implementation which supports both [EventListener] and [CoroutineEventListener].
 *
 * This enables [the coroutine listener extension][listener].
 */
open class CoroutineEventManager(
    val eventThreadPool: ThreadPoolExecutor,
    scope: CoroutineScope = AlunaDispatchers.InternalScope,
    /** Timeout [Duration] each event listener is allowed to run. Set to [Duration.INFINITE] for no timeout. Default: [Duration.INFINITE] */
    var timeout: Duration = Duration.INFINITE
) : IEventManager, CoroutineScope by scope {
    private val listeners = CopyOnWriteArrayList<Any>()

    val logger: Logger = LoggerFactory.getLogger(CoroutineEventManager::class.java)

    protected fun timeout(listener: Any) = when {
        listener is CoroutineEventListener && listener.timeout != EventTimeout.Inherit -> listener.timeout.time
        else -> timeout
    }

    override fun handle(event: GenericEvent) {
        launch(AlunaDispatchers.Internal) {
            for (listener in listeners) try {
                val actualTimeout = timeout(listener)
                if (actualTimeout.isPositive() && actualTimeout.isFinite()) {
                    // Timeout only works when the continuations implement a cancellation handler
                    val result = withTimeoutOrNull(actualTimeout.inWholeMilliseconds) {
                        runListener(listener, event)
                    }
                    if (result == null) {
                        logger.debug("Event of type ${event.javaClass.simpleName} timed out.")
                    }
                } else {
                    runListener(listener, event)
                }
            } catch (ex: Exception) {
                logger.error("Uncaught exception in event listener", ex)
            }
        }
    }

    protected open suspend fun runListener(listener: Any, event: GenericEvent) = when (listener) {
        is CoroutineEventListener -> listener.onEvent(event)
        is EventListener -> eventThreadPool.execute { listener.onEvent(event) }
        else -> Unit
    }

    override fun register(listener: Any) {
        listeners.add(
            when (listener) {
                is EventListener, is CoroutineEventListener -> listener
                else -> throw IllegalArgumentException("Listener must implement either EventListener or CoroutineEventListener")
            }
        )
    }

    override fun getRegisteredListeners(): MutableList<Any> = mutableListOf(listeners)

    override fun unregister(listener: Any) {
        listeners.remove(listener)
    }
}