package io.viascom.discord.bot.aluna.configuration.scope

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ConfigurableApplicationContext

open class ScopeBeanFactoryPostProcessor(
    private val context: ConfigurableApplicationContext
) : BeanFactoryPostProcessor {

    override fun postProcessBeanFactory(factory: ConfigurableListableBeanFactory) {
        factory.registerScope("command", CommandScope(context))
    }

}
