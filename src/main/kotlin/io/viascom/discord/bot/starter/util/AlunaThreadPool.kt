package io.viascom.discord.bot.starter.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.concurrent.*

object AlunaThreadPool {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun getDynamicThreadPool(nThreads: Int, ttl: Long, name: String): ThreadPoolExecutor {
        val threadPoolUtil = AlunaThreadPoolExecutor(
            nThreads, nThreads,
            ttl, TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            ThreadFactoryBuilder().setNameFormat(name).setUncaughtExceptionHandler { t, e ->
                logger.warn("Uncaught Exception in Thread: ${t.name} - ${e.message}\n${e.stackTraceToString()}")
            }.build()
        )
        threadPoolUtil.allowCoreThreadTimeOut(true)
        return threadPoolUtil
    }

    fun getDynamicSingleThreadPool(ttl: Long, name: String): ThreadPoolExecutor {
        val threadPoolUtil = AlunaThreadPoolExecutor(
            0, 1,
            ttl, TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            ThreadFactoryBuilder().setNameFormat(name).setUncaughtExceptionHandler { t, e ->
                logger.warn("Uncaught Exception in Thread: ${t.name} - ${e.message}\n${e.stackTraceToString()}")
            }.build()
        )
        threadPoolUtil.allowCoreThreadTimeOut(true)
        return threadPoolUtil
    }

    fun getScheduledThreadPool(nThreads: Int, name: String): ScheduledThreadPoolExecutor {
        val threadPoolUtil = AlunaScheduledThreadPoolExecutor(nThreads, ThreadFactoryBuilder().setNameFormat(name).setUncaughtExceptionHandler { t, e ->
            logger.warn("Uncaught Exception in Thread: ${t.name} - ${e.message}\n${e.stackTraceToString()}")
        }.build())

        return threadPoolUtil
    }

    class AlunaThreadPoolExecutor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit,
        workQueue: BlockingQueue<Runnable>,
        threadFactory: ThreadFactory,
        val fixedContext: Map<String?, String?>? = null,
    ) : ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory) {

        private val useFixedContext = (fixedContext != null)

        companion object {
            fun newWithCurrentMdc(
                corePoolSize: Int,
                maximumPoolSize: Int,
                keepAliveTime: Long,
                unit: TimeUnit,
                workQueue: BlockingQueue<Runnable>,
                threadFactory: ThreadFactory
            ): AlunaThreadPoolExecutor =
                AlunaThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, MDC.getCopyOfContextMap())
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
        threadFactory: ThreadFactory,
        val fixedContext: Map<String?, String?>? = null,
    ) : ScheduledThreadPoolExecutor(corePoolSize, threadFactory) {

        private val useFixedContext = (fixedContext != null)

        companion object {
            fun newWithCurrentMdc(
                corePoolSize: Int,
                maximumPoolSize: Int,
                keepAliveTime: Long,
                unit: TimeUnit,
                workQueue: BlockingQueue<Runnable>,
                threadFactory: ThreadFactory
            ): AlunaThreadPoolExecutor =
                AlunaThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, MDC.getCopyOfContextMap())
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
