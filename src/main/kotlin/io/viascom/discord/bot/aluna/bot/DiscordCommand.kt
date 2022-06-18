package io.viascom.discord.bot.aluna.bot

import datadog.trace.api.Trace
import io.viascom.discord.bot.aluna.bot.emotes.AlunaEmote
import io.viascom.discord.bot.aluna.bot.handler.*
import io.viascom.discord.bot.aluna.configuration.Experimental
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.property.OwnerIdProvider
import io.viascom.discord.bot.aluna.translation.MessageService
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.util.StopWatch
import java.time.Duration
import java.util.*
import java.util.function.Consumer

abstract class DiscordCommand(
    name: String,
    description: String,
    //val localizations: HashMap<Locale, Pair<String, String>> = hashMapOf(),
    val observeAutoComplete: Boolean = false
) : CommandDataImpl(name, description),
    SlashCommandData, CommandScopedObject {

    @Autowired
    lateinit var alunaProperties: AlunaProperties

    @Autowired
    lateinit var discordCommandConditions: DiscordCommandConditions

    @Autowired
    lateinit var discordCommandAdditionalConditions: DiscordCommandAdditionalConditions

    @Autowired
    lateinit var discordCommandLoadAdditionalData: DiscordCommandLoadAdditionalData

    @Autowired
    lateinit var discordCommandMetaDataHandler: DiscordCommandMetaDataHandler

    @Autowired
    lateinit var eventPublisher: EventPublisher

    @Autowired
    lateinit var discordBot: DiscordBot

    @Autowired
    lateinit var ownerIdProvider: OwnerIdProvider

    @Autowired(required = false)
    lateinit var messageService: MessageService

    @Autowired(required = false)
    private lateinit var messageSource: MessageSource

//    @Autowired(required = false)
//    private lateinit var localizationFunction: LocalizationFunction

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    //This gets set by the CommandContext automatically
    override lateinit var uniqueId: String

    var useScope = UseScope.GLOBAL
    internal var specificServer: String? = null

    var isOwnerCommand = false
    var isAdministratorOnlyCommand = false

    var isEarlyAccessCommand = false

    var commandDevelopmentStatus = DevelopmentStatus.LIVE

    override var beanTimoutDelay: Duration = Duration.ofMinutes(15)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = true
    override var beanCallOnDestroy: Boolean = true

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
     * <br></br>These are only checked in a [Guild][net.dv8tion.jda.core.entities.Guild] environment.
     */
    var userPermissions = arrayListOf<Permission>()

    /**
     * Any [Permission]s the bot must have to use a command.
     * <br></br>These are only checked in a [Guild][net.dv8tion.jda.core.entities.Guild] environment.
     */
    var botPermissions = arrayListOf<Permission>()

    var subCommandUseScope = hashMapOf<String, UseScope>()

    lateinit var channel: MessageChannel
    lateinit var author: User

    var server: Guild? = null
    var serverChannel: GuildChannel? = null
    var member: Member? = null

    var userLocale: Locale = Locale.ENGLISH
    var serverLocale: Locale = Locale.ENGLISH

    var stopWatch: StopWatch? = null

    /**
     * Method to implement for command execution
     *
     * @param event The [SlashCommandInteractionEvent] that triggered this Command
     */
    @Trace
    protected abstract fun execute(event: SlashCommandInteractionEvent)

    /**
     * This method gets triggered, as soon as a button event for this command is called.
     * Make sure that you register your message id: discordBot.registerMessageForButtonEvents(it, this)
     *
     * @param event
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    @Trace
    open fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }

    /**
     * This method gets triggered, as soon as a button event observer duration timeout is reached.
     *
     * @param event
     */
    @Trace
    open fun onButtonInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    /**
     * This method gets triggered, as soon as a select event for this command is called.
     * Make sure that you register your message id: discordBot.registerMessageForSelectEvents(it, this)
     *
     * @param event
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    @Trace
    open fun onSelectMenuInteraction(event: SelectMenuInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }

    /**
     * This method gets triggered, as soon as a select event observer duration timeout is reached.
     */
    @Trace
    open fun onSelectMenuInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    /**
     * On destroy gets called, when the object gets destroyed after the defined beanTimoutDelay.
     *
     */
    @Trace
    open fun onDestroy() {
    }

    /**
     * This method gets triggered, as soon as an autocomplete event for this command is called.
     * This will always use the same instance if user and server is the same. The command itself will than override this instance.
     * Before calling this method, Aluna will execute discordCommandLoadAdditionalData.loadData()
     *
     * @param event
     */
    @Trace
    open fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
    }

    internal fun onAutoCompleteEventCall(option: String, event: CommandAutoCompleteInteractionEvent) {
        discordCommandLoadAdditionalData.loadData(this, event)
        onAutoCompleteEvent(option, event)
    }

    /**
     * This method gets triggered, as soon as a modal event for this command is called.
     *
     * @param event
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    @Trace
    open fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }

    /**
     * This method gets triggered, as soon as a modal event observer duration timeout is reached.
     */
    @Trace
    open fun onModalInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    open fun onOwnerCommandNotAllowedByUser(event: SlashCommandInteractionEvent) {
        event.deferReply(true).setContent("${AlunaEmote.SMALL_CROSS.asMention()} This command is to powerful for you.").queue()
    }

    open fun onWrongUseScope(event: SlashCommandInteractionEvent, wrongUseScope: WrongUseScope) {
        when {
            wrongUseScope.serverOnly -> {
                event.deferReply(true).setContent("${AlunaEmote.SMALL_CROSS.asMention()} This command can only be used on a server directly.").queue()
            }
            wrongUseScope.subCommandServerOnly -> {
                event.deferReply(true).setContent("${AlunaEmote.SMALL_CROSS.asMention()} This command can only be used on a server directly.").queue()
            }
        }
    }

    open fun onMissingUserPermission(event: SlashCommandInteractionEvent, missingPermissions: MissingPermissions) {
        val textChannelPermissions = missingPermissions.textChannel.joinToString("\n") { "└ ${it.getName()}" }
        val voiceChannelPermissions = missingPermissions.voiceChannel.joinToString("\n") { "└ ${it.getName()}" }
        val serverPermissions = missingPermissions.server.joinToString("\n") { "└ ${it.getName()}" }
        event.deferReply(true).setContent(
            "${AlunaEmote.SMALL_CROSS.asMention()} You are missing the following permission to execute this command:\n" +
                    (if (textChannelPermissions.isNotBlank()) textChannelPermissions + "\n" else "") +
                    (if (voiceChannelPermissions.isNotBlank()) voiceChannelPermissions + "\n" else "") +
                    (if (serverPermissions.isNotBlank()) serverPermissions + "\n" else "")
        ).queue()
    }

    open fun onMissingBotPermission(event: SlashCommandInteractionEvent, missingPermissions: MissingPermissions) {
        when {
            missingPermissions.notInVoice -> {
                event.deferReply(true)
                    .setContent("${AlunaEmote.SMALL_CROSS.asMention()} You need to be in a voice channel yourself to execute this command").queue()

            }
            (missingPermissions.hasMissingPermissions) -> {
                event.deferReply(true).setContent("${AlunaEmote.SMALL_CROSS.asMention()} I'm missing the following permission to execute this command:\n" +
                        missingPermissions.textChannel.joinToString("\n") { "└ ${it.getName()}" } + "\n" +
                        missingPermissions.voiceChannel.joinToString("\n") { "└ ${it.getName()}" } + "\n" +
                        missingPermissions.server.joinToString("\n") { "└ ${it.getName()}" }
                ).queue()
            }
        }
    }

    open fun onFailedAdditionalRequirements(event: SlashCommandInteractionEvent, additionalRequirements: AdditionalRequirements) {
        event.deferReply(true).setContent("${AlunaEmote.SMALL_CROSS.asMention()} Additional requirements for this command failed.").queue()
    }

    open fun onExecutionException(event: SlashCommandInteractionEvent, exception: Exception) {
        throw exception
    }

    open fun initCommandOptions() {}
    open fun initSubCommands() {}

    open fun getServerSpecificData(): HashMap<String, Any> {
        return hashMapOf()
    }

    fun prepareCommand() {
        processDevelopmentStatus()
    }

    @Experimental("This gets called by Aluna, but is currently only a preparation for Localization.")
    fun prepareLocalization() {
        if (alunaProperties.translation.enabled) {
//            this.setLocalizationMapper(LocalizationMapper.fromFunction(localizationFunction))
            this.toData()
        }
    }

    /**
     * Runs checks for the [DiscordCommand] with the given [SlashCommandInteractionEvent] that called it.
     *
     * @param event The CommandEvent that triggered this Command
     */
    @Trace
    fun run(event: SlashCommandInteractionEvent) {
        if (alunaProperties.debug.useStopwatch) {
            stopWatch = StopWatch()
            stopWatch!!.start()
        }

        MDC.put("command", event.commandPath)

        server = event.guild
        server?.let { MDC.put("discord.server", "${it.id} (${it.name})") }
        channel = event.channel
        MDC.put("discord.channel", channel.id)
        author = event.user
        MDC.put("author", "${author.id} (${author.name})")

        userLocale = event.userLocale

        if (server != null) {
            member = server!!.getMember(author)
            serverChannel = event.guildChannel
            serverLocale = event.guildLocale
        }

        if (isOwnerCommand && author.idLong !in ownerIdProvider.getOwnerIds()) {
            onOwnerCommandNotAllowedByUser(event)
            return
        }

        //checkForBlockedIds(event)
        //checkIfLocalDevelopment(event)
        //checkCommandStatus(event)

        val wrongUseScope = discordCommandConditions.checkUseScope(this, useScope, subCommandUseScope, event)
        if (wrongUseScope.wrongUseScope) {
            onWrongUseScope(event, wrongUseScope)
            return
        }

        //executeCategoryChecks(event)

        //checkChannelTopics(event)

        if (isAdministratorOnlyCommand) {
            val missingAdministratorPermission = discordCommandConditions.checkForNeededUserPermissions(this, arrayListOf(Permission.ADMINISTRATOR), event)
            if (missingAdministratorPermission.hasMissingPermissions) {
                onMissingUserPermission(event, missingAdministratorPermission)
                return
            }
        }

        val missingUserPermissions = discordCommandConditions.checkForNeededUserPermissions(this, userPermissions, event)
        if (missingUserPermissions.hasMissingPermissions) {
            onMissingUserPermission(event, missingUserPermissions)
            return
        }

        val missingBotPermissions = discordCommandConditions.checkForNeededBotPermissions(this, botPermissions, event)
        if (missingBotPermissions.hasMissingPermissions) {
            onMissingBotPermission(event, missingBotPermissions)
            return
        }

        //checkForCommandCooldown(event)

        //checkAdditionalRequirements(event)
        val additionalRequirements = discordCommandAdditionalConditions.checkForAdditionalRequirements(this, event)
        if (additionalRequirements.failed) {
            onFailedAdditionalRequirements(event, additionalRequirements)
            return
        }

        //Load additional data for this command
        discordCommandLoadAdditionalData.loadData(this, event)

        try {
            //Run onCommandExecution in asyncExecutor to ensure it is not blocking the execution of the command itself
            discordBot.asyncExecutor.execute {
                discordCommandMetaDataHandler.onCommandExecution(this, event)
            }
            if (alunaProperties.discord.publishDiscordCommandEvent) {
                eventPublisher.publishDiscordCommandEvent(author, channel, server, event.commandPath, this)
            }
            logger.info("Run command /${event.commandPath}" + if (alunaProperties.debug.showHashCode) " [${this.hashCode()}]" else "")
            execute(event)
        } catch (e: Exception) {
            try {
                onExecutionException(event, e)
            } catch (exceptionError: Exception) {
                discordCommandMetaDataHandler.onGenericExecutionException(this, e, exceptionError, event)
            }
        } finally {
            exitCommand(event)
        }

    }

    private fun exitCommand(event: SlashCommandInteractionEvent) {
        if (alunaProperties.debug.useStopwatch && stopWatch != null) {
            stopWatch!!.stop()
            logger.info("/${event.commandPath} (${this.author.id})${if (alunaProperties.debug.showHashCode) " [${this.hashCode()}]" else ""} -> ${stopWatch!!.totalTimeMillis}ms")
            when {
                (stopWatch!!.totalTimeMillis > 3000) -> logger.warn("The execution of the command /${event.commandPath} until it got completed took longer than 3 second. Make sure you acknowledge the event as fast as possible. If it got acknowledge at the end of the method, the interaction token was no longer valid.")
                (stopWatch!!.totalTimeMillis > 1500) -> logger.warn("The execution of the command /${event.commandPath} until it got completed took longer than 1.5 second. Make sure that you acknowledge the event as fast as possible. Because the initial interaction token is only 3 seconds valid.")
            }
        }
        discordBot.asyncExecutor.execute {
            discordCommandMetaDataHandler.onExitCommand(this, stopWatch, event)
        }
    }

    private fun processDevelopmentStatus() {
        when (commandDevelopmentStatus) {
            DevelopmentStatus.IN_DEVELOPMENT,
            DevelopmentStatus.ALPHA -> {
                this.isEarlyAccessCommand = false
            }
            DevelopmentStatus.EARLY_ACCESS -> {
                this.isEarlyAccessCommand = true
            }
            DevelopmentStatus.LIVE -> {
                this.isEarlyAccessCommand = false
            }
        }
    }

    enum class DevelopmentStatus {
        IN_DEVELOPMENT,
        ALPHA,
        EARLY_ACCESS,
        LIVE
    }

    enum class UseScope {
        GLOBAL,
        GUILD_ONLY,

        @Experimental("This UseScope is currently not in use")
        GUILD_SPECIFIC
    }

    class MissingPermissions(
        val textChannel: ArrayList<Permission> = arrayListOf(),
        val voiceChannel: ArrayList<Permission> = arrayListOf(),
        val server: ArrayList<Permission> = arrayListOf(),
        var notInVoice: Boolean = false,
    ) {
        val hasMissingPermissions: Boolean
            get() = textChannel.isNotEmpty() || voiceChannel.isNotEmpty() || server.isNotEmpty()
    }

    class WrongUseScope(var serverOnly: Boolean = false, var subCommandServerOnly: Boolean = false) {
        val wrongUseScope: Boolean
            get() = serverOnly || subCommandServerOnly
    }

    class AdditionalRequirements(val failedRequirements: HashMap<String, Any> = hashMapOf()) {
        val failed: Boolean
            get() = failedRequirements.isNotEmpty()
    }

    fun MessageService.getForUser(key: String, vararg args: String): String = this.get(key, userLocale, *args)
    fun MessageService.getForServer(key: String, vararg args: String): String = this.get(key, serverLocale, *args)

    enum class EventRegisterType {
        BUTTON, SELECT, MODAL
    }
}

