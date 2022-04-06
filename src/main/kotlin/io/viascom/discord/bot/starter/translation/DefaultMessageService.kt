package io.viascom.discord.bot.starter.translation

import com.vdurmont.emoji.EmojiParser
import io.viascom.discord.bot.starter.property.AlunaProperties
import org.springframework.context.MessageSource
import java.text.NumberFormat
import java.util.*

class DefaultMessageService(
    private val messageSource: MessageSource,
    private val alunaProperties: AlunaProperties
) : MessageService {

    override fun get(key: String, language: String, vararg args: Any): String {
        val correctLanguage = if (alunaProperties.useEnGbForEnInProduction && language == "en" && alunaProperties.productionMode) "en-GB" else language
        var message = messageSource.getMessage(key, args, Locale.forLanguageTag(correctLanguage))
        message = message.replace("''", "'")
        message = message.replace("``", "`")
        return EmojiParser.parseToUnicode(message)
    }

    override fun get(key: String, locale: Locale, vararg args: Any): String = get(key, locale.language, args)

    override fun formatNumber(number: Double, locale: Locale): String {
        return NumberFormat.getNumberInstance(locale).format(number)
    }

}
