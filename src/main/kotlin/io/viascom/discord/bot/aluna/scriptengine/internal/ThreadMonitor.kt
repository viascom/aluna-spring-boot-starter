package io.viascom.discord.bot.aluna.scriptengine.internal

import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.lang.management.ThreadMXBean
import java.util.concurrent.atomic.AtomicBoolean

class ThreadMonitor internal constructor(maxCPUTime: Long, private val maxMemory: Long) {
    val maxCPUTime: Long
    val stop: AtomicBoolean

    /** Check if interrupted script has finished.  */
    val scriptFinished: AtomicBoolean

    /** Check if script should be killed to stop it when abusive.  */
    val scriptKilled: AtomicBoolean
    val cpuLimitExceeded: AtomicBoolean
    val memoryLimitExceeded: AtomicBoolean
    val monitor: Object
    var threadToMonitor: Thread? = null
    var timedOutWaitingForThreadToMonitor = false
    lateinit var threadBean: ThreadMXBean
    var memoryCounter: com.sun.management.ThreadMXBean?

    val logger = LoggerFactory.getLogger(javaClass)


    private fun reset() {
        stop.set(false)
        scriptFinished.set(false)
        scriptKilled.set(false)
        cpuLimitExceeded.set(false)
        threadToMonitor = null
    }

    fun run() {
        try {
            // wait, for threadToMonitor to be set in JS evaluator thread
            synchronized(monitor) {
                if (threadToMonitor == null) {
                    monitor.wait((maxCPUTime + 500) / MILI_TO_NANO)
                }
                if (threadToMonitor == null) {
                    timedOutWaitingForThreadToMonitor = true
                    throw IllegalStateException("Executor thread not set after " + maxCPUTime / MILI_TO_NANO + " ms")
                }
            }
            val startCPUTime = cPUTime
            val startMemory = currentMemory
            while (!stop.get()) {
                val runtime = cPUTime - startCPUTime
                val memory = currentMemory - startMemory
                if (isCpuTimeExided(runtime) || isMemoryExided(memory)) {
                    cpuLimitExceeded.set(isCpuTimeExided(runtime))
                    memoryLimitExceeded.set(isMemoryExided(memory))
                    threadToMonitor!!.interrupt()
                    synchronized(monitor) { monitor.wait(50) }
                    if (stop.get()) {
                        return
                    }
                    if (!scriptFinished.get()) {
                        logger.error(this.javaClass.simpleName + ": Thread hard shutdown!")
                        threadToMonitor!!.stop()
                        scriptKilled.set(true)
                    }
                    return
                } else {
                }
                synchronized(monitor) {
                    var waitTime = getCheckInterval(runtime)
                    if (waitTime == 0L) {
                        waitTime = 1
                    }
                    monitor.wait(waitTime)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun getCheckInterval(runtime: Long): Long {
        if (maxCPUTime == 0L) {
            return 10
        }
        return if (maxMemory == 0L) {
            Math.max((maxCPUTime - runtime) / MILI_TO_NANO, 5)
        } else Math.min(
            (maxCPUTime - runtime) / MILI_TO_NANO,
            10
        )
    }

    private fun isCpuTimeExided(runtime: Long): Boolean {
        return if (maxCPUTime == 0L) {
            false
        } else runtime > maxCPUTime
    }

    private fun isMemoryExided(memory: Long): Boolean {
        return if (maxMemory == 0L) {
            false
        } else memory > maxMemory
    }

    /**
     * Obtain current evaluation thread memory usage.
     *
     * @return current memory usage
     */
    private val currentMemory: Long
        get() = if (maxMemory > 0 && memoryCounter != null) {
            memoryCounter!!.getThreadAllocatedBytes(threadToMonitor!!.id)
        } else 0L
    private val cPUTime: Long
        get() = if (maxCPUTime > 0 && threadBean != null) {
            threadBean.getThreadCpuTime(threadToMonitor!!.id)
        } else {
            0L
        }

    fun stopMonitor() {
        synchronized(monitor) {
            stop.set(true)
            monitor.notifyAll()
        }
    }

    fun registerThreadToMonitor(t: Thread?): Boolean {
        synchronized(monitor) {
            if (timedOutWaitingForThreadToMonitor) {
                return false
            }
            reset()
            threadToMonitor = t
            monitor.notifyAll()
            return true
        }
    }

    fun scriptFinished() {
        scriptFinished.set(false)
    }

    val isCPULimitExceeded: Boolean
        get() = cpuLimitExceeded.get()

    fun isMemoryLimitExceeded(): Boolean {
        return memoryLimitExceeded.get()
    }

    fun isScriptKilled(): Boolean {
        return scriptKilled.get()
    }

    companion object {
        private const val MILI_TO_NANO = 1000000
    }

    init {
        this.maxCPUTime = maxCPUTime * 1000000
        stop = AtomicBoolean(false)
        scriptFinished = AtomicBoolean(false)
        scriptKilled = AtomicBoolean(false)
        cpuLimitExceeded = AtomicBoolean(false)
        memoryLimitExceeded = AtomicBoolean(false)
        monitor = Object()

        // ensure the ThreadMXBean is supported in the JVM
        try {
            threadBean = ManagementFactory.getThreadMXBean()
            // ensure the ThreadMXBean is enabled for CPU time measurement
            threadBean.isThreadCpuTimeEnabled = true
        } catch (ex: UnsupportedOperationException) {
            if (maxCPUTime > 0) {
                throw UnsupportedOperationException("JVM does not support thread CPU time measurement")
            }
        }
        if (threadBean is com.sun.management.ThreadMXBean) {
            memoryCounter = threadBean as com.sun.management.ThreadMXBean
            // ensure this feature is enabled
            memoryCounter?.isThreadAllocatedMemoryEnabled = true
        } else {
            if (maxMemory > 0) {
                throw UnsupportedOperationException("JVM does not support thread memory counting")
            }
            memoryCounter = null
        }
    }
}
