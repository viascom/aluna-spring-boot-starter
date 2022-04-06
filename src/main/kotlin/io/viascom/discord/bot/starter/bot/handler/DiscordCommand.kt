package io.viascom.discord.bot.starter.bot.handler

import datadog.trace.api.Trace
import io.viascom.discord.bot.starter.property.AlunaProperties
import io.viascom.discord.bot.starter.translation.MessageService
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StopWatch
import java.util.*

abstract class DiscordCommand(name: String, description: String, val observeAutoComplete: Boolean = false) : CommandDataImpl(name, description),
    SlashCommandData {

    @Autowired
    lateinit var alunaProperties: AlunaProperties

    @Autowired
    lateinit var discordCommandConditions: DiscordCommandConditions

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    lateinit var commandId: String

    var useScope = UseScope.GLOBAL

    var isOwnerCommand = false
    var isAdministratorOnlyCommand = false
    var isEarlyAccessCommand = false
    var isHidden = false

    var commandDevelopmentStatus = DevelopmentStatus.LIVE
        set(value) {
            field = value
            processDevelopmentStatus()
        }

    /**
     * The [CooldownScope][Command.CooldownScope] of the command. This defines how far of a scope cooldowns have.
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
     * This method gets triggered, as soon as an autocomplete event for this event is called. Spring will create a new instance of the command!
     *
     * @param event
     */
    @Trace
    open fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
    }

    open fun initCommandOptions() {}
    open fun initSubCommands() {}

    open fun getServerSpecificData(): HashMap<String, Any> {
        return hashMapOf()
    }

    /**
     * Runs checks for the [DiscordCommand] with the given [SlashCommandInteractionEvent] that called it.
     *
     * @param event The CommandEvent that triggered this Command
     */
    @Trace
    open fun run(event: SlashCommandInteractionEvent) {
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

        try {
            writeToStats()
            logger.info("Run command /${event.commandPath}")
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

    private fun exitCommand(event: net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent) {
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

    fun MessageService.getForUser(key: String, vararg args: Any): String = this.get(key, userLocale, args)
    fun MessageService.getForServer(key: String, vararg args: Any): String = this.get(key, serverLocale, args)
}
