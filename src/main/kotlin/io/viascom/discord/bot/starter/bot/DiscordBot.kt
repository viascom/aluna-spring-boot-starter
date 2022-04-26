package io.viascom.discord.bot.starter.bot

import io.viascom.discord.bot.starter.bot.handler.DiscordCommand
import io.viascom.discord.bot.starter.bot.handler.DiscordContextMenu
import io.viascom.discord.bot.starter.model.ObserveCommandInteraction
import io.viascom.discord.bot.starter.property.AlunaProperties
import io.viascom.discord.bot.starter.util.AlunaThreadPool
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@Service
open class DiscordBot(
    private val context: ConfigurableApplicationContext,
    private val alunaProperties: AlunaProperties
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    var shardManager: ShardManager? = null
        private set

    val commands = hashMapOf<String, Class<DiscordCommand>>()
    val contextMenus = hashMapOf<String, Class<DiscordContextMenu>>()
    val commandsWithAutocomplete = arrayListOf<String>()

    var messagesToObserveButton: MutableMap<String, ObserveCommandInteraction> = Collections.synchronizedMap(hashMapOf<String, ObserveCommandInteraction>())
        private set
    var messagesToObserveSelect: MutableMap<String, ObserveCommandInteraction> = Collections.synchronizedMap(hashMapOf<String, ObserveCommandInteraction>())
        private set
    var messagesToObserveModal: MutableMap<String, ObserveCommandInteraction> = Collections.synchronizedMap(hashMapOf<String, ObserveCommandInteraction>())
        private set

    private val messagesToObserveScheduledThreadPool =
        AlunaThreadPool.getScheduledThreadPool(alunaProperties.thread.messagesToObserveScheduledThreadPool, "Aluna-Message-Observer-Timeout-Pool-%d", true)

    val commandExecutor =
        AlunaThreadPool.getDynamicThreadPool(alunaProperties.thread.commandExecutorCount, alunaProperties.thread.commandExecutorTtl, "Aluna-Command-%d")
    val asyncExecutor =
        AlunaThreadPool.getDynamicThreadPool(alunaProperties.thread.asyncExecutorCount, alunaProperties.thread.asyncExecutorTtl, "Aluna-Async-%d")

    fun registerMessageForButtonEvents(
        messageId: String,
        command: DiscordCommand,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        commandUserOnly: Boolean = false
    ) {
        logger.debug("Register message $messageId for button events to command /${command.name}" + if (commandUserOnly) " (only specified users can use it)" else "")
        if (!additionalData.containsKey("message.id")) {
            additionalData["message.id"] = messageId
        }

        val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
            try {
                context.getBean(command::class.java).onButtonInteractionTimeout(additionalData)
            } catch (e: Exception) {
            }
            removeMessageForButtonEvents(messageId)
        }, duration.seconds, TimeUnit.SECONDS)

        messagesToObserveButton[messageId] =
            ObserveCommandInteraction(
                command::class,
                command.uniqueId,
                LocalDateTime.now(),
                duration,
                persist,
                additionalData,
                authorIds,
                commandUserOnly,
                timeoutTask
            )
    }

    fun registerMessageForButtonEvents(
        hook: InteractionHook,
        command: DiscordCommand,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        commandUserOnly: Boolean = false
    ) {
        hook.retrieveOriginal().queue { message ->
            logger.debug("Register message ${message.id} for button events to command /${command.name}" + if (commandUserOnly) " (only specified users can use it)" else "")
            if (!additionalData.containsKey("message.id")) {
                additionalData["message.id"] = message.id
            }
            val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
                try {
                    context.getBean(command::class.java).onButtonInteractionTimeout(additionalData)
                } catch (e: Exception) {
                }
                removeMessageForButtonEvents(message.id)
            }, duration.seconds, TimeUnit.SECONDS)

            messagesToObserveButton[message.id] =
                ObserveCommandInteraction(
                    command::class,
                    command.uniqueId,
                    LocalDateTime.now(),
                    duration,
                    persist,
                    additionalData,
                    authorIds,
                    commandUserOnly,
                    timeoutTask
                )
        }
    }

    fun registerMessageForSelectEvents(
        messageId: String,
        command: DiscordCommand,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        commandUserOnly: Boolean = false
    ) {
        logger.debug("Register message $messageId for select events to command /${command.name}" + if (commandUserOnly) " (only specified users can use it)" else "")
        if (!additionalData.containsKey("message.id")) {
            additionalData["message.id"] = messageId
        }
        val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
            try {
                context.getBean(command::class.java).onSelectMenuInteractionTimeout(additionalData)
            } catch (e: Exception) {
            }
            removeMessageForSelectEvents(messageId)
        }, duration.seconds, TimeUnit.SECONDS)

        messagesToObserveSelect[messageId] =
            ObserveCommandInteraction(
                command::class,
                command.uniqueId,
                LocalDateTime.now(),
                duration,
                persist,
                additionalData,
                authorIds,
                commandUserOnly,
                timeoutTask
            )
    }

    fun registerMessageForSelectEvents(
        hook: InteractionHook,
        command: DiscordCommand,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        commandUserOnly: Boolean = false
    ) {
        hook.retrieveOriginal().queue { message ->
            logger.debug("Register message ${message.id} for select events to command /${command.name}" + if (commandUserOnly) " (only specified users can use it)" else "")
            if (!additionalData.containsKey("message.id")) {
                additionalData["message.id"] = message.id
            }
            val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
                try {
                    context.getBean(command::class.java).onSelectMenuInteractionTimeout(additionalData)
                } catch (e: Exception) {
                }
                removeMessageForSelectEvents(message.id)
            }, duration.seconds, TimeUnit.SECONDS)

            messagesToObserveSelect[message.id] =
                ObserveCommandInteraction(
                    command::class,
                    command.uniqueId,
                    LocalDateTime.now(),
                    duration,
                    persist,
                    additionalData,
                    authorIds,
                    commandUserOnly,
                    timeoutTask
                )
        }
    }

    fun registerMessageForModalEvents(
        authorId: String,
        command: DiscordCommand,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf()
    ) {
        logger.debug("Register user $authorId for modal events to command /${command.name}")
        val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
            try {
                context.getBean(command::class.java).onModalInteractionTimeout(additionalData)
            } catch (e: Exception) {
            }
            removeMessageForModalEvents(authorId)
        }, duration.seconds, TimeUnit.SECONDS)

        messagesToObserveModal[authorId] =
            ObserveCommandInteraction(
                command::class,
                command.uniqueId,
                LocalDateTime.now(),
                duration,
                false,
                additionalData,
                arrayListOf(authorId),
                true,
                timeoutTask
            )
    }

    fun removeMessageForButtonEvents(messageId: String) = messagesToObserveButton.remove(messageId)
    fun removeMessageForSelectEvents(messageId: String) = messagesToObserveSelect.remove(messageId)
    fun removeMessageForModalEvents(userId: String) = messagesToObserveModal.remove(userId)

}
