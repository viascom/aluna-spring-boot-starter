package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.handler.Command
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

@Command
class BoxedSubCommand2(

) : ExtendedSubCommand() {

    var test: String = "test"

    fun execute(event: SlashCommandInteractionEvent) {
        event.reply(test).queue()
    }
}