fun <T : Any> RestAction<T>.queueAndRegisterInteraction(
    hook: InteractionHook,
    command: DiscordCommand,
    type: ArrayList<DiscordCommand.EventRegisterType> = arrayListOf(DiscordCommand.EventRegisterType.BUTTON),
    persist: Boolean = false,
    duration: Duration = Duration.ofMinutes(15),
    additionalData: HashMap<String, Any?> = hashMapOf(),
    authorIds: ArrayList<String>? = arrayListOf(command.author.id),
    commandUserOnly: Boolean = true,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in T>? = null
) {
    this.queue({
        if (type.contains(DiscordCommand.EventRegisterType.BUTTON)) {
            command.discordBot.registerMessageForButtonEvents(hook, command, persist, duration, additionalData, authorIds, commandUserOnly)
        }
        if (type.contains(DiscordCommand.EventRegisterType.SELECT)) {
            command.discordBot.registerMessageForSelectEvents(hook, command, persist, duration, additionalData, authorIds, commandUserOnly)
        }
        if (type.contains(DiscordCommand.EventRegisterType.MODAL)) {
            command.discordBot.registerMessageForModalEvents(command.author.id, command, persist, duration, additionalData)
        }
        success?.accept(it)
    }, {
        failure?.accept(it)
    })
}

