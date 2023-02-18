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

import io.viascom.discord.bot.aluna.bot.event.AlunaCoroutinesDispatcher
import io.viascom.discord.bot.aluna.bot.event.CoroutineEventManager
import io.viascom.discord.bot.aluna.bot.handler.CooldownScope
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.scope.InteractionScope
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.model.GatewayResponse
import io.viascom.discord.bot.aluna.model.ObserveInteraction
import io.viascom.discord.bot.aluna.property.AlunaDiscordProperties
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.AlunaThreadPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.function.Consumer
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Service
@ConditionalOnJdaEnabled
open class DiscordBot(
    private val alunaProperties: AlunaProperties,
    private val configurableListableBeanFactory: ConfigurableListableBeanFactory
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    var shardManager: ShardManager? = null

    @set:JvmSynthetic
    var interactionsInitialized: Boolean = false
        internal set

    val commands = hashMapOf<InteractionId, Class<DiscordCommand>>()
    val contextMenus = hashMapOf<InteractionId, Class<DiscordContextMenu>>()
    val commandsWithAutocomplete = arrayListOf<InteractionId>()
    val commandsWithPersistentInteractions = arrayListOf<InteractionId>()
    val autoCompleteHandlers = hashMapOf<Pair<InteractionId, OptionName?>, Class<out AutoCompleteHandler>>()

    @get:JvmSynthetic
    internal val discordRepresentations = hashMapOf<InteractionName, Command>()

    @get:JvmSynthetic
    internal val cooldowns = hashMapOf<CooldownKey, LastUsage>()

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var messagesToObserveButton: MutableMap<MessageId, ObserveInteraction> = Collections.synchronizedMap(hashMapOf<MessageId, ObserveInteraction>())

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var messagesToObserveStringSelect: MutableMap<MessageId, ObserveInteraction> = Collections.synchronizedMap(hashMapOf<MessageId, ObserveInteraction>())

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var messagesToObserveEntitySelect: MutableMap<MessageId, ObserveInteraction> = Collections.synchronizedMap(hashMapOf<MessageId, ObserveInteraction>())

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var messagesToObserveModal: MutableMap<MessageId, ObserveInteraction> = Collections.synchronizedMap(hashMapOf<MessageId, ObserveInteraction>())

    @get:JvmSynthetic
    internal val messagesToObserveScheduledThreadPool =
        AlunaThreadPool.getScheduledThreadPool(
            1,
            alunaProperties.thread.messagesToObserveScheduledThreadPool,
            Duration.ofSeconds(30),
            "Aluna-Message-Observer-Timeout-Pool-%d",
            true
        )

    @set:JvmSynthetic
    var totalShards: Int = 1
        internal set

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var coroutineEventManager: CoroutineEventManager = CoroutineEventManager(timeout = kotlin.time.Duration.INFINITE)

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var sessionStartLimits: GatewayResponse.SessionStartLimit? = null

    fun getDiscordCommandByName(name: String): Command? {
        return discordRepresentations.getOrElse(name) { null }
    }

    fun getDiscordCommandByClass(clazz: Class<DiscordCommand>): Command? {
        return commands.entries.firstOrNull { it.value == clazz }?.key?.let { discordRepresentations[it] }
    }

    fun getCooldownKey(scope: CooldownScope, interactionId: InteractionId, userId: String? = null, channelId: String? = null, guildId: String? = null): CooldownKey {
        return when (scope) {
            CooldownScope.USER -> "$interactionId:U:$userId"
            CooldownScope.CHANNEL -> "$interactionId:C:$channelId"
            CooldownScope.GUILD -> "$interactionId:G:$guildId"
            CooldownScope.USER_GUILD -> "$interactionId:U:$userId|G:$guildId"
            CooldownScope.GLOBAL -> "$interactionId:A"
            CooldownScope.NO_COOLDOWN -> ""
        }
    }

    /**
     * Check if the cooldown is active.
     *
     * This method will remove the expired cooldown for the given key.
     *
     * @param key
     * @param cooldown
     * @return
     */
    fun isCooldownActive(key: CooldownKey, cooldown: Duration): Boolean {
        //If not known, there is no cooldown
        if (!cooldowns.contains(key)) {
            return false
        }

        if (cooldowns[key]?.plusNanos(cooldown.toNanos())?.isAfter(LocalDateTime.now(ZoneOffset.UTC)) == true) {
            return true
        }

        cooldowns.remove(key)

        return false
    }

    fun getCooldown(key: CooldownKey): LocalDateTime? {
        return cooldowns[key]
    }

    fun isMasterNode(): Boolean {
        return if (alunaProperties.discord.sharding.type == AlunaDiscordProperties.Sharding.Type.SUBSET) {
            alunaProperties.discord.sharding.fromShard == 0
        } else true
    }

    fun getNodeNumber(): Int {
        return alunaProperties.nodeNumber
    }

    @JvmOverloads
    fun registerMessageForButtonEvents(
        messageId: String,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        val discordBot = this
        runBlocking(AlunaCoroutinesDispatcher.Default) {
            logger.debug("Register message $messageId for button events to interaction '${getInteractionName(interaction)}'" + if (interactionUserOnly) " (only specified users can use it)" else "")
            if (!additionalData.containsKey("message.id")) {
                additionalData["message.id"] = messageId
            }

            messagesToObserveButton[messageId] =
                ObserveInteraction(
                    interaction::class,
                    interaction.uniqueId,
                    LocalDateTime.now(),
                    duration,
                    multiUse,
                    additionalData,
                    authorIds,
                    interactionUserOnly,
                    ObserveInteraction.scheduleButtonTimeout(interaction, duration, messageId, discordBot, logger, additionalData)
                )

            val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
            interactionScope.setMessageIdForInstance(interaction.uniqueId, messageId)
        }
    }

    @JvmOverloads
    fun registerMessageForButtonEvents(
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        hook.retrieveOriginal()
            .queue { registerMessageForButtonEvents(it.id, interaction, multiUse, duration, additionalData, authorIds, interactionUserOnly) }

    }

    @JvmOverloads
    fun registerMessageForStringSelectEvents(
        messageId: String,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        val discordBot = this
        runBlocking(AlunaCoroutinesDispatcher.Default) {
            logger.debug("Register message $messageId for string select events to interaction '${getInteractionName(interaction)}'" + if (interactionUserOnly) " (only specified users can use it)" else "")
            if (!additionalData.containsKey("message.id")) {
                additionalData["message.id"] = messageId
            }

            messagesToObserveStringSelect[messageId] =
                ObserveInteraction(
                    interaction::class,
                    interaction.uniqueId,
                    LocalDateTime.now(),
                    duration,
                    multiUse,
                    additionalData,
                    authorIds,
                    interactionUserOnly,
                    ObserveInteraction.scheduleStringSelectTimeout(interaction, duration, messageId, discordBot, logger, additionalData)
                )

            val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
            interactionScope.setMessageIdForInstance(interaction.uniqueId, messageId)
        }
    }

    @JvmOverloads
    fun registerMessageForStringSelectEvents(
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        hook.retrieveOriginal()
            .queue { registerMessageForStringSelectEvents(it.id, interaction, multiUse, duration, additionalData, authorIds, interactionUserOnly) }
    }

    @JvmOverloads
    fun registerMessageForEntitySelectEvents(
        messageId: String,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        val discordBot = this
        runBlocking(AlunaCoroutinesDispatcher.Default) {
            logger.debug("Register message $messageId for entity select events to interaction '${getInteractionName(interaction)}'" + if (interactionUserOnly) " (only specified users can use it)" else "")
            if (!additionalData.containsKey("message.id")) {
                additionalData["message.id"] = messageId
            }

            messagesToObserveEntitySelect[messageId] =
                ObserveInteraction(
                    interaction::class,
                    interaction.uniqueId,
                    LocalDateTime.now(),
                    duration,
                    multiUse,
                    additionalData,
                    authorIds,
                    interactionUserOnly,
                    ObserveInteraction.scheduleEntitySelectTimeout(interaction, duration, messageId, discordBot, logger, additionalData)
                )

            val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
            interactionScope.setMessageIdForInstance(interaction.uniqueId, messageId)
        }
    }

    @JvmOverloads
    fun registerMessageForEntitySelectEvents(
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        hook.retrieveOriginal()
            .queue { registerMessageForEntitySelectEvents(it.id, interaction, multiUse, duration, additionalData, authorIds, interactionUserOnly) }
    }

    @JvmOverloads
    fun registerMessageForModalEvents(
        authorId: String,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf()
    ) {
        val discordBot = this
        runBlocking(AlunaCoroutinesDispatcher.Default) {
            logger.debug("Register user $authorId for modal events to interaction '${getInteractionName(interaction)}'")

            messagesToObserveModal[authorId] =
                ObserveInteraction(
                    interaction::class,
                    interaction.uniqueId,
                    LocalDateTime.now(),
                    duration,
                    multiUse,
                    additionalData,
                    arrayListOf(authorId),
                    true,
                    ObserveInteraction.scheduleModalTimeout(interaction, duration, authorId, discordBot, logger, additionalData)
                )
        }
    }

    fun removeMessageForButtonEvents(messageId: String) = messagesToObserveButton.remove(messageId)
    fun removeMessageForStringSelectEvents(messageId: String) = messagesToObserveStringSelect.remove(messageId)
    fun removeMessageForEntitySelectEvents(messageId: String) = messagesToObserveStringSelect.remove(messageId)
    fun removeMessageForModalEvents(userId: String) = messagesToObserveModal.remove(userId)

    @JvmOverloads
    fun <T : Any> queueAndRegisterInteraction(
        action: RestAction<T>,
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = arrayListOf(interaction.author.id),
        interactionUserOnly: Boolean = true,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in T>? = null
    ) {
        action.queue({
            val discordBot = this
            runBlocking(AlunaCoroutinesDispatcher.Default) {
                launch(AlunaCoroutinesDispatcher.Default) {
                    if (type.contains(EventRegisterType.BUTTON)) {
                        discordBot.registerMessageForButtonEvents(hook, interaction, multiUse, duration, additionalData, authorIds, interactionUserOnly)
                    }
                }
                launch(AlunaCoroutinesDispatcher.Default) {
                    if (type.contains(EventRegisterType.STRING_SELECT)) {
                        discordBot.registerMessageForStringSelectEvents(hook, interaction, multiUse, duration, additionalData, authorIds, interactionUserOnly)
                    }
                }
                launch(AlunaCoroutinesDispatcher.Default) {
                    if (type.contains(EventRegisterType.ENTITY_SELECT)) {
                        discordBot.registerMessageForEntitySelectEvents(hook, interaction, multiUse, duration, additionalData, authorIds, interactionUserOnly)
                    }
                }
                launch(AlunaCoroutinesDispatcher.Default) {
                    if (type.contains(EventRegisterType.MODAL)) {
                        discordBot.registerMessageForModalEvents(interaction.author.id, interaction, multiUse, duration, additionalData)
                    }
                }
                success?.accept(it)
            }
        }, {
            failure?.accept(it) ?: throw Exception(it)
        })
    }

    @JvmOverloads
    fun queueAndRegisterInteraction(
        action: RestAction<Void>,
        interaction: DiscordInteractionHandler,
        type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.MODAL),
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in Void>? = null
    ) {
        action.queue({
            val discordBot = this
            runBlocking(AlunaCoroutinesDispatcher.Default) {
                launch(AlunaCoroutinesDispatcher.Default) {
                    if (type.contains(EventRegisterType.MODAL)) {
                        discordBot.registerMessageForModalEvents(interaction.author.id, interaction, multiUse, duration, additionalData)
                    }
                }
                launch(AlunaCoroutinesDispatcher.Default) {
                    success?.accept(it)
                }
            }
        }, {
            runBlocking(AlunaCoroutinesDispatcher.Default) {
                failure?.accept(it) ?: throw Exception(it)
            }
        })
    }

    @JvmOverloads
    fun queueAndRegisterInteraction(
        action: ReplyCallbackAction,
        interaction: DiscordInteractionHandler,
        type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = arrayListOf(interaction.author.id),
        interactionUserOnly: Boolean = true,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in InteractionHook>? = null
    ) {
        action.queue({
            val discordBot = this
            runBlocking(AlunaCoroutinesDispatcher.Default) {

                launch(AlunaCoroutinesDispatcher.Default) {
                    if (type.contains(EventRegisterType.BUTTON)) {
                        discordBot.registerMessageForButtonEvents(it, interaction, multiUse, duration, additionalData, authorIds, interactionUserOnly)
                    }
                }
                launch(AlunaCoroutinesDispatcher.Default) {
                    if (type.contains(EventRegisterType.STRING_SELECT)) {
                        discordBot.registerMessageForStringSelectEvents(it, interaction, multiUse, duration, additionalData, authorIds, interactionUserOnly)
                    }
                }
                launch(AlunaCoroutinesDispatcher.Default) {
                    if (type.contains(EventRegisterType.ENTITY_SELECT)) {
                        discordBot.registerMessageForEntitySelectEvents(it, interaction, multiUse, duration, additionalData, authorIds, interactionUserOnly)
                    }
                }
                launch(AlunaCoroutinesDispatcher.Default) {
                    if (type.contains(EventRegisterType.MODAL)) {
                        discordBot.registerMessageForModalEvents(interaction.author.id, interaction, multiUse, duration, additionalData)
                    }
                }
                launch(AlunaCoroutinesDispatcher.Default) {
                    success?.accept(it)
                }
            }

        }, {
            runBlocking(AlunaCoroutinesDispatcher.Default) {
                failure?.accept(it) ?: throw Exception(it)
            }
        })
    }

    @JvmOverloads
    fun <T : Any> queueAndSetMessageId(
        action: RestAction<T>, hook: InteractionHook, interaction: DiscordInteractionHandler, failure: Consumer<in Throwable>? = null, success: Consumer<in T>? = null
    ) {
        action.queue({
            runBlocking(AlunaCoroutinesDispatcher.Default) {
                launch(AlunaCoroutinesDispatcher.Default) {
                    hook.retrieveOriginal().queue {
                        val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
                        interactionScope.setMessageIdForInstance(interaction.uniqueId, it.id)
                    }
                }

                launch(AlunaCoroutinesDispatcher.Default) {
                    success?.accept(it)
                }
            }
        }, {
            runBlocking(AlunaCoroutinesDispatcher.Default) {
                failure?.accept(it) ?: throw Exception(it)
            }
        })
    }

    @JvmOverloads
    fun queueAndSetMessageId(
        action: ReplyCallbackAction,
        interaction: DiscordInteractionHandler,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in InteractionHook>? = null
    ) {
        action.queue({
            runBlocking(AlunaCoroutinesDispatcher.Default) {
                launch(AlunaCoroutinesDispatcher.Default) {
                    it.retrieveOriginal().queue {
                        val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
                        interactionScope.setMessageIdForInstance(interaction.uniqueId, it.id)
                    }
                }

                launch(AlunaCoroutinesDispatcher.Default) {
                    success?.accept(it)
                }
            }
        }, {
            runBlocking(AlunaCoroutinesDispatcher.Default) {
                failure?.accept(it) ?: throw Exception(it)
            }
        })
    }

    @JvmSynthetic
    internal fun getInteractionName(interaction: DiscordInteractionHandler): String {
        val field = interaction::class.memberProperties.first { it.name == "name" }
        field.isAccessible = true
        return field.getter.call(interaction) as String
    }

}

