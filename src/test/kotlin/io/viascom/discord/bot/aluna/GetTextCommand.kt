package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.handler.Command
import io.viascom.discord.bot.aluna.bot.handler.DiscordMessageContextMenu
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent

@Command
class GetTextCommand : DiscordMessageContextMenu(
    "Upload Report"
) {

    override fun execute(event: MessageContextInteractionEvent) {
        event.reply("Hello").queue()
    }

}
