/*
 * Copyright 2025 Viascom Ltd liab. Co
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
import io.viascom.discord.bot.aluna.bot.listener.ShardStartListener
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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.ICommandReference
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.Response
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Service
@ConditionalOnJdaEnabled
public open class DiscordBot(
    private val alunaProperties: AlunaProperties,
    private val configurableListableBeanFactory: ConfigurableListableBeanFactory,
    private val shardStartListener: ShardStartListener
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    public var shardManager: ShardManager? = null
    public var isLoggedIn: Boolean = false

    public val commands: HashMap<InteractionId, Class<DiscordCommandHandler>> = hashMapOf()
    public val contextMenus: HashMap<InteractionId, Class<DiscordContextMenuHandler>> = hashMapOf()
    public val commandsWithAutocomplete: ArrayList<InteractionId> = arrayListOf()
    public val commandsWithPersistentInteractions: ArrayList<InteractionId> = arrayListOf()
    public val autoCompleteHandlers: HashMap<Pair<InteractionId, OptionName?>, Class<out AutoCompleteHandler>> = hashMapOf()

    @JvmSynthetic
    internal var latchCount: Int = 0

    @set:JvmSynthetic
    public var interactionsInitialized: Boolean = false
        internal set

    @get:JvmSynthetic
    internal val discordRepresentations = hashMapOf<InteractionId, Command>()

    @get:JvmSynthetic
    internal val cooldowns = hashMapOf<CooldownKey, LastUsage>()

    @JvmSynthetic
    internal var messagesToObserveButton: MutableMap<MessageId, ObserveInteraction> = Collections.synchronizedMap(hashMapOf<MessageId, ObserveInteraction>())

    @JvmSynthetic
    internal var messagesToObserveStringSelect: MutableMap<MessageId, ObserveInteraction> = Collections.synchronizedMap(hashMapOf<MessageId, ObserveInteraction>())

    @JvmSynthetic
    internal var messagesToObserveEntitySelect: MutableMap<MessageId, ObserveInteraction> = Collections.synchronizedMap(hashMapOf<MessageId, ObserveInteraction>())

    @JvmSynthetic
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
    public var totalShards: Int = 1
        internal set

    @JvmSynthetic
    internal var sessionStartLimits: GatewayResponse.SessionStartLimit? = null

    @JvmSynthetic
    internal val commandHistory = MutableSharedFlow<CommandUsage>(replay = 15, extraBufferCapacity = 15, BufferOverflow.DROP_OLDEST)
    public val commandHistoryFlow: SharedFlow<CommandUsage> = commandHistory.asSharedFlow()

    public fun login() {
        if (isLoggedIn) {
            logger.warn("ShardManager already logged in. Skipping login.")
            return
        }

        logger.info("Spawning {} shards...", latchCount)
        shardStartListener.latch = CountDownLatch(latchCount)
        val start = System.currentTimeMillis()

        this.shardManager!!.login()
        isLoggedIn = true

        AlunaDispatchers.InternalScope.launch {
            logger.debug("Awaiting for $latchCount shards to connect")
            try {
                shardStartListener.latch.await()
                val elapsed = System.currentTimeMillis() - start
                logger.debug("All shards are connected! Took ${TimeUnit.MILLISECONDS.toSeconds(elapsed)} seconds")
                shardManager!!.removeEventListener(shardStartListener)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Get discord command by name
     *
     * @param name name of the command
     * @return class of the command
     */
    public fun getDiscordCommandByName(name: String): ICommandReference? {
        if (name.isEmpty()) return null

        //Check if subcommand
        if (name.contains(" ")) {
            val split = name.split(" ")
            val commandName = split[0]
            val firstSubCommandName = split[1]

            if (split.size > 2) {
                val secondSubCommandName = split[2]
                return discordRepresentations.values.firstOrNull { it.name == commandName }?.subcommandGroups?.firstOrNull { it.name == firstSubCommandName }?.subcommands?.firstOrNull { it.name == secondSubCommandName }
            }

            return discordRepresentations.values.firstOrNull { it.name == commandName }?.subcommands?.firstOrNull { it.name == firstSubCommandName }
        }

        return discordRepresentations.values.firstOrNull { it.name == name }
    }

    /**
     * Get discord command by class
     *
     * @param clazz class of the command
     * @return command
     */
    public fun getDiscordCommandByClass(clazz: Class<DiscordCommandHandler>): Command? {
        return commands.entries.firstOrNull { it.value == clazz }?.key?.let { discordRepresentations[it] }
    }

    public fun getCooldownKey(scope: CooldownScope, interactionId: InteractionId, userId: String? = null, channelId: String? = null, guildId: String? = null): CooldownKey {
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
     * Checks if a cooldown is active for the given key.
     *
     * @param key The key associated with the cooldown.
     * @param cooldown The duration of the cooldown.
     * @return `true` if the cooldown is active, `false` otherwise.
     */
    public fun isCooldownActive(key: CooldownKey, cooldown: Duration): Boolean {
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

    /**
     * Retrieves the cooldown time for the given key.
     *
     * @param key The key used to identify the cooldown.
     * @return The cooldown time as a LocalDateTime object, or null if the key is not found.
     */
    public fun getCooldown(key: CooldownKey): LocalDateTime? {
        return cooldowns[key]
    }

    /**
     * Check if the current instance is the master node
     *
     * @return true if the current instance is the master node
     */
    public fun isMasterNode(): Boolean {
        return if (alunaProperties.discord.sharding.type == AlunaDiscordProperties.Sharding.Type.SUBSET) {
            alunaProperties.discord.sharding.fromShard == 0
        } else true
    }

    /**
     * Get the current node number
     *
     * @return current node number
     */
    public fun getNodeNumber(): Int {
        return alunaProperties.nodeNumber
    }

    /**
     * Get the lower bound of Snowflakes.
     *
     * This method calculates the lower bound of Snowflakes based on the sharding configuration.
     *
     * @return The lower bound of Snowflakes as a [Long] value.
     */
    public fun getLowerBoundOfSnowflake(): Long {
        return if (alunaProperties.discord.sharding.type == AlunaDiscordProperties.Sharding.Type.SUBSET) {
            alunaProperties.discord.sharding.fromShard * (Long.MAX_VALUE / alunaProperties.discord.sharding.totalShards)
        } else {
            0L
        }
    }

    /**
     * Get the upper bound of Snowflakes.
     *
     * This method calculates the upper bound ofSnowflakes based on the sharding configuration.
     *
     *  @return The upper bound of Snowflakes as a [Long] value.
     */
    public fun getUpperBoundOfSnowflake(): Long {
        return if (alunaProperties.discord.sharding.type == AlunaDiscordProperties.Sharding.Type.SUBSET) {
            (alunaProperties.discord.sharding.fromShard + alunaProperties.discord.sharding.shardAmount) * (Long.MAX_VALUE / alunaProperties.discord.sharding.totalShards) - 1
        } else {
            Long.MAX_VALUE
        }
    }

    public fun getShardBySnowflake(snowflake: Long): Long {
        return (snowflake shr 22) % alunaProperties.discord.sharding.totalShards
    }

    public fun getShardOfBotDM(): Long {
        return alunaProperties.discord.applicationId?.let { getShardBySnowflake(it.toLong()) } ?: throw IllegalArgumentException("alunaProperties.discord.applicationId is not set")
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
    public fun registerMessageForButtonEvents(
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
    public fun registerMessageForButtonEvents(
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        if (hook.hasCallbackResponse()) {
            hook.callbackResponse.message?.let { registerMessageForButtonEvents(it.id, interaction, multiUse, duration, authorIds, interactionUserOnly) }
                ?: throw ErrorResponseException.create(ErrorResponse.UNKNOWN_MESSAGE, (object : Response(null, 400, "", 0, emptySet()) {}))
        } else {
            hook.retrieveOriginal().queue {
                registerMessageForButtonEvents(it.id, interaction, multiUse, duration, authorIds, interactionUserOnly)
            }
        }
    }

    /**
     * Register a message for string select events. If such an event happens, Aluna will trigger the onStringSelectInteraction method of the interaction handler.
     *
     * @param messageId id of the message
     * @param interaction interaction handler
     * @param multiUse if the interaction can be used multiple times
     * @param duration timout of the interaction
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     */
    @JvmOverloads
    public fun registerMessageForStringSelectEvents(
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
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     */
    @JvmOverloads
    public fun registerMessageForStringSelectEvents(
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        if (hook.hasCallbackResponse()) {
            hook.callbackResponse.message?.let { registerMessageForStringSelectEvents(it.id, interaction, multiUse, duration, authorIds, interactionUserOnly) }
                ?: throw ErrorResponseException.create(ErrorResponse.UNKNOWN_MESSAGE, (object : Response(null, 400, "", 0, emptySet()) {}))
        } else {
            hook.retrieveOriginal().queue {
                registerMessageForStringSelectEvents(it.id, interaction, multiUse, duration, authorIds, interactionUserOnly)
            }
        }

    }

    /**
     * Register a message for entity select events. If such an event happens, Aluna will trigger the onEntitySelectInteraction method of the interaction handler.
     *
     * @param messageId id of the message
     * @param interaction interaction handler
     * @param multiUse if the interaction can be used multiple times
     * @param duration timout of the interaction
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     */
    @JvmOverloads
    public fun registerMessageForEntitySelectEvents(
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
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     */
    @JvmOverloads
    public fun registerMessageForEntitySelectEvents(
        hook: InteractionHook,
        interaction: DiscordInteractionHandler,
        multiUse: Boolean = false,
        duration: Duration = Duration.ofMinutes(14),
        authorIds: ArrayList<String>? = null,
        interactionUserOnly: Boolean = false
    ) {
        if (hook.hasCallbackResponse()) {
            hook.callbackResponse.message?.let { registerMessageForEntitySelectEvents(it.id, interaction, multiUse, duration, authorIds, interactionUserOnly) }
                ?: throw ErrorResponseException.create(ErrorResponse.UNKNOWN_MESSAGE, (object : Response(null, 400, "", 0, emptySet()) {}))
        } else {
            hook.retrieveOriginal().queue {
                registerMessageForEntitySelectEvents(it.id, interaction, multiUse, duration, authorIds, interactionUserOnly)
            }
        }
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
    public fun registerMessageForModalEvents(
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
    public fun removeMessageForButtonEvents(messageId: String): ObserveInteraction? = messagesToObserveButton.remove(messageId)

    /**
     * Remove listener for string select events by message id.
     *
     * @param messageId message id
     */
    public fun removeMessageForStringSelectEvents(messageId: String): ObserveInteraction? = messagesToObserveStringSelect.remove(messageId)

    /**
     * Remove listener for entity select events by message id.
     *
     * @param messageId message id
     */
    public fun removeMessageForEntitySelectEvents(messageId: String): ObserveInteraction? = messagesToObserveStringSelect.remove(messageId)

    /**
     * Remove listener for modal events by user id.
     *
     * @param messageId message id
     */
    public fun removeMessageForModalEvents(userId: String): ObserveInteraction? = messagesToObserveModal.remove(userId)

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
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     * @param failure callback for failure
     * @param success callback for success
     */
    @JvmOverloads
    public fun <T : Any> queueAndRegisterInteraction(
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
     * @param failure callback for failure
     * @param success callback for success
     */
    @JvmOverloads
    public fun queueAndRegisterInteraction(
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
     * @param authorIds only specified users can use it
     * @param interactionUserOnly only the user who created the interaction can use it
     * @param failure callback for failure
     * @param success callback for success
     */
    @JvmOverloads
    public fun queueAndRegisterInteraction(
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
    public fun <T : Any> queueAndSetMessageId(
        action: RestAction<T>, hook: InteractionHook, interaction: DiscordInteractionHandler, failure: Consumer<in Throwable>? = null, success: Consumer<in T>? = null
    ) {
        action.queue({
            AlunaDispatchers.InternalScope.launch {
                launch {
                    hook.callbackResponse.message?.let {
                        val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
                        interactionScope.setMessageIdForInstance(interaction.uniqueId, it.id)
                    } ?: throw ErrorResponseException.create(ErrorResponse.UNKNOWN_MESSAGE, (object : Response(null, 400, "", 0, emptySet()) {}))
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
    public fun queueAndSetMessageId(
        action: ReplyCallbackAction,
        interaction: DiscordInteractionHandler,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in InteractionHook>? = null
    ) {
        action.queue({
            AlunaDispatchers.InternalScope.launch {
                launch {
                    it.callbackResponse.message?.let {
                        val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
                        interactionScope.setMessageIdForInstance(interaction.uniqueId, it.id)
                    } ?: throw ErrorResponseException.create(ErrorResponse.UNKNOWN_MESSAGE, (object : Response(null, 400, "", 0, emptySet()) {}))
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

/**
 * Queue an interaction and register listeners for it.
 *
 * @param T type of the rest action
 * @param hook interaction hook
 * @param interaction interaction handler
 * @param type event types to register
 * @param multiUse if the interaction can be used multiple times
 * @param duration timout of the interaction
 * @param authorIds only specified users can use it
 * @param interactionUserOnly only the user who created the interaction can use it
 * @param failure callback for failure
 * @param success callback for success
 */
public fun <T : Any> RestAction<T>.queueAndRegisterInteraction(
    hook: InteractionHook,
    interaction: DiscordInteractionHandler,
    type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
    multiUse: Boolean = false,
    duration: Duration = Duration.ofMinutes(14),
    authorIds: ArrayList<String>? = arrayListOf(interaction.author.id),
    interactionUserOnly: Boolean = true,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in T>? = null
): Unit = interaction.discordBot.queueAndRegisterInteraction(
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
 * @param interaction interaction handler
 * @param type event types to register
 * @param multiUse if the interaction can be used multiple times
 * @param duration timout of the interaction
 * @param failure callback for failure
 * @param success callback for success
 */
public fun RestAction<Void>.queueAndRegisterInteraction(
    interaction: DiscordInteractionHandler,
    type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.MODAL),
    multiUse: Boolean = false,
    duration: Duration = Duration.ofMinutes(14),
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in Void>? = null
): Unit = interaction.discordBot.queueAndRegisterInteraction(this, interaction, type, multiUse, duration, failure, success)

/**
 * Queue an action and set the message id for the interaction.
 *
 * @param hook interaction hook
 * @param interaction interaction handler
 * @param failure callback for failure
 * @param success callback for success
 */
public fun <T : Any> RestAction<T>.queueAndSetMessageId(
    hook: InteractionHook,
    interaction: DiscordInteractionHandler,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in T>? = null
): Unit = interaction.discordBot.queueAndSetMessageId(this, hook, interaction, failure, success)

/**
 * Queue an interaction and register listeners for it.
 *
 * @param interaction interaction handler
 * @param type event types to register
 * @param multiUse if the interaction can be used multiple times
 * @param duration timout of the interaction
 * @param authorIds only specified users can use it
 * @param interactionUserOnly only the user who created the interaction can use it
 * @param failure callback for failure
 * @param success callback for success
 */
public fun ReplyCallbackAction.queueAndRegisterInteraction(
    interaction: DiscordInteractionHandler,
    type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
    multiUse: Boolean = false,
    duration: Duration = Duration.ofMinutes(14),
    authorIds: ArrayList<String>? = arrayListOf(interaction.author.id),
    interactionUserOnly: Boolean = true,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in InteractionHook>? = null
): Unit = interaction.discordBot.queueAndRegisterInteraction(
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
 * @param interaction interaction handler
 * @param failure callback for failure
 * @param success callback for success
 */
public fun ReplyCallbackAction.queueAndSetMessageId(
    interaction: DiscordInteractionHandler,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in InteractionHook>? = null
): Unit = interaction.discordBot.queueAndSetMessageId(this, interaction, failure, success)

/**
 * The coroutine scope used by the underlying [CoroutineEventManager].
 * If this instance does not use the coroutine event manager, this returns the default scope from [getDefaultJDAScope].
 */
public val JDA.scope: CoroutineScope get() = (eventManager as? CoroutineEventManager) ?: AlunaDispatchers.InternalScope

/**
 * The coroutine scope used by the underlying [CoroutineEventManager].
 * If this instance does not use the coroutine event manager, this returns the default scope from [getDefaultJDAScope].
 */
public val ShardManager.scope: CoroutineScope get() = (shardCache.firstOrNull()?.eventManager as? CoroutineEventManager) ?: AlunaDispatchers.InternalScope

internal typealias InteractionId = String
internal typealias OptionName = String
internal typealias CooldownKey = String
internal typealias LastUsage = LocalDateTime

public typealias UserId = Long
public typealias GuildId = Long
public typealias ChannelId = Long
public typealias MessageId = String
