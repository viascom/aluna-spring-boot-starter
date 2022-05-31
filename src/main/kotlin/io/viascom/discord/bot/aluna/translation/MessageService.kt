package io.viascom.discord.bot.aluna.translation

import java.util.*

interface MessageService {
    fun get(key: String, locale: Locale, vararg args: String): String
    fun getWithDefault(key: String, locale: Locale, default: String, vararg args: String): String
    fun formatNumber(number: Double, locale: Locale): String
}
