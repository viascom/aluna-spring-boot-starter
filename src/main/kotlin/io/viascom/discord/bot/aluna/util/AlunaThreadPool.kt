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

package io.viascom.discord.bot.aluna.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Duration
import java.util.concurrent.*

object AlunaThreadPool {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * Creates a new AlunaThreadPoolExecutor with the flowing feature:
     * - dynamic thread count
     * - max thread amount
     * - scales back to 0 idle threads
     * - uncaught exception handler
     * - MDC handling
     *
     * @param nThreads the maximum number of threads to allow in the pool.
     * @param ttl the maximum time that excess idle threads will wait for new tasks before terminating.
     * @param name the name which is used for this pool.
     * @param uncaughtExceptionHandler the handler which is invoked on an uncaught exception during the execution.
     */
    @JvmOverloads
    fun getDynamicThreadPool(
        nThreads: Int,
        ttl: Duration,
        name: String,
        uncaughtExceptionHandler: (Thread, Throwable) -> (Unit) = { t, e -> logger.warn("Uncaught Exception in Thread: ${t.name} - ${e.message}\n${e.stackTraceToString()}") }
    ): ThreadPoolExecutor {
        return getDynamicThreadPool(nThreads, nThreads, ttl, true, name, uncaughtExceptionHandler)
    }

    /**
     * Creates a new AlunaThreadPoolExecutor with the flowing feature:
     * - dynamic thread count
     * - max thread amount
     * - scales back to 0 or minimum idle threads
     * - uncaught exception handler
     * - MDC handling
     *
     * @param minThreads the minimum number of threads to keep in the pool.
     * @param maxThreads the maximum number of threads to allow in the pool.
     * @param ttl the maximum time that excess idle threads will wait for new tasks before terminating.
     * @param scaleToZero if true the pool scales the threads down to 0. If false the minThreads amount will be kept alive.
     * @param name the name which is used for this pool.
     * @param uncaughtExceptionHandler the handler which is invoked on an uncaught exception during the execution.
     */
    @JvmOverloads
    fun getDynamicThreadPool(
        minThreads: Int,
        maxThreads: Int,
        ttl: Duration,
        scaleToZero: Boolean,
        name: String,
        uncaughtExceptionHandler: (Thread, Throwable) -> (Unit) = { t, e -> logger.warn("Uncaught Exception in Thread: ${t.name} - ${e.message}\n${e.stackTraceToString()}") }
    ): ThreadPoolExecutor {
        val threadPoolUtil = AlunaThreadPoolExecutor(
            minThreads, maxThreads,
            ttl,
            LinkedBlockingQueue(),
            ThreadFactoryBuilder().setNameFormat(name).setUncaughtExceptionHandler(uncaughtExceptionHandler).build()
        )
        threadPoolUtil.allowCoreThreadTimeOut(scaleToZero)
        return threadPoolUtil
    }

    /**
     * Creates a new AlunaThreadPoolExecutor with the flowing feature:
     * - max thread amount of 1
     * - scales back to 0 idle threads
     * - uncaught exception handler
     * - MDC handling
     *
     * @param ttl the maximum time that excess idle threads will wait for new tasks before terminating.
     * @param name the name which is used for this pool.
     * @param uncaughtExceptionHandler the handler which is invoked on an uncaught exception during the execution.
     */
    @JvmOverloads
    fun getDynamicSingleThreadPool(
        ttl: Duration, name: String,
        uncaughtExceptionHandler: (Thread, Throwable) -> (Unit) = { t, e -> logger.warn("Uncaught Exception in Thread: ${t.name} - ${e.message}\n${e.stackTraceToString()}") }
    ): ThreadPoolExecutor {
        val threadPoolUtil = AlunaThreadPoolExecutor(
            0, 1,
            ttl,
            LinkedBlockingQueue(),
            ThreadFactoryBuilder().setNameFormat(name).setUncaughtExceptionHandler(uncaughtExceptionHandler).build()
        )
        threadPoolUtil.allowCoreThreadTimeOut(true)
        return threadPoolUtil
    }

    /**
     * Creates a new AlunaScheduledThreadPoolExecutor with the flowing feature:
     * - dynamic thread count
     * - max thread amount
     * - scales back to defined idle threads
     * - uncaught exception handler
     * - MDC handling
     *
     * @param minThreads the minimum number of threads to keep in the pool.
     * @param maxThreads the maximum number of threads to allow in the pool.
     * @param ttl the maximum time that excess idle threads will wait for new tasks before terminating.
     * @param name the name which is used for this pool.
     * @param removeTaskOnCancelPolicy True if ScheduledFutureTask.cancel should remove from queue.
     * @param uncaughtExceptionHandler the handler which is invoked on an uncaught exception during the execution.
     */
    @JvmOverloads
    fun getScheduledThreadPool(
        minThreads: Int,
        maxThreads: Int,
        ttl: Duration,
        name: String,
        removeTaskOnCancelPolicy: Boolean = false,
        uncaughtExceptionHandler: (Thread, Throwable) -> (Unit) = { t, e -> logger.warn("Uncaught Exception in Thread: ${t.name} - ${e.message}\n${e.stackTraceToString()}") }
    ): ScheduledThreadPoolExecutor {
        return AlunaScheduledThreadPoolExecutor(
            minThreads,
            maxThreads,
            ttl,
            ThreadFactoryBuilder().setNameFormat(name).setUncaughtExceptionHandler(uncaughtExceptionHandler).build(),
            removeTaskOnCancelPolicy
        )
    }

