package io.viascom.discord.bot.starter

import io.viascom.discord.bot.starter.bot.handler.AutoCompleteHandler
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent

class AutoTest : AutoCompleteHandler(PingCommand::class, "name") {
    override fun onRequest(event: CommandAutoCompleteInteractionEvent) {
        event.replyChoice("Hallo", "Hallo").queue()
    }
}
