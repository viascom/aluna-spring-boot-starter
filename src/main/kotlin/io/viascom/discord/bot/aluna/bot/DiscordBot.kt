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
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.model.ObserveInteraction
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
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

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
    internal var messagesToObserveButton: MutableMap<String, ObserveInteraction> =
        Collections.synchronizedMap(hashMapOf<String, ObserveInteraction>())

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var messagesToObserveSelect: MutableMap<String, ObserveInteraction> =
        Collections.synchronizedMap(hashMapOf<String, ObserveInteraction>())

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var messagesToObserveModal: MutableMap<String, ObserveInteraction> =
        Collections.synchronizedMap(hashMapOf<String, ObserveInteraction>())

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
    internal val interactionExecutor =
        AlunaThreadPool.getDynamicThreadPool(
            alunaProperties.thread.interactionExecutorCount,
            alunaProperties.thread.interactionExecutorTtl,
            "Aluna-Interaction-%d"
        )

    @get:JvmSynthetic
    internal val asyncExecutor =
        AlunaThreadPool.getDynamicThreadPool(alunaProperties.thread.asyncExecutorCount, alunaProperties.thread.asyncExecutorTtl, "Aluna-Async-%d")

    @JvmOverloads
    fun registerMessageForButtonEvents(
        messageId: String,
        interaction: DiscordInteractionHandler,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        logger.debug("Register message $messageId for button events to interaction '${getInteractionName(interaction)}'" + if (interactionUserOnly) " (only specified users can use it)" else "")
        if (!additionalData.containsKey("message.id")) {
            additionalData["message.id"] = messageId
        }

        val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
            try {
                context.getBean(interaction::class.java).onButtonInteractionTimeout(additionalData)
            } catch (e: Exception) {
                logger.debug("Could not run onButtonInteractionTimeout for interaction '${getInteractionName(interaction)}'\n${e.stackTraceToString()}")
            }
            removeMessageForButtonEvents(messageId)
        }, duration.seconds, TimeUnit.SECONDS)

        messagesToObserveButton[messageId] =
            ObserveInteraction(
                interaction::class,
                interaction.uniqueId,
                LocalDateTime.now(),
                duration,
                persist,
                additionalData,
                authorIds,
                interactionUserOnly,
                timeoutTask
            )
    }

    @JvmOverloads
    fun registerMessageForButtonEvents(
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        hook.retrieveOriginal().queue { message ->
            logger.debug("Register message ${message.id} for button events to interaction '${getInteractionName(interaction)}'" + if (interactionUserOnly) " (only specified users can use it)" else "")
            if (!additionalData.containsKey("message.id")) {
                additionalData["message.id"] = message.id
            }
            val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
                try {
                    context.getBean(interaction::class.java).onButtonInteractionTimeout(additionalData)
                } catch (e: Exception) {
                    logger.debug("Could not run onButtonInteractionTimeout for interaction '${getInteractionName(interaction)}'\n${e.stackTraceToString()}")
                }
                removeMessageForButtonEvents(message.id)
            }, duration.seconds, TimeUnit.SECONDS)

            messagesToObserveButton[message.id] =
                ObserveInteraction(
                    interaction::class,
                    interaction.uniqueId,
                    LocalDateTime.now(),
                    duration,
                    persist,
                    additionalData,
                    authorIds,
                    interactionUserOnly,
                    timeoutTask
                )
        }
    }

    @JvmOverloads
    fun registerMessageForSelectEvents(
        messageId: String,
        interaction: DiscordInteractionHandler,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        logger.debug("Register message $messageId for select events to interaction '${getInteractionName(interaction)}'" + if (interactionUserOnly) " (only specified users can use it)" else "")
        if (!additionalData.containsKey("message.id")) {
            additionalData["message.id"] = messageId
        }
        val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
            try {
                context.getBean(interaction::class.java).onSelectMenuInteractionTimeout(additionalData)
            } catch (e: Exception) {
                logger.debug("Could not run onSelectMenuInteractionTimeout for interaction '${getInteractionName(interaction)}'\n${e.stackTraceToString()}")
            }
            removeMessageForSelectEvents(messageId)
        }, duration.seconds, TimeUnit.SECONDS)

        messagesToObserveSelect[messageId] =
            ObserveInteraction(
                interaction::class,
                interaction.uniqueId,
                LocalDateTime.now(),
                duration,
                persist,
                additionalData,
                authorIds,
                interactionUserOnly,
                timeoutTask
            )
    }

    @JvmOverloads
    fun registerMessageForSelectEvents(
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        hook.retrieveOriginal().queue { message ->
            logger.debug("Register message ${message.id} for select events to interaction '${getInteractionName(interaction)}'" + if (interactionUserOnly) " (only specified users can use it)" else "")
            if (!additionalData.containsKey("message.id")) {
                additionalData["message.id"] = message.id
            }
            val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
                try {
                    context.getBean(interaction::class.java).onSelectMenuInteractionTimeout(additionalData)
                } catch (e: Exception) {
                    logger.debug("Could not run onSelectMenuInteractionTimeout for interaction '${getInteractionName(interaction)}'\n${e.stackTraceToString()}")
                }
                removeMessageForSelectEvents(message.id)
            }, duration.seconds, TimeUnit.SECONDS)

            messagesToObserveSelect[message.id] =
                ObserveInteraction(
                    interaction::class,
                    interaction.uniqueId,
                    LocalDateTime.now(),
                    duration,
                    persist,
                    additionalData,
                    authorIds,
                    interactionUserOnly,
                    timeoutTask
                )
        }
    }

    @JvmOverloads
    fun registerMessageForModalEvents(
        authorId: String,
        interaction: DiscordInteractionHandler,
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf()
    ) {
        logger.debug("Register user $authorId for modal events to interaction '${getInteractionName(interaction)}'")
        val timeoutTask = messagesToObserveScheduledThreadPool.schedule({
            try {
                context.getBean(interaction::class.java).onModalInteractionTimeout(additionalData)
            } catch (e: Exception) {
                logger.debug("Could not run onModalInteractionTimeout for interaction '${getInteractionName(interaction)}'\n${e.stackTraceToString()}")
            }
            removeMessageForModalEvents(authorId)
        }, duration.seconds, TimeUnit.SECONDS)

        messagesToObserveModal[authorId] =
            ObserveInteraction(
                interaction::class,
                interaction.uniqueId,
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
        interaction: DiscordInteractionHandler,
        type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = arrayListOf(interaction.author.id),
        interactionUserOnly: Boolean = true,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in T>? = null
    ) {
        action.queue({
            if (type.contains(EventRegisterType.BUTTON)) {
                this.registerMessageForButtonEvents(hook, interaction, persist, duration, additionalData, authorIds, interactionUserOnly)
            }
            if (type.contains(EventRegisterType.SELECT)) {
                this.registerMessageForSelectEvents(hook, interaction, persist, duration, additionalData, authorIds, interactionUserOnly)
            }
            if (type.contains(EventRegisterType.MODAL)) {
                this.registerMessageForModalEvents(interaction.author.id, interaction, persist, duration, additionalData)
            }
            success?.accept(it)
        }, {
            failure?.accept(it)
        })
    }

    @JvmOverloads
    fun queueAndRegisterInteraction(
        action: RestAction<Void>,
        interaction: DiscordInteractionHandler,
        type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.MODAL),
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in Void>? = null
    ) {
        action.queue({
            if (type.contains(EventRegisterType.MODAL)) {
                this.registerMessageForModalEvents(interaction.author.id, interaction, persist, duration, additionalData)
            }
            success?.accept(it)
        }, {
            failure?.accept(it)
        })
    }

    @JvmOverloads
    fun queueAndRegisterInteraction(
        action: ReplyCallbackAction,
        interaction: DiscordInteractionHandler,
        type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = arrayListOf(interaction.author.id),
        interactionUserOnly: Boolean = true,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in InteractionHook>? = null
    ) {
        action.queue({
            if (type.contains(EventRegisterType.BUTTON)) {
                this.registerMessageForButtonEvents(it, interaction, persist, duration, additionalData, authorIds, interactionUserOnly)
            }
            if (type.contains(EventRegisterType.SELECT)) {
                this.registerMessageForSelectEvents(it, interaction, persist, duration, additionalData, authorIds, interactionUserOnly)
            }
            if (type.contains(EventRegisterType.MODAL)) {
                this.registerMessageForModalEvents(interaction.author.id, interaction, persist, duration, additionalData)
            }
            success?.accept(it)
        }, {
            failure?.accept(it)
        })
    }

    private fun getInteractionName(interaction: DiscordInteractionHandler): String {
        val field = interaction::class.memberProperties.first { it.name == "name" }
        field.isAccessible = true
        return field.getter.call(interaction) as String
    }
}

