package io.viascom.discord.bot.starter.configuration.scope

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory

open class ScopeBeanFactoryPostProcessor : BeanFactoryPostProcessor {

    override fun postProcessBeanFactory(factory: ConfigurableListableBeanFactory) {
        factory.registerScope("command", CommandScope())
    }

}
