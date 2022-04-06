package io.viascom.discord.bot.starter.configuration

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.env.Environment

@Configuration
open class MessageSourceConfig {

    @Bean
    open fun messageSource(environment: Environment): MessageSource {
        val translationPath = environment.getProperty("aluna.translation-path") ?: "classpath:i18n/messages"
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasename(translationPath)
        messageSource.setDefaultEncoding("UTF-8")
        messageSource.setCacheSeconds(60)
        messageSource.setUseCodeAsDefaultMessage(true)
        messageSource.setFallbackToSystemLocale(false)
        return messageSource
    }

}
