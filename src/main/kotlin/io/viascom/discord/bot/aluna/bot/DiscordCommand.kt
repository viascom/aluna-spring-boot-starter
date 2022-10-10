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
import io.viascom.discord.bot.aluna.configuration.Experimental
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.exception.AlunaInteractionRepresentationNotFoundException
import io.viascom.discord.bot.aluna.model.*
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.property.OwnerIdProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
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
import org.springframework.util.StopWatch
import java.time.Duration
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
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
    val handleSubCommands: Boolean = false
) : CommandDataImpl(name, description),
    SlashCommandData, InteractionScopedObject, DiscordInteractionHandler {

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
    lateinit var ownerIdProvider: OwnerIdProvider

    @Autowired(required = false)
    lateinit var localizationProvider: DiscordInteractionLocalization

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * This gets set by the CommandContext automatically and should not be changed
     */
    override lateinit var uniqueId: String

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
     * If true, only users which are returned by [OwnerIdProvider.getOwnerIds] are allowed to use it.
     */
    var isOwnerCommand = false

    /**
     * If true, this command is only seen by users with the administrator permission on the server by default!
     * Aluna will set `this.defaultPermissions = DefaultMemberPermissions.DISABLED` if true.
     */
    var isAdministratorOnlyCommand = false

    var interactionDevelopmentStatus = DevelopmentStatus.LIVE

    override var beanTimoutDelay: Duration = Duration.ofMinutes(14)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = true
    override var beanCallOnDestroy: Boolean = true

    /**
     * Discord representation of this interaction
     */
    lateinit var discordRepresentation: Command

    private val subCommandElements: HashMap<String, DiscordSubCommandElement> = hashMapOf()

    /**
     * Current sub command path gets set when the command gets used.
     *
     * *This variable is used by the internal sub command handling.*
     */
    private var currentSubCommandPath: String = ""

    /**
     * The [CooldownScope][Command.CooldownScope] of the command. This defines how far from a scope cooldowns have.
     * <br></br>Default [CooldownScope.USER][Command.CooldownScope.USER].
     */
    @Experimental("Cooldowns are currently not supported")
    var cooldownScope = CooldownScope.USER

    @Experimental("Cooldowns are currently not supported")
    var cooldown = 0

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

    var subCommandUseScope = hashMapOf<String, UseScope>()

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
     * @param event [SelectMenuInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */

    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return if (handleSubCommands) {
            handleSubCommand(event, { it.onSelectMenuInteraction(event) }, { onSubCommandInteractionFallback(event) })
        } else {
            true
        }
    }

    /**
     * This method gets triggered, as soon as a select event observer duration timeout is reached.
     */

    override fun onSelectMenuInteractionTimeout(additionalData: HashMap<String, Any?>) {
        if (handleSubCommands) {
            handleSubCommand(null, {
                it.onSelectMenuInteractionTimeout(additionalData)
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
            handleSubCommand(event, { it.onModalInteraction(event, additionalData) }, { onSubCommandInteractionFallback(event) })
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
        discordInteractionLoadAdditionalData.loadDataBeforeAdditionalRequirements(this, event)

        //Check additional requirements for this command
        val additionalRequirements = discordInteractionAdditionalConditions.checkForAdditionalCommandRequirements(this, event)
        if (additionalRequirements.failed) {
            onFailedAdditionalRequirements(event, additionalRequirements)
            return
        }

        discordInteractionLoadAdditionalData.loadData(this, event)
        onAutoCompleteEvent(option, event)
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

    open fun onWrongUseScope(event: SlashCommandInteractionEvent, wrongUseScope: WrongUseScope) {
        if (wrongUseScope.subCommandServerOnly) {
            event.deferReply(true).setContent("⛔ This command can only be used on a server directly.").queue()
        }
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

    open fun onFailedAdditionalRequirements(event: SlashCommandInteractionEvent, additionalRequirements: AdditionalRequirements) {
        event.deferReply(true).setContent("⛔ Additional requirements for this command failed.").queue()
    }

    open fun onFailedAdditionalRequirements(event: CommandAutoCompleteInteractionEvent, additionalRequirements: AdditionalRequirements) {
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
            this::class.primaryConstructor!!.parameters.forEach {
                if (it.findAnnotation<SubCommandElement>() != null && (it.type.classifier as KClass<*>).isSubclassOf(DiscordSubCommandElement::class)) {
                    val field = this::class.memberProperties.firstOrNull { member -> member.name == it.name }
                        ?: throw IllegalArgumentException("Couldn't access ${it.name} parameter because it is not a property. To fix this, make sure that your parameter is defined as property.")
                    field.isAccessible = true
                    registerSubCommands(field.getter.call(this) as DiscordSubCommandElement)
                }
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

        currentSubCommandPath = event.commandPath
        MDC.put("interaction", event.commandPath)
        MDC.put("uniqueId", uniqueId)

        guild = event.guild
        guild?.let { MDC.put("discord.server", "${it.id} (${it.name})") }
        channel = event.channel
        MDC.put("discord.channel", channel.id)
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

        //Check if this is an owner command
        if (isOwnerCommand && author.idLong !in ownerIdProvider.getOwnerIds()) {
            onOwnerCommandNotAllowedByUser(event)
            return
        }

        //Check use scope of this command
        val wrongUseScope = discordInteractionConditions.checkUseScope(this, subCommandUseScope, event)
        if (wrongUseScope.wrongUseScope) {
            onWrongUseScope(event, wrongUseScope)
            return
        }

        //Check needed user permissions for this command
        val missingUserPermissions = discordInteractionConditions.checkForNeededUserPermissions(this, userPermissions, event)
        if (missingUserPermissions.hasMissingPermissions) {
            onMissingUserPermission(event, missingUserPermissions)
            return
        }

        //Check needed bot permissions for this command
        val missingBotPermissions = discordInteractionConditions.checkForNeededBotPermissions(this, botPermissions, event)
        if (missingBotPermissions.hasMissingPermissions) {
            onMissingBotPermission(event, missingBotPermissions)
            return
        }

        //checkForCommandCooldown(event)

        discordInteractionLoadAdditionalData.loadDataBeforeAdditionalRequirements(this, event)

        //Check additional requirements for this command
        val additionalRequirements = discordInteractionAdditionalConditions.checkForAdditionalCommandRequirements(this, event)
        if (additionalRequirements.failed) {
            onFailedAdditionalRequirements(event, additionalRequirements)
            return
        }

        //Load additional data for this command
        discordInteractionLoadAdditionalData.loadData(this, event)

        runBlocking(AlunaCoroutinesDispatcher.Default) {
            //Run onCommandExecution in async to ensure it is not blocking the execution of the command itself
            async(AlunaCoroutinesDispatcher.IO) { discordInteractionMetaDataHandler.onCommandExecution(command, event) }
            async(AlunaCoroutinesDispatcher.IO) {
                if (alunaProperties.discord.publishDiscordCommandEvent) {
                    eventPublisher.publishDiscordCommandEvent(author, channel, guild, event.commandPath, command)
                }
            }

            try {
                logger.info("Run command /${event.commandPath}" + if (alunaProperties.debug.showHashCode) " [${command.hashCode()}]" else "")
                execute(event)
                if (handleSubCommands) {
                    logger.debug("Handle sub command /${event.commandPath}")
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
                    logger.info("Command /${event.commandPath} (${command.author.id})${if (alunaProperties.debug.showHashCode) " [${command.hashCode()}]" else ""} took ${stopWatch!!.totalTimeMillis}ms")
                    when {
                        (stopWatch!!.totalTimeMillis > 3000) -> logger.warn("The execution of the command /${event.commandPath} until it got completed took longer than 3 second. Make sure you acknowledge the event as fast as possible. If it got acknowledge at the end of the method, the interaction token was no longer valid.")
                        (stopWatch!!.totalTimeMillis > 1500) -> logger.warn("The execution of the command /${event.commandPath} until it got completed took longer than 1.5 second. Make sure that you acknowledge the event as fast as possible. Because the initial interaction token is only 3 seconds valid.")
                    }
                }

            }
            launch(AlunaCoroutinesDispatcher.IO) {
                discordInteractionMetaDataHandler.onExitInteraction(command, stopWatch, event)
            }
        }
    }

    open fun registerSubCommands(vararg elements: DiscordSubCommandElement) {
        elements.forEach { element ->
            subCommandElements.putIfAbsent(element.getName(), element)

            if (element::class.isSubclassOf(DiscordSubCommand::class)) {
                element as DiscordSubCommand
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

    open fun handleSubCommandExecution(event: SlashCommandInteractionEvent, fallback: (SlashCommandInteractionEvent) -> (Unit)) {
        loadDynamicSubCommandElements()

        val path = event.commandPath.split("/")
        val firstLevel = path[1]
        if (!subCommandElements.containsKey(firstLevel)) {
            logger.debug("Command path '${event.commandPath}' not found in the registered elements")
            fallback.invoke(event)
            return
        }

        val firstElement = subCommandElements[firstLevel]!!

        //Check if it is a SubCommand
        if (firstElement::class.isSubclassOf(DiscordSubCommand::class)) {
            (firstElement as DiscordSubCommand).execute(event, null, this)
            return
        }

        //Check if it is a SubCommand in a SubCommandGroup
        val secondLevel = path[2]
        if (!(firstElement as DiscordSubCommandGroup).subCommands.containsKey(secondLevel)) {
            logger.debug("Command path '${event.commandPath}' not found in the registered elements")
            fallback.invoke(event)
            return
        }

        firstElement.subCommands[secondLevel]!!.execute(event, null, this)
    }

    open fun handleSubCommand(
        event: GenericInteractionCreateEvent?,
        function: (DiscordSubCommand) -> (Boolean),
        fallback: (GenericInteractionCreateEvent?) -> (Boolean)
    ): Boolean {
        loadDynamicSubCommandElements()

        val path = currentSubCommandPath.split("/")
        val firstLevel = path[1]
        if (!subCommandElements.containsKey(firstLevel)) {
            logger.debug("Command path '${currentSubCommandPath}' not found in the registered elements")
            return fallback.invoke(event)
        }

        val firstElement = subCommandElements[firstLevel]!!

        //Check if it is a SubCommand
        if (firstElement::class.isSubclassOf(DiscordSubCommand::class)) {
            return function.invoke((firstElement as DiscordSubCommand))
        }

        //Check if it is a SubCommand in a SubCommandGroup
        val secondLevel = path[2]
        if (!(firstElement as DiscordSubCommandGroup).subCommands.containsKey(secondLevel)) {
            logger.debug("Command path '${currentSubCommandPath}' not found in the registered elements")
            return fallback.invoke(event)
        }

        return function.invoke(firstElement.subCommands[secondLevel]!!)
    }
}
