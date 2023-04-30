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

package io.viascom.discord.bot.aluna

import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object AlunaDispatchers {

    @get:JvmSynthetic
    internal val Internal: CoroutineContext
        get() = InternalScope.coroutineContext

    @get:JvmSynthetic
    internal val InternalScope: CoroutineScope
        get() = getScope("Aluna-Internal", Dispatchers.Default, cancelParent = true)

    val Interaction: CoroutineContext
        get() = InteractionScope.coroutineContext

    @OptIn(ExperimentalCoroutinesApi::class)
    val InteractionScope: CoroutineScope
        get() = getScope("Aluna-Interaction", Dispatchers.IO.limitedParallelism(50), cancelParent = true)

    val Detached: CoroutineContext
        get() = DetachedScope.coroutineContext

    @OptIn(ExperimentalCoroutinesApi::class)
    val DetachedScope: CoroutineScope
        get() = getScope("Aluna-Detached", Dispatchers.IO.limitedParallelism(50), cancelParent = false)

    private fun getScope(
        name: String = "Aluna",
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        parent: Job = SupervisorJob(),
        errorHandler: CoroutineExceptionHandler? = null,
        context: CoroutineContext = EmptyCoroutineContext,
        cancelParent: Boolean = false //TODO we should allow the developer to decide if the parent should be cancelled or not. As maybe we don't want to cancel the parent if the exception happens in a logger
    ): CoroutineScope {
        val exceptionHandler = errorHandler ?: CoroutineExceptionHandler { _, throwable ->
            LoggerFactory.getLogger(AlunaDispatchers::class.java).also {
                it.error("Uncaught exception from coroutine", throwable)
            }
            if (throwable is Error) {
                if (cancelParent) {
                    parent.cancel()
                }
                throw throwable
            }
        }

        return CoroutineScope(dispatcher + parent + exceptionHandler + context + CoroutineName(name) + MDCContext())
    }

//    private fun getDispatcher(nThreads: Int = 10, name: String = "AlunaDispatcher"): CoroutineDispatcher {
//        val dynamicThreadPool = AlunaThreadPool.getDynamicThreadPool(nThreads, Duration.ofSeconds(10), name)
//        val threadNo = AtomicInteger()
//        dynamicThreadPool.setThreadFactory { runnable ->
//            Thread(runnable, if (nThreads == 1) name else name + "-" + threadNo.incrementAndGet()).also { it.isDaemon = true }
//        }
//        return dynamicThreadPool.asCoroutineDispatcher()
//    }

}