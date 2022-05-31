package io.viascom.discord.bot.aluna.scriptengine

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import io.viascom.discord.bot.aluna.scriptengine.exception.ScriptCPUAbuseException
import io.viascom.discord.bot.aluna.scriptengine.exception.ScriptMemoryAbuseException
import io.viascom.discord.bot.aluna.scriptengine.internal.*
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.PolyglotAccess
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Writer
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import javax.script.*

class GraalSandbox(vararg params: String) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    val scriptEngine: ScriptEngine = GraalJSScriptEngine.create(
        Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build(), Context.newBuilder()
            .allowExperimentalOptions(true)
            .allowPolyglotAccess(PolyglotAccess.NONE)
            .allowHostAccess(HostAccess.NONE)
            .allowAllAccess(false)
    )

    val sandboxClassFilter = SandboxClassFilter()

    /** Maximum CPU time in milliseconds.  */
    var maxCPUTime = 0L

    /** Maximum memory of executor thread used.  */
    var maxMemory = 0L

    var executor: ExecutorService? = null
    var allowPrintFunctions = false
    var allowReadFunctions = false
    var allowLoadFunctions = false
    var allowExitFunctions = false
    var allowGlobalsObjects = false
    var allowNoBraces = false
    var suppliedCache: SecuredJsCache? = null
    lateinit var cached: Bindings
    var engineAsserted: AtomicBoolean = AtomicBoolean(false)

    var evaluator: JsEvaluator? = null
    var sanitizer: JsSanitizer? = null
    var maxPreparedStatements = 0
    var lazyInvocable: Invocable? = null

    init {
        for (param in params) {
            require(param != "--no-java") { "The engine parameter --no-java is not supported. Using it would interfere with the injected code to test for infinite loops." }
        }
    }

    fun createBindings(): Bindings {
        return scriptEngine.createBindings()
    }

    private fun produceSecureBindings() {
        try {
            val sb = StringBuilder()
            cached = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE)
            sanitizeBindings(cached)
            if (!allowExitFunctions) {
                sb.append("var quit=function(){};var exit=function(){};")
            }
            if (!allowPrintFunctions) {
                sb.append("var print=function(){};var echo = function(){};")
            }
            if (!allowReadFunctions) {
                sb.append("var readFully=function(){};").append("var readLine=function(){};")
            }
            if (!allowLoadFunctions) {
                sb.append("var load=function(){};var loadWithNewGlobal=function(){};")
            }
            if (!allowGlobalsObjects) {
                sb.append("var \$ARG=null;var \$ENV=null;var \$EXEC=null;")
                sb.append("var \$OPTIONS=null;var \$OUT=null;var \$ERR=null;var \$EXIT=null;")
            }
            scriptEngine.eval(sb.toString())
            engineAsserted.set(true)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun secureBindings(bindings: Bindings?): Bindings? {
        if (bindings == null) return null
        val toRemove: MutableSet<String> = HashSet()
        if (bindings !== cached) {
            for (entry in bindings.entries) {
                if (cached.putIfAbsent(entry.key, entry.value) != null) toRemove.add(entry.key)
            }
        }
        for (key in toRemove) bindings.remove(key)
        return cached
    }

    fun resetEngineBindings() {
        val bindings: Bindings = createBindings()
        sanitizeBindings(bindings)
        bindings.putAll(cached)
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
    }

    fun sanitizeBindings(bindings: Bindings) {
        if (!allowExitFunctions) {
            bindings.remove("quit")
            bindings.remove("exit")
        }
        if (!allowPrintFunctions) {
            bindings.remove("print")
            bindings.remove("echo")
        }
        if (!allowReadFunctions) {
            bindings.remove("readFully")
            bindings.remove("readLine")
        }
        if (!allowLoadFunctions) {
            bindings.remove("load")
            bindings.remove("loadWithNewGlobal")
        }
    }

    @Synchronized
    private fun assertScriptEngine() {
        try {
            if (!engineAsserted.get()) {
                produceSecureBindings()
            } else if (!engineBindingUnchanged()) {
                resetEngineBindings()
            }
        } catch (e: java.lang.Exception) {
            throw RuntimeException(e)
        }
    }

    // This will detect whether the current engine bindings match the cached
    // protected bindings
    private fun engineBindingUnchanged(): Boolean {
        val current = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE)
        for ((key, value) in cached) {
            if (!current.containsKey(key) || current[key] != value) {
                return false
            }
        }
        return true
    }

    @Throws(ScriptCPUAbuseException::class, ScriptException::class)
    fun eval(js: String): Any {
        return eval(js, null, null)
    }

    @Throws(ScriptCPUAbuseException::class, ScriptException::class)
    fun eval(js: String, bindings: Bindings?): Any {
        return eval(js, null, bindings)
    }

    @Throws(ScriptCPUAbuseException::class, ScriptException::class)
    fun eval(js: String, scriptContext: ScriptContext?): Any {
        return eval(js, scriptContext, null)
    }

    @Throws(ScriptCPUAbuseException::class, ScriptException::class)
    fun eval(js: String, scriptContext: ScriptContext?, bindings: Bindings?): Any {
        assertScriptEngine()
        val sanitizer: JsSanitizer = getNewSanitizer()
        // see https://github.com/javadelight/delight-nashorn-sandbox/issues/73
        val blockAccessToEngine = ("Object.defineProperty(this, 'engine', {});"
                + "Object.defineProperty(this, 'context', {});delete this.__noSuchProperty__;")
        val securedJs: String
        securedJs = if (scriptContext == null) {
            blockAccessToEngine + sanitizer.secureJs(js)
        } else {
            // Unfortunately, blocking access to the engine property inteferes with setting
            // a script context
            // needs further investigation
            sanitizer.secureJs(js)
        }
        val securedBindings = secureBindings(bindings)
        val op = EvaluateOperation(securedJs, scriptContext, securedBindings)
        return executeSandboxedOperation(op)
    }

    @Throws(ScriptCPUAbuseException::class, ScriptException::class)
    protected fun executeSandboxedOperation(op: ScriptEngineOperation): Any {
        assertScriptEngine()
        return try {
            if (maxCPUTime == 0L && maxMemory == 0L) {
                return op.executeScriptEngineOperation(scriptEngine)
            }
            checkExecutorPresence()
            val evaluator: JsEvaluator = getEvaluator(op)
            executor!!.execute(evaluator)
            evaluator.runMonitor()
            if (evaluator.isCPULimitExceeded) {
                throw ScriptCPUAbuseException(
                    "Script used more than the allowed [$maxCPUTime ms] of CPU time.",
                    evaluator.isScriptKilled, evaluator.exception
                )
            } else if (evaluator.isMemoryLimitExceeded) {
                throw ScriptMemoryAbuseException(
                    "Script used more than the allowed [$maxMemory B] of memory.",
                    evaluator.isScriptKilled, evaluator.exception
                )
            }
            if (evaluator.exception != null) {
                throw evaluator.exception!!
            }
            evaluator.result!!
        } catch (e: RuntimeException) {
            throw e
        } catch (e: ScriptException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RuntimeException(e)
        }
    }

    private fun getEvaluator(op: ScriptEngineOperation): JsEvaluator {
        return JsEvaluator(scriptEngine, maxCPUTime, maxMemory, op)
    }

    private fun checkExecutorPresence() {
        checkNotNull(executor) {
            ("When a CPU time or memory limit is set, an executor "
                    + "needs to be provided by calling .setExecutor(...)")
        }
    }

    private fun getNewSanitizer(): JsSanitizer {
        if (sanitizer == null) {
            if (suppliedCache == null) {
                sanitizer = JsSanitizer(scriptEngine, maxPreparedStatements, allowNoBraces)
            } else {
                sanitizer = JsSanitizer(scriptEngine, allowNoBraces, suppliedCache)
            }
        }
        return sanitizer!!
    }

    fun allow(clazz: Class<*>) {
        sandboxClassFilter.add(clazz)
    }

    fun disallow(clazz: Class<*>) {
        sandboxClassFilter.remove(clazz)
    }

    fun isAllowed(clazz: Class<*>): Boolean {
        return sandboxClassFilter.contains(clazz)
    }

    fun disallowAllClasses() {
        sandboxClassFilter.clear()
        // this class must always be allowed, see issue 54
        // https://github.com/javadelight/delight-nashorn-sandbox/issues/54
        allow(InterruptTest::class.java)
    }

    fun inject(variableName: String?, `object`: Any?) {
        if (`object` != null && !sandboxClassFilter.contains(`object`.javaClass)) {
            allow(`object`.javaClass)
        }
        scriptEngine.put(variableName, `object`)
    }

    operator fun get(variableName: String?): Any? {
        return scriptEngine[variableName]
    }

    fun allowPrintFunctions(v: Boolean) {
        check(!engineAsserted.get()) { "Please set this property before calling eval." }
        allowPrintFunctions = v
    }

    fun allowReadFunctions(v: Boolean) {
        check(!engineAsserted.get()) { "Please set this property before calling eval." }
        allowReadFunctions = v
    }

    fun allowLoadFunctions(v: Boolean) {
        check(!engineAsserted.get()) { "Please set this property before calling eval." }
        allowLoadFunctions = v
    }

    fun allowExitFunctions(v: Boolean) {
        check(!engineAsserted.get()) { "Please set this property before calling eval." }
        allowExitFunctions = v
    }

    fun allowGlobalsObjects(v: Boolean) {
        check(!engineAsserted.get()) { "Please set this property before calling eval." }
        allowGlobalsObjects = v
    }

    fun allowNoBraces(v: Boolean) {
        if (allowNoBraces != v) {
            sanitizer = null
        }
        allowNoBraces = v
    }

    fun setWriter(writer: Writer?) {
        scriptEngine.context.writer = writer
    }


    fun getSandboxedInvocable(): Invocable? {
        return if (maxMemory == 0L && maxCPUTime == 0L) {
            scriptEngine as Invocable
        } else getLazySandboxedInvocable()
    }

    private fun getLazySandboxedInvocable(): Invocable? {
        if (lazyInvocable == null) {
            val sandboxInvocable: Invocable = object : Invocable {
                @Throws(ScriptException::class, NoSuchMethodException::class)
                override fun invokeMethod(thiz: Any, name: String, vararg args: Any): Any {
                    val op = InvokeOperation(thiz, name, args)
                    return try {
                        executeSandboxedOperation(op)!!
                    } catch (e: ScriptException) {
                        throw e
                    } catch (e: java.lang.Exception) {
                        throw ScriptException(e)
                    }
                }

                @Throws(ScriptException::class, NoSuchMethodException::class)
                override fun invokeFunction(name: String, vararg args: Any): Any {
                    val op = InvokeOperation(null, name, args)
                    return try {
                        executeSandboxedOperation(op)!!
                    } catch (e: ScriptException) {
                        throw e
                    } catch (e: java.lang.Exception) {
                        throw ScriptException(e)
                    }
                }

                override fun <T> getInterface(thiz: Any, clasz: Class<T>): T {
                    // TODO add proxy wrapper for proper sandboxing
                    throw IllegalStateException("Not yet implemented")
                }

                override fun <T> getInterface(clasz: Class<T>): T {
                    // TODO add proxy wrapper for proper sandboxing
                    throw IllegalStateException("Not yet implemented")
                }
            }
            lazyInvocable = sandboxInvocable
        }
        return lazyInvocable
    }
}
