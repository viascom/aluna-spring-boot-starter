package io.viascom.discord.bot.starter.bot.handler

import datadog.trace.api.Trace
import io.viascom.discord.bot.starter.bot.DiscordBot
import io.viascom.discord.bot.starter.property.AlunaProperties
import io.viascom.discord.bot.starter.translation.MessageService
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.command.*
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StopWatch
import java.util.*


abstract class DiscordContextMenu(type: Command.Type, name: String) : CommandDataImpl(type, name) {

    @Autowired
    lateinit var alunaProperties: AlunaProperties

    @Autowired
    lateinit var discordCommandConditions: DiscordCommandConditions

    @Autowired
    lateinit var discordBot: DiscordBot

    @Autowired(required = false)
    lateinit var messageService: MessageService

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    var commandDevelopmentStatus = DiscordCommand.DevelopmentStatus.LIVE

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

    /**
     * This method gets triggered, as soon as a modal event for this command is called.
     *
     * @param event
     */
    @Trace
    private fun onModalInteraction(option: String, event: CommandAutoCompleteInteractionEvent) {
    }

    fun writeToStats() {

    }

    fun exitCommand(event: GenericCommandInteractionEvent) {
        if (alunaProperties.useStopwatch && stopWatch != null) {
            stopWatch!!.stop()
            println("${event.name} (${this.author.id}) [${this.hashCode()}] -> ${stopWatch!!.totalTimeMillis}ms")
        }
    }
}
