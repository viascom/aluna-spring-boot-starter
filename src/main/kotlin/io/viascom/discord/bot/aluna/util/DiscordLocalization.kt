package io.viascom.discord.bot.aluna.util

import java.util.*

/**
 * Discord localization based on [discord docs](https://discord.com/developers/docs/reference#locales)
 */
enum class DiscordLocalization(val id: String, val locale: Locale, val languageName: String, val nativeName: String) {
    DA("da", Locale.forLanguageTag("da"), "Danish", "Dansk"),
    DE("de", Locale.forLanguageTag("de"), "German", "Deutsch"),
    EN_GB("en-GB", Locale.forLanguageTag("en-GB"), "English, UK", "English, UK"),
    EN_US("en-US", Locale.forLanguageTag("en-US"), "English, US", "English, US"),
    ES("es-ES", Locale.forLanguageTag("es-ES"), "Spanish", "Español"),
    FR("fr", Locale.forLanguageTag("fr"), "French", "Français"),
    HR("hr", Locale.forLanguageTag("hr"), "Croatian", "Hrvatski"),
    IT("it", Locale.forLanguageTag("it"), "Italian", "Italiano"),
    LT("lt", Locale.forLanguageTag("lt"), "Lithuanian", "Lietuviškai"),
    HU("hu", Locale.forLanguageTag("hu"), "Hungarian", "Magyar"),
    NL("nl", Locale.forLanguageTag("nl"), "Dutch", "Nederlands"),
    NO("no", Locale.forLanguageTag("no"), "Norwegian", "Norsk"),
    PL("pl", Locale.forLanguageTag("pl"), "Polish", "Polski"),
    PT("pt-BR", Locale.forLanguageTag("pt-BR"), "Portuguese, Brazilian", "Português do Brasil"),
    RO("ro", Locale.forLanguageTag("ro"), "Romanian, Romania", "Română"),
    FI("fi", Locale.forLanguageTag("fi"), "Finnish", "Suomi"),
    SV("sv-SE", Locale.forLanguageTag("sv-SE"), "Swedish", "Svenska"),
    VI("vi", Locale.forLanguageTag("vi"), "Vietnamese", "Tiếng Việt"),
    TR("tr", Locale.forLanguageTag("tr"), "Turkish", "Türkçe"),
    CS("cs", Locale.forLanguageTag("cs"), "Czech", "Čeština"),
    EL("el", Locale.forLanguageTag("el"), "Greek", "Ελληνικά"),
    BG("bg", Locale.forLanguageTag("bg"), "Bulgarian", "български"),
    RU("ru", Locale.forLanguageTag("ru"), "Russian", "Pусский"),
    UK("uk", Locale.forLanguageTag("uk"), "Ukrainian", "Українська"),
    HI("hi", Locale.forLanguageTag("hi"), "Hindi", "हिन्दी"),
    TH("th", Locale.forLanguageTag("th"), "Thai", "ไทย"),
    ZH_CN("zh-CN", Locale.forLanguageTag("zh-CN"), "Chinese, China", "中文"),
    JA("ja", Locale.forLanguageTag("ja"), "Japanese", "日本語"),
    ZH_TW("zh-TW", Locale.forLanguageTag("zh-TW"), "Chinese, Taiwan", "繁體中文"),
    KO("ko", Locale.forLanguageTag("ko"), "Korean", "한국어"),
}