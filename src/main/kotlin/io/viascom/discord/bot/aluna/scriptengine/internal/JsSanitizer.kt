package io.viascom.discord.bot.aluna.scriptengine.internal

import io.viascom.discord.bot.aluna.scriptengine.exception.BracesException
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.SoftReference
import java.nio.charset.StandardCharsets
import java.util.function.Function
import java.util.function.Supplier
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.script.ScriptEngine
import javax.script.ScriptException

class JsSanitizer {
    private class PoisonPil internal constructor(pattern: Pattern, replacement: String) {
        var pattern: Pattern
        var replacement: String

        init {
            this.pattern = pattern
            this.replacement = replacement
        }
    }

    companion object {
        /** The resource name of beautify.min.js script.  */
        private const val BEAUTIFY_JS = "/js/lib/beautify.js"

        /** The beautify function search list.  */
        private val BEAUTIFY_FUNCTIONS: List<String> = arrayListOf(
            "window.js_beautify;", "exports.js_beautify;",
            "global.js_beautify;"
        )

        /** Pattern for back braces.  */
        private val LACK_EXPECTED_BRACES: List<Pattern> = arrayListOf(
            Pattern.compile("for [^\\{]+$"),
            Pattern.compile("^\\s*do [^\\{]*$", Pattern.MULTILINE),
            Pattern.compile("^[^\\}]*while [^\\{]+$", Pattern.MULTILINE)
        )

        /**
         * The name of the JS function to be inserted into user script. To prevent
         * collisions random suffix is added.
         */
        const val JS_INTERRUPTED_FUNCTION = "__if"

        /**
         * The name of the variable which holds reference to interruption checking
         * class. To prevent collisions random suffix is added.
         */
        const val JS_INTERRUPTED_TEST = "__it"
        private val POISON_PILLS: List<PoisonPil> = arrayListOf( // every 10th statements ended with semicolon put interrupt checking function
            PoisonPil(
                Pattern.compile("(([^;]+;){9}[^;]+(?<!break|continue);\\n(?![\\W]*(\\/\\/.+[\\W]+)*else))"),
                """
                $JS_INTERRUPTED_FUNCTION();
                
                """.trimIndent()
            ),  // every (except switch) block start brace put interrupt checking function
            PoisonPil(Pattern.compile("(\\s*for\\s*\\([^\\{]+\\)\\s*\\{)"), "$JS_INTERRUPTED_FUNCTION();"),  // for
            // with
            // block
            PoisonPil(Pattern.compile("(\\s*for\\s*\\([^\\{]+\\)\\s*[^\\{]+;)"), "$JS_INTERRUPTED_FUNCTION();"),  // for
            // without
            // block
            //
            PoisonPil(
                Pattern.compile("(\\s*([^\"]?function)\\s*[^\"}]*\\([^\\{]*\\)\\s*\\{)"),
                "$JS_INTERRUPTED_FUNCTION();"
            ),  // function except when enclosed in quotes
            PoisonPil(Pattern.compile("(\\s*while\\s*\\([^\\{]+\\{)"), "$JS_INTERRUPTED_FUNCTION();"),
            PoisonPil(Pattern.compile("(\\s*do\\s*\\{)"), "$JS_INTERRUPTED_FUNCTION();")
        )

        /**
         * The beautifier options. Don't change if you are not know what you are doing,
         * because regexps are depended on it.
         */
        private val BEAUTIFY_OPTIONS: HashMap<String, Any> = HashMap()

        /** Soft reference to the text of the js script.  */
        private var beautifysScript: SoftReference<String> = SoftReference(null)
        private fun getBeautifHandler(scriptEngine: ScriptEngine): Any {
            try {
                for (name in BEAUTIFY_FUNCTIONS) {
                    val somWindow: Any? = scriptEngine.eval(name)
                    if (somWindow != null) {
                        return somWindow
                    }
                }
                throw RuntimeException("Cannot find function 'js_beautify' in: window, exports, global")
            } catch (e: ScriptException) {
                // should never happen
                throw RuntimeException(e)
            }
        }

        private val beautifyJs: String?
            get() {
                var script: String? = beautifysScript.get()
                if (script == null) {
                    try {
                        BufferedReader(
                            InputStreamReader(
                                BufferedInputStream(JsSanitizer::class.java.getResourceAsStream(BEAUTIFY_JS)), StandardCharsets.UTF_8
                            )
                        ).use { reader ->
                            val sb = StringBuilder()
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                sb.append(line).append('\n')
                            }
                            script = sb.toString()
                        }
                    } catch (e: IOException) {
                        throw RuntimeException("Cannot find file: " + BEAUTIFY_JS, e)
                    }
                    beautifysScript = SoftReference(script)
                }
                return script
            }

