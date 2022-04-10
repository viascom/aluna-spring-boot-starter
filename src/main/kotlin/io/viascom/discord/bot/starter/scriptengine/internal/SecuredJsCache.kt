package io.viascom.discord.bot.starter.scriptengine.internal

import java.util.function.Supplier

/**
 * A cache used to store pre-processed javascript strings, which can be used to share
 * these among different [NashornSandbox]es. The interface provides a facility
 * to implement concurrent caches, but the actual thread safety is at
 * the implementor's discretion.
 *
 */
interface SecuredJsCache {
    /**
     * Gets a value from the cache and tries to create it if it couldn't be found.
     * @param js the raw javascript code
     * @param allowNoBraces whether missing braces are allowed.
     * @param producer if no cached value could be found, this is used to create the value
     * @return the cached or created value, or null if it could be neither found in the cache nor created.
     */
    fun getOrCreate(js: String?, allowNoBraces: Boolean, producer: Supplier<String?>?): String?
}
