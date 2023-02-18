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
import io.viascom.discord.bot.aluna.bot.handler.*
import io.viascom.discord.bot.aluna.configuration.scope.InteractionScope
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.exception.AlunaInteractionRepresentationNotFoundException
import io.viascom.discord.bot.aluna.model.AdditionalRequirements
import io.viascom.discord.bot.aluna.model.DevelopmentStatus
import io.viascom.discord.bot.aluna.model.MissingPermissions
import io.viascom.discord.bot.aluna.model.UseScope
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.property.OwnerIdProvider
import io.viascom.discord.bot.aluna.util.InternalUtil
import io.viascom.discord.bot.aluna.util.TimestampFormat
import io.viascom.discord.bot.aluna.util.toDiscordTimestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.util.StopWatch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible

abstract class DiscordCommand @JvmOverloads constructor(
    name: String,
    description: String,

    /**
     * Define a [LocalizationFunction] for this command. If set no null, Aluna will take the implementation of [DiscordInteractionLocalization].
     */
    var localizations: LocalizationFunction? = null,

    /**
     * If enabled, Aluna will register an event listener for auto complete requests and link it to this command.
     *
     * If such an event gets triggered, the method [onAutoCompleteEvent] will be invoked.
     */
    val observeAutoComplete: Boolean = false,

    /**
     * If enabled, Aluna will automatically forward the command execution as well as interaction events to the matching sub command.
     *
     * For this to work, you need to annotate your autowired [DiscordSubCommand] or [DiscordSubCommandGroup] implementation with [@SubCommandElement][SubCommandElement]
     * or register them manually with [registerSubCommands] during [initSubCommands].
     *
     * The Top-Level command can not be used (limitation of Discord), but Aluna will nevertheless always call [execute] on the top-level command before executing the sub command method if you need to do some general stuff.
     */
    val handleSubCommands: Boolean = false,

    /**
     * If enabled, Aluna will direct matching interactions to this command.
     * If a matching instance of this command (based on uniqueId or message) is found, the corresponding method is called. If not, a new instance gets created.
     */
    val handlePersistentInteractions: Boolean = false
) : CommandDataImpl(name, description), SlashCommandData, InteractionScopedObject, DiscordInteractionHandler {

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

    @Autowired
    lateinit var ownerIdProvider: OwnerIdProvider

    @Autowired(required = false)
    lateinit var localizationProvider: DiscordInteractionLocalization

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * This gets set by the CommandContext automatically and should not be changed
     */
    override var uniqueId: String = ""

    /**
     * Defines the use scope of this command.
     *
     * *This gets mapped to [isGuildOnly] if set to [UseScope.GUILD_ONLY].*
     */
    var useScope = UseScope.GLOBAL

    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var specificServer: String? = null

    /**
     * Sets whether this command can only be used by users which are returned by [OwnerIdProvider.getOwnerIds].
     */
    var isOwnerCommand = false

    /**
     * Sets whether this command can only be seen by users with the administrator permission on the server!
     *
     * ! Aluna will set `this.defaultPermissions = DefaultMemberPermissions.DISABLED` if true.
     */
    var isAdministratorOnlyCommand = false

    /**
     * Sets whether this command should redirect auto complete events to the corresponding sub commands
     */
    var redirectAutoCompleteEventsToSubCommands: Boolean = true

    /**
     * Interaction development status
     */
    var interactionDevelopmentStatus = DevelopmentStatus.LIVE

    override var beanTimoutDelay: Duration = Duration.ofMinutes(14)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = true
    override var beanResetObserverTimeoutOnBeanExtend: Boolean = true
    override var beanCallOnDestroy: Boolean = true
    override var freshInstance: Boolean = true

    /**
     * Discord representation of this interaction
     */
    @set:JvmSynthetic
    lateinit var discordRepresentation: Command
        internal set

    private val subCommandElements: HashMap<String, DiscordSubCommandElement> = hashMapOf()

    /**
     * Current sub command path gets set when the command gets used.
     *
     * *This variable is used by the internal sub command handling.*
     */
    private var currentSubFullCommandName: String = ""

    /**
     * The [CooldownScope][CooldownScope] of the command.
     */
    var cooldownScope = CooldownScope.NO_COOLDOWN

    var cooldown: Duration = Duration.ZERO

    /**
     * Any [Permission]s a Member must have to use this command.
     *
     * These are only checked in a [Guild] environment.
     */
    var userPermissions = arrayListOf<Permission>()

    /**
     * Any [Permission]s the bot must have to use a command.
     *
     *These are only checked in a [Guild] environment.
     */
    var botPermissions = arrayListOf<Permission>()

    /**
     * [MessageChannel] in which the command was used in.
     */
    lateinit var channel: MessageChannel

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
     * Stop watch used if enabled by properties
     */
    var stopWatch: StopWatch? = null

    /**
     * Method to implement for command execution
     *
     * @param event The [SlashCommandInteractionEvent] that triggered this Command
     */

    protected abstract fun execute(event: SlashCommandInteractionEvent)

    /**
     * This method gets triggered, as soon as a button event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForButtonEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [ButtonInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */

    override fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return if (handleSubCommands) {
            handleSubCommand(event, { it.onButtonInteraction(event) }, { onSubCommandInteractionFallback(event) })
        } else {
            true
        }
    }

    /**
     * This method gets triggered, as soon as a button event observer duration timeout is reached.
     */

    override fun onButtonInteractionTimeout(additionalData: HashMap<String, Any?>) {
        if (handleSubCommands) {
            handleSubCommand(null, {
                it.onButtonInteractionTimeout(additionalData)
                true
            }, {
                onSubCommandInteractionTimeoutFallback()
                true
            })
        }
    }

    /**
     * This method gets triggered, as soon as a select event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForSelectEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [StringSelectInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    override fun onStringSelectInteraction(
        event: StringSelectInteractionEvent,
        additionalData: HashMap<String, Any?>
    ): Boolean {
        return if (handleSubCommands) {
            handleSubCommand(event, { it.onStringSelectInteraction(event) }, { onSubCommandInteractionFallback(event) })
        } else {
            true
        }
    }

    /**
     * This method gets triggered, as soon as a select event observer duration timeout is reached.
     */
    override fun onStringSelectInteractionTimeout(additionalData: HashMap<String, Any?>) {
        if (handleSubCommands) {
            handleSubCommand(null, {
                it.onStringSelectInteractionTimeout(additionalData)
                true
            }, {
                onSubCommandInteractionTimeoutFallback()
                true
            })
        }
    }

    /**
     * This method gets triggered, as soon as a select event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForSelectEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [EntitySelectInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    override fun onEntitySelectInteraction(
        event: EntitySelectInteractionEvent,
        additionalData: HashMap<String, Any?>
    ): Boolean {
        return if (handleSubCommands) {
            handleSubCommand(event, { it.onEntitySelectInteraction(event) }, { onSubCommandInteractionFallback(event) })
        } else {
            true
        }
    }

    override fun onEntitySelectInteractionTimeout(additionalData: HashMap<String, Any?>) {
        if (handleSubCommands) {
            handleSubCommand(null, {
                it.onEntitySelectInteractionTimeout(additionalData)
                true
            }, {
                onSubCommandInteractionTimeoutFallback()
                true
            })
        }
    }

    /**
     * This method gets triggered, as soon as a modal event for this command is called.
     * Make sure that you register your message id: `discordBot.registerMessageForModalEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [ModalInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */

    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return if (handleSubCommands) {
            handleSubCommand(
                event,
                { it.onModalInteraction(event, additionalData) },
                { onSubCommandInteractionFallback(event) })
        } else {
            true
        }
    }

    /**
     * This method gets triggered, as soon as a modal event observer duration timeout is reached.
     */

    override fun onModalInteractionTimeout(additionalData: HashMap<String, Any?>) {
        if (handleSubCommands) {
            handleSubCommand(null, {
                it.onModalInteractionTimeout(additionalData)
                true
            }, {
                onSubCommandInteractionTimeoutFallback()
                true
            })
        }
    }

    /**
     * On destroy gets called, when the object gets destroyed after the defined beanTimoutDelay.
     */

    open fun onDestroy() {
    }

    /**
     * This method gets triggered, as soon as an auto complete event for this command is called.
     * This will always use the same instance if user and server is the same. The command itself will than override this instance.
     * Before calling this method, Aluna will execute discordCommandLoadAdditionalData.loadData()
     *
     * @param option name of the option
     * @param event [CommandAutoCompleteInteractionEvent] this method is based on
     */

    open fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
    }

    @JvmSynthetic
    internal fun onAutoCompleteEventCall(option: String, event: CommandAutoCompleteInteractionEvent) {
        setProperties(event)

        currentSubFullCommandName = event.fullCommandName
        MDC.put("interaction", event.fullCommandName)

        discordInteractionLoadAdditionalData.loadDataBeforeAdditionalRequirements(this, event)

        //Check additional requirements for this command
        val additionalRequirements =
            discordInteractionAdditionalConditions.checkForAdditionalCommandRequirements(this, event)
        if (additionalRequirements.failed) {
            onFailedAdditionalRequirements(event, additionalRequirements)
            return
        }

        discordInteractionLoadAdditionalData.loadData(this, event)

        if (handleSubCommands && redirectAutoCompleteEventsToSubCommands) {
            handleSubCommand(event, {
                it.onAutoCompleteEvent(option, event)
                true
            }, { onSubCommandInteractionFallback(event) })
        } else {
            onAutoCompleteEvent(option, event)
        }
    }

    @JvmSynthetic
    internal fun setProperties(event: GenericInteractionCreateEvent) {
        MDC.put("uniqueId", uniqueId)

        guild = event.guild
        guild?.let { MDC.put("discord.server", "${it.id} (${it.name})") }
        author = event.user
        MDC.put("discord.author", "${author.id} (${author.asTag})")

        userLocale = event.userLocale
        MDC.put("discord.author_locale", userLocale.locale)

        if (guild != null) {
            member = guild!!.getMember(author)
            guildChannel = event.guildChannel
            guildLocale = event.guildLocale
            MDC.put("discord.server_locale", guildLocale.locale)
        }
    }

    /**
     * This method gets called if Aluna can not find a registered sub command
     *
     * @param event Original [SlashCommandInteractionEvent]
     */
    open fun onSubCommandFallback(event: SlashCommandInteractionEvent) {
    }

    /**
     * This method gets called if Aluna can not find a registered sub command for an interaction event
     *
     * @param event Original [SlashCommandInteractionEvent]
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    open fun onSubCommandInteractionFallback(event: GenericInteractionCreateEvent): Boolean {
        return true
    }

    /**
     * This method gets called if Aluna can not find a registered sub command for an interaction timeout
     */
    open fun onSubCommandInteractionTimeoutFallback() {
    }

    open fun onOwnerCommandNotAllowedByUser(event: SlashCommandInteractionEvent) {
        event.deferReply(true).setContent("⛔ This command is to powerful for you.").queue()
    }

    open fun onMissingUserPermission(event: SlashCommandInteractionEvent, missingPermissions: MissingPermissions) {
        val textChannelPermissions = missingPermissions.textChannel.joinToString("\n") { "└ ${it.getName()}" }
        val voiceChannelPermissions = missingPermissions.voiceChannel.joinToString("\n") { "└ ${it.getName()}" }
        val guildPermissions = missingPermissions.guild.joinToString("\n") { "└ ${it.getName()}" }
        event.deferReply(true).setContent(
            "⛔ You are missing the following permission to execute this command:\n" +
                    (if (textChannelPermissions.isNotBlank()) textChannelPermissions + "\n" else "") +
                    (if (voiceChannelPermissions.isNotBlank()) voiceChannelPermissions + "\n" else "") +
                    (if (guildPermissions.isNotBlank()) guildPermissions + "\n" else "")
        ).queue()
    }

    open fun onMissingBotPermission(event: SlashCommandInteractionEvent, missingPermissions: MissingPermissions) {
        when {
            missingPermissions.notInVoice -> {
                event.deferReply(true)
                    .setContent("⛔ You need to be in a voice channel yourself to execute this command").queue()

            }

            (missingPermissions.hasMissingPermissions) -> {
                event.deferReply(true).setContent("⛔ I'm missing the following permission to execute this command:\n" +
                        missingPermissions.textChannel.joinToString("\n") { "└ ${it.getName()}" } + "\n" +
                        missingPermissions.voiceChannel.joinToString("\n") { "└ ${it.getName()}" } + "\n" +
                        missingPermissions.guild.joinToString("\n") { "└ ${it.getName()}" }
                ).queue()
            }
        }
    }

    open fun onFailedAdditionalRequirements(
        event: SlashCommandInteractionEvent,
        additionalRequirements: AdditionalRequirements
    ) {
        event.deferReply(true).setContent("⛔ Additional requirements for this command failed.").queue()
    }

    open fun onCooldownStillActive(
        event: SlashCommandInteractionEvent,
        lastUse: LocalDateTime
    ) {
        event.deferReply(true)
            .setContent("⛔ This interaction is still on cooldown and will be usable ${lastUse.plusNanos(cooldown.toNanos()).toDiscordTimestamp(TimestampFormat.RELATIVE_TIME)}.")
            .queue()
    }

    open fun onFailedAdditionalRequirements(
        event: CommandAutoCompleteInteractionEvent,
        additionalRequirements: AdditionalRequirements
    ) {
    }

    open fun onExecutionException(event: SlashCommandInteractionEvent, exception: Exception) {
        throw exception
    }

    open fun initCommandOptions() {}
    open fun initSubCommands() {}

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

        loadDynamicSubCommandElements()
    }

    private fun loadDynamicSubCommandElements() {
        if (subCommandElements.isEmpty()) {
            initSubCommands()
        }

        if (subCommandElements.isEmpty()) {
            InternalUtil.getSubCommandElements(this).forEach { field ->
                field.isAccessible = true
                registerSubCommands(field.getter.call(this) as DiscordSubCommandElement)
            }
        }
    }

    fun prepareLocalization() {
        if (alunaProperties.translation.enabled) {
            if (localizations == null) {
                localizations = localizationProvider.getLocalizationFunction()
            }

            this.setLocalizationFunction(localizations!!)
            this.toData()
        }
    }

    /**
     * Runs checks for the [DiscordCommand] with the given [SlashCommandInteractionEvent] that called it.
     *
     * @param event The CommandEvent that triggered this Command
     */

    @JvmSynthetic
    internal fun run(event: SlashCommandInteractionEvent) {
        val command = this

        if (alunaProperties.debug.useStopwatch) {
            stopWatch = StopWatch()
            stopWatch!!.start()
        }

        if (!discordBot.discordRepresentations.containsKey(event.name)) {
            val exception = AlunaInteractionRepresentationNotFoundException(event.name)
            try {
                onExecutionException(event, exception)
            } catch (exceptionError: Exception) {
                discordInteractionMetaDataHandler.onGenericExecutionException(command, exception, exceptionError, event)
            }
            return
        }

        discordRepresentation = discordBot.discordRepresentations[event.name]!!

        setProperties(event)

        currentSubFullCommandName = event.fullCommandName
        MDC.put("interaction", event.fullCommandName)
        channel = event.channel
        MDC.put("discord.channel", channel.id)

        //Check if this is an owner command
        if (isOwnerCommand && author.idLong !in ownerIdProvider.getOwnerIds()) {
            onOwnerCommandNotAllowedByUser(event)
            return
        }

        //Check needed user permissions for this command
        val missingUserPermissions =
            discordInteractionConditions.checkForNeededUserPermissions(this, userPermissions, event)
        if (missingUserPermissions.hasMissingPermissions) {
            onMissingUserPermission(event, missingUserPermissions)
            return
        }

        //Check needed bot permissions for this command
        val missingBotPermissions =
            discordInteractionConditions.checkForNeededBotPermissions(this, botPermissions, event)
        if (missingBotPermissions.hasMissingPermissions) {
            onMissingBotPermission(event, missingBotPermissions)
            return
        }

        //checkForCommandCooldown(event)

        discordInteractionLoadAdditionalData.loadDataBeforeAdditionalRequirements(this, event)

        //Check additional requirements for this command
        val additionalRequirements =
            discordInteractionAdditionalConditions.checkForAdditionalCommandRequirements(this, event)
        if (additionalRequirements.failed) {
            onFailedAdditionalRequirements(event, additionalRequirements)
            return
        }

        //Check for cooldown
        val cooldownKey = discordBot.getCooldownKey(cooldownScope, discordRepresentation.id, author.id, channel.id, guild?.id)
        if (cooldownScope != CooldownScope.NO_COOLDOWN) {
            if (discordBot.isCooldownActive(cooldownKey, cooldown)) {
                onCooldownStillActive(event, discordBot.cooldowns[cooldownKey]!!)
                return
            }
        }
        discordBot.cooldowns[cooldownKey] = LocalDateTime.now(ZoneOffset.UTC)


        //Load additional data for this command
        discordInteractionLoadAdditionalData.loadData(this, event)

        runBlocking(AlunaCoroutinesDispatcher.Default) {
            //Run onCommandExecution in async to ensure it is not blocking the execution of the command itself
            async(AlunaCoroutinesDispatcher.IO) { discordInteractionMetaDataHandler.onCommandExecution(command, event) }
            async(AlunaCoroutinesDispatcher.IO) {
                if (alunaProperties.discord.publishDiscordCommandEvent) {
                    eventPublisher.publishDiscordCommandEvent(author, channel, guild, event.fullCommandName, command)
                }
            }

            try {
                logger.info("Run command /${event.fullCommandName}" + if (alunaProperties.debug.showHashCode) " [${command.hashCode()}]" else "")
                execute(event)
                if (handleSubCommands) {
                    logger.debug("Handle sub command /${event.fullCommandName}")
                    handleSubCommandExecution(event) { onSubCommandFallback(event) }
                }
            } catch (e: Exception) {
                try {
                    onExecutionException(event, e)
                } catch (exceptionError: Exception) {
                    discordInteractionMetaDataHandler.onGenericExecutionException(command, e, exceptionError, event)
                }
            } finally {
                exitCommand(event)
            }
        }
    }

    @JvmSynthetic
    internal fun exitCommand(event: SlashCommandInteractionEvent) {
        val command = this
        runBlocking(AlunaCoroutinesDispatcher.Default) {
            launch(AlunaCoroutinesDispatcher.Default) {
                if (alunaProperties.debug.useStopwatch && stopWatch != null) {
                    stopWatch!!.stop()
                    MDC.put("duration", stopWatch!!.totalTimeMillis.toString())
                    logger.info("Command /${event.fullCommandName} (${command.author.id})${if (alunaProperties.debug.showHashCode) " [${command.hashCode()}]" else ""} took ${stopWatch!!.totalTimeMillis}ms")
                    when {
                        (stopWatch!!.totalTimeMillis > 3000) -> logger.warn("The execution of the command /${event.fullCommandName} until it got completed took longer than 3 second. Make sure you acknowledge the event as fast as possible. If it got acknowledge at the end of the method, the interaction token was no longer valid.")
                        (stopWatch!!.totalTimeMillis > 1500) -> logger.warn("The execution of the command /${event.fullCommandName} until it got completed took longer than 1.5 second. Make sure that you acknowledge the event as fast as possible. Because the initial interaction token is only 3 seconds valid.")
                    }
                }

            }
            launch(AlunaCoroutinesDispatcher.IO) {
                discordInteractionMetaDataHandler.onExitInteraction(command, stopWatch, event)
            }
        }
    }

    open fun registerSubCommands(vararg elements: DiscordSubCommandElement) {
        elements
            .filter { element ->
                when {
                    alunaProperties.includeInDevelopmentInteractions -> true
                    alunaProperties.productionMode && (element::class.isSubclassOf(DiscordSubCommand::class)) && (element as DiscordSubCommand).interactionDevelopmentStatus == DevelopmentStatus.IN_DEVELOPMENT -> false
                    alunaProperties.productionMode && (element::class.isSubclassOf(DiscordSubCommandGroup::class)) && (element as DiscordSubCommandGroup).interactionDevelopmentStatus == DevelopmentStatus.IN_DEVELOPMENT -> false
                    else -> true
                }
            }
            .forEach { element ->
                subCommandElements.putIfAbsent(element.getName(), element)

                if (element::class.isSubclassOf(DiscordSubCommand::class)) {
                    element as DiscordSubCommand
                    element.parentCommand = this
                    element.initCommandOptions()
                    this.addSubcommands(element)
                }

                if (element::class.isSubclassOf(DiscordSubCommandGroup::class)) {
                    element as DiscordSubCommandGroup
                    element.initSubCommands()
                    this.addSubcommandGroups(element)
                }
            }
    }

    open fun handleSubCommandExecution(
        event: SlashCommandInteractionEvent,
        fallback: (SlashCommandInteractionEvent) -> (Unit)
    ) {
        loadDynamicSubCommandElements()

        val path = event.fullCommandName.split(" ")
        discordRepresentation = discordBot.discordRepresentations[path[0]]!!

        val firstLevel = path[1]
        if (!subCommandElements.containsKey(firstLevel)) {
            logger.debug("Command path '${event.fullCommandName}' not found in the registered elements")
            fallback.invoke(event)
            return
        }

        val firstElement = subCommandElements[firstLevel]!!

        //Check if it is a SubCommand
        if (firstElement::class.isSubclassOf(DiscordSubCommand::class)) {
            (firstElement as DiscordSubCommand).initialize(event.fullCommandName, this, discordRepresentation)
            firstElement.run(event)
            return
        }

        //Check if it is a SubCommand in a SubCommandGroup
        val secondLevel = path[2]
        if (!(firstElement as DiscordSubCommandGroup).subCommands.containsKey(secondLevel)) {
            logger.debug("Command path '${event.fullCommandName}' not found in the registered elements")
            fallback.invoke(event)
            return
        }

        firstElement.subCommands[secondLevel]!!.initialize(event.fullCommandName, this, discordRepresentation)
        firstElement.subCommands[secondLevel]!!.run(event)
    }

    open fun handleSubCommand(
        event: GenericInteractionCreateEvent?,
        function: (DiscordSubCommand) -> (Boolean),
        fallback: (GenericInteractionCreateEvent?) -> (Boolean)
    ): Boolean {
        loadDynamicSubCommandElements()

        val path = currentSubFullCommandName.split(" ")
        discordRepresentation = discordBot.discordRepresentations[path[0]]!!

        val firstLevel = path[1]
        if (!subCommandElements.containsKey(firstLevel)) {
            logger.debug("Command path '${currentSubFullCommandName}' not found in the registered elements")
            return fallback.invoke(event)
        }

        val firstElement = subCommandElements[firstLevel]!!

        //Check if it is a SubCommand
        if (firstElement::class.isSubclassOf(DiscordSubCommand::class)) {
            (firstElement as DiscordSubCommand).initialize(currentSubFullCommandName, this, discordRepresentation)
            return function.invoke(firstElement)
        }

        //Check if it is a SubCommand in a SubCommandGroup
        val secondLevel = path[2]
        if (!(firstElement as DiscordSubCommandGroup).subCommands.containsKey(secondLevel)) {
            logger.debug("Command path '${currentSubFullCommandName}' not found in the registered elements")
            return fallback.invoke(event)
        }

        (firstElement as DiscordSubCommand).initialize(currentSubFullCommandName, this, discordRepresentation)
        return function.invoke(firstElement.subCommands[secondLevel]!!)
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
    fun destroyThisInstance(
        removeObservers: Boolean = true,
        removeObserverTimeouts: Boolean = true,
        callOnDestroy: Boolean = false,
        callButtonTimeout: Boolean = false,
        callStringSelectTimeout: Boolean = false,
        callEntitySelectTimeout: Boolean = false,
        callModalTimeout: Boolean = false
    ) {
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

        runBlocking(AlunaCoroutinesDispatcher.Default) {
            async { if (callOnDestroy) onDestroy() }
            async { if (callButtonTimeout) buttonObserver?.let { onButtonInteractionTimeout(it.value.additionalData) } }
            async { if (callStringSelectTimeout) stringSelectObserver?.let { onStringSelectInteractionTimeout(it.value.additionalData) } }
            async { if (callEntitySelectTimeout) entitySelectObserver?.let { onEntitySelectInteractionTimeout(it.value.additionalData) } }
            async { if (callModalTimeout) modalObserver?.let { onModalInteractionTimeout(it.value.additionalData) } }
        }
    }

    @JvmSynthetic
    internal fun onButtonGlobalInteraction(event: ButtonInteractionEvent) {
        this.onButtonInteraction(event, hashMapOf())
    }

    @JvmSynthetic
    internal fun onStringSelectGlobalInteraction(event: StringSelectInteractionEvent) {
        this.onStringSelectInteraction(event, hashMapOf())
    }

    @JvmSynthetic
    internal fun onEntitySelectGlobalInteraction(event: EntitySelectInteractionEvent) {
        this.onEntitySelectInteraction(event, hashMapOf())
    }

    @JvmSynthetic
    internal fun onModalGlobalInteraction(event: ModalInteractionEvent) {
        this.onModalInteraction(event, hashMapOf())
    }

    fun generateGlobalInteractionId(componentId: String): String {
        if (componentId.length + 43 > 100) {
            throw IllegalArgumentException("componentId can not be longer than 57 characters")
        }
        return "/${this.discordRepresentation.id}:${this.uniqueId}:$componentId"
    }

    fun extractGlobalInteractionId(componentId: String): String {
        val commandId = componentId.split(":")[0]
        val uniqueId = componentId.split(":")[1]
        return componentId.substring(commandId.length + 1 + uniqueId.length + 1)
    }
}