package io.viascom.discord.bot.starter.scriptengine.internal

import org.slf4j.LoggerFactory
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptException

class EvaluateOperation(val js: String, scriptContext: ScriptContext?, bindings: Bindings?) : ScriptEngineOperation {
    val logger = LoggerFactory.getLogger(javaClass)

    private val scriptContext: ScriptContext?
    private val bindings: Bindings?
    fun getScriptContext(): ScriptContext? {
        return scriptContext
    }

    fun getBindings(): Bindings? {
        return bindings
    }

    @Throws(ScriptException::class)
    override fun executeScriptEngineOperation(scriptEngine: ScriptEngine): Any {
        if (logger.isDebugEnabled()) {
            logger.debug("--- Running JS ---")
            logger.debug(js)
            logger.debug("--- JS END ---")
        }
        return if (bindings != null) {
            scriptEngine.eval(js, bindings)
        } else if (scriptContext != null) {
            scriptEngine.eval(js, scriptContext)
        } else {
            scriptEngine.eval(js)
        }
    }

    init {
        this.scriptContext = scriptContext
        this.bindings = bindings
    }
}
