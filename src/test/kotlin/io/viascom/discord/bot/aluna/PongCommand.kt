package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.Command
import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.util.removeActionRows
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.time.Duration

@Command
class PongCommand : DiscordCommand(
    "pong",
    "Send a pong"
) {

    init {
        this.commandDevelopmentStatus = DevelopmentStatus.IN_DEVELOPMENT
        this.beanTimoutDelay = Duration.ofSeconds(30)
    }

    val lastEmbed = null

    override fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply().queue { hook ->
            logger.debug(this.hashCode().toString())
            hook.editOriginal("I'm currently Online").setActionRows(ActionRow.of(Button.primary("hi", "Hi")))
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
