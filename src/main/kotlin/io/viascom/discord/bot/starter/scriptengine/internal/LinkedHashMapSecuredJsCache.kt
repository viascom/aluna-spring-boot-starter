package io.viascom.discord.bot.starter.scriptengine.internal

import java.util.function.Supplier

/**
 * Default implementation of [SecuredJsCache], uses a [LinkedHashMap]
 * as its cache and is not thread-safe. Also, mixing scripts with missing braces
 * allowed and not allowed is forbidden.
 */
class LinkedHashMapSecuredJsCache(private val map: LinkedHashMap<String, String?>, private val allowNoBraces: Boolean) : SecuredJsCache {
    override fun getOrCreate(js: String?, allowNoBraces: Boolean, producer: Supplier<String?>?): String? {
        assertConfiguration(allowNoBraces)
        var result = map[js]
        if (result == null) {
            result = producer?.get()
            map[js!!] = result
        }
        return result
    }

    private fun assertConfiguration(allowNoBraces: Boolean) {
        require(allowNoBraces == this.allowNoBraces) { "Non-matching cache configuration" }
    }
}
