package io.viascom.discord.bot.aluna.bot.handler

import datadog.trace.api.Trace
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.translation.MessageService
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.*
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StopWatch
import java.time.Duration
import java.util.*
import java.util.function.Consumer


abstract class DiscordContextMenu(type: Command.Type, name: String) : CommandDataImpl(type, name), CommandScopedObject {

    @Autowired
    lateinit var alunaProperties: AlunaProperties

    @Autowired
    lateinit var discordCommandConditions: DiscordCommandConditions

    @Autowired
    lateinit var discordBot: DiscordBot

    @Autowired(required = false)
    lateinit var messageService: MessageService

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override lateinit var uniqueId: String

    var commandDevelopmentStatus = DiscordCommand.DevelopmentStatus.LIVE

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
    lateinit var author: User

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
     *
     * @param event
     */
    @Trace
    open fun onSelectMenuInteractionTimeout(additionalData: HashMap<String, Any?>) {
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

    @Trace
    open fun onDestroy() {
    }


    fun writeToStats() {

    }

    fun exitCommand(event: GenericCommandInteractionEvent) {
        if (alunaProperties.useStopwatch && stopWatch != null) {
            stopWatch!!.stop()
            logger.info("${event.name} (${this.author.id})${if (alunaProperties.showHashCode) " [${this.hashCode()}]" else ""} -> ${stopWatch!!.totalTimeMillis}ms")
        }
    }

    fun <T : Any> RestAction<T>.queueAndRegisterInteraction(
        hook: InteractionHook,
        command: DiscordCommand,
        type: ArrayList<DiscordCommand.EventRegisterType> = arrayListOf(DiscordCommand.EventRegisterType.BUTTON),
        persist: Boolean = false,
        duration: Duration = Duration.ofMinutes(15),
        additionalData: HashMap<String, Any?> = hashMapOf(),
        authorIds: ArrayList<String>? = arrayListOf(author.id),
        commandUserOnly: Boolean = true,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in T>? = null
    ) {
        this.queue({
            if (type.contains(DiscordCommand.EventRegisterType.BUTTON)) {
                discordBot.registerMessageForButtonEvents(hook, command, persist, duration, additionalData, authorIds, commandUserOnly)
            }
            if (type.contains(DiscordCommand.EventRegisterType.SELECT)) {
                discordBot.registerMessageForSelectEvents(hook, command, persist, duration, additionalData, authorIds, commandUserOnly)
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
        authorIds: ArrayList<String>? = arrayListOf(author.id),
        commandUserOnly: Boolean = true,
        failure: Consumer<in Throwable>? = null,
        success: Consumer<in InteractionHook>? = null
    ) {
        this.queue({
            if (type.contains(DiscordCommand.EventRegisterType.BUTTON)) {
                discordBot.registerMessageForButtonEvents(it, command, persist, duration, additionalData, authorIds, commandUserOnly)
            }
            if (type.contains(DiscordCommand.EventRegisterType.SELECT)) {
                discordBot.registerMessageForSelectEvents(it, command, persist, duration, additionalData, authorIds, commandUserOnly)
            }
            success?.accept(it)
        }, {
            failure?.accept(it)
        })
    }
}