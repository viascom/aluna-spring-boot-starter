package io.viascom.discord.bot.starter

import io.viascom.discord.bot.starter.bot.emotes.AlunaEmote
import io.viascom.discord.bot.starter.bot.handler.Command
import io.viascom.discord.bot.starter.bot.handler.DiscordCommand
import io.viascom.discord.bot.starter.util.removeActionRows
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.util.concurrent.TimeUnit

@Command
class PongCommand : DiscordCommand(
    "pong",
    "Send a pong"
) {

    init {
        this.commandDevelopmentStatus = DevelopmentStatus.IN_DEVELOPMENT
        this.beanTimoutDelay = 30L
        this.beanTimoutDelayUnit = TimeUnit.SECONDS
    }

    val lastEmbed = null

    override fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply().queue { hook ->
            logger.debug(this.hashCode().toString())
            hook.editOriginal("I'm currently ${AlunaEmote.ONLINE.asMention()} Online").setActionRows(ActionRow.of(Button.primary("hi", "Hi")))
                .queueAndRegisterInteraction(hook, this, persist = true, type = arrayListOf(EventRegisterType.BUTTON, EventRegisterType.SELECT))

        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        logger.debug(this.hashCode().toString())
        if (event.componentId == "hi") {
            event.editMessage("Oh hi :)").removeActionRows().queue()
        }

        return true
    }

    override fun onDestroy() {
        logger.debug("/pong got destroyed " + this.hashCode().toString())
    }
}
