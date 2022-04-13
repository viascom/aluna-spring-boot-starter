package io.viascom.discord.bot.starter.bot.handler

import datadog.trace.api.Trace
import io.viascom.discord.bot.starter.bot.DiscordBot
import io.viascom.discord.bot.starter.property.AlunaProperties
import io.viascom.discord.bot.starter.translation.MessageService
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
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
import org.springframework.util.StopWatch
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

abstract class DiscordCommand(name: String, description: String, val observeAutoComplete: Boolean = false) : CommandDataImpl(name, description),
    SlashCommandData, CommandScopedObject {

    @Autowired
    lateinit var alunaProperties: AlunaProperties

    @Autowired
    lateinit var discordCommandConditions: DiscordCommandConditions

    @Autowired
    lateinit var discordCommandLoadAdditionalData: DiscordCommandLoadAdditionalData

    @Autowired
    lateinit var discordBot: DiscordBot

    @Autowired(required = false)
    lateinit var messageService: MessageService

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override lateinit var uniqueId: String

    var useScope = UseScope.GLOBAL

    var isOwnerCommand = false
    var isAdministratorOnlyCommand = false
    var isEarlyAccessCommand = false
    var isHidden = false

    var commandDevelopmentStatus = DevelopmentStatus.LIVE

    override var beanTimoutDelay: Long = 15
    override var beanTimoutDelayUnit: TimeUnit = TimeUnit.MINUTES
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = true

    /**
     * The [CooldownScope][Command.CooldownScope] of the command. This defines how far from a scope cooldowns have.
     * <br></br>Default [CooldownScope.USER][Command.CooldownScope.USER].
     */
    var cooldownScope = CooldownScope.USER
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

    /**
     * `true` if this command checks a channel topic for topic-tags.
     * <br></br>This means that putting `{-commandname}`, `{-command category}`, `{-all}` in a channel topic
     * will cause this command to terminate.
     * <br></br>Default `true`.
     */
    protected var usesTopicTags = true

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
     * The main body method of a [DiscordCommand].
     * <br></br>This is the "response" for a successful
     * [#run(DiscordaCommand)][DiscordCommand.execute].
     *
     * @param event The [DiscordCommandEvent] that triggered this Command
     */
    @Trace
    protected abstract fun execute(event: SlashCommandInteractionEvent)

    /**
     * This method gets triggered, as soon as a button event for this command is called.
     * Make sure that you register your message id: discordBot.registerMessageForButtonEvents(it, this)
     *
     * @param event
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
     */
    @Trace
    open fun onSelectMenuInteraction(event: SelectMenuInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }

    /**
     * This method gets triggered, as soon as a select event observer duration timeout is reached.
     *
     * @param event
     */
    @Trace
    open fun onSelectMenuInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    @Trace
    open fun onDestroy() {
    }

    /**
     * This method gets triggered, as soon as an autocomplete event for this command is called.
     * This will always use the same instance if user and server is the same. The command itself will than override this instance.
     *
     * @param event
     */
    @Trace
    open fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
        discordCommandLoadAdditionalData.loadData(this, event)
    }

    /**
     * This method gets triggered, as soon as a modal event for this command is called.
     *
     * @param event
     */
    @Trace
    private fun onModalInteraction(option: String, event: CommandAutoCompleteInteractionEvent) {
    }

    open fun initCommandOptions() {}
    open fun initSubCommands() {}

    open fun getServerSpecificData(): HashMap<String, Any> {
        return hashMapOf()
    }

    fun prepareCommand() {
        processDevelopmentStatus()
    }

    /**
     * Runs checks for the [DiscordCommand] with the given [SlashCommandInteractionEvent] that called it.
     *
     * @param event The CommandEvent that triggered this Command
     */
    @Trace
    fun run(event: SlashCommandInteractionEvent) {
        if (alunaProperties.useStopwatch) {
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

        //checkForBlockedIds(event)
        //checkIfLocalDevelopment(event)
        //checkCommandStatus(event)

        if (!discordCommandConditions.checkUseScope(event, useScope, subCommandUseScope)) {
            return
        }

        //addOrUpdateUserInDatabase(event)
        //loadProperties(event)

        //checkForNeededBotPermissions(event)

        //executeCategoryChecks(event)

        //checkChannelTopics(event)

        if (discordCommandConditions.checkForNeededUserPermissions(event, userPermissions).hasMissingPermissions) {
            return
        }

        if (discordCommandConditions.checkForNeededAleevaPermissions(event, botPermissions).hasMissingPermissions) {
            return
        }

        //checkForCommandCooldown(event)

        //checkAdditionalRequirements(event)

        discordCommandLoadAdditionalData.loadData(this, event)

        try {
            writeToStats()
            logger.info("Run command /${event.commandPath} [${this.hashCode()}]")
            execute(event)
            exitCommand(event)
        } catch (t: Throwable) {
            //ExceptionUtil.sendExceptionToAleevaServer(t, memberEntity.id.toString(), guildEntity?.id.toString(), if (parentName != "") "$parentName $name" else name)
            //if (event.client.listener != null) {
            //    event.client.listener!!.onCommandException(event, this, t)
            //
            //    return
            //}
            // otherwise we rethrow
            exitCommand(event)
            throw t
        }

    }

    private fun exitCommand(event: SlashCommandInteractionEvent) {
        if (alunaProperties.useStopwatch && stopWatch != null) {
            stopWatch!!.stop()
            println("/${event.commandPath} (${this.author.id}) [${this.hashCode()}] -> ${stopWatch!!.totalTimeMillis}ms")
        }
    }

    private fun writeToStats() {

    }

    private fun processDevelopmentStatus() {
        when (commandDevelopmentStatus) {
            DevelopmentStatus.IN_DEVELOPMENT,
            DevelopmentStatus.ALPHA -> {
                if (alunaProperties.productionMode) {
                    this.isHidden = true
                }
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

    fun MessageService.getForUser(key: String, vararg args: String): String = this.get(key, userLocale, *args)
    fun MessageService.getForServer(key: String, vararg args: String): String = this.get(key, serverLocale, *args)

    fun <T : Any> RestAction<T>.queueAndRegisterInteraction(
        hook: InteractionHook,
        command: DiscordCommand,
        type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = arrayListOf(author.id),
        commandUserOnly: Boolean = true,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in T>? = null
    ) {
        this.queue({
            if (type.contains(EventRegisterType.BUTTON)) {
                discordBot.registerMessageForButtonEvents(hook, command, persist, duration, additionalData, authorIds, commandUserOnly)
            }
            if (type.contains(EventRegisterType.SELECT)) {
                discordBot.registerMessageForSelectEvents(hook, command, persist, duration, additionalData, authorIds, commandUserOnly)
            }
            success?.accept(it)
        }, {
            failure?.accept(it)
        })
    }

    fun ReplyCallbackAction.queueAndRegisterInteraction(
        command: DiscordCommand,
        type: ArrayList<EventRegisterType> = arrayListOf(EventRegisterType.BUTTON),
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = arrayListOf(author.id),
        commandUserOnly: Boolean = true,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in InteractionHook>? = null
    ) {
        this.queue({
            if (type.contains(EventRegisterType.BUTTON)) {
                discordBot.registerMessageForButtonEvents(it, command, persist, duration, additionalData, authorIds, commandUserOnly)
            }
            if (type.contains(EventRegisterType.SELECT)) {
                discordBot.registerMessageForSelectEvents(it, command, persist, duration, additionalData, authorIds, commandUserOnly)
            }
            success?.accept(it)
        }, {
            failure?.accept(it)
        })
    }

    enum class EventRegisterType {
        BUTTON, SELECT
    }
}
