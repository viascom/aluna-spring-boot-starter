package io.viascom.discord.bot.aluna

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class PingCommandListener : ListenerAdapter() {

    //This gets called when a slash command gets used.
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        event.reply("Pong!").queue()
    }
}
