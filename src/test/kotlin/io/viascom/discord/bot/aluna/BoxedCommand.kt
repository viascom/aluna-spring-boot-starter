package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.handler.Command
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

@Command
class BoxedCommand(
    private val boxedSubCommand: BoxedSubCommand
) : ExtendedCommand(
    "boxed",
    "Noice boxed command"
) {
    override fun execute(event: SlashCommandInteractionEvent) {
        boxedSubCommand.execute(event)
    }

}