        init {
            BEAUTIFY_OPTIONS["brace_style"] = "collapse"
            BEAUTIFY_OPTIONS["preserve_newlines"] = false
            BEAUTIFY_OPTIONS["indent_size"] = 1
            BEAUTIFY_OPTIONS["max_preserve_newlines"] = 0
        }
    }

    private val scriptEngine: ScriptEngine

    /** JS beautify() function reference.  */
    private val jsBeautify: Any
    private val securedJsCache: SecuredJsCache?

    /** `true` when lack of braces is allowed.  */
    private val allowNoBraces: Boolean

    internal constructor(scriptEngine: ScriptEngine, maxPreparedStatements: Int, allowBraces: Boolean) {
        this.scriptEngine = scriptEngine
        allowNoBraces = allowBraces
        securedJsCache = createSecuredJsCache(maxPreparedStatements)
        assertScriptEngine()
        jsBeautify = getBeautifHandler(scriptEngine)
    }

    internal constructor(scriptEngine: ScriptEngine, allowBraces: Boolean, cache: SecuredJsCache?) {
        this.scriptEngine = scriptEngine
        allowNoBraces = allowBraces
        securedJsCache = cache
        assertScriptEngine()
        jsBeautify = getBeautifHandler(scriptEngine)
    }

    private fun assertScriptEngine() {
        try {
            scriptEngine.eval("var window = {};")
            scriptEngine.eval(beautifyJs)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun createSecuredJsCache(maxPreparedStatements: Int): SecuredJsCache? {
        // Create cache
        return if (maxPreparedStatements == 0) {
            null
        } else {
            newSecuredJsCache(maxPreparedStatements)
        }
    }

    private fun newSecuredJsCache(maxPreparedStatements: Int): SecuredJsCache {
        val linkedHashMap: LinkedHashMap<String, String?> = object : LinkedHashMap<String, String?>(maxPreparedStatements + 1, .75f, true) {

            // This method is called just after a new entry has been added
            override fun removeEldestEntry(eldest: Map.Entry<String?, String?>?): Boolean {
                return size > maxPreparedStatements
            }
        }
        return LinkedHashMapSecuredJsCache(linkedHashMap, allowNoBraces)
    }

    /**
     * After beautifyier every braces should be in place, if not, or too many we need
     * to prevent script execution.
     *
     * @param beautifiedJs
     * evaluated script
     * @throws BracesException
     * when braces are incorrect
     */
    @Throws(BracesException::class)
    fun checkBraces(beautifiedJs: String) {
        if (allowNoBraces) {
            return
        }
        for (pattern in LACK_EXPECTED_BRACES) {
            val matcher: Matcher = pattern.matcher(RemoveComments.perform(beautifiedJs))
            if (matcher.find()) {
                var line = ""
                var index: Int = matcher.start()
                while (index >= 0 && beautifiedJs[index] != '\n') {
                    line = beautifiedJs[index].toString() + line
                    index--
                }
                val singleParaCount = line.length - line.replace("'", "").length
                val doubleParaCount = line.length - line.replace("\"", "").length
                if (singleParaCount % 2 != 0 || doubleParaCount % 2 != 0) {
                    // for in string
                } else {
                    throw BracesException("No block braces after function|for|while|do. Found [" + matcher.group().toString() + "]")
                }
            }
        }
    }

    fun injectInterruptionCalls(str: String): String {
        var current = str
        for (pp in POISON_PILLS) {
            val sb = StringBuffer()
            val matcher: Matcher = pp.pattern.matcher(current)
            while (matcher.find()) {
                matcher.appendReplacement(sb, "$1" + pp.replacement)
            }
            matcher.appendTail(sb)
            current = sb.toString()
        }
        return current
    }

    private val preamble: String
        private get() {
            val clazzName: String = InterruptTest::class.java.getName()
            val sb = StringBuilder()
            sb.append("var ").append(JS_INTERRUPTED_TEST).append("=Java.type('").append(clazzName).append("');")
            sb.append("var ").append(JS_INTERRUPTED_FUNCTION).append("=function(){")
            sb.append(JS_INTERRUPTED_TEST).append(".test();};\n")
            return sb.toString()
        }

    private fun checkJs(js: String) {
        require(!(js.contains(JS_INTERRUPTED_FUNCTION) || js.contains(JS_INTERRUPTED_TEST))) { "Script contains the illegal string [$JS_INTERRUPTED_TEST,$JS_INTERRUPTED_FUNCTION]" }
    }

    @Throws(ScriptException::class)
    fun secureJs(js: String): String {
        if (securedJsCache == null) {
            return secureJsImpl(js)
        }
        val ex: ArrayList<ScriptException> = arrayListOf()
        val securedJs = securedJsCache.getOrCreate(js, allowNoBraces, Supplier {
            try {
                secureJsImpl(js)
            } catch (e: BracesException) {
                ex[0] = e
                null
            }
        })
        if (ex.isNotEmpty()) {
            throw ex[0]
        }
        return securedJs!!
    }

    @Throws(BracesException::class)
    private fun secureJsImpl(js: String): String {
        checkJs(js)
        val beautifiedJs = beautifyJs(js)
        checkBraces(beautifiedJs)
        val injectedJs = injectInterruptionCalls(beautifiedJs)
        // if no injection, no need to add preamble
        return if (beautifiedJs == injectedJs) {
            beautifiedJs
        } else {
            val preamble = preamble
            preamble + injectedJs
        }
    }

    fun beautifyJs(js: String): String {
        return if (jsBeautify is ScriptObjectMirror) {
            jsBeautify.call(
                "beautify",
                js,
                BEAUTIFY_OPTIONS
            ) as String
        } else if (jsBeautify is Function<*, *>) {
            (jsBeautify as Function<Array<Any>, Any>).apply(
                arrayOf(js, BEAUTIFY_OPTIONS)
            ) as String
        } else {
            throw RuntimeException("Unsupported handler type for jsBeautify: " + jsBeautify.javaClass.name)
        }
    }

    private fun getBeautifyJs(): String? {
        var script = beautifysScript.get()
        if (script == null) {
            try {
                BufferedReader(
                    InputStreamReader(
                        BufferedInputStream(JsSanitizer::class.java.getResourceAsStream(BEAUTIFY_JS)),
                        StandardCharsets.UTF_8
                    )
                ).use { reader ->
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line).append('\n')
                    }
                    script = sb.toString()
                }
            } catch (e: IOException) {
                throw RuntimeException("Cannot find file: $BEAUTIFY_JS", e)
            }
            beautifysScript = SoftReference(script)
        }
        return script
    }
}
