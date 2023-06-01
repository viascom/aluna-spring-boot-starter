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

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.event.CoroutineEventManager
import io.viascom.discord.bot.aluna.bot.handler.*
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.scope.InteractionScope
import io.viascom.discord.bot.aluna.model.CommandUsage
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.model.GatewayResponse
import io.viascom.discord.bot.aluna.model.ObserveInteraction
import io.viascom.discord.bot.aluna.property.AlunaDiscordProperties
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.AlunaThreadPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
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

    @JvmSynthetic
    internal val eventThreadPool = AlunaThreadPool.getDynamicThreadPool(
        0,
        alunaProperties.thread.eventThreadPool,
        java.time.Duration.ofMinutes(1),
        true,
        "Aluna-Event-Pool-%d"
    )

    @set:JvmSynthetic
    var interactionsInitialized: Boolean = false
        internal set

    val commands = hashMapOf<InteractionId, Class<DiscordCommandHandler>>()
    val contextMenus = hashMapOf<InteractionId, Class<DiscordContextMenuHandler>>()
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

    @JvmSynthetic
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
    internal var sessionStartLimits: GatewayResponse.SessionStartLimit? = null

    @JvmSynthetic
    internal val commandHistory = MutableSharedFlow<CommandUsage>(replay = 15, extraBufferCapacity = 15, BufferOverflow.DROP_OLDEST)
    val commandHistoryFlow = commandHistory.asSharedFlow()

    /**
     * Get discord command by name
     *
     * @param name name of the command
     * @return class of the command
     */
    fun getDiscordCommandByName(name: String): Command? {
        return discordRepresentations.getOrElse(name) { null }
    }

    /**
     * Get discord command by class
     *
     * @param clazz class of the command
     * @return command
     */
    fun getDiscordCommandByClass(clazz: Class<DiscordCommandHandler>): Command? {
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

    /**
     * Check if the current instance is the master node
     *
     * @return true if the current instance is the master node
     */
    fun isMasterNode(): Boolean {
        return if (alunaProperties.discord.sharding.type == AlunaDiscordProperties.Sharding.Type.SUBSET) {
            alunaProperties.discord.sharding.fromShard == 0
        } else true
    }

    /**
     * Get the current node number
     *
     * @return current node number
     */
    fun getNodeNumber(): Int {
        return alunaProperties.nodeNumber
    }

    /**
     * Register a message for button events. If such an event happens, Aluna will trigger the onButtonInteraction method of the interaction handler.
     *
     * @param messageId message id
     * @param interaction interaction handler
     * @param multiUse if the interaction can be used multiple times
     * @param duration timout of the interaction
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     */
    @JvmOverloads
    fun registerMessageForButtonEvents(
        messageId: String,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        val discordBot = this
        AlunaDispatchers.InternalScope.launch {
            logger.debug("Register message $messageId for button events to interaction '${getInteractionName(interaction)}'" + if (interactionUserOnly) " (only specified users can use it)" else "")

            messagesToObserveButton[messageId] =
                ObserveInteraction(
                    interaction::class,
                    interaction.uniqueId,
                    LocalDateTime.now(ZoneOffset.UTC),
                    duration,
                    multiUse,
                    authorIds,
                    interactionUserOnly,
                    ObserveInteraction.scheduleButtonTimeout(interaction, duration, messageId, discordBot, logger)
                )

            val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
            interactionScope.setMessageIdForInstance(interaction.uniqueId, messageId)
        }
    }

    /**
     * Register a message for button events. The message id is retrieved from the provided hook. If such an event happens, Aluna will trigger the onButtonInteraction method of the interaction handler.
     *
     * @param hook interaction hook
     * @param interaction interaction handler
     * @param multiUse if the interaction can be used multiple times
     * @param duration timout of the interaction
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     */
    @JvmOverloads
    fun registerMessageForButtonEvents(
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        hook.retrieveOriginal()
            .queue { registerMessageForButtonEvents(it.id, interaction, multiUse, duration, authorIds, interactionUserOnly) }

    }

    /**
     * Register a message for string select events. If such an event happens, Aluna will trigger the onStringSelectInteraction method of the interaction handler.
     *
     * @param hook interaction hook
     * @param interaction interaction handler
     * @param multiUse if the interaction can be used multiple times
     * @param duration timout of the interaction
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     */
    @JvmOverloads
    fun registerMessageForStringSelectEvents(
        messageId: String,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        val discordBot = this
        AlunaDispatchers.InternalScope.launch {
            logger.debug("Register message $messageId for string select events to interaction '${getInteractionName(interaction)}'" + if (interactionUserOnly) " (only specified users can use it)" else "")

            messagesToObserveStringSelect[messageId] =
                ObserveInteraction(
                    interaction::class,
                    interaction.uniqueId,
                    LocalDateTime.now(ZoneOffset.UTC),
                    duration,
                    multiUse,
                    authorIds,
                    interactionUserOnly,
                    ObserveInteraction.scheduleStringSelectTimeout(interaction, duration, messageId, discordBot, logger)
                )

            val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
            interactionScope.setMessageIdForInstance(interaction.uniqueId, messageId)
        }
    }

    /**
     * Register a message for string select events. The message id is retrieved from the provided hook. If such an event happens, Aluna will trigger the onStringSelectInteraction method of the interaction handler.
     *
     * @param hook interaction hook
     * @param interaction interaction handler
     * @param multiUse if the interaction can be used multiple times
     * @param duration timout of the interaction
     * @param additionalData additional data
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     */
    @JvmOverloads
    fun registerMessageForStringSelectEvents(
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        hook.retrieveOriginal()
            .queue { registerMessageForStringSelectEvents(it.id, interaction, multiUse, duration, authorIds, interactionUserOnly) }
    }

    /**
     * Register a message for entity select events. If such an event happens, Aluna will trigger the onEntitySelectInteraction method of the interaction handler.
     *
     * @param hook interaction hook
     * @param interaction interaction handler
     * @param multiUse if the interaction can be used multiple times
     * @param duration timout of the interaction
     * @param additionalData additional data
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     */
    @JvmOverloads
    fun registerMessageForEntitySelectEvents(
        messageId: String,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        val discordBot = this
        AlunaDispatchers.InternalScope.launch {
            logger.debug("Register message $messageId for entity select events to interaction '${getInteractionName(interaction)}'" + if (interactionUserOnly) " (only specified users can use it)" else "")

            messagesToObserveEntitySelect[messageId] =
                ObserveInteraction(
                    interaction::class,
                    interaction.uniqueId,
                    LocalDateTime.now(ZoneOffset.UTC),
                    duration,
                    multiUse,
                    authorIds,
                    interactionUserOnly,
                    ObserveInteraction.scheduleEntitySelectTimeout(interaction, duration, messageId, discordBot, logger)
                )

            val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
            interactionScope.setMessageIdForInstance(interaction.uniqueId, messageId)
        }
    }

    /**
     * Register a message for entity select events. The message id is retrieved from the provided hook. If such an event happens, Aluna will trigger the onEntitySelectInteraction method of the interaction handler.
     *
     * @param hook interaction hook
     * @param interaction interaction handler
     * @param multiUse if the interaction can be used multiple times
     * @param duration timout of the interaction
     * @param additionalData additional data
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     */
    @JvmOverloads
    fun registerMessageForEntitySelectEvents(
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        hook.retrieveOriginal()
            .queue { registerMessageForEntitySelectEvents(it.id, interaction, multiUse, duration, authorIds, interactionUserOnly) }
    }

    /**
     * Register a listener for modal events. If such an event happens, Aluna will trigger the onModalInteraction method of the interaction handler.
     *
     * @param authorId author id
     * @param interaction interaction handler
     * @param multiUse if the interaction can be used multiple times
     * @param duration timout of the interaction
     * @param additionalData additional data
     */
    @JvmOverloads
    fun registerMessageForModalEvents(
        authorId: String,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14)
    ) {
        val discordBot = this
        AlunaDispatchers.InternalScope.launch {
            logger.debug("Register user $authorId for modal events to interaction '${getInteractionName(interaction)}'")

            messagesToObserveModal[authorId] =
                ObserveInteraction(
                    interaction::class,
                    interaction.uniqueId,
                    LocalDateTime.now(ZoneOffset.UTC),
                    duration,
                    multiUse,
                    arrayListOf(authorId),
                    true,
                    ObserveInteraction.scheduleModalTimeout(interaction, duration, authorId, discordBot, logger)
                )
        }
    }

    /**
     * Remove listener for button events by message id.
     *
     * @param messageId message id
     */
    fun removeMessageForButtonEvents(messageId: String) = messagesToObserveButton.remove(messageId)

    /**
     * Remove listener for string select events by message id.
     *
     * @param messageId message id
     */
    fun removeMessageForStringSelectEvents(messageId: String) = messagesToObserveStringSelect.remove(messageId)

    /**
     * Remove listener for entity select events by message id.
     *
     * @param messageId message id
     */
    fun removeMessageForEntitySelectEvents(messageId: String) = messagesToObserveStringSelect.remove(messageId)

    /**
     * Remove listener for modal events by user id.
     *
     * @param messageId message id
     */
    fun removeMessageForModalEvents(userId: String) = messagesToObserveModal.remove(userId)

    /**
     * Queue an interaction and register listeners for it.
     *
     * @param T type of the rest action
     * @param action action to queue
     * @param hook interaction hook
     * @param interaction interaction handler
     * @param type event types to register
     * @param multiUse if the interaction can be used multiple times
     * @param duration timout of the interaction
     * @param additionalData additional data
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     * @param failure callback for failure
     * @param success callback for success
     */
    @JvmOverloads
    fun <T : Any> queueAndRegisterInteraction(
        action: RestAction<T>,
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        authorIds: ArrayList<String>? = arrayListOf(interaction.author.id),
        interactionUserOnly: Boolean = true,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in T>? = null
    ) {
        action.queue({
            val discordBot = this
            AlunaDispatchers.InternalScope.launch {
                launch {
                    if (type.contains(EventRegisterType.BUTTON)) {
                        discordBot.registerMessageForButtonEvents(hook, interaction, multiUse, duration, authorIds, interactionUserOnly)
                    }
                }
                launch {
                    if (type.contains(EventRegisterType.STRING_SELECT)) {
                        discordBot.registerMessageForStringSelectEvents(hook, interaction, multiUse, duration, authorIds, interactionUserOnly)
                    }
                }
                launch {
                    if (type.contains(EventRegisterType.ENTITY_SELECT)) {
                        discordBot.registerMessageForEntitySelectEvents(hook, interaction, multiUse, duration, authorIds, interactionUserOnly)
                    }
                }
                launch {
                    if (type.contains(EventRegisterType.MODAL)) {
                        discordBot.registerMessageForModalEvents(interaction.author.id, interaction, multiUse, duration)
                    }
                }
                success?.accept(it)
            }
        }, {
            failure?.accept(it) ?: throw Exception(it)
        })
    }

    /**
     * Queue an interaction and register listeners for it.
     *
     * @param action action to queue
     * @param interaction interaction handler
     * @param type event types to register
     * @param multiUse if the interaction can be used multiple times
     * @param duration timout of the interaction
     * @param additionalData additional data
     * @param failure callback for failure
     * @param success callback for success
     */
    @JvmOverloads
    fun queueAndRegisterInteraction(
        action: RestAction<Void>,
        interaction: DiscordInteractionHandler,
        type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.MODAL),
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in Void>? = null
    ) {
        action.queue({
            val discordBot = this
            AlunaDispatchers.InternalScope.launch {
                launch {
                    if (type.contains(EventRegisterType.MODAL)) {
                        discordBot.registerMessageForModalEvents(interaction.author.id, interaction, multiUse, duration)
                    }
                }
                launch(AlunaDispatchers.Detached) {
                    success?.accept(it)
                }
            }
        }, {
            AlunaDispatchers.DetachedScope.launch {
                failure?.accept(it) ?: throw Exception(it)
            }
        })
    }

    /**
     * Queue an interaction and register listeners for it.
     *
     * @param action action to queue
     * @param interaction interaction handler
     * @param type event types to register
     * @param multiUse if the interaction can be used multiple times
     * @param duration timout of the interaction
     * @param additionalData additional data
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     * @param failure callback for failure
     * @param success callback for success
     */
    @JvmOverloads
    fun queueAndRegisterInteraction(
        action: ReplyCallbackAction,
        interaction: DiscordInteractionHandler,
        type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        authorIds: ArrayList<String>? = arrayListOf(interaction.author.id),
        interactionUserOnly: Boolean = true,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in InteractionHook>? = null
    ) {
        action.queue({
            val discordBot = this
            AlunaDispatchers.InternalScope.launch {

                launch {
                    if (type.contains(EventRegisterType.BUTTON)) {
                        discordBot.registerMessageForButtonEvents(it, interaction, multiUse, duration, authorIds, interactionUserOnly)
                    }
                }
                launch {
                    if (type.contains(EventRegisterType.STRING_SELECT)) {
                        discordBot.registerMessageForStringSelectEvents(it, interaction, multiUse, duration, authorIds, interactionUserOnly)
                    }
                }
                launch {
                    if (type.contains(EventRegisterType.ENTITY_SELECT)) {
                        discordBot.registerMessageForEntitySelectEvents(it, interaction, multiUse, duration, authorIds, interactionUserOnly)
                    }
                }
                launch {
                    if (type.contains(EventRegisterType.MODAL)) {
                        discordBot.registerMessageForModalEvents(interaction.author.id, interaction, multiUse, duration)
                    }
                }
                launch(AlunaDispatchers.Detached) {
                    success?.accept(it)
                }
            }

        }, {
            AlunaDispatchers.DetachedScope.launch {
                failure?.accept(it) ?: throw Exception(it)
            }
        })
    }

    /**
     * Queue an action and set the message id for the interaction.
     *
     * @param action action to queue
     * @param hook interaction hook
     * @param interaction interaction handler
     * @param failure callback for failure
     * @param success callback for success
     */
    @JvmOverloads
    fun <T : Any> queueAndSetMessageId(
        action: RestAction<T>, hook: InteractionHook, interaction: DiscordInteractionHandler, failure: Consumer<in Throwable>? = null, success: Consumer<in T>? = null
    ) {
        action.queue({
            AlunaDispatchers.InternalScope.launch {
                launch {
                    hook.retrieveOriginal().queue {
                        val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
                        interactionScope.setMessageIdForInstance(interaction.uniqueId, it.id)
                    }
                }

                launch(AlunaDispatchers.Detached) {
                    success?.accept(it)
                }
            }
        }, {
            AlunaDispatchers.DetachedScope.launch {
                failure?.accept(it) ?: throw Exception(it)
            }
        })
    }

    /**
     * Queue an action and set the message id for the interaction.
     *
     * @param action action to queue
     * @param interaction interaction handler
     * @param failure callback for failure
     * @param success callback for success
     */
    @JvmOverloads
    fun queueAndSetMessageId(
        action: ReplyCallbackAction,
        interaction: DiscordInteractionHandler,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in InteractionHook>? = null
    ) {
        action.queue({
            AlunaDispatchers.InternalScope.launch {
                launch {
                    it.retrieveOriginal().queue {
                        val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
                        interactionScope.setMessageIdForInstance(interaction.uniqueId, it.id)
                    }
                }

                launch(AlunaDispatchers.Detached) {
                    success?.accept(it)
                }
            }
        }, {
            AlunaDispatchers.DetachedScope.launch {
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

/**
 * Queue an interaction and register listeners for it.
 *
 * @param T type of the rest action
 * @param action action to queue
 * @param hook interaction hook
 * @param interaction interaction handler
 * @param type event types to register
 * @param multiUse if the interaction can be used multiple times
 * @param duration timout of the interaction
 * @param additionalData additional data
 * @param authorIds only specified users can use it
 * @param interactionUserOnly only the user who created the interaction can use it
 * @param failure callback for failure
 * @param success callback for success
 */
fun <T : Any> RestAction<T>.queueAndRegisterInteraction(
    hook: InteractionHook,
    interaction: DiscordInteractionHandler,
    type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
    multiUse: Boolean = false,
    duration: Duration = Duration.ofMinutes(14),
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
    authorIds,
    interactionUserOnly,
    failure,
    success
)

/**
 * Queue an interaction and register listeners for it.
 *
 * @param action action to queue
 * @param interaction interaction handler
 * @param type event types to register
 * @param multiUse if the interaction can be used multiple times
 * @param duration timout of the interaction
 * @param additionalData additional data
 * @param failure callback for failure
 * @param success callback for success
 */
fun RestAction<Void>.queueAndRegisterInteraction(
    interaction: DiscordInteractionHandler,
    type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.MODAL),
    multiUse: Boolean = false,
    duration: Duration = Duration.ofMinutes(14),
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in Void>? = null
) = interaction.discordBot.queueAndRegisterInteraction(this, interaction, type, multiUse, duration, failure, success)

/**
 * Queue an action and set the message id for the interaction.
 *
 * @param action action to queue
 * @param hook interaction hook
 * @param interaction interaction handler
 * @param failure callback for failure
 * @param success callback for success
 */
fun <T : Any> RestAction<T>.queueAndSetMessageId(
    hook: InteractionHook,
    interaction: DiscordInteractionHandler,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in T>? = null
) = interaction.discordBot.queueAndSetMessageId(this, hook, interaction, failure, success)

/**
 * Queue an interaction and register listeners for it.
 *
 * @param action action to queue
 * @param interaction interaction handler
 * @param type event types to register
 * @param multiUse if the interaction can be used multiple times
 * @param duration timout of the interaction
 * @param additionalData additional data
 * @param authorIds only specified users can use it
 * @param interactionUserOnly only the user who created the interaction can use it
 * @param failure callback for failure
 * @param success callback for success
 */
fun ReplyCallbackAction.queueAndRegisterInteraction(
    interaction: DiscordInteractionHandler,
    type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
    multiUse: Boolean = false,
    duration: Duration = Duration.ofMinutes(14),
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
    authorIds,
    interactionUserOnly,
    failure,
    success
)

/**
 * Queue an action and set the message id for the interaction.
 *
 * @param action action to queue
 * @param interaction interaction handler
 * @param failure callback for failure
 * @param success callback for success
 */
fun ReplyCallbackAction.queueAndSetMessageId(
    interaction: DiscordInteractionHandler,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in InteractionHook>? = null
) = interaction.discordBot.queueAndSetMessageId(this, interaction, failure, success)

/**
 * The coroutine scope used by the underlying [CoroutineEventManager].
 * If this instance does not use the coroutine event manager, this returns the default scope from [getDefaultJDAScope].
 */
val JDA.scope: CoroutineScope get() = (eventManager as? CoroutineEventManager) ?: AlunaDispatchers.InternalScope

/**
 * The coroutine scope used by the underlying [CoroutineEventManager].
 * If this instance does not use the coroutine event manager, this returns the default scope from [getDefaultJDAScope].
 */
val ShardManager.scope: CoroutineScope get() = (shardCache.firstOrNull()?.eventManager as? CoroutineEventManager) ?: AlunaDispatchers.InternalScope