/*
 * Copyright 2022 Viascom Ltd liab. Co
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.model.ObserveCommandInteraction
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.AlunaThreadPool
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

@Service
@ConditionalOnJdaEnabled
open class DiscordBot(
    private val context: ConfigurableApplicationContext,
    private val alunaProperties: AlunaProperties
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    var shardManager: ShardManager? = null

    val commands = hashMapOf<String, Class<DiscordCommand>>()
    val contextMenus = hashMapOf<String, Class<DiscordContextMenu>>()
    val commandsWithAutocomplete = arrayListOf<String>()
    val autoCompleteHandlers = hashMapOf<Pair<String, String?>, Class<out AutoCompleteHandler>>()

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var messagesToObserveButton: MutableMap<String, ObserveCommandInteraction> =
        Collections.synchronizedMap(hashMapOf<String, ObserveCommandInteraction>())

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var messagesToObserveSelect: MutableMap<String, ObserveCommandInteraction> =
        Collections.synchronizedMap(hashMapOf<String, ObserveCommandInteraction>())

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var messagesToObserveModal: MutableMap<String, ObserveCommandInteraction> =
        Collections.synchronizedMap(hashMapOf<String, ObserveCommandInteraction>())

    @get:JvmSynthetic
    internal val messagesToObserveScheduledThreadPool =
        AlunaThreadPool.getScheduledThreadPool(
            1,
            alunaProperties.thread.messagesToObserveScheduledThreadPool,
            Duration.ofSeconds(30),
            "Aluna-Message-Observer-Timeout-Pool-%d",
            true
        )

    @get:JvmSynthetic
    internal val commandExecutor =
        AlunaThreadPool.getDynamicThreadPool(alunaProperties.thread.commandExecutorCount, alunaProperties.thread.commandExecutorTtl, "Aluna-Command-%d")

    @get:JvmSynthetic
    internal val asyncExecutor =
        AlunaThreadPool.getDynamicThreadPool(alunaProperties.thread.asyncExecutorCount, alunaProperties.thread.asyncExecutorTtl, "Aluna-Async-%d")

    @JvmOverloads
    fun registerMessageForButtonEvents(
        messageId: String,
        command: DiscordInteractionHandler,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        commandUserOnly: Boolean = false
    ) {
        logger.debug("Register message $messageId for button events to interaction '${command.interactionName}'" + if (commandUserOnly) " (only specified users can use it)" else "")
        if (!additionalData.containsKey("message.id")) {
            additionalData["message.id"] = messageId
        }

        val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
            try {
                context.getBean(command::class.java).onButtonInteractionTimeout(additionalData)
            } catch (e: Exception) {
                logger.debug("Could not run onButtonInteractionTimeout for interaction '${command.interactionName}'\"\n${e.stackTraceToString()}")
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

    @JvmOverloads
    fun registerMessageForButtonEvents(
        hook: InteractionHook,
        command: DiscordInteractionHandler,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        commandUserOnly: Boolean = false
    ) {
        hook.retrieveOriginal().queue { message ->
            logger.debug("Register message ${message.id} for button events to interaction '${command.interactionName}'" + if (commandUserOnly) " (only specified users can use it)" else "")
            if (!additionalData.containsKey("message.id")) {
                additionalData["message.id"] = message.id
            }
            val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
                try {
                    context.getBean(command::class.java).onButtonInteractionTimeout(additionalData)
                } catch (e: Exception) {
                    logger.debug("Could not run onButtonInteractionTimeout for interaction '${command.interactionName}'\"\n${e.stackTraceToString()}")
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

    @JvmOverloads
    fun registerMessageForSelectEvents(
        messageId: String,
        command: DiscordInteractionHandler,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        commandUserOnly: Boolean = false
    ) {
        logger.debug("Register message $messageId for select events to interaction '${command.interactionName}'" + if (commandUserOnly) " (only specified users can use it)" else "")
        if (!additionalData.containsKey("message.id")) {
            additionalData["message.id"] = messageId
        }
        val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
            try {
                context.getBean(command::class.java).onSelectMenuInteractionTimeout(additionalData)
            } catch (e: Exception) {
                logger.debug("Could not run onSelectMenuInteractionTimeout for interaction '${command.interactionName}'\"\n${e.stackTraceToString()}")
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

    @JvmOverloads
    fun registerMessageForSelectEvents(
        hook: InteractionHook,
        command: DiscordInteractionHandler,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        commandUserOnly: Boolean = false
    ) {
        hook.retrieveOriginal().queue { message ->
            logger.debug("Register message ${message.id} for select events to interaction '${command.interactionName}'" + if (commandUserOnly) " (only specified users can use it)" else "")
            if (!additionalData.containsKey("message.id")) {
                additionalData["message.id"] = message.id
            }
            val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
                try {
                    context.getBean(command::class.java).onSelectMenuInteractionTimeout(additionalData)
                } catch (e: Exception) {
                    logger.debug("Could not run onSelectMenuInteractionTimeout for interaction '${command.interactionName}'\"\n${e.stackTraceToString()}")
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

    @JvmOverloads
    fun registerMessageForModalEvents(
        authorId: String,
        command: DiscordInteractionHandler,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf()
    ) {
        logger.debug("Register user $authorId for modal events to interaction '${command.interactionName}'")
        val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
            try {
                context.getBean(command::class.java).onModalInteractionTimeout(additionalData)
            } catch (e: Exception) {
                logger.debug("Could not run onModalInteractionTimeout for interaction '${command.interactionName}'\"\n${e.stackTraceToString()}")
            }
            removeMessageForModalEvents(authorId)
        }, duration.seconds, TimeUnit.SECONDS)

        messagesToObserveModal[authorId] =
            ObserveCommandInteraction(
                command::class,
                command.uniqueId,
                LocalDateTime.now(),
                duration,
                persist,
                additionalData,
                arrayListOf(authorId),
                true,
                timeoutTask
            )
    }

    fun removeMessageForButtonEvents(messageId: String) = messagesToObserveButton.remove(messageId)
    fun removeMessageForSelectEvents(messageId: String) = messagesToObserveSelect.remove(messageId)
    fun removeMessageForModalEvents(userId: String) = messagesToObserveModal.remove(userId)

    @JvmOverloads
    fun <T : Any> queueAndRegisterInteraction(
        action: RestAction<T>,
        hook: InteractionHook,
        command: DiscordInteractionHandler,
        type: ArrayList<DiscordCommand.EventRegisterType> = arrayListOf(DiscordCommand.EventRegisterType.BUTTON),
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = arrayListOf(command.author.id),
        commandUserOnly: Boolean = true,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in T>? = null
    ) {
        action.queue({
            if (type.contains(DiscordCommand.EventRegisterType.BUTTON)) {
                this.registerMessageForButtonEvents(hook, command, persist, duration, additionalData, authorIds, commandUserOnly)
            }
            if (type.contains(DiscordCommand.EventRegisterType.SELECT)) {
                this.registerMessageForSelectEvents(hook, command, persist, duration, additionalData, authorIds, commandUserOnly)
            }
            if (type.contains(DiscordCommand.EventRegisterType.MODAL)) {
                this.registerMessageForModalEvents(command.author.id, command, persist, duration, additionalData)
            }
            success?.accept(it)
        }, {
            failure?.accept(it)
        })
    }

    @JvmOverloads
    fun queueAndRegisterInteraction(
        action: RestAction<Void>,
        command: DiscordInteractionHandler,
        type: ArrayList<DiscordCommand.EventRegisterType> = arrayListOf(DiscordCommand.EventRegisterType.MODAL),
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in Void>? = null
    ) {
        action.queue({
            if (type.contains(DiscordCommand.EventRegisterType.MODAL)) {
                this.registerMessageForModalEvents(command.author.id, command, persist, duration, additionalData)
            }
            success?.accept(it)
        }, {
            failure?.accept(it)
        })
    }

    @JvmOverloads
    fun queueAndRegisterInteraction(
        action: ReplyCallbackAction,
        command: DiscordInteractionHandler,
        type: ArrayList<DiscordCommand.EventRegisterType> = arrayListOf(DiscordCommand.EventRegisterType.BUTTON),
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = arrayListOf(command.author.id),
        commandUserOnly: Boolean = true,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in InteractionHook>? = null
    ) {
        action.queue({
            if (type.contains(DiscordCommand.EventRegisterType.BUTTON)) {
                this.registerMessageForButtonEvents(it, command, persist, duration, additionalData, authorIds, commandUserOnly)
            }
            if (type.contains(DiscordCommand.EventRegisterType.SELECT)) {
                this.registerMessageForSelectEvents(it, command, persist, duration, additionalData, authorIds, commandUserOnly)
            }
            if (type.contains(DiscordCommand.EventRegisterType.MODAL)) {
                this.registerMessageForModalEvents(command.author.id, command, persist, duration, additionalData)
            }
            success?.accept(it)
        }, {
            failure?.accept(it)
        })
    }
}

fun <T : Any> RestAction<T>.queueAndRegisterInteraction(
    hook: InteractionHook,
    command: DiscordInteractionHandler,
    type: ArrayList<DiscordCommand.EventRegisterType> = arrayListOf(DiscordCommand.EventRegisterType.BUTTON),
    persist: Boolean = false,
    duration: Duration = Duration.ofMinutes(15),
    additionalData: HashMap<String, Any?> = hashMapOf(),
    authorIds: ArrayList<String>? = arrayListOf(command.author.id),
    commandUserOnly: Boolean = true,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in T>? = null
) = command.discordBot.queueAndRegisterInteraction(this, hook, command, type, persist, duration, additionalData, authorIds, commandUserOnly, failure, success)

fun RestAction<Void>.queueAndRegisterInteraction(
    command: DiscordInteractionHandler,
    type: ArrayList<DiscordCommand.EventRegisterType> = arrayListOf(DiscordCommand.EventRegisterType.MODAL),
    persist: Boolean = false,
    duration: Duration = Duration.ofMinutes(15),
    additionalData: HashMap<String, Any?> = hashMapOf(),
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in Void>? = null
) = command.discordBot.queueAndRegisterInteraction(this, command, type, persist, duration, additionalData, failure, success)

fun ReplyCallbackAction.queueAndRegisterInteraction(
    command: DiscordInteractionHandler,
    type: ArrayList<DiscordCommand.EventRegisterType> = arrayListOf(DiscordCommand.EventRegisterType.BUTTON),
    persist: Boolean = false,
    duration: Duration = Duration.ofMinutes(15),
    additionalData: HashMap<String, Any?> = hashMapOf(),
    authorIds: ArrayList<String>? = arrayListOf(command.author.id),
    commandUserOnly: Boolean = true,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in InteractionHook>? = null
) = command.discordBot.queueAndRegisterInteraction(this, command, type, persist, duration, additionalData, authorIds, commandUserOnly, failure, success)
