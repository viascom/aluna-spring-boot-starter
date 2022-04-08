package io.viascom.discord.bot.starter.configuration.scope

import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.config.Scope
import org.springframework.core.NamedInheritableThreadLocal
import java.util.*


class CommandScope : Scope {

    private val scopedObjects = Collections.synchronizedMap(HashMap<String, HashMap<String, Any>>())

    override fun get(name: String, objectFactory: ObjectFactory<*>): Any {
        if (scopedObjects.none { it.key == DiscordContext.discordState?.id }) {
            scopedObjects[DiscordContext.discordState?.id] = hashMapOf()
        }

        if (!scopedObjects[DiscordContext.discordState?.id]!!.containsKey(name) || DiscordContext.discordState?.isCommandEvent == true) {
            scopedObjects[DiscordContext.discordState?.id] = hashMapOf()
            scopedObjects[DiscordContext.discordState?.id]!![name] = objectFactory.getObject()
        }

        return scopedObjects[DiscordContext.discordState?.id]!![name]!!
    }

    override fun remove(name: String): Any? {
        return scopedObjects.remove(DiscordContext.discordState?.id)
    }

    override fun registerDestructionCallback(name: String, callback: Runnable) {
    }

    override fun resolveContextualObject(key: String): Any? {
        return null
    }

    override fun getConversationId(): String {
        return DiscordContext.discordState?.id ?: ""
    }
}

object DiscordContext {
    private val CONTEXT = NamedInheritableThreadLocal<Data>("discord")
    fun setDiscordState(userId: String, serverId: String? = null, isCommandEvent: Boolean = false) {
        CONTEXT.set(Data(userId + ":" + (serverId ?: ""), isCommandEvent))
    }

    var discordState: Data?
        get() = CONTEXT.get()
        set(discordStateId) {
            CONTEXT.set(discordStateId)
        }

    fun clear() {
        CONTEXT.remove()
    }

    class Data(
        val id: String,
        val isCommandEvent: Boolean = false
    )
}

