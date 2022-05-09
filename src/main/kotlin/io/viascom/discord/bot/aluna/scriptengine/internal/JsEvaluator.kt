package io.viascom.discord.bot.aluna.scriptengine.internal

import javax.script.ScriptEngine

class JsEvaluator(scriptEngine: ScriptEngine, maxCPUTime: Long, maxMemory: Long, operation: ScriptEngineOperation) : Runnable {
    private val threadMonitor: ThreadMonitor
    private val scriptEngine: ScriptEngine
    var result: Any? = null
        private set
    var exception: Exception? = null
        private set
    private val operation: ScriptEngineOperation
    val isScriptKilled: Boolean
        get() = threadMonitor.scriptKilled.get()
    val isCPULimitExceeded: Boolean
        get() = threadMonitor.isCPULimitExceeded
    val isMemoryLimitExceeded: Boolean
        get() = threadMonitor.isMemoryLimitExceeded()

    /**
     * Enter the monitor method. It should be called from main thread.
     */
    fun runMonitor() {
        threadMonitor.run()
    }

    override fun run() {
        try {
            val registered = threadMonitor.registerThreadToMonitor(Thread.currentThread())
            if (registered) {
                result = operation.executeScriptEngineOperation(scriptEngine)
            }
        } catch (e: RuntimeException) {
            // InterruptedException means script was successfully interrupted,
            // so no exception should be propagated
            if (e.cause !is InterruptedException) {
                exception = e
            }
        } catch (e: Exception) {
            exception = e
        } finally {
            threadMonitor.scriptFinished()
            threadMonitor.stopMonitor()
        }
    }

    init {
        this.scriptEngine = scriptEngine
        threadMonitor = ThreadMonitor(maxCPUTime, maxMemory)
        this.operation = operation
    }
}