fun <T : Any> RestAction<T>.queueAndRegisterInteraction(
    hook: InteractionHook,
    interaction: DiscordInteractionHandler,
    type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
    persist: Boolean = false,
    duration: Duration = Duration.ofMinutes(14),
    additionalData: HashMap<String, Any?> = hashMapOf(),
    authorIds: ArrayList<String>? = arrayListOf(interaction.author.id),
    interactionUserOnly: Boolean = true,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in T>? = null
) = interaction.discordBot.queueAndRegisterInteraction(
    this,
    hook,
    interaction,
    type,
    persist,
    duration,
    additionalData,
    authorIds,
    interactionUserOnly,
    failure,
    success
)

fun RestAction<Void>.queueAndRegisterInteraction(
    interaction: DiscordInteractionHandler,
    type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.MODAL),
    persist: Boolean = false,
    duration: Duration = Duration.ofMinutes(14),
    additionalData: HashMap<String, Any?> = hashMapOf(),
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in Void>? = null
) = interaction.discordBot.queueAndRegisterInteraction(this, interaction, type, persist, duration, additionalData, failure, success)

fun ReplyCallbackAction.queueAndRegisterInteraction(
    interaction: DiscordInteractionHandler,
    type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
    persist: Boolean = false,
    duration: Duration = Duration.ofMinutes(14),
    additionalData: HashMap<String, Any?> = hashMapOf(),
    authorIds: ArrayList<String>? = arrayListOf(interaction.author.id),
    interactionUserOnly: Boolean = true,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in InteractionHook>? = null
) = interaction.discordBot.queueAndRegisterInteraction(
    this,
    interaction,
    type,
    persist,
    duration,
    additionalData,
    authorIds,
    interactionUserOnly,
    failure,
    success
)
