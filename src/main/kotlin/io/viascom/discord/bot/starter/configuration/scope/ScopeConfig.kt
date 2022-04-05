package io.viascom.discord.bot.starter.configuration.scope

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ScopeConfig {

    @Bean
    open fun beanFactoryPostProcessor(): BeanFactoryPostProcessor {
        return ScopeBeanFactoryPostProcessor()
    }

}
