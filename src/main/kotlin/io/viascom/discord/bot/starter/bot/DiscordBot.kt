package io.viascom.discord.bot.starter.bot

import io.viascom.discord.bot.starter.bot.handler.DiscordCommand
import io.viascom.discord.bot.starter.util.AlunaThreadPool
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.reflect.KClass

@Service
open class DiscordBot {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    var shardManager: ShardManager? = null
        private set

    val commands = hashMapOf<String, Class<DiscordCommand>>()
    val commandsWithAutocomplete = arrayListOf<String>()

    var messagesToObserveButton = hashMapOf<String, Triple<KClass<out DiscordCommand>, LocalDateTime, Boolean>>()
        private set
    var messagesToObserveSelect = hashMapOf<String, Triple<KClass<out DiscordCommand>, LocalDateTime, Boolean>>()
        private set

    val commandExecutor = AlunaThreadPool.getDynamicThreadPool(100, 30, "Aluna-Command-%d")
    val asyncExecutor = AlunaThreadPool.getDynamicThreadPool(100, 10, "Aluna-Async-%d")

    fun registerMessageForButtonEvents(messageId: String, command: DiscordCommand, persist: Boolean = false) {
        logger.debug("Register message $messageId for button events to command /${command.name}")
        messagesToObserveButton[messageId] = Triple(command::class, LocalDateTime.now(), persist)
    }

    fun registerMessageForButtonEvents(hook: InteractionHook, command: DiscordCommand, persist: Boolean = false) =
        hook.retrieveOriginal().queue { registerMessageForButtonEvents(it.id, command, persist) }

    fun registerMessageForSelectEvents(messageId: String, command: DiscordCommand, persist: Boolean = false) {
        logger.debug("Register message $messageId for select events to command /${command.name}")
        messagesToObserveSelect[messageId] = Triple(command::class, LocalDateTime.now(), persist)
    }

    fun registerMessageForSelectEvents(hook: InteractionHook, command: DiscordCommand, persist: Boolean = false) =
        hook.retrieveOriginal().queue { registerMessageForSelectEvents(it.id, command, persist) }

    fun removeMessageForButtonEvents(messageId: String) = messagesToObserveButton.remove(messageId)

    fun removeMessageForSelectEvents(messageId: String) = messagesToObserveSelect.remove(messageId)

}
