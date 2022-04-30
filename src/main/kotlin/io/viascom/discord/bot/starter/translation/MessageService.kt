package io.viascom.discord.bot.starter.translation

import java.util.*

interface MessageService {
    fun get(key: String, locale: Locale, vararg args: String): String
    fun getWithDefault(key: String, locale: Locale, default: String, vararg args: String): String
    fun formatNumber(number: Double, locale: Locale): String
}
