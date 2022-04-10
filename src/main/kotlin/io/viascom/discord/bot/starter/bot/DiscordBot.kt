package io.viascom.discord.bot.starter.bot

import io.viascom.discord.bot.starter.bot.handler.DiscordCommand
import io.viascom.discord.bot.starter.bot.handler.DiscordContextMenu
import io.viascom.discord.bot.starter.model.ObserveCommandInteraction
import io.viascom.discord.bot.starter.util.AlunaThreadPool
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
open class DiscordBot(
    private val context: ConfigurableApplicationContext
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    var shardManager: ShardManager? = null
        private set

    val commands = hashMapOf<String, Class<DiscordCommand>>()
    val contextMenus = hashMapOf<String, Class<DiscordContextMenu>>()
    val commandsWithAutocomplete = arrayListOf<String>()

    var messagesToObserveButton = hashMapOf<String, ObserveCommandInteraction>()
        private set
    var messagesToObserveSelect = hashMapOf<String, ObserveCommandInteraction>()
        private set

    val messagesToObserveScheduledThreadPool = AlunaThreadPool.getScheduledThreadPool(50, "Aluna-Message-Observer-Timeout-Pool-%d")

    val commandExecutor = AlunaThreadPool.getDynamicThreadPool(100, 30, "Aluna-Command-%d")
    val asyncExecutor = AlunaThreadPool.getDynamicThreadPool(100, 10, "Aluna-Async-%d")

    fun registerMessageForButtonEvents(
        messageId: String,
        command: DiscordCommand,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf()
    ) {
        logger.debug("Register message $messageId for button events to command /${command.name}")
        if (!additionalData.containsKey("message.id")) {
            additionalData["message.id"] = messageId
        }
        messagesToObserveButton[messageId] = ObserveCommandInteraction(command::class, LocalDateTime.now(), duration, persist, null, additionalData)
        messagesToObserveScheduledThreadPool.schedule({
            try {
                context.getBean(command::class.java).onButtonInteractionTimeout(null, additionalData)
            } catch (e: Exception) {
            }
            removeMessageForButtonEvents(messageId)
        }, duration.seconds, TimeUnit.SECONDS)
    }

    fun registerMessageForButtonEvents(
        hook: InteractionHook,
        command: DiscordCommand,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf()
    ) {
        hook.retrieveOriginal().queue { message ->
            logger.debug("Register message ${message.id} for button events to command /${command.name}")
            if (!additionalData.containsKey("message.id")) {
                additionalData["message.id"] = message.id
            }
            messagesToObserveButton[message.id] = ObserveCommandInteraction(command::class, LocalDateTime.now(), duration, persist, hook, additionalData)
            messagesToObserveScheduledThreadPool.schedule({
                try {
                    context.getBean(command::class.java).onButtonInteractionTimeout(hook, additionalData)
                } catch (e: Exception) {
                }
                removeMessageForButtonEvents(message.id)
            }, duration.seconds, TimeUnit.SECONDS)
        }
    }

    fun registerMessageForSelectEvents(
        messageId: String,
        command: DiscordCommand,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf()
    ) {
        logger.debug("Register message $messageId for select events to command /${command.name}")
        if (!additionalData.containsKey("message.id")) {
            additionalData["message.id"] = messageId
        }
        messagesToObserveSelect[messageId] = ObserveCommandInteraction(command::class, LocalDateTime.now(), duration, persist, null, additionalData)
        messagesToObserveScheduledThreadPool.schedule({
            try {
                context.getBean(command::class.java).onSelectMenuInteractionTimeout(null, additionalData)
            } catch (e: Exception) {
            }
            removeMessageForButtonEvents(messageId)
        }, duration.seconds, TimeUnit.SECONDS)
    }

    fun registerMessageForSelectEvents(
        hook: InteractionHook,
        command: DiscordCommand,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf()
    ) {
        hook.retrieveOriginal().queue { message ->
            logger.debug("Register message ${message.id} for select events to command /${command.name}")
            if (!additionalData.containsKey("message.id")) {
                additionalData["message.id"] = message.id
            }
            messagesToObserveSelect[message.id] = ObserveCommandInteraction(command::class, LocalDateTime.now(), duration, persist, hook, additionalData)
            messagesToObserveScheduledThreadPool.schedule({
                try {
                    context.getBean(command::class.java).onSelectMenuInteractionTimeout(hook, additionalData)
                } catch (e: Exception) {
                }
                removeMessageForButtonEvents(message.id)
            }, duration.seconds, TimeUnit.SECONDS)
        }
    }

    fun removeMessageForButtonEvents(messageId: String) = messagesToObserveButton.remove(messageId)

    fun removeMessageForSelectEvents(messageId: String) = messagesToObserveSelect.remove(messageId)

}