internal typealias InteractionId = String
internal typealias InteractionName = String
internal typealias OptionName = String
internal typealias CooldownKey = String
internal typealias MessageId = String
internal typealias LastUsage = LocalDateTime

fun <T : Any> RestAction<T>.queueAndRegisterInteraction(
    hook: InteractionHook,
    interaction: DiscordInteractionHandler,
    type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
    multiUse: Boolean = false,
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
    multiUse,
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
    multiUse: Boolean = false,
    duration: Duration = Duration.ofMinutes(14),
    additionalData: HashMap<String, Any?> = hashMapOf(),
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in Void>? = null
) = interaction.discordBot.queueAndRegisterInteraction(this, interaction, type, multiUse, duration, additionalData, failure, success)

fun <T : Any> RestAction<T>.queueAndSetMessageId(
    hook: InteractionHook,
    interaction: DiscordInteractionHandler,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in T>? = null
) = interaction.discordBot.queueAndSetMessageId(this, hook, interaction, failure, success)

fun ReplyCallbackAction.queueAndRegisterInteraction(
    interaction: DiscordInteractionHandler,
    type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
    multiUse: Boolean = false,
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
    multiUse,
    duration,
    additionalData,
    authorIds,
    interactionUserOnly,
    failure,
    success
)

fun ReplyCallbackAction.queueAndSetMessageId(
    interaction: DiscordInteractionHandler,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in InteractionHook>? = null
) = interaction.discordBot.queueAndSetMessageId(this, interaction, failure, success)

/**
 * The coroutine scope used by the underlying [CoroutineEventManager].
 * If this instance does not use the coroutine event manager, this returns the default scope from [getDefaultJDAScope].
 */
val JDA.scope: CoroutineScope get() = (eventManager as? CoroutineEventManager) ?: AlunaCoroutinesDispatcher.DefaultScope

/**
 * The coroutine scope used by the underlying [CoroutineEventManager].
 * If this instance does not use the coroutine event manager, this returns the default scope from [getDefaultJDAScope].
 */
val ShardManager.scope: CoroutineScope get() = (shardCache.firstOrNull()?.eventManager as? CoroutineEventManager) ?: AlunaCoroutinesDispatcher.DefaultScope