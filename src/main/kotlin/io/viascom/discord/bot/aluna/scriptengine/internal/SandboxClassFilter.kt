package io.viascom.discord.bot.aluna.scriptengine.internal

import org.openjdk.nashorn.api.scripting.ClassFilter

class SandboxClassFilter : ClassFilter {
    private val allowed: HashSet<Class<*>> = hashSetOf()
    private val stringCache: HashSet<String> = hashSetOf()

    override fun exposeToScripts(className: String): Boolean {
        return stringCache.contains(className)
    }

    fun add(clazz: Class<*>) {
        allowed.add(clazz)
        stringCache.add(clazz.name)
    }

    fun remove(clazz: Class<*>) {
        allowed.remove(clazz)
        stringCache.remove(clazz.name)
    }

    fun clear() {
        allowed.clear()
        stringCache.clear()
    }

    operator fun contains(clazz: Class<*>): Boolean {
        return allowed.contains(clazz)
    }
}
