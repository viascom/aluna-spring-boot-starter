package io.viascom.discord.bot.starter.configuration.scope

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.config.Scope
import org.springframework.core.NamedInheritableThreadLocal
import java.util.*


class CommandScope : Scope {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val scopedObjects = Collections.synchronizedMap(HashMap<String, HashMap<String, Pair<DiscordContext.Type, Any>>>())

    override fun get(name: String, objectFactory: ObjectFactory<*>): Any {
        //If state id is not set, a new instance is returned
        if (DiscordContext.discordState?.id == null) {
            return objectFactory.getObject()
        }

        //Reset current state if a command is requesting it
        if (scopedObjects.none { it.key == DiscordContext.discordState?.id } || DiscordContext.discordState?.type == DiscordContext.Type.COMMAND) {
            scopedObjects[DiscordContext.discordState?.id] = hashMapOf()
        }

        //Reset current state if an auto-complete is requesting it and last request was from a command
        if (scopedObjects.getOrElse(DiscordContext.discordState?.id) { hashMapOf() }?.getOrElse(name) { null }?.first == DiscordContext.Type.COMMAND &&
            DiscordContext.discordState?.type == DiscordContext.Type.AUTO_COMPLETE
        ) {
            scopedObjects[DiscordContext.discordState?.id] = hashMapOf()
        }

        //Create new instance if no instance got found, or it is a command
        if (scopedObjects.getOrElse(DiscordContext.discordState?.id) { hashMapOf() }
                ?.containsKey(name) != true || DiscordContext.discordState?.type == DiscordContext.Type.COMMAND) {
            scopedObjects[DiscordContext.discordState?.id]!![name] =
                Pair(DiscordContext.discordState?.type ?: DiscordContext.Type.OTHER, objectFactory.getObject())
        }

        return scopedObjects.getOrElse(DiscordContext.discordState?.id) { hashMapOf() }?.getOrElse(name) { null }?.second ?: objectFactory.getObject()
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
    fun setDiscordState(userId: String, serverId: String? = null, type: Type = Type.OTHER) {
        CONTEXT.set(Data(userId + ":" + (serverId ?: ""), type))
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
        val type: Type = Type.OTHER
    )

    enum class Type {
        COMMAND, AUTO_COMPLETE, OTHER
    }
}

