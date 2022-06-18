package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.bot.handler.DiscordCommandLoadAdditionalData
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

abstract class AutoCompleteHandler @JvmOverloads constructor(val commands: ArrayList<Class<out DiscordCommand>>, val option: String? = null) :
    CommandScopedObject {

    constructor(command: Class<out DiscordCommand>, option: String? = null) : this(arrayListOf(command), option)

    @Autowired
    lateinit var discordBot: DiscordBot

    @Autowired
    lateinit var discordCommandLoadAdditionalData: DiscordCommandLoadAdditionalData

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    //This gets set by the CommandContext automatically
    override lateinit var uniqueId: String

    override var beanTimoutDelay: Duration = Duration.ofMinutes(5)
    override var beanUseAutoCompleteBean: Boolean = false
    override var beanRemoveObserverOnDestroy: Boolean = false
    override var beanCallOnDestroy: Boolean = false

    /**
     * This method gets triggered, as soon as an autocomplete event for the option is called.
     * Before calling this method, Aluna will execute discordCommandLoadAdditionalData.loadData()
     *
     * @param event
     */
    abstract fun onRequest(event: CommandAutoCompleteInteractionEvent)

    internal fun onRequestCall(event: CommandAutoCompleteInteractionEvent) {
        discordCommandLoadAdditionalData.loadData(event)
        onRequest(event)
    }
}
