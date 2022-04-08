package io.viascom.discord.bot.starter.bot.listener

import io.viascom.discord.bot.starter.bot.DiscordBot
import io.viascom.discord.bot.starter.configuration.scope.DiscordContext
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
class SlashCommandInteractionEventListener(
    private val discordBot: DiscordBot,
    private val context: ConfigurableApplicationContext
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        discordBot.asyncExecutor.execute {
            DiscordContext.setDiscordState(event.user.id, event.guild?.id, true)
            discordBot.commands[event.name]?.let { command -> discordBot.commandExecutor.execute { context.getBean(command).run(event) } }
        }
    }

}
