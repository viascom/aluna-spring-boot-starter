package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.Command
import io.viascom.discord.bot.aluna.bot.DiscordUserContextMenu
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent

@Command
class CheckUserContextMenu : DiscordUserContextMenu("test") {
    override fun execute(event: UserContextInteractionEvent) {
        event.reply("Hello").queue()
    }

}
