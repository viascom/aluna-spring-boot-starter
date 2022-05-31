package io.viascom.discord.bot.aluna.bot.handler
//
//import io.viascom.discord.bot.aluna.util.DiscordLocalization
//import net.dv8tion.jda.api.interactions.commands.LocalizationFunction
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//import org.springframework.context.MessageSource
//import org.springframework.context.NoSuchMessageException
//import org.springframework.context.support.MessageSourceAccessor
//import java.util.*
//
//class AlunaLocalizationFunction(
//    messageSource: MessageSource
//) : LocalizationFunction {
//
//    private val messageSourceAccessor: MessageSourceAccessor = MessageSourceAccessor(messageSource)
//
//    val logger: Logger = LoggerFactory.getLogger(javaClass)
//
//    override fun apply(localizationKey: String): MutableMap<Locale, String> {
//        val map: MutableMap<Locale, String> = hashMapOf()
//        val key = "interaction.$localizationKey"
//        logger.debug("Search translation for $key")
//        DiscordLocalization.values().forEach {
//            val i18nName = try {
//                messageSourceAccessor.getMessage(key, it.locale)
//            } catch (e: NoSuchMessageException) {
//                null
//            }
//            if (i18nName != null && i18nName != key) {
//                map[it.locale] = i18nName
//            }
//        }
//
//        return map
//    }
//}
