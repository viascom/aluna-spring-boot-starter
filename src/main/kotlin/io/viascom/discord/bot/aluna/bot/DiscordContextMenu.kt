package io.viascom.discord.bot.aluna.bot

import datadog.trace.api.Trace
import io.viascom.discord.bot.aluna.bot.emotes.AlunaEmote
import io.viascom.discord.bot.aluna.bot.handler.DiscordCommandAdditionalConditions
import io.viascom.discord.bot.aluna.bot.handler.DiscordCommandConditions
import io.viascom.discord.bot.aluna.bot.handler.DiscordCommandLoadAdditionalData
import io.viascom.discord.bot.aluna.bot.handler.DiscordCommandMetaDataHandler
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.translation.MessageService
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.*
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StopWatch
import java.time.Duration
import java.util.*

abstract class DiscordContextMenu(type: Command.Type, name: String) : CommandDataImpl(type, name), CommandScopedObject, DiscordInteractionHandler {

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
    override lateinit var discordBot: DiscordBot

    @Autowired(required = false)
    lateinit var messageService: MessageService

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    //This gets set by the CommandContext automatically
    override lateinit var uniqueId: String

    override val interactionName = name

    var commandDevelopmentStatus = DiscordCommand.DevelopmentStatus.LIVE
    private var isEarlyAccessCommand = false

    override var beanTimoutDelay: Duration = Duration.ofMinutes(15)
    override var beanUseAutoCompleteBean: Boolean = false
    override var beanRemoveObserverOnDestroy: Boolean = true
    override var beanCallOnDestroy: Boolean = true

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

    var channel: Channel? = null
    override lateinit var author: User

    var server: Guild? = null
    var serverChannel: GuildChannel? = null
    var member: Member? = null

    var userLocale: Locale = Locale.ENGLISH
    var serverLocale: Locale = Locale.ENGLISH

    var stopWatch: StopWatch? = null

    /**
     * This method gets triggered, as soon as a button event for this command is called.
     * Make sure that you register your message id: discordBot.registerMessageForButtonEvents(it, this)
     *
     * @param event
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    @Trace
    override fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }

    /**
     * This method gets triggered, as soon as a button event observer duration timeout is reached.
     *
     * @param event
     */
    @Trace
    override fun onButtonInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    /**
     * This method gets triggered, as soon as a select event for this command is called.
     * Make sure that you register your message id: discordBot.registerMessageForSelectEvents(it, this)
     *
     * @param event
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    @Trace
    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }

    /**
     * This method gets triggered, as soon as a select event observer duration timeout is reached.
     *
     * @param event
     */
    @Trace
    override fun onSelectMenuInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    /**
     * This method gets triggered, as soon as a modal event for this command is called.
     *
     * @param event
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    @Trace
    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }

    /**
     * This method gets triggered, as soon as a modal event observer duration timeout is reached.
     */
    @Trace
    override fun onModalInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    @Trace
    open fun onDestroy() {
    }

    fun prepareCommand() {
        processDevelopmentStatus()
    }

    open fun onMissingUserPermission(event: GenericCommandInteractionEvent, missingPermissions: DiscordCommand.MissingPermissions) {
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

    open fun onMissingBotPermission(event: GenericCommandInteractionEvent, missingPermissions: DiscordCommand.MissingPermissions) {
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

    open fun onFailedAdditionalRequirements(event: GenericCommandInteractionEvent, additionalRequirements: DiscordCommand.AdditionalRequirements) {
        event.deferReply(true).setContent("${AlunaEmote.SMALL_CROSS.asMention()} Additional requirements for this command failed.").queue()
    }

    open fun onExecutionException(event: GenericCommandInteractionEvent, exception: Exception) {
        throw exception
    }

    fun exitCommand(event: GenericCommandInteractionEvent) {
        if (alunaProperties.debug.useStopwatch && stopWatch != null) {
            stopWatch!!.stop()
            MDC.put("duration", stopWatch!!.totalTimeMillis.toString())
            logger.info("Context menu '${event.commandPath}' (${this.author.id})${if (alunaProperties.debug.showHashCode) " [${this.hashCode()}]" else ""} took ${stopWatch!!.totalTimeMillis}ms")
            when {
                (stopWatch!!.totalTimeMillis > 3000) -> logger.warn("The execution of the context menu ${event.commandPath} until it got completed took longer than 3 second. Make sure you acknowledge the event as fast as possible. If it got acknowledge at the end of the method, the interaction token was no longer valid.")
                (stopWatch!!.totalTimeMillis > 1500) -> logger.warn("The execution of the context menu ${event.commandPath} until it got completed took longer than 1.5 second. Make sure that you acknowledge the event as fast as possible. Because the initial interaction token is only 3 seconds valid.")
            }
            discordBot.asyncExecutor.execute {
                discordCommandMetaDataHandler.onExitCommand(this, stopWatch, event)
            }
        }
    }

    private fun processDevelopmentStatus() {
        when (commandDevelopmentStatus) {
            DiscordCommand.DevelopmentStatus.IN_DEVELOPMENT,
            DiscordCommand.DevelopmentStatus.ALPHA -> {
                this.isEarlyAccessCommand = false
            }
            DiscordCommand.DevelopmentStatus.EARLY_ACCESS -> {
                this.isEarlyAccessCommand = true
            }
            DiscordCommand.DevelopmentStatus.LIVE -> {
                this.isEarlyAccessCommand = false
            }
        }
    }
}
