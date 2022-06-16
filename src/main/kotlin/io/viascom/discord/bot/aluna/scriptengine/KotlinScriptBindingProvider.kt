package io.viascom.discord.bot.aluna.scriptengine

/**
 * Kotlin script binding provider.
 * Implement this interface in order to add additional bindings and imports to the KotlinScriptService.
 *
 */
interface KotlinScriptBindingProvider {

    /**
     * Get binding definitions.
     *
     * @return List of Bindings.
     */
    fun getBindings(): List<KotlinScriptService.Binding>

    /**
     * Get import definitions
     *
     * @return List of imports
     */
    fun getImports(): List<String>
}
