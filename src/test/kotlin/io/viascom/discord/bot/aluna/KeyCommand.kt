package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.handler.Command
import io.viascom.discord.bot.aluna.bot.handler.DiscordCommand
import io.viascom.discord.bot.aluna.bot.handler.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.util.createTextInput
import io.viascom.discord.bot.aluna.util.getValueAsString
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle

@Command
class KeyCommand : DiscordCommand(
    "key",
    "Set Key"
) {

    init {
        this.commandDevelopmentStatus = DevelopmentStatus.IN_DEVELOPMENT
    }

    var lastHook: InteractionHook? = null

    override fun execute(event: SlashCommandInteractionEvent) {
        val key: TextInput = createTextInput("key", "Api-Key", TextInputStyle.SHORT, "Enter your Api-Key", 10, 200)

        val modal: Modal = Modal.create("api-key", "Set new Api-Key")
            .addActionRows(ActionRow.of(key))
            .build()

        event.replyModal(modal).queueAndRegisterInteraction(this)
    }

    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        event.deferReply().queue {
            it.editOriginal("Set key to: ${event.getValueAsString("key", "n/a")}").queue()
        }
        return true
    }

    override fun onDestroy() {
        logger.debug("/key got destroyed " + this.hashCode().toString())
    }
}
