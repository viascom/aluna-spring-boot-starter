package io.viascom.discord.bot.aluna.translation

import com.vdurmont.emoji.EmojiParser
import io.viascom.discord.bot.aluna.property.AlunaProperties
import org.springframework.context.MessageSource
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import java.text.NumberFormat
import java.util.*


class DefaultMessageService(
    private val messageSource: MessageSource,
    private val reloadableMessageSource: ReloadableResourceBundleMessageSource,
    private val alunaProperties: AlunaProperties
) : MessageService {

    override fun get(key: String, locale: Locale, vararg args: String): String {
        val correctLocale = if (alunaProperties.useEnGbForEnInProduction && locale == Locale.ENGLISH && alunaProperties.productionMode) {
            Locale.forLanguageTag("en-GB")
        } else {
            locale
        }

        var message = messageSource.getMessage(key, args, correctLocale)
        message = message.replace("''", "'")
        message = message.replace("``", "`")
        return EmojiParser.parseToUnicode(message)
    }

    override fun getWithDefault(key: String, locale: Locale, default: String, vararg args: String): String {
        val correctLocale = if (alunaProperties.useEnGbForEnInProduction && locale == Locale.ENGLISH && alunaProperties.productionMode) {
            Locale.forLanguageTag("en-GB")
        } else {
            locale
        }
        var message = reloadableMessageSource.getMessage(key, args, default, correctLocale)
        message = message.replace("''", "'")
        message = message.replace("``", "`")
        return EmojiParser.parseToUnicode(message)
    }

    override fun formatNumber(number: Double, locale: Locale): String {
        return NumberFormat.getNumberInstance(locale).format(number)
    }

}
