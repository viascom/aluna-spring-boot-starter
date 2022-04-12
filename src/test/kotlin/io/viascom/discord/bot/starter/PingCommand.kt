package io.viascom.discord.bot.starter

import io.viascom.discord.bot.starter.bot.handler.Command
import io.viascom.discord.bot.starter.bot.handler.DiscordCommand
import io.viascom.discord.bot.starter.bot.listener.EventWaiter
import io.viascom.discord.bot.starter.util.getOptionAsString
import io.viascom.discord.bot.starter.util.removeActionRows
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button


@Command
class PingCommand(
    private val eventWaiter: EventWaiter
) : DiscordCommand(
    "ping",
    "Send a ping",
    true
) {

    override fun initCommandOptions() {
        val mapOption = OptionData(OptionType.STRING, "map", "Select a map", true, true)

        this.addOptions(mapOption)
    }

    override fun execute(event: SlashCommandInteractionEvent) {

        val selectedMap = event.getOptionAsString("map", "all")

        event.reply("Pong\nYour locale is:${this.userLocale}").addActionRows(ActionRow.of(Button.primary("hi", "Hi"))).queue {
            eventWaiter.waitForInteraction("command:ping:" + author.id,
                ButtonInteractionEvent::class.java,
                hook = it,
                action = {
                    if (it.componentId == "hi") {
                        it.editMessage("Oh hi :)").removeActionRows().queue()
                    }
                })
        }
    }

    override fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
        logger.debug(this.hashCode().toString())
        event.replyChoice("Hallo", "Hallo").queue()
    }
}
