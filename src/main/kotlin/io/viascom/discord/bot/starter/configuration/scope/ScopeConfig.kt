package io.viascom.discord.bot.starter.configuration.scope

import io.viascom.discord.bot.starter.property.AlunaProperties
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ScopeConfig {

    @Bean
    open fun beanFactoryPostProcessor(context: ConfigurableApplicationContext, alunaProperties: AlunaProperties): BeanFactoryPostProcessor {
        return ScopeBeanFactoryPostProcessor(context, alunaProperties)
    }

}
