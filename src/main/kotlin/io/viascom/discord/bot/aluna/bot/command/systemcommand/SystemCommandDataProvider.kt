package io.viascom.discord.bot.aluna.bot.command.systemcommand

import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.handler.CommandScopedObject
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.time.Duration

abstract class SystemCommandDataProvider(
    val id: String,
    val name: String,
    var ephemeral: Boolean = true,
    var allowMods: Boolean = false,
    /**
     * Should Aluna call the onArgsAutoComplete method when the user focusing the args field.
     */
    var supportArgsAutoComplete: Boolean = false,
    /**
     * Should Aluna keep the event open or not. If not, Aluna will acknowledge the event before calling execute() and hook is in this case null.
     */
    var autoAcknowledgeEvent: Boolean = true
) : CommandScopedObject {

    override lateinit var uniqueId: String
    override var beanTimoutDelay: Duration = Duration.ofMinutes(15)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = false
    override var beanCallOnDestroy: Boolean = false

    abstract fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand)
    open fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        return true
    }

    open fun onButtonInteractionTimeout() {}
    open fun onSelectMenuInteraction(event: SelectMenuInteractionEvent): Boolean {
        return true
    }

    open fun onSelectMenuInteractionTimeout() {}
    open fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent) {}


    open fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }
    open fun onModalInteractionTimeout(additionalData: HashMap<String, Any?>) {}
}
