package io.viascom.discord.bot.starter.translation

import java.util.*

interface MessageService {

    fun get(key: String, language: String, vararg args: Any): String
    fun get(key: String, locale: Locale, vararg args: Any): String
    fun formatNumber(number: Double, locale: Locale): String
}
