package io.viascom.discord.bot.aluna.scriptengine.internal

import javax.script.Invocable
import javax.script.ScriptEngine

class InvokeOperation(private val thisObj: Any?, private val name: String, private val args: Array<out Any>) : ScriptEngineOperation {
    @Throws(Exception::class)
    override fun executeScriptEngineOperation(scriptEngine: ScriptEngine): Any {
        return if (thisObj == null) {
            (scriptEngine as Invocable).invokeFunction(name, args)
        } else {
            (scriptEngine as Invocable).invokeMethod(thisObj, name, args)
        }
    }
}
