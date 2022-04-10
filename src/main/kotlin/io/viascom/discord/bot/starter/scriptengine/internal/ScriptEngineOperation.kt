package io.viascom.discord.bot.starter.scriptengine.internal

import javax.script.ScriptEngine
import javax.script.ScriptException

interface ScriptEngineOperation {
    @Throws(ScriptException::class, Exception::class)
    fun executeScriptEngineOperation(scriptEngine: ScriptEngine): Any
}
