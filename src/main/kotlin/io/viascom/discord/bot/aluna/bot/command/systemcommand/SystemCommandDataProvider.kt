package io.viascom.discord.bot.aluna.bot.command.systemcommand

import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.handler.CommandScopedObject
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.util.concurrent.TimeUnit

abstract class SystemCommandDataProvider(
    val id: String,
    val name: String,
    var ephemeral: Boolean = true,
    var allowMods: Boolean = false,
    var supportArgsAutoComplete: Boolean = false,
    var keepCommandOpen: Boolean = false
) : CommandScopedObject {

    override lateinit var uniqueId: String
    override var beanTimoutDelay: Long = 15
    override var beanTimoutDelayUnit: TimeUnit = TimeUnit.MINUTES
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = false

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
