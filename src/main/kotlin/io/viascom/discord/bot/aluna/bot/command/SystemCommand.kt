package io.viascom.discord.bot.aluna.bot.command

import io.viascom.discord.bot.aluna.bot.command.systemcommand.SystemCommandDataProvider
import io.viascom.discord.bot.aluna.bot.emotes.AlunaEmote
import io.viascom.discord.bot.aluna.bot.handler.Command
import io.viascom.discord.bot.aluna.bot.handler.DiscordCommand
import io.viascom.discord.bot.aluna.util.getOptionAsString
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.sharding.ShardManager

@Command
class SystemCommand(
    private val dataProviders: List<SystemCommandDataProvider>,
    private val shardManager: ShardManager
) : DiscordCommand(
    "system-command",
    "Runs a system command.",
    observeAutoComplete = true
) {

    private var selectedProvider: SystemCommandDataProvider? = null

    override fun initCommandOptions() {
        specificServer = alunaProperties.command.systemCommand.server
        addOption(OptionType.STRING, "command", "System command to execute", true, true)
        addOption(OptionType.STRING, "args", "Arguments", false, true)
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        selectedProvider = dataProviders.firstOrNull { it.id == event.getOptionAsString("command", "") }

        if (selectedProvider == null) {
            event.reply("Command not found!").setEphemeral(true).queue()
            return
        }

        //Check if it is an owner or (mod and mod is allowed)
        if (event.user.idLong !in alunaProperties.ownerIds && !(selectedProvider!!.allowMods && event.user.idLong in alunaProperties.modIds)) {
            event.deferReply(true).setContent("${AlunaEmote.BOT_CROSS.asMention()} This command is to powerful for you.").queue()
            return
        }

        val ephemeral = if (event.user.idLong in alunaProperties.modIds) {
            false
        } else {
            selectedProvider!!.ephemeral
        }

        if(selectedProvider!!.keepCommandOpen){
            selectedProvider!!.execute(event, null, this)
        } else{
            val hook = event.deferReply(ephemeral).complete()
            selectedProvider!!.execute(event, hook, this)
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return selectedProvider?.onButtonInteraction(event) ?: true
    }

    override fun onButtonInteractionTimeout(additionalData: HashMap<String, Any?>) {
        selectedProvider?.onButtonInteractionTimeout()
    }

    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return selectedProvider?.onSelectMenuInteraction(event) ?: true
    }

    override fun onSelectMenuInteractionTimeout(additionalData: HashMap<String, Any?>) {
        selectedProvider?.onSelectMenuInteractionTimeout()
    }

    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return selectedProvider?.onModalInteraction(event, additionalData) ?: true
    }

    override fun onModalInteractionTimeout(additionalData: HashMap<String, Any?>) {
        selectedProvider?.onModalInteractionTimeout(additionalData)
    }

    override fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
        super.onAutoCompleteEvent(option, event)

        if (option == "command") {
            val input = event.getOptionAsString(option, "")!!

            val options = if (input.isEmpty()) {
                dataProviders.filter { event.user.idLong in alunaProperties.ownerIds || (it.allowMods && event.user.idLong in alunaProperties.modIds) }
            } else {
                dataProviders.filter { event.user.idLong in alunaProperties.ownerIds || (it.allowMods && event.user.idLong in alunaProperties.modIds) }
                    .filter { it.name.lowercase().contains(input.lowercase()) }
            }.take(25).map {
                net.dv8tion.jda.api.interactions.commands.Command.Choice(it.name, it.id)
            }

            event.replyChoices(options).queue()
            return
        }

        if (option == "args") {
            val possibleProvider = dataProviders.firstOrNull { it.id == event.getOptionAsString("command", "")!! && it.supportArgsAutoComplete }
            if (possibleProvider != null) {
                possibleProvider.onArgsAutoComplete(event)
            } else {
                event.replyChoices().queue()
            }
        }
    }
}
