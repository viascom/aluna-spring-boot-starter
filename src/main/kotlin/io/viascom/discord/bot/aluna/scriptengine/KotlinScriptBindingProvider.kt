package io.viascom.discord.bot.aluna.scriptengine

import kotlin.reflect.KClass

interface KotlinScriptBindingProvider {

    fun getBindings(): List<Triple<String, Any, KClass<out Any>>>
    fun getImports(): List<String>

}
