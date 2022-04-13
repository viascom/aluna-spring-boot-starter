package io.viascom.discord.bot.starter.configuration.scope

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ScopeConfig {

    @Bean
    open fun beanFactoryPostProcessor(context: ConfigurableApplicationContext): BeanFactoryPostProcessor {
        return ScopeBeanFactoryPostProcessor(context)
    }

}
