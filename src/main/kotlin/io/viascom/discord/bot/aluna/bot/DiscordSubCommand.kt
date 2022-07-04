package io.viascom.discord.bot.aluna.bot

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import java.time.Duration

abstract class DiscordSubCommand(name: String, description: String) : SubcommandData(name, description), InteractionScopedObject, DiscordSubCommandElement {

    override lateinit var uniqueId: String
    override var beanTimoutDelay: Duration = Duration.ofMinutes(14)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = true
    override var beanCallOnDestroy: Boolean = true

    abstract fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: DiscordCommand)

    open fun initCommandOptions() {}

    open fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        return true
    }

    open fun onButtonInteractionTimeout(additionalData: HashMap<String, Any?>) {}
    open fun onSelectMenuInteraction(event: SelectMenuInteractionEvent): Boolean {
        return true
    }

    open fun onSelectMenuInteractionTimeout(additionalData: HashMap<String, Any?>) {}
    open fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent, command: DiscordCommand) {}


    open fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }

    open fun onModalInteractionTimeout(additionalData: HashMap<String, Any?>) {}
}