package io.viascom.discord.bot.aluna.property

import org.springframework.boot.convert.DurationUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

class AlunaTranslationProperties {

    /**
     * Enable Translation
     */
    var enabled: Boolean = false

    /**
     * Translation path
     *
     * Format: <code>file:/</code> or <code>classpath:</code>
     *
     * If not set, Aluna will fall back to <code>classpath:i18n/messages</code>
     */
    var basePath: String? = null

    /**
     * Use en_GB for en in production
     */
    var useEnGbForEnInProduction: Boolean = false

    /**
     * Set the default charset to use for parsing properties files.
     * Used if no file-specific charset is specified for a file.
     */
    var defaultEncoding: String = "UTF-8"

    /**
     * Duration to cache loaded properties files.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    var cacheDuration: Duration = Duration.ofSeconds(60)

    /**
     * Set whether to use the message code as default message instead of throwing a NoSuchMessageException.
     * Useful for development and debugging.
     */
    var useCodeAsDefaultMessage: Boolean = true

    /**
     * Set whether to fall back to the system Locale if no files for a specific Locale have been found.
     * If this is turned off, the only fallback will be the default file (e.g. "messages.properties" for basename "messages").
     */
    var fallbackToSystemLocale: Boolean = false
}