    /**
     * Creates a new AlunaScheduledThreadPoolExecutor with the flowing feature:
     * - fixed & persistent thread count
     * - uncaught exception handler
     * - MDC handling
     *
     * @param nThreads the number of threads in the pool.
     * @param name the name which is used for this pool.
     * @param removeTaskOnCancelPolicy True if ScheduledFutureTask.cancel should remove from queue.
     * @param uncaughtExceptionHandler the handler which is invoked on an uncaught exception during the execution.
     */
    @JvmOverloads
    fun getFixedScheduledThreadPool(
        nThreads: Int,
        name: String,
        removeTaskOnCancelPolicy: Boolean = false,
        uncaughtExceptionHandler: (Thread, Throwable) -> (Unit) = { t, e -> logger.warn("Uncaught Exception in Thread: ${t.name} - ${e.message}\n${e.stackTraceToString()}") }
    ): ScheduledThreadPoolExecutor {
        return AlunaScheduledThreadPoolExecutor(
            nThreads,
            nThreads,
            null,
            ThreadFactoryBuilder().setNameFormat(name).setUncaughtExceptionHandler(uncaughtExceptionHandler).build(),
            removeTaskOnCancelPolicy
        )
    }

    /**
     * Executes the given [block] of code with a specified [timeout] on a given [executor].
     *
     * @param T The type of the result returned by [block].
     * @param executor The [ThreadPoolExecutor] where the [block] is executed.
     * @param timeout The maximum time to wait for the [block] to complete.
     * @param block The code block to be executed.
     *
     * @return The result of type [T] returned by [block].
     *
     * @throws TimeoutException If the [block] execution exceeds the given [timeout].
     * @throws ExecutionException If an exception occurred during the [block] execution.
     * @throws InterruptedException If the thread was interrupted during execution.
     */
    suspend fun <T> runWithTimeout(executor: ThreadPoolExecutor, timeout: Duration, block: () -> T): T {
        val future = CompletableFuture.supplyAsync({
            try {
                block()
            } catch (e: Exception) {
                throw e.cause ?: e
            }
        }, executor)

        return try {
            withContext(Dispatchers.IO) {
                future.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
            }
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw TimeoutException("Execution timed out after ${timeout.toString()}")
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        } catch (e: InterruptedException) {
            throw e
        }
    }

    class AlunaThreadPoolExecutor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Duration,
        workQueue: BlockingQueue<Runnable>,
        threadFactory: ThreadFactory,
        private val fixedContext: Map<String?, String?>? = null,
    ) : ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime.seconds, TimeUnit.SECONDS, workQueue, threadFactory) {

        private val useFixedContext = (fixedContext != null)

        companion object {
            fun newWithCurrentMdc(
                corePoolSize: Int,
                maximumPoolSize: Int,
                keepAliveTime: Duration,
                workQueue: BlockingQueue<Runnable>,
                threadFactory: ThreadFactory
            ): AlunaThreadPoolExecutor =
                AlunaThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, workQueue, threadFactory, MDC.getCopyOfContextMap())
        }

        private fun getContextForTask(): Map<String?, String?>? {
            return if (useFixedContext) fixedContext else MDC.getCopyOfContextMap()
        }

        /**
         * All executions will have MDC injected. `ThreadPoolExecutor`'s submission methods (`submit()` etc.)
         * all delegate to this.
         */
        override fun execute(command: Runnable) {
            super.execute(wrap(command, getContextForTask()))
        }

        private fun wrap(runnable: Runnable, context: Map<String?, String?>?): Runnable = Runnable {
            val previous: Map<String?, String?>? = MDC.getCopyOfContextMap()
            if (context == null) {
                MDC.clear()
            } else {
                MDC.setContextMap(context)
            }
            try {
                runnable.run()
            } finally {
                if (previous == null) {
                    MDC.clear()
                } else {
                    MDC.setContextMap(previous)
                }
            }
        }
    }

    class AlunaScheduledThreadPoolExecutor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        ttl: Duration? = null,
        threadFactory: ThreadFactory,
        removeTaskOnCancelPolicy: Boolean = false,
        private val fixedContext: Map<String?, String?>? = null,
    ) : ScheduledThreadPoolExecutor(corePoolSize, threadFactory) {

        private val useFixedContext = (fixedContext != null)

        init {
            this.removeOnCancelPolicy = removeTaskOnCancelPolicy
            this.maximumPoolSize = maximumPoolSize
            if (ttl != null) {
                this.setKeepAliveTime(ttl.seconds, TimeUnit.SECONDS)
                this.allowCoreThreadTimeOut(true)
            }
        }

        companion object {
            fun newWithCurrentMdc(
                corePoolSize: Int,
                maximumPoolSize: Int,
                ttl: Duration,
                threadFactory: ThreadFactory,
                removeTaskOnCancelPolicy: Boolean = false,
            ): AlunaScheduledThreadPoolExecutor =
                AlunaScheduledThreadPoolExecutor(corePoolSize, maximumPoolSize, ttl, threadFactory, removeTaskOnCancelPolicy, MDC.getCopyOfContextMap())
        }

        private fun getContextForTask(): Map<String?, String?>? {
            return if (useFixedContext) fixedContext else MDC.getCopyOfContextMap()
        }

        /**
         * All executions will have MDC injected. `ThreadPoolExecutor`'s submission methods (`submit()` etc.)
         * all delegate to this.
         */
        override fun execute(command: Runnable) {
            super.execute(wrap(command, getContextForTask()))
        }

        private fun wrap(runnable: Runnable, context: Map<String?, String?>?): Runnable = Runnable {
            val previous: Map<String?, String?>? = MDC.getCopyOfContextMap()
            if (context == null) {
                MDC.clear()
            } else {
                MDC.setContextMap(context)
            }
            try {
                runnable.run()
            } finally {
                if (previous == null) {
                    MDC.clear()
                } else {
                    MDC.setContextMap(previous)
                }
            }
        }

    }
}
