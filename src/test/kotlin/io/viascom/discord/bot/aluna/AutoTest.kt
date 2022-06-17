package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.AutoComplete
import io.viascom.discord.bot.aluna.bot.AutoCompleteHandler
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent

@AutoComplete
class AutoTest : AutoCompleteHandler(PingCommand::class.java) {
    override fun onRequest(event: CommandAutoCompleteInteractionEvent) {
        event.replyChoice("Hallo", "Hallo").queue()
    }
}
