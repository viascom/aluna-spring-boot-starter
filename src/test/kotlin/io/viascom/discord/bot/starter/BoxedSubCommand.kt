package io.viascom.discord.bot.starter

import io.viascom.discord.bot.starter.bot.handler.Command
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

@Command
class BoxedSubCommand : ExtendedSubCommand() {
    fun execute(event: SlashCommandInteractionEvent) {
        event.reply("test").queue()
    }
}
