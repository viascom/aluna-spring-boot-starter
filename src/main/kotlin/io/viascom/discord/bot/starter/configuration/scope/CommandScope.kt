package io.viascom.discord.bot.starter.configuration.scope

import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.config.Scope

class CommandScope : Scope {

    override fun get(name: String, objectFactory: ObjectFactory<*>): Any {
        return objectFactory.getObject()
    }

    override fun remove(name: String): Any? {
        return null
    }

    override fun registerDestructionCallback(name: String, callback: Runnable) {
    }

    override fun resolveContextualObject(key: String): Any? {
        return null
    }

    override fun getConversationId(): String? {
        return null
    }
}
