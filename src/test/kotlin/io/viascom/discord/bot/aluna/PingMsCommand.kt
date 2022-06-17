package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.Command
import io.viascom.discord.bot.aluna.bot.DiscordCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

@Command
class PingMsCommand : DiscordCommand(
    "ping_ms",
    "Send a ping and get ms"
) {

    override fun execute(event: SlashCommandInteractionEvent) {
        val startTime = System.currentTimeMillis()
        event.reply("Ping ...").setEphemeral(true).queue {
            it.editOriginal("Pong: ${System.currentTimeMillis() - startTime}ms").queue()
        }
    }

    override fun onDestroy() {
        logger.debug("/ping got destroyed " + this.hashCode().toString())
    }

}
