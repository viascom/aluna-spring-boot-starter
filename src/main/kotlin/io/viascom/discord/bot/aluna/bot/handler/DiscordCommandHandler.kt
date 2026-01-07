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

package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.*
import io.viascom.discord.bot.aluna.bot.handler.*
import io.viascom.discord.bot.aluna.configuration.Experimental
import io.viascom.discord.bot.aluna.configuration.scope.InteractionScope
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.exception.AlunaInteractionRepresentationNotFoundException
import io.viascom.discord.bot.aluna.model.*
import io.viascom.discord.bot.aluna.model.TimeMarkStep.*
import io.viascom.discord.bot.aluna.property.AlunaDebugProperties
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.property.OwnerIdProvider
import io.viascom.discord.bot.aluna.util.InternalUtil
import io.viascom.discord.bot.aluna.util.TimestampFormat
import io.viascom.discord.bot.aluna.util.toDiscordTimestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
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
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.properties.Delegates
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible
import kotlin.time.TimeSource.Monotonic.markNow


public abstract class DiscordCommandHandler(
    name: String,
    description: String,

    /**
     * Define a [LocalizationFunction] for this command. If set no null, Aluna will take the implementation of [DiscordInteractionLocalization].
     */
    public open var localizations: LocalizationFunction? = null,

    /**
     * If enabled, Aluna will register an event listener for auto complete requests and link it to this command.
     *
     * If such an event gets triggered, the method [runOnAutoCompleteEvent] will be invoked.
     */
    public open val observeAutoComplete: Boolean = false,

    /**
     * If enabled, Aluna will automatically forward the command execution as well as interaction events to the matching sub command.
     *
     * For this to work, you need to annotate your autowired [DiscordSubCommand] or [DiscordSubCommandGroup] implementation with [@SubCommandElement][SubCommandElement]
     * or register them manually with [registerSubCommands] during [initSubCommands].
     *
     * The Top-Level command can not be used (limitation of Discord), but Aluna will nevertheless always call [execute] on the top-level command before executing the sub command method if you need to do some general stuff.
     */
    public open val handleSubCommands: Boolean = false,

    /**
     * If enabled, Aluna will direct matching interactions to this command.
     * If a matching instance of this command (based on uniqueId or message) is found, the corresponding method is called. If not, a new instance gets created.
     */
    public open val handlePersistentInteractions: Boolean = false
) : CommandDataImpl(name, description), SlashCommandData, InteractionScopedObject, DiscordInteractionHandler {

    @Autowired
    public lateinit var alunaProperties: AlunaProperties

    @Autowired
    public lateinit var discordInteractionConditions: DiscordInteractionConditions

    @Autowired
    public lateinit var discordInteractionAdditionalConditions: DiscordInteractionAdditionalConditions

    @Autowired
    public lateinit var discordInteractionLoadAdditionalData: DiscordInteractionLoadAdditionalData

    @Autowired
    public lateinit var discordInteractionMetaDataHandler: DiscordInteractionMetaDataHandler

    @Autowired
    public lateinit var eventPublisher: EventPublisher

    @Autowired
    override lateinit var discordBot: DiscordBot

    @Autowired
    private lateinit var configurableListableBeanFactory: ConfigurableListableBeanFactory

    @Autowired
    public lateinit var ownerIdProvider: OwnerIdProvider

    @Autowired(required = false)
    public lateinit var localizationProvider: DiscordInteractionLocalization

    public val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * This gets set by the CommandContext automatically and should not be changed
     */
    override var uniqueId: String = ""

    /**
     * Restrict this command to specific servers. If null, the command is available in all servers. When enabled, make sure to also enable 'alunaProperties.command.enableServerSpecificCommands' in your configuration.
     */
    @set:Experimental("This is an experimental feature and my not always work as expected. Please report any issues you find.")
    public var specificServers: ArrayList<String>? = null

    /**
     * Sets whether this command can only be used by users which are returned by [OwnerIdProvider.getOwnerIds].
     */
    public var isOwnerCommand: Boolean = false

    /**
     * Sets whether this command can only be seen by users with the administrator permission on the server!
     *
     * ! Aluna will set `this.defaultPermissions = DefaultMemberPermissions.DISABLED` if true.
     */
    public var isAdministratorOnlyCommand: Boolean = false

    /**
     * Sets whether this command should redirect auto complete events to the corresponding sub commands
     */
    public var redirectAutoCompleteEventsToSubCommands: Boolean = true

    /**
     * Interaction development status
     */
    public var interactionDevelopmentStatus: DevelopmentStatus = DevelopmentStatus.LIVE

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
    public lateinit var discordRepresentation: Command
        internal set

    /**
     * Discord id of this interaction
     */
    @set:JvmSynthetic
    public lateinit var discordInteractionId: String
        internal set

    private val subCommandElements: HashMap<String, DiscordSubCommandElement> = hashMapOf()

    /**
     * Current sub command path gets set when the command gets used.
     *
     * *This variable is used by the internal sub command handling.*
     */
    @JvmSynthetic
    internal var currentSubFullCommandName: String = ""

    /**
     * The [CooldownScope][CooldownScope] of the command.
     */
    public var cooldownScope: CooldownScope = CooldownScope.NO_COOLDOWN

    public var cooldown: Duration = Duration.ZERO

    /**
     * Any [Permission]s a Member must have to use this command.
     *
     * These are only checked in a [Guild] environment.
     */
    public var userPermissions: ArrayList<Permission> = arrayListOf()

    /**
     * Any [Permission]s the bot must have to use a command.
     *
     *These are only checked in a [Guild] environment.
     */
    public var botPermissions: ArrayList<Permission> = arrayListOf()

    /**
     * [MessageChannel] in which the command was used in.
     */
    public lateinit var channel: MessageChannel

    /**
     * [Author][User] of the command
     */
    override lateinit var author: User

    /**
     * [Guild] in which the command was used in. Can be null if the command was used in direct messages.
     */
    public var guild: Guild? = null

    /**
     * [GuildChannel] in which the command was used in. Can be null if the command was used in direct messages.
     */
    public var guildChannel: GuildChannel? = null

    /**
     * [Member] which used the command. Can be null if the command was used in direct messages.
     */
    public var member: Member? = null

    /**
     * User [Locale]
     *
     * *This is set by Aluna based on the information provided by Discord*
     */
    public var userLocale: DiscordLocale = DiscordLocale.ENGLISH_US

    /**
     * Guild [Locale]
     *
     * *This is set by Aluna based on the information provided by Discord*
     */
    @set:JvmSynthetic
    public var guildLocale: DiscordLocale = DiscordLocale.ENGLISH_US
        internal set

    /**
     * TimeMarks used if enabled by properties
     */
    @set:JvmSynthetic
    public var timeMarks: ArrayList<TimeMarkRecord>? = null
        internal set

    @set:JvmSynthetic
    public var isUserIntegration: Boolean by Delegates.notNull()
        internal set

    @set:JvmSynthetic
    public var isGuildIntegration: Boolean by Delegates.notNull()
        internal set

    @set:JvmSynthetic
    public var isInBotDM: Boolean by Delegates.notNull()
        internal set

    @JvmSynthetic
    internal abstract suspend fun runExecute(event: SlashCommandInteractionEvent)

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

    @JvmSynthetic
    internal abstract suspend fun runOnAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent)


    override suspend fun handleOnButtonInteraction(event: ButtonInteractionEvent): Boolean {
        return if (handleSubCommands) {
            handleSubCommandInteraction(event, { it.runOnButtonInteraction(event) }, { onSubCommandInteractionFallback(event) })
        } else {
            runOnButtonInteraction(event)
        }
    }

    override suspend fun handleOnButtonInteractionTimeout() {
        if (handleSubCommands) {
            handleSubCommandInteraction(null, { it.runOnButtonInteractionTimeout(); true }, { onSubCommandInteractionTimeoutFallback(); true })
        }

        runOnButtonInteractionTimeout()
    }

    override suspend fun handleOnStringSelectInteraction(event: StringSelectInteractionEvent): Boolean {
        return if (handleSubCommands) {
            handleSubCommandInteraction(event, { it.runOnStringSelectInteraction(event) }, { onSubCommandInteractionFallback(event) })
        } else {
            runOnStringSelectInteraction(event)
        }
    }


    override suspend fun handleOnStringSelectInteractionTimeout() {
        if (handleSubCommands) {
            handleSubCommandInteraction(null, { it.runOnStringSelectInteractionTimeout(); true }, { onSubCommandInteractionTimeoutFallback(); true })
        }

        runOnStringSelectInteractionTimeout()
    }


    override suspend fun handleOnEntitySelectInteraction(event: EntitySelectInteractionEvent): Boolean {
        return if (handleSubCommands) {
            handleSubCommandInteraction(event, { it.runOnEntitySelectInteraction(event) }, { onSubCommandInteractionFallback(event) })
        } else {
            runOnEntitySelectInteraction(event)
        }
    }

    override suspend fun handleOnEntitySelectInteractionTimeout() {
        if (handleSubCommands) {
            handleSubCommandInteraction(null, { it.runOnEntitySelectInteractionTimeout(); true }, { onSubCommandInteractionTimeoutFallback(); true })
        }

        runOnEntitySelectInteractionTimeout()
    }


    override suspend fun handleOnModalInteraction(event: ModalInteractionEvent): Boolean {
        return if (handleSubCommands) {
            handleSubCommandInteraction(event, { it.runOnModalInteraction(event) }, { onSubCommandInteractionFallback(event) })
        } else {
            runOnModalInteraction(event)
        }
    }

    override suspend fun handleOnModalInteractionTimeout() {
        if (handleSubCommands) {
            handleSubCommandInteraction(null, { it.runOnModalInteractionTimeout(); true }, { onSubCommandInteractionTimeoutFallback(); true })
        }

        runOnModalInteractionTimeout()
    }

    /**
     * This method is used to initialize the command handler from a global component event.
     */
    @JvmSynthetic
    internal fun initHandlerFromComponent(event: GenericComponentInteractionCreateEvent): Boolean {
        discordInteractionId = event.componentId.split(":")[0].substring(1)
        discordRepresentation = discordBot.discordRepresentations[discordInteractionId]!!
        setProperties(event)

        if (shouldLoadAdditionalData(name, alunaProperties)) {
            discordInteractionLoadAdditionalData.loadDataBeforeAdditionalRequirements(this, event)
        }

        //Check additional requirements for this command
        if (shouldCheckAdditionalConditions(name, alunaProperties)) {
            val additionalRequirements = discordInteractionAdditionalConditions.checkForAdditionalCommandRequirements(this, event)
            if (additionalRequirements.failed) {
                onFailedAdditionalRequirements(event, additionalRequirements)
                return false
            }
        }

        if (shouldLoadAdditionalData(name, alunaProperties)) {
            discordInteractionLoadAdditionalData.loadData(this, event)
        }

        return true
    }

    @JvmSynthetic
    internal fun initHandlerFromComponent(event: ModalInteractionEvent): Boolean {
        discordInteractionId = event.modalId.split(":")[0].substring(1)
        discordRepresentation = discordBot.discordRepresentations[discordInteractionId]!!
        setProperties(event)

        if (shouldLoadAdditionalData(name, alunaProperties)) {
            discordInteractionLoadAdditionalData.loadDataBeforeAdditionalRequirements(this, event)
        }

        //Check additional requirements for this command
        if (shouldCheckAdditionalConditions(name, alunaProperties)) {
            val additionalRequirements = discordInteractionAdditionalConditions.checkForAdditionalCommandRequirements(this, event)
            if (additionalRequirements.failed) {
                onFailedAdditionalRequirements(event, additionalRequirements)
                return false
            }
        }

        if (shouldLoadAdditionalData(name, alunaProperties)) {
            discordInteractionLoadAdditionalData.loadData(this, event)
        }

        return true
    }

    @JvmSynthetic
    internal suspend fun handleAutoCompleteEventCall(option: String, event: CommandAutoCompleteInteractionEvent) {
        discordRepresentation = discordBot.discordRepresentations[event.commandId]!!
        setProperties(event)

        currentSubFullCommandName = event.fullCommandName
        MDC.put("interaction", event.fullCommandName)

        if (shouldLoadAdditionalData(name, alunaProperties)) {
            discordInteractionLoadAdditionalData.loadDataBeforeAdditionalRequirements(this, event)
        }

        //Check additional requirements for this command
        if (shouldCheckAdditionalConditions(name, alunaProperties)) {
            val additionalRequirements = discordInteractionAdditionalConditions.checkForAdditionalCommandRequirements(this, event)
            if (additionalRequirements.failed) {
                onFailedAdditionalRequirements(event, additionalRequirements)
                return
            }
        }

        if (shouldLoadAdditionalData(name, alunaProperties)) {
            discordInteractionLoadAdditionalData.loadData(this, event)
        }

        if (handleSubCommands && redirectAutoCompleteEventsToSubCommands) {
            handleSubCommandInteraction(event, { it.runOnAutoCompleteEvent(option, event); true }, { onSubCommandInteractionFallback(event) })
        } else {
            runOnAutoCompleteEvent(option, event)
        }
    }

    @JvmSynthetic
    internal fun setProperties(event: GenericInteractionCreateEvent) {
        MDC.put("uniqueId", uniqueId)

        val integrationType = if (event.integrationOwners.isGuildIntegration) "GUILD" else "USER"
        MDC.put("discord.integration.type", integrationType)
        guild = event.guild

        isGuildIntegration = event.integrationOwners.isGuildIntegration
        isUserIntegration = event.integrationOwners.isUserIntegration

        isInBotDM = event.context == InteractionContextType.BOT_DM

        if (event.integrationOwners.isGuildIntegration) {
            guild?.let { MDC.put("discord.server", "${it.id} (${it.name})") }
            MDC.put("discord.integration.guild", event.integrationOwners.authorizingGuildId)
        } else {
            guild?.let { MDC.put("discord.server", it.id) }
            MDC.put("discord.integration.user", event.integrationOwners.authorizingUserId)
        }

        if (event.integrationOwners.isUserIntegration) {
            MDC.put("discord.integration.user", event.integrationOwners.authorizingUserId)
        }

        author = event.user
        MDC.put("discord.author", "${author.id} (${author.name})")

        userLocale = event.userLocale
        MDC.put("discord.author_locale", userLocale.locale)

        if (guild != null) {
            if (event.integrationOwners.isGuildIntegration) {
                member = guild!!.getMember(author)
            }
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
    public open fun onSubCommandFallback(event: SlashCommandInteractionEvent) {
    }

    /**
     * This method gets called if Aluna can not find a registered sub command for an interaction event
     *
     * @param event Original [SlashCommandInteractionEvent]
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    public open fun onSubCommandInteractionFallback(event: GenericInteractionCreateEvent): Boolean {
        return true
    }

    /**
     * This method gets called if Aluna can not find a registered sub command for an interaction timeout
     */
    public open fun onSubCommandInteractionTimeoutFallback() {
    }

    public open fun onOwnerCommandNotAllowedByUser(event: SlashCommandInteractionEvent) {
        event.deferReply(true).setContent("⛔ This command is to powerful for you.").queue()
    }

    public open fun onMissingUserPermission(event: SlashCommandInteractionEvent, missingPermissions: MissingPermissions) {
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

    public open fun onMissingBotPermission(event: SlashCommandInteractionEvent, missingPermissions: MissingPermissions) {
        when {
            missingPermissions.notInVoice -> {
                event.deferReply(true).setContent("⛔ You need to be in a voice channel yourself to execute this command").queue()
            }

            (missingPermissions.hasMissingPermissions) -> {
                event.deferReply(true).setContent(
                    "⛔ I'm missing the following permission to execute this command:\n" +
                            missingPermissions.textChannel.joinToString("\n") { "└ ${it.getName()}" } + "\n" +
                            missingPermissions.voiceChannel.joinToString("\n") { "└ ${it.getName()}" } + "\n" +
                            missingPermissions.guild.joinToString("\n") { "└ ${it.getName()}" }
                ).queue()
            }
        }
    }

    public open fun onFailedAdditionalRequirements(event: SlashCommandInteractionEvent, additionalRequirements: AdditionalRequirements) {
        if (!event.isAcknowledged) {
            event.deferReply(true).setContent("⛔ Additional requirements for this command failed.").queue()
        }
    }

    public open fun onCooldownStillActive(event: SlashCommandInteractionEvent, lastUse: LocalDateTime) {
        event.deferReply(true)
            .setContent("⛔ This interaction is still on cooldown and will be usable ${lastUse.plusNanos(cooldown.toNanos()).toDiscordTimestamp(TimestampFormat.RELATIVE_TIME)}.")
            .queue()
    }

    public open fun onFailedAdditionalRequirements(event: GenericComponentInteractionCreateEvent, additionalRequirements: AdditionalRequirements) {
        if (!event.isAcknowledged) {
            event.deferReply(true).setContent("⛔ Additional requirements for this command failed.").queue()
        }
    }

    public open fun onFailedAdditionalRequirements(event: ModalInteractionEvent, additionalRequirements: AdditionalRequirements) {
        if (!event.isAcknowledged) {
            event.deferReply(true).setContent("⛔ Additional requirements for this command failed.").queue()
        }
    }

    public open fun onFailedAdditionalRequirements(event: CommandAutoCompleteInteractionEvent, additionalRequirements: AdditionalRequirements) {
    }

    public open fun onExecutionException(event: SlashCommandInteractionEvent, exception: Exception) {
        throw exception
    }

    public open fun initCommandOptions() {}
    public open fun initSubCommands() {}

    public suspend fun prepareInteraction(): Unit = withContext(AlunaDispatchers.Internal) {
        if (isAdministratorOnlyCommand) {
            this@DiscordCommandHandler.defaultPermissions = DefaultMemberPermissions.DISABLED
        }

        if (this@DiscordCommandHandler.contexts.contains(InteractionContextType.PRIVATE_CHANNEL) && !this@DiscordCommandHandler.integrationTypes.contains(IntegrationType.USER_INSTALL)) {
            logger.warn("The interaction '$name' contains the context PRIVATE_CHANNEL enabled, but does not have the integration type USER_INSTALL enabled. This interaction will therefore not be available in private channels!")
        }

        if (!alunaProperties.productionMode) {
            if ((isAdministratorOnlyCommand || this@DiscordCommandHandler.defaultPermissions == DefaultMemberPermissions.DISABLED) && !this@DiscordCommandHandler.contexts.contains(
                    InteractionContextType.GUILD
                )
            ) {
                logger.warn("The interaction '$name' has a default permission for administrator only but is not restricted to guild only. All users will be able to use this interaction in DMs with your bot!")
            }
            if (this@DiscordCommandHandler.defaultPermissions != DefaultMemberPermissions.ENABLED && this@DiscordCommandHandler.defaultPermissions != DefaultMemberPermissions.DISABLED && !this@DiscordCommandHandler.contexts.contains(
                    InteractionContextType.GUILD
                )
            ) {
                logger.warn("The interaction '$name' has a default permission restriction for a specific user permission but is not restricted to guild only. All users will be able to use this interaction in DMs with your bot!")
            }
        }

        loadDynamicSubCommandElements()
    }

    private suspend fun loadDynamicSubCommandElements() = withContext(AlunaDispatchers.Internal) {
        if (subCommandElements.isEmpty()) {
            initSubCommands()
        }

        if (subCommandElements.isEmpty()) {
            InternalUtil.getSubCommandElements(this@DiscordCommandHandler).forEach { field ->
                field.isAccessible = true
                registerSubCommands(field.getter.call(this@DiscordCommandHandler) as DiscordSubCommandElement)
            }
        }
    }

    public fun prepareLocalization() {
        if (alunaProperties.translation.enabled) {
            if (localizations == null) {
                localizations = localizationProvider.getLocalizationFunction()
            }

            this.setLocalizationFunction(localizations!!)
            this.toData()
        }
    }

    /**
     * Runs checks for the [DiscordCommandHandler] with the given [SlashCommandInteractionEvent] that called it.
     *
     * @param event The CommandEvent that triggered this Command
     */
    @JvmSynthetic
    internal suspend fun run(event: SlashCommandInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        if (alunaProperties.debug.useTimeMarks) {
            timeMarks = arrayListOf()
        }
        timeMarks?.add(START at markNow())

        if (!discordBot.discordRepresentations.containsKey(event.commandId)) {
            val exception = AlunaInteractionRepresentationNotFoundException("${event.name} - ${event.id}")
            try {
                onExecutionException(event, exception)
            } catch (exceptionError: Exception) {
                discordInteractionMetaDataHandler.onGenericExecutionException(this@DiscordCommandHandler, exception, exceptionError, event)
            }
            return@withContext
        }

        discordRepresentation = discordBot.discordRepresentations[event.commandId]!!

        setProperties(event)

        currentSubFullCommandName = event.fullCommandName
        MDC.put("interaction", event.fullCommandName)
        channel = event.channel
        MDC.put("discord.channel", channel.id)

        if (alunaProperties.command.printArgs) {
            event.options.map { MDC.put("interaction.arg.${it.name.replace("-", "_")}", it.asString) }
        }

        val mdcMap = MDC.getCopyOfContextMap()

        timeMarks?.add(INITIALIZED at markNow())

        //Check if this is an owner command
        if (isOwnerCommand && author.idLong !in ownerIdProvider.getOwnerIds()) {
            onOwnerCommandNotAllowedByUser(event)
            return@withContext
        }

        timeMarks?.add(OWNER_CHECKED at markNow())

        //Check needed user permissions for this command
        val missingUserPermissions = async(AlunaDispatchers.Detached) {
            MDC.setContextMap(mdcMap)
            val missingPermissions = discordInteractionConditions.checkForNeededUserPermissions(this@DiscordCommandHandler, userPermissions, event)
            mdcMap.putAll(MDC.getCopyOfContextMap())
            missingPermissions
        }.await()
        if (missingUserPermissions.hasMissingPermissions) {
            onMissingUserPermission(event, missingUserPermissions)
            return@withContext
        }

        timeMarks?.add(NEEDED_USER_PERMISSIONS at markNow())

        //Check needed bot permissions for this command
        val missingBotPermissions = async(AlunaDispatchers.Detached) {
            MDC.setContextMap(mdcMap)
            val missingPermissions = discordInteractionConditions.checkForNeededBotPermissions(this@DiscordCommandHandler, botPermissions, event)
            mdcMap.putAll(MDC.getCopyOfContextMap())
            missingPermissions
        }.await()
        if (missingBotPermissions.hasMissingPermissions) {
            onMissingBotPermission(event, missingBotPermissions)
            return@withContext
        }

        timeMarks?.add(NEEDED_BOT_PERMISSIONS at markNow())

        if (shouldLoadAdditionalData(name, alunaProperties)) {
            async(AlunaDispatchers.Detached) {
                MDC.setContextMap(mdcMap)
                discordInteractionLoadAdditionalData.loadDataBeforeAdditionalRequirements(this@DiscordCommandHandler, event)
                mdcMap.putAll(MDC.getCopyOfContextMap())
            }.await()
            timeMarks?.add(LOAD_DATA_BEFORE_ADDITIONAL_REQUIREMENTS at markNow())
        }

        //Check additional requirements for this command
        if (shouldCheckAdditionalConditions(name, alunaProperties)) {
            val additionalRequirements = async(AlunaDispatchers.Detached) {
                MDC.setContextMap(mdcMap)
                val requirements = discordInteractionAdditionalConditions.checkForAdditionalCommandRequirements(this@DiscordCommandHandler, event)
                mdcMap.putAll(MDC.getCopyOfContextMap())
                requirements
            }.await()
            timeMarks?.add(CHECK_FOR_ADDITIONAL_COMMAND_REQUIREMENTS at markNow())
            if (additionalRequirements.failed) {
                onFailedAdditionalRequirements(event, additionalRequirements)
                return@withContext
            }
        } else {
            timeMarks?.add(CHECK_FOR_ADDITIONAL_COMMAND_REQUIREMENTS at markNow())
        }

        //Check for cooldown
        if (cooldownScope != CooldownScope.NO_COOLDOWN) {
            val cooldownKey = discordBot.getCooldownKey(cooldownScope, discordRepresentation.id, author.id, channel.id, guild?.id)
            if (discordBot.isCooldownActive(cooldownKey, cooldown)) {
                onCooldownStillActive(event, discordBot.cooldowns[cooldownKey]!!)
                return@withContext
            }
            discordBot.cooldowns[cooldownKey] = LocalDateTime.now(ZoneOffset.UTC)
        }
        timeMarks?.add(CHECK_COOLDOWN at markNow())

        //Load additional data for this command
        if (shouldLoadAdditionalData(name, alunaProperties)) {
            async(AlunaDispatchers.Detached) {
                MDC.setContextMap(mdcMap)
                discordInteractionLoadAdditionalData.loadData(this@DiscordCommandHandler, event)
                mdcMap.putAll(MDC.getCopyOfContextMap())
            }.await()
            timeMarks?.add(LOAD_ADDITIONAL_DATA at markNow())
        }

        //Run onCommandExecution in async to ensure it is not blocking the execution of the command itself
        launch(AlunaDispatchers.Detached) {
            MDC.setContextMap(mdcMap)
            discordInteractionMetaDataHandler.onCommandExecution(this@DiscordCommandHandler, event)
            mdcMap.putAll(MDC.getCopyOfContextMap())
        }
        launch {
            discordBot.commandHistory.emit(CommandUsage(event.fullCommandName, uniqueId, this@DiscordCommandHandler::class.java, author.id, guild?.id))
        }
        launch(AlunaDispatchers.Detached) {
            if (alunaProperties.discord.publishDiscordCommandEvent) {
                eventPublisher.publishDiscordCommandEvent(author, channel, guild, event.fullCommandName, this@DiscordCommandHandler)
            }
        }

        timeMarks?.add(ASYNC_TASKS_STARTED at markNow())

        var endedWithException = false

        try {
            MDC.setContextMap(mdcMap)
            logger.info(
                "Run command /${event.fullCommandName}" +
                        (if (alunaProperties.command.printArgs) " [" + event.options.joinToString { "${it.name}: ${it.asString}" } + "]" else "") +
                        (if (alunaProperties.debug.showHashCode) " [${this@DiscordCommandHandler.hashCode()}]" else ""))
            runExecute(event)
            timeMarks?.add(RUN_EXECUTE at markNow())
            if (handleSubCommands) {
                logger.debug("Handle sub command /${event.fullCommandName}")
                handleSubCommandExecution(event) { onSubCommandFallback(event) }
                timeMarks?.add(HANDLE_SUB_COMMAND_EXECUTION at markNow())
            }
        } catch (e: Exception) {
            endedWithException = true
            try {
                async(AlunaDispatchers.Detached) {
                    MDC.setContextMap(mdcMap)
                    onExecutionException(event, e)
                }.await()
                timeMarks?.add(ON_EXECUTION_EXCEPTION at markNow())
            } catch (exceptionError: Exception) {
                async(AlunaDispatchers.Detached) {
                    MDC.setContextMap(mdcMap)
                    discordInteractionMetaDataHandler.onGenericExecutionException(this@DiscordCommandHandler, e, exceptionError, event)
                }.await()
                timeMarks?.add(ON_EXECUTION_EXCEPTION at markNow())
            }
        } finally {
            MDC.setContextMap(mdcMap)
            exitCommand(event, endedWithException, mdcMap)
        }
    }

    @JvmSynthetic
    internal suspend fun exitCommand(event: SlashCommandInteractionEvent, endedWithException: Boolean, mdcMap: Map<String, String>?) = withContext(AlunaDispatchers.Detached) {
        launch {
            timeMarks?.add(EXIT_COMMAND at markNow())

            MDC.setContextMap(mdcMap)

            if (alunaProperties.debug.useTimeMarks && timeMarks != null) {

                val duration = timeMarks!!.getDuration()
                val executeDuration = timeMarks!!.getDurationRunExecute()
                val neededUserPermissionsDuration = timeMarks!!.getDurationNeededUserPermissions()
                val neededBotPermissionsDuration = timeMarks!!.getDurationNeededBotPermissions()
                val loadDataBeforeAdditionalRequirementsDuration = timeMarks!!.getDurationLoadDataBeforeAdditionalRequirements()
                val checkForAdditionalCommandRequirementsDuration = timeMarks!!.getDurationCheckForAdditionalCommandRequirements()
                val loadAdditionalDataDuration = timeMarks!!.getDurationLoadAdditionalData()

                MDC.put("duration", duration.inWholeMicroseconds.toString())

                if (alunaProperties.debug.showDetailTimeMarks != AlunaDebugProperties.ShowDetailTimeMarks.NONE) {
                    neededUserPermissionsDuration?.let { MDC.put("duration-details.neededUserPermissionsDuration", it.inWholeMicroseconds.toString()) }
                    neededBotPermissionsDuration?.let { MDC.put("duration-details.neededBotPermissionsDuration", it.inWholeMicroseconds.toString()) }
                    loadDataBeforeAdditionalRequirementsDuration?.let {
                        MDC.put(
                            "duration-details.loadDataBeforeAdditionalRequirementsDuration",
                            it.inWholeMicroseconds.toString()
                        )
                    }
                    checkForAdditionalCommandRequirementsDuration?.let {
                        MDC.put(
                            "duration-details.checkForAdditionalCommandRequirementsDuration",
                            it.inWholeMicroseconds.toString()
                        )
                    }
                    loadAdditionalDataDuration?.let { MDC.put("duration-details.loadAdditionalDataDuration", it.inWholeMicroseconds.toString()) }
                    executeDuration?.let { MDC.put("duration-details.executeDuration", it.inWholeMicroseconds.toString()) }
                }

                logger.info("Command /${event.fullCommandName} (${this@DiscordCommandHandler.author.id})${if (alunaProperties.debug.showHashCode) " [${this@DiscordCommandHandler.hashCode()}]" else ""} took $duration (execute method: $executeDuration)")

                val beforeDuration = kotlin.time.Duration.ZERO
                neededUserPermissionsDuration?.let { beforeDuration.plus(it) }
                neededUserPermissionsDuration?.let { beforeDuration.plus(it) }
                neededBotPermissionsDuration?.let { beforeDuration.plus(it) }
                loadDataBeforeAdditionalRequirementsDuration?.let { beforeDuration.plus(it) }
                checkForAdditionalCommandRequirementsDuration?.let { beforeDuration.plus(it) }
                loadAdditionalDataDuration?.let { beforeDuration.plus(it) }

                when {
                    beforeDuration.inWholeMilliseconds > 1500 -> {
                        logger.warn("The execution of the command /${event.fullCommandName} until calling your execute method took over 1.5 seconds. Make sure you don't do time consuming tasks in you implementation of DiscordInteractionLoadAdditionalData, DefaultDiscordInteractionAdditionalConditions or DiscordInteractionConditions.")
                    }
                }

                when {
                    (duration.inWholeMilliseconds > 3000) -> logger.warn("The execution of the command /${event.fullCommandName} until it got completed took longer than 3 second. Make sure you acknowledge the event as fast as possible. If it got acknowledge at the end of the method, the interaction token was no longer valid.")
                    (duration.inWholeMilliseconds > 1500) -> logger.warn("The execution of the command /${event.fullCommandName} until it got completed took longer than 1.5 second. Make sure that you acknowledge the event as fast as possible. Because the initial interaction token is only 3 seconds valid.")
                }

                if (alunaProperties.debug.showDetailTimeMarks == AlunaDebugProperties.ShowDetailTimeMarks.ALWAYS ||
                    (alunaProperties.debug.showDetailTimeMarks == AlunaDebugProperties.ShowDetailTimeMarks.ON_EXCEPTION && endedWithException)
                ) {
                    logger.info(timeMarks!!.printTimeMarks("/${event.fullCommandName}"))
                }
            }

        }
        launch {
            discordInteractionMetaDataHandler.onExitInteraction(this@DiscordCommandHandler, timeMarks, event)
        }
    }

    public open suspend fun registerSubCommands(vararg elements: DiscordSubCommandElement): Unit = withContext(AlunaDispatchers.Internal) {
        elements.filter { element ->
            when {
                alunaProperties.includeInDevelopmentInteractions -> true
                alunaProperties.productionMode && (element::class.isSubclassOf(DiscordSubCommandHandler::class)) && (element as DiscordSubCommandHandler).interactionDevelopmentStatus == DevelopmentStatus.IN_DEVELOPMENT -> false
                alunaProperties.productionMode && (element::class.isSubclassOf(DiscordSubCommandGroup::class)) && (element as DiscordSubCommandGroup).interactionDevelopmentStatus == DevelopmentStatus.IN_DEVELOPMENT -> false
                else -> true
            }
        }.forEach { element ->
            subCommandElements.putIfAbsent(element.getName(), element)

            if (element::class.isSubclassOf(DiscordSubCommandHandler::class)) {
                element as DiscordSubCommandHandler
                element.parentCommand = this@DiscordCommandHandler
                element.runInitCommandOptions()
                this@DiscordCommandHandler.addSubcommands(element)
            }

            if (element::class.isSubclassOf(DiscordSubCommandGroupHandler::class)) {
                element as DiscordSubCommandGroupHandler
                element.initSubCommands()
                this@DiscordCommandHandler.addSubcommandGroups(element)
            }
        }
    }

    public open suspend fun handleSubCommandExecution(event: SlashCommandInteractionEvent, fallback: (SlashCommandInteractionEvent) -> (Unit)) {
        loadDynamicSubCommandElements()
        timeMarks?.add(LOAD_DYNAMIC_SUB_COMMAND_ELEMENTS at markNow())

        val path = event.fullCommandName.split(" ")
        discordRepresentation = discordBot.discordRepresentations[event.commandId]!!

        val firstLevel = path[1]
        if (!subCommandElements.containsKey(firstLevel)) {
            logger.debug("Command path '${event.fullCommandName}' not found in the registered elements")
            fallback.invoke(event)
            return
        }

        val firstElement = subCommandElements[firstLevel]!!
        timeMarks?.add(CHECK_SUB_COMMAND_PATH at markNow())

        //Check if it is a SubCommand
        if (firstElement::class.isSubclassOf(DiscordSubCommandHandler::class)) {
            (firstElement as DiscordSubCommandHandler).initialize(event.fullCommandName, this, discordRepresentation)
            timeMarks?.add(SUB_COMMAND_INITIALIZED at markNow())
            firstElement.run(event)
            timeMarks?.add(SUB_COMMAND_RUN_EXECUTE at markNow())
            return
        }

        //Check if it is a SubCommand in a SubCommandGroup
        val secondLevel = path[2]
        if (!(firstElement as DiscordSubCommandGroupHandler).subCommands.containsKey(secondLevel)) {
            logger.debug("Command path '${event.fullCommandName}' not found in the registered elements")
            fallback.invoke(event)
            return
        }
        timeMarks?.add(CHECK_SECOND_SUB_COMMAND_PATH at markNow())

        firstElement.subCommands[secondLevel]!!.initialize(event.fullCommandName, this, discordRepresentation)
        timeMarks?.add(SECOND_SUB_COMMAND_INITIALIZED at markNow())
        firstElement.subCommands[secondLevel]!!.run(event)
        timeMarks?.add(SECOND_SUB_COMMAND_RUN_EXECUTE at markNow())
    }

    public open suspend fun handleSubCommandInteraction(
        event: GenericInteractionCreateEvent?,
        function: suspend (DiscordSubCommandHandler) -> (Boolean),
        fallback: (GenericInteractionCreateEvent?) -> (Boolean)
    ): Boolean = withContext(AlunaDispatchers.Interaction) {
        loadDynamicSubCommandElements()


        val path = currentSubFullCommandName.split(" ")
        //Check if discordRepresentation is initialized
        if (!this@DiscordCommandHandler::discordRepresentation.isInitialized) {
            discordRepresentation = discordBot.discordRepresentations[discordInteractionId] ?: run {
                logger.debug("Command '${path[0]}' not found in the registered elements")
                return@withContext fallback.invoke(event)
            }
        }

        val firstLevel = path[1]
        if (!subCommandElements.containsKey(firstLevel)) {
            logger.debug("Command path '${currentSubFullCommandName}' not found in the registered elements")
            return@withContext fallback.invoke(event)
        }

        val firstElement = subCommandElements[firstLevel]!!

        //Check if it is a SubCommand
        if (firstElement::class.isSubclassOf(DiscordSubCommandHandler::class)) {
            (firstElement as DiscordSubCommandHandler).initialize(currentSubFullCommandName, this@DiscordCommandHandler, discordRepresentation)
            val result = function.invoke(firstElement)
            return@withContext result
        }

        //Check if it is a SubCommand in a SubCommandGroup
        val secondLevel = path[2]
        if (!(firstElement as DiscordSubCommandGroup).subCommands.containsKey(secondLevel)) {
            logger.debug("Command path '${currentSubFullCommandName}' not found in the registered elements")
            return@withContext fallback.invoke(event)
        }

        (firstElement as DiscordSubCommandGroupHandler).subCommands[secondLevel]!!.initialize(currentSubFullCommandName, this@DiscordCommandHandler, discordRepresentation)
        val result = function.invoke(firstElement.subCommands[secondLevel]!!)
        return@withContext result
    }

    public fun updateMessageIdForScope(messageId: String) {
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
    public suspend fun destroyThisInstance(
        removeObservers: Boolean,
        removeObserverTimeouts: Boolean,
        callOnDestroy: Boolean,
        callButtonTimeout: Boolean,
        callStringSelectTimeout: Boolean,
        callEntitySelectTimeout: Boolean,
        callModalTimeout: Boolean
    ): Job = withContext(AlunaDispatchers.Interaction) {
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

        launch(AlunaDispatchers.Detached) { if (callOnDestroy) runOnDestroy() }
        launch(AlunaDispatchers.Detached) { if (callButtonTimeout) buttonObserver?.let { runOnButtonInteractionTimeout() } }
        launch(AlunaDispatchers.Detached) { if (callStringSelectTimeout) stringSelectObserver?.let { runOnStringSelectInteractionTimeout() } }
        launch(AlunaDispatchers.Detached) { if (callEntitySelectTimeout) entitySelectObserver?.let { runOnEntitySelectInteractionTimeout() } }
        launch(AlunaDispatchers.Detached) { if (callModalTimeout) modalObserver?.let { runOnModalInteractionTimeout() } }
    }

    @JvmSynthetic
    internal suspend fun onButtonGlobalInteraction(event: ButtonInteractionEvent, fullCommandPath: String) {
        if (freshInstance && !initHandlerFromComponent(event)) {
            return
        }

        if (handleSubCommands) {
            currentSubFullCommandName = fullCommandPath
        }

        this.handleOnButtonInteraction(event)
    }

    @JvmSynthetic
    internal suspend fun onStringSelectGlobalInteraction(event: StringSelectInteractionEvent, fullCommandPath: String) {
        if (freshInstance && !initHandlerFromComponent(event)) {
            return
        }

        if (handleSubCommands) {
            currentSubFullCommandName = fullCommandPath
        }

        this.handleOnStringSelectInteraction(event)
    }

    @JvmSynthetic
    internal suspend fun onEntitySelectGlobalInteraction(event: EntitySelectInteractionEvent, fullCommandPath: String) {
        if (freshInstance && !initHandlerFromComponent(event)) {
            return
        }

        if (handleSubCommands) {
            currentSubFullCommandName = fullCommandPath
        }

        this.handleOnEntitySelectInteraction(event)
    }

    @JvmSynthetic
    internal suspend fun onModalGlobalInteraction(event: ModalInteractionEvent, fullCommandPath: String) {
        if (freshInstance && !initHandlerFromComponent(event)) {
            return
        }

        if (handleSubCommands) {
            currentSubFullCommandName = fullCommandPath
        }

        this.handleOnModalInteraction(event)
    }

    @JvmSynthetic
    internal fun shouldLoadAdditionalData(name: String, properties: AlunaProperties): Boolean {
        val isSystemCommand = name == "system-command"
        val isHelpCommand = name == "help"

        return when {
            name !in listOf("system-command", "help") -> true
            isSystemCommand && !properties.command.systemCommand.enabled -> true
            isSystemCommand && properties.command.systemCommand.executeLoadAdditionalData -> true
            isHelpCommand && !properties.command.helpCommand.enabled -> true
            isHelpCommand && properties.command.helpCommand.executeLoadAdditionalData -> true
            else -> false
        }
    }

    @JvmSynthetic
    internal fun shouldCheckAdditionalConditions(name: String, properties: AlunaProperties): Boolean {
        val isSystemCommand = name == "system-command"
        val isHelpCommand = name == "help"

        return when {
            name !in listOf("system-command", "help") -> true
            isSystemCommand && !properties.command.systemCommand.enabled -> true
            isSystemCommand && properties.command.systemCommand.checkAdditionalConditions -> true
            isHelpCommand && !properties.command.helpCommand.enabled -> true
            isHelpCommand && properties.command.helpCommand.checkAdditionalConditions -> true
            else -> false
        }
    }

    /**
     * Generate global interaction id
     *
     * @param componentId The id of the component
     * @param userId The id of the user which is allowed to use the interaction (optional, default value is null which means that all users are allowed to use the interaction)
     * @return The generated global interaction id
     */
    @JvmOverloads
    public fun generateGlobalInteractionId(componentId: String, userId: String? = null): String {
        //Check if discordRepresentation is initialized
        if (!this::discordRepresentation.isInitialized) {
            throw IllegalStateException("discordRepresentation is not initialized. generateGlobalInteractionId() can only be called after Aluna initialized the command. This happens when an interaction is used.")
        }

        var prefix = "/${discordRepresentation.id}:${this.uniqueId}"
        prefix += if (userId != null) ":$userId" else ":*"
        if (componentId.length + prefix.length > 100) {
            throw IllegalArgumentException("componentId can not be longer than ${100 - prefix.length} characters")
        }
        return "$prefix:$componentId"
    }

    public fun extractGlobalInteractionId(componentId: String): String {
        val commandId = componentId.split(":")[0]
        val uniqueId = componentId.split(":")[1]
        val userId = componentId.split(":")[2]
        return componentId.substring(commandId.length + 1 + uniqueId.length + 1 + userId.length + 1)
    }

}
