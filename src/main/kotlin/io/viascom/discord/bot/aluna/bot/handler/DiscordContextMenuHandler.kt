/*
 * Copyright 2023 Viascom Ltd liab. Co
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

package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.InteractionScopedObject
import io.viascom.discord.bot.aluna.configuration.scope.InteractionScope
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.model.*
import io.viascom.discord.bot.aluna.model.TimeMarkStep.EXIT_COMMAND
import io.viascom.discord.bot.aluna.property.AlunaProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import java.time.Duration
import java.util.*
import kotlin.time.TimeSource.Monotonic.markNow

abstract class DiscordContextMenuHandler(
    type: Command.Type,
    name: String,

    /**
     * Define a [LocalizationFunction] for this command. If set no null, Aluna will take the implementation of [DiscordInteractionLocalization].
     */
    var localizations: LocalizationFunction? = null
) : CommandDataImpl(type, name), InteractionScopedObject, DiscordInteractionHandler {

    @Autowired
    lateinit var alunaProperties: AlunaProperties

    @Autowired
    lateinit var discordInteractionConditions: DiscordInteractionConditions

    @Autowired
    lateinit var discordInteractionAdditionalConditions: DiscordInteractionAdditionalConditions

    @Autowired
    lateinit var discordInteractionLoadAdditionalData: DiscordInteractionLoadAdditionalData

    @Autowired
    lateinit var discordInteractionMetaDataHandler: DiscordInteractionMetaDataHandler

    @Autowired
    lateinit var eventPublisher: EventPublisher

    @Autowired
    override lateinit var discordBot: DiscordBot

    @Autowired
    private lateinit var configurableListableBeanFactory: ConfigurableListableBeanFactory

    @Autowired(required = false)
    lateinit var localizationProvider: DiscordInteractionLocalization

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * This gets set by the CommandContext automatically and should not be changed
     */
    override var uniqueId: String = ""

    /**
     * If true, this command is only seen by users with the administrator permission on the server by default!
     * Aluna will set `this.defaultPermissions = DefaultMemberPermissions.DISABLED` if true.
     */
    var isAdministratorOnlyCommand = false

    var interactionDevelopmentStatus = DevelopmentStatus.LIVE

    /**
     * Defines the use scope of this command.
     *
     * *This gets mapped to [isGuildOnly] if set to [UseScope.GUILD_ONLY].*
     */
    var useScope = UseScope.GLOBAL

    override var beanTimoutDelay: Duration = Duration.ofMinutes(14)
    override var beanUseAutoCompleteBean: Boolean = false
    override var beanRemoveObserverOnDestroy: Boolean = true
    override var beanResetObserverTimeoutOnBeanExtend: Boolean = true
    override var beanCallOnDestroy: Boolean = true
    override var freshInstance: Boolean = true

    /**
     * Discord representation of this interaction
     */
    lateinit var discordRepresentation: Command

    /**
     * Any [Permission]s a Member must have to use this command.
     * <br></br>These are only checked in a [Guild] environment.
     */
    var userPermissions = arrayListOf<Permission>()

    /**
     * Any [Permission]s the bot must have to use a command.
     * <br></br>These are only checked in a [Guild] environment.
     */
    var botPermissions = arrayListOf<Permission>()

    /**
     * [Channel] in which the command was used in.
     */
    var channel: Channel? = null

    /**
     * [Author][User] of the command
     */
    override lateinit var author: User

    /**
     * [Guild] in which the command was used in. Can be null if the command was used in direct messages.
     */
    var guild: Guild? = null

    /**
     * [GuildChannel] in which the command was used in. Can be null if the command was used in direct messages.
     */
    var guildChannel: GuildChannel? = null

    /**
     * [Member] which used the command. Can be null if the command was used in direct messages.
     */
    var member: Member? = null

    /**
     * User [Locale]
     *
     * *This is set by Aluna based on the information provided by Discord*
     */
    var userLocale: DiscordLocale = DiscordLocale.ENGLISH_US

    /**
     * Guild [Locale]
     *
     * *This is set by Aluna based on the information provided by Discord*
     */
    var guildLocale: DiscordLocale = DiscordLocale.ENGLISH_US

    /**
     * TimeMarks used if enabled by properties
     */
    @set:JvmSynthetic
    var timeMarks: ArrayList<TimeMarkRecord>? = null
        internal set


    @JvmSynthetic
    internal abstract suspend fun runOnButtonInteraction(event: ButtonInteractionEvent): Boolean

    @JvmSynthetic
    internal abstract suspend fun runOnButtonInteractionTimeout()

    @JvmSynthetic
    internal abstract suspend fun runOnStringSelectInteraction(event: StringSelectInteractionEvent): Boolean

    @JvmSynthetic
    internal abstract suspend fun runOnStringSelectInteractionTimeout()

    @JvmSynthetic
    internal abstract suspend fun runOnEntitySelectInteraction(event: EntitySelectInteractionEvent): Boolean

    @JvmSynthetic
    internal abstract suspend fun runOnEntitySelectInteractionTimeout()

    @JvmSynthetic
    internal abstract suspend fun runOnModalInteraction(event: ModalInteractionEvent): Boolean

    @JvmSynthetic
    internal abstract suspend fun runOnModalInteractionTimeout()

    fun prepareInteraction() {
        if (isAdministratorOnlyCommand) {
            this.defaultPermissions = DefaultMemberPermissions.DISABLED
        }
        this.isGuildOnly = (useScope == UseScope.GUILD_ONLY)

        if (!alunaProperties.productionMode) {
            if ((isAdministratorOnlyCommand || this.defaultPermissions == DefaultMemberPermissions.DISABLED) && !this.isGuildOnly) {
                logger.warn("The interaction '$name' has a default permission for administrator only but is not restricted to guild only. All users will be able to use this interaction in DMs with your bot!")
            }
            if (this.defaultPermissions != DefaultMemberPermissions.ENABLED && this.defaultPermissions != DefaultMemberPermissions.DISABLED && !this.isGuildOnly) {
                logger.warn("The interaction '$name' has a default permission restriction for a specific user permission but is not restricted to guild only. All users will be able to use this interaction in DMs with your bot!")
            }
        }
    }

    fun prepareLocalization() {
        if (alunaProperties.translation.enabled) {
            if (localizations == null) {
                localizations = localizationProvider.getLocalizationFunction()
            }

            this.setLocalizationFunction(localizations!!)
        }
    }

    open fun onMissingUserPermission(event: GenericCommandInteractionEvent, missingPermissions: MissingPermissions) {
        val textChannelPermissions = missingPermissions.textChannel.joinToString("\n") { "└ ${it.getName()}" }
        val voiceChannelPermissions = missingPermissions.voiceChannel.joinToString("\n") { "└ ${it.getName()}" }
        val guildPermissions = missingPermissions.guild.joinToString("\n") { "└ ${it.getName()}" }
        event.deferReply(true).setContent(
            "⛔ You are missing the following permission to execute this interaction:\n" +
                    (if (textChannelPermissions.isNotBlank()) textChannelPermissions + "\n" else "") +
                    (if (voiceChannelPermissions.isNotBlank()) voiceChannelPermissions + "\n" else "") +
                    (if (guildPermissions.isNotBlank()) guildPermissions + "\n" else "")
        ).queue()
    }

    open fun onMissingBotPermission(event: GenericCommandInteractionEvent, missingPermissions: MissingPermissions) {
        when {
            missingPermissions.notInVoice -> {
                event.deferReply(true)
                    .setContent("⛔ You need to be in a voice channel yourself to execute this interaction").queue()

            }

            (missingPermissions.hasMissingPermissions) -> {
                event.deferReply(true).setContent("⛔ I'm missing the following permission to execute this interaction:\n" +
                        missingPermissions.textChannel.joinToString("\n") { "└ ${it.getName()}" } + "\n" +
                        missingPermissions.voiceChannel.joinToString("\n") { "└ ${it.getName()}" } + "\n" +
                        missingPermissions.guild.joinToString("\n") { "└ ${it.getName()}" }
                ).queue()
            }
        }
    }

    open fun onFailedAdditionalRequirements(event: GenericCommandInteractionEvent, additionalRequirements: AdditionalRequirements) {
        event.deferReply(true).setContent("⛔ Additional requirements for this interaction failed.").queue()
    }

    open fun onExecutionException(event: GenericCommandInteractionEvent, exception: Exception) {
        throw exception
    }

    @JvmSynthetic
    internal suspend fun exitCommand(event: GenericCommandInteractionEvent) = withContext(AlunaDispatchers.Detached) {
        launch {
            timeMarks?.add(EXIT_COMMAND at markNow())

            if (alunaProperties.debug.useTimeMarks && timeMarks != null) {

                val duration = timeMarks!!.getDuration()
                val executeDuration = timeMarks!!.getDurationRunExecute()
                val neededUserPermissionsDuration = timeMarks!!.getDurationNeededUserPermissions()
                val neededBotPermissionsDuration = timeMarks!!.getDurationNeededBotPermissions()
                val loadDataBeforeAdditionalRequirementsDuration = timeMarks!!.getDurationLoadDataBeforeAdditionalRequirements()
                val checkForAdditionalCommandRequirementsDuration = timeMarks!!.getDurationCheckForAdditionalCommandRequirements()
                val loadAdditionalDataDuration = timeMarks!!.getDurationLoadAdditionalData()
                MDC.put("duration", duration.inWholeMilliseconds.toString())

                if (alunaProperties.debug.showDetailTimeMarks) {
                    neededUserPermissionsDuration?.let { MDC.put("duration-details.neededUserPermissionsDuration", it.inWholeMilliseconds.toString()) }
                    neededBotPermissionsDuration?.let { MDC.put("duration-details.neededBotPermissionsDuration", it.inWholeMilliseconds.toString()) }
                    loadDataBeforeAdditionalRequirementsDuration?.let {
                        MDC.put(
                            "duration-details.loadDataBeforeAdditionalRequirementsDuration",
                            it.inWholeMilliseconds.toString()
                        )
                    }
                    checkForAdditionalCommandRequirementsDuration?.let {
                        MDC.put(
                            "duration-details.checkForAdditionalCommandRequirementsDuration",
                            it.inWholeMilliseconds.toString()
                        )
                    }
                    loadAdditionalDataDuration?.let { MDC.put("duration-details.loadAdditionalDataDuration", it.inWholeMilliseconds.toString()) }
                    executeDuration?.let { MDC.put("duration-details.executeDuration", it.inWholeMilliseconds.toString()) }
                }

                logger.info("Context menu '${event.fullCommandName}' (${this@DiscordContextMenuHandler.author.id})${if (alunaProperties.debug.showHashCode) " [${this@DiscordContextMenuHandler.hashCode()}]" else ""} took $duration (execute method: $executeDuration)")

                val beforeDuration = kotlin.time.Duration.ZERO
                neededUserPermissionsDuration?.let { beforeDuration.plus(it) }
                neededUserPermissionsDuration?.let { beforeDuration.plus(it) }
                neededBotPermissionsDuration?.let { beforeDuration.plus(it) }
                loadDataBeforeAdditionalRequirementsDuration?.let { beforeDuration.plus(it) }
                checkForAdditionalCommandRequirementsDuration?.let { beforeDuration.plus(it) }
                loadAdditionalDataDuration?.let { beforeDuration.plus(it) }

                when {
                    beforeDuration.inWholeMilliseconds > 1500 -> {
                        logger.warn("The execution of the context menu ${event.fullCommandName} until calling your execute method took over 1.5 seconds. Make sure you don't do time consuming tasks in you implementation of DiscordInteractionLoadAdditionalData, DefaultDiscordInteractionAdditionalConditions or DiscordInteractionConditions.")
                    }
                }

                when {
                    (duration.inWholeMilliseconds > 3000) -> logger.warn("The execution of the context menu ${event.fullCommandName} until it got completed took longer than 3 second. Make sure you acknowledge the event as fast as possible. If it got acknowledge at the end of the method, the interaction token was no longer valid.")
                    (duration.inWholeMilliseconds > 1500) -> logger.warn("The execution of the context menu ${event.fullCommandName} until it got completed took longer than 1.5 second. Make sure that you acknowledge the event as fast as possible. Because the initial interaction token is only 3 seconds valid.")
                }

                if (alunaProperties.debug.showDetailTimeMarks) {
                    logger.info(timeMarks!!.printTimeMarks(event.fullCommandName))
                }
            }
        }

        launch {
            discordInteractionMetaDataHandler.onExitInteraction(this@DiscordContextMenuHandler, timeMarks, event)
        }
    }

    fun updateMessageIdForScope(messageId: String) {
        val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
        interactionScope.setMessageIdForInstance(uniqueId, messageId)
    }

    /**
     * Destroy this bean instance. This will remove the bean from the interaction scope as well as remove the bean timout.
     *
     * @param removeObservers Remove all observers
     * @param removeObserverTimeouts Remove all observer timeouts
     * @param callOnDestroy Call onDestroy of this bean
     * @param callButtonTimeout Call onButtonInteractionTimeout of this bean
     * @param callStringSelectTimeout Call onStringSelectInteractionTimeout of this bean
     * @param callEntitySelectTimeout Call onEntitySelectInteractionTimeout of this bean
     * @param callModalTimeout Call onModalInteractionTimeout of this bean
     */
    suspend fun destroyThisInstance(
        removeObservers: Boolean = true,
        removeObserverTimeouts: Boolean = true,
        callOnDestroy: Boolean = false,
        callButtonTimeout: Boolean = false,
        callStringSelectTimeout: Boolean = false,
        callEntitySelectTimeout: Boolean = false,
        callModalTimeout: Boolean = false
    ) = withContext(AlunaDispatchers.Interaction) {
        val interactionScope = configurableListableBeanFactory.getRegisteredScope("interaction") as InteractionScope
        interactionScope.removeByUniqueId(uniqueId)

        val buttonObserver = discordBot.messagesToObserveButton.entries.firstOrNull { it.value.uniqueId == uniqueId }
        val stringSelectObserver = discordBot.messagesToObserveStringSelect.entries.firstOrNull { it.value.uniqueId == uniqueId }
        val entitySelectObserver = discordBot.messagesToObserveEntitySelect.entries.firstOrNull { it.value.uniqueId == uniqueId }
        val modalObserver = discordBot.messagesToObserveModal.entries.firstOrNull { it.value.uniqueId == uniqueId }

        if (removeObservers) {
            buttonObserver?.key?.let { discordBot.messagesToObserveButton.remove(it) }
            stringSelectObserver?.key?.let { discordBot.messagesToObserveStringSelect.remove(it) }
            entitySelectObserver?.key?.let { discordBot.messagesToObserveEntitySelect.remove(it) }
            modalObserver?.key?.let { discordBot.messagesToObserveModal.remove(it) }
        }

        if (removeObserverTimeouts) {
            buttonObserver?.value?.timeoutTask?.cancel(true)
            stringSelectObserver?.value?.timeoutTask?.cancel(true)
            entitySelectObserver?.value?.timeoutTask?.cancel(true)
            modalObserver?.value?.timeoutTask?.cancel(true)
        }

        launch { if (callOnDestroy) runOnDestroy() }
        launch { if (callButtonTimeout) buttonObserver?.let { runOnButtonInteractionTimeout() } }
        launch { if (callStringSelectTimeout) stringSelectObserver?.let { runOnStringSelectInteractionTimeout() } }
        launch { if (callEntitySelectTimeout) entitySelectObserver?.let { runOnEntitySelectInteractionTimeout() } }
        launch { if (callModalTimeout) modalObserver?.let { runOnModalInteractionTimeout() } }
    }

    override suspend fun handleOnButtonInteraction(event: ButtonInteractionEvent): Boolean = runOnButtonInteraction(event)
    override suspend fun handleOnButtonInteractionTimeout() = runOnButtonInteractionTimeout()
    override suspend fun handleOnStringSelectInteraction(event: StringSelectInteractionEvent) = runOnStringSelectInteraction(event)
    override suspend fun handleOnStringSelectInteractionTimeout() = runOnStringSelectInteractionTimeout()
    override suspend fun handleOnEntitySelectInteraction(event: EntitySelectInteractionEvent) = runOnEntitySelectInteraction(event)
    override suspend fun handleOnEntitySelectInteractionTimeout() = runOnEntitySelectInteractionTimeout()
    override suspend fun handleOnModalInteraction(event: ModalInteractionEvent) = runOnModalInteraction(event)
    override suspend fun handleOnModalInteractionTimeout() = runOnModalInteractionTimeout()
}
