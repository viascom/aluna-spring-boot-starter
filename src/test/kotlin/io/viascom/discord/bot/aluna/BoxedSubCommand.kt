package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.Command
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

@Command
class BoxedSubCommand(

) : ExtendedSubCommand() {

    var test: String = "test"

    fun execute(event: SlashCommandInteractionEvent) {
        event.reply(test).queue()
    }
}
