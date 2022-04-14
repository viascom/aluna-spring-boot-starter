package io.viascom.discord.bot.starter.configuration.scope

import io.viascom.discord.bot.starter.property.AlunaProperties
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ConfigurableApplicationContext

open class ScopeBeanFactoryPostProcessor(
    private val context: ConfigurableApplicationContext,
    private val alunaProperties: AlunaProperties
) : BeanFactoryPostProcessor {

    override fun postProcessBeanFactory(factory: ConfigurableListableBeanFactory) {
        factory.registerScope("command", CommandScope(context, alunaProperties))
    }

}
