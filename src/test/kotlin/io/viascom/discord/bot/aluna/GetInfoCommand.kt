package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.handler.Command
import io.viascom.discord.bot.aluna.bot.handler.DiscordUserContextMenu
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent

@Command
class GetInfoCommand : DiscordUserContextMenu(
    "Say Hi"
) {

    override fun execute(event: UserContextInteractionEvent) {
        event.reply("Hello").queue()
    }
}
