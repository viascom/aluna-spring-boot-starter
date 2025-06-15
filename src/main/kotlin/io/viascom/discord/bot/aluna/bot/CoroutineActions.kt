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

@file:JvmName("CoroutineActions") @file:JvmMultifileClass

package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.AlunaDispatchers
import kotlinx.coroutines.*
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

public suspend fun <T> RestAction<T>.coQueue(success: suspend CoroutineScope.(T) -> Unit): Unit = this@coQueue.queue({
    AlunaDispatchers.InternalScope.launch {
        success(it)
    }
}, null)

public suspend fun ReplyCallbackAction.coQueue(success: suspend CoroutineScope.(InteractionHook) -> Unit): Unit = this@coQueue.queue({
    AlunaDispatchers.InternalScope.launch {
        success(it)
    }
}, null)

public suspend fun <T> RestAction<T>.coQueue(success: (suspend CoroutineScope.(T) -> Unit)?, failure: (suspend CoroutineScope.(Throwable) -> Unit)?): Unit = this@coQueue.queue({
    if (success != null) {
        AlunaDispatchers.InternalScope.launch { success(it) }
    }
}, {
    if (failure != null) {
        AlunaDispatchers.InternalScope.launch { failure(it) }
    }
})

public suspend fun ReplyCallbackAction.coQueue(success: (suspend CoroutineScope.(InteractionHook) -> Unit)?, failure: (suspend CoroutineScope.(Throwable) -> Unit)?): Unit =
    this@coQueue.queue({
        if (success != null) {
            AlunaDispatchers.InternalScope.launch { success(it) }
        }
    }, {
        if (failure != null) {
            AlunaDispatchers.InternalScope.launch { failure(it) }
        }
    })

public suspend fun <T> RestAction<T>.coComplete(shouldQueue: Boolean = true): Deferred<T> = withContext(AlunaDispatchers.Internal) {
    return@withContext async {
        this@coComplete.complete(shouldQueue)
    }
}

public suspend fun ReplyCallbackAction.coComplete(shouldQueue: Boolean = true): Deferred<InteractionHook> = withContext(AlunaDispatchers.Internal) {
    return@withContext async {
        this@coComplete.complete(shouldQueue)
    }
}

public suspend fun <T> RestAction<T>.coOnSuccess(success: suspend CoroutineScope.(T) -> Unit): RestAction<T> = this@coOnSuccess.onSuccess {
    AlunaDispatchers.InternalScope.launch {
        success(it)
    }
}

public suspend fun ReplyCallbackAction.coOnSuccess(success: suspend CoroutineScope.(InteractionHook) -> Unit): RestAction<InteractionHook> = this@coOnSuccess.onSuccess {
    AlunaDispatchers.InternalScope.launch {
        success(it)
    }
}

public suspend fun <T> RestAction<T>.coQueueAfter(
    delay: Long, unit: TimeUnit, executor: ScheduledExecutorService? = null, success: (suspend CoroutineScope.(T) -> Unit)? = null
): ScheduledFuture<*> = this@coQueueAfter.queueAfter(delay, unit, {
    if (success != null) {
        AlunaDispatchers.InternalScope.launch { success(it) }
    }
}, null, executor)

public suspend fun <T> RestAction<T>.coQueueAfter(
    delay: Long,
    unit: TimeUnit,
    executor: ScheduledExecutorService? = null,
    success: (suspend CoroutineScope.(T) -> Unit)? = null,
    failure: (suspend CoroutineScope.(Throwable) -> Unit)? = null
): ScheduledFuture<*> = this@coQueueAfter.queueAfter(delay, unit, {
    if (success != null) {
        AlunaDispatchers.InternalScope.launch { success(it) }
    }
}, {
    if (failure != null) {
        AlunaDispatchers.InternalScope.launch { failure(it) }
    }
}, executor)

public suspend fun ReplyCallbackAction.coQueueAfter(
    delay: Long, unit: TimeUnit, executor: ScheduledExecutorService? = null, success: (suspend CoroutineScope.(InteractionHook) -> Unit)? = null
): ScheduledFuture<*> = this@coQueueAfter.queueAfter(delay, unit, {
    if (success != null) {
        AlunaDispatchers.InternalScope.launch { success(it) }
    }
}, null, executor)

public suspend fun ReplyCallbackAction.coQueueAfter(
    delay: Long,
    unit: TimeUnit,
    executor: ScheduledExecutorService? = null,
    success: (suspend CoroutineScope.(InteractionHook) -> Unit)? = null,
    failure: (suspend CoroutineScope.(Throwable) -> Unit)? = null
): ScheduledFuture<*> = this@coQueueAfter.queueAfter(delay, unit, {
    if (success != null) {
        AlunaDispatchers.InternalScope.launch { success(it) }
    }
}, {
    if (failure != null) {
        AlunaDispatchers.InternalScope.launch { failure(it) }
    }
}, executor)
