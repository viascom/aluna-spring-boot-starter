package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.bot.handler.DiscordCommand
import io.viascom.discord.bot.aluna.bot.handler.DiscordContextMenu
import io.viascom.discord.bot.aluna.model.ObserveCommandInteraction
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.AlunaThreadPool
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true, havingValue = "true")
open class DiscordBot(
    private val context: ConfigurableApplicationContext,
    private val alunaProperties: AlunaProperties
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    var shardManager: ShardManager? = null

    val commands = hashMapOf<String, Class<DiscordCommand>>()
    val contextMenus = hashMapOf<String, Class<DiscordContextMenu>>()
    val commandsWithAutocomplete = arrayListOf<String>()

    internal var messagesToObserveButton: MutableMap<String, ObserveCommandInteraction> =
        Collections.synchronizedMap(hashMapOf<String, ObserveCommandInteraction>())
        private set
    internal var messagesToObserveSelect: MutableMap<String, ObserveCommandInteraction> =
        Collections.synchronizedMap(hashMapOf<String, ObserveCommandInteraction>())
        private set
    internal var messagesToObserveModal: MutableMap<String, ObserveCommandInteraction> =
        Collections.synchronizedMap(hashMapOf<String, ObserveCommandInteraction>())
        private set

    internal val messagesToObserveScheduledThreadPool =
        AlunaThreadPool.getFixedScheduledThreadPool(
            alunaProperties.thread.messagesToObserveScheduledThreadPool,
            "Aluna-Message-Observer-Timeout-Pool-%d",
            true
        )

    internal val commandExecutor =
        AlunaThreadPool.getDynamicThreadPool(alunaProperties.thread.commandExecutorCount, alunaProperties.thread.commandExecutorTtl, "Aluna-Command-%d")
    internal val asyncExecutor =
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
                logger.debug("Could not run onButtonInteractionTimeout for command /${command.name}\"\n${e.stackTraceToString()}")
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
                    logger.debug("Could not run onButtonInteractionTimeout for command /${command.name}\"\n${e.stackTraceToString()}")
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
                logger.debug("Could not run onSelectMenuInteractionTimeout for command /${command.name}\"\n${e.stackTraceToString()}")
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
                    logger.debug("Could not run onSelectMenuInteractionTimeout for command /${command.name}\"\n${e.stackTraceToString()}")
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
                logger.debug("Could not run onModalInteractionTimeout for command /${command.name}\"\n${e.stackTraceToString()}")
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