fun RestAction<Void>.queueAndRegisterInteraction(
    command: DiscordCommand,
    type: ArrayList<DiscordCommand.EventRegisterType> = arrayListOf(DiscordCommand.EventRegisterType.MODAL),
    persist: Boolean = false,
    duration: Duration = Duration.ofMinutes(15),
    additionalData: HashMap<String, Any?> = hashMapOf(),
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in Void>? = null
) {
    this.queue({
        if (type.contains(DiscordCommand.EventRegisterType.MODAL)) {
            command.discordBot.registerMessageForModalEvents(command.author.id, command, persist, duration, additionalData)
        }
        success?.accept(it)
    }, {
        failure?.accept(it)
    })
}

fun ReplyCallbackAction.queueAndRegisterInteraction(
    command: DiscordCommand,
    type: ArrayList<DiscordCommand.EventRegisterType> = arrayListOf(DiscordCommand.EventRegisterType.BUTTON),
    persist: Boolean = false,
    duration: Duration = Duration.ofMinutes(15),
    additionalData: HashMap<String, Any?> = hashMapOf(),
    authorIds: ArrayList<String>? = arrayListOf(command.author.id),
    commandUserOnly: Boolean = true,
    failure: Consumer<in Throwable>? = null,
    success: Consumer<in InteractionHook>? = null
) {
    this.queue({
        if (type.contains(DiscordCommand.EventRegisterType.BUTTON)) {
            command.discordBot.registerMessageForButtonEvents(it, command, persist, duration, additionalData, authorIds, commandUserOnly)
        }
        if (type.contains(DiscordCommand.EventRegisterType.SELECT)) {
            command.discordBot.registerMessageForSelectEvents(it, command, persist, duration, additionalData, authorIds, commandUserOnly)
        }
        if (type.contains(DiscordCommand.EventRegisterType.MODAL)) {
            command.discordBot.registerMessageForModalEvents(command.author.id, command, persist, duration, additionalData)
        }
        success?.accept(it)
    }, {
        failure?.accept(it)
    })
}
