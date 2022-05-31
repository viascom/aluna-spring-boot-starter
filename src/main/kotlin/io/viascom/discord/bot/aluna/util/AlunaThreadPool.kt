package io.viascom.discord.bot.aluna.util

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

    class AlunaThreadPoolExecutor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Duration,
        workQueue: BlockingQueue<Runnable>,
        threadFactory: ThreadFactory,
        val fixedContext: Map<String?, String?>? = null,
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

        fun wrap(runnable: Runnable, context: Map<String?, String?>?): Runnable {
            return Runnable {
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

    class AlunaScheduledThreadPoolExecutor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        ttl: Duration? = null,
        threadFactory: ThreadFactory,
        removeTaskOnCancelPolicy: Boolean = false,
        val fixedContext: Map<String?, String?>? = null,
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

        fun wrap(runnable: Runnable, context: Map<String?, String?>?): Runnable {
            return Runnable {
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
}
