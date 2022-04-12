package io.viascom.discord.bot.starter

import io.viascom.discord.bot.starter.bot.handler.Command
import io.viascom.discord.bot.starter.bot.handler.DiscordCommand
import io.viascom.discord.bot.starter.util.removeActionRows
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button

@Command
class PongCommand : DiscordCommand(
    "pong",
    "Send a pong"
) {

    init {
        this.commandDevelopmentStatus = DevelopmentStatus.IN_DEVELOPMENT
    }

    val lastEmbed = null

    override fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply().queue { hook ->

            hook.editOriginal("").setActionRows(ActionRow.of(Button.primary("hi", "Hi"))).queueAndRegisterInteraction(hook,this, persist = true)

        }


        val action = event.reply("Pong\nYour locale is:${this.userLocale}").addActionRows(ActionRow.of(Button.primary("hi", "Hi")))
        action.queueAndRegisterInteraction(this)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        if (event.componentId == "hi") {
            event.editMessage("Oh hi :)").removeActionRows().queue()
        }

        return true
    }
}
