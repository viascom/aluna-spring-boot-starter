package io.viascom.discord.bot.starter.bot.listener

import io.viascom.discord.bot.starter.bot.DiscordBot
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
class GenericAutoCompleteListener(
    private val discordBot: DiscordBot,
    private val context: ConfigurableApplicationContext
) : ListenerAdapter() {

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        val commandId = discordBot.commandsWithAutoComplete.entries.firstOrNull { it.key == event.name }?.key
        if (event.name == commandId) {
            discordBot.commandsWithAutoComplete[event.name]?.let { command -> discordBot.commandExecutor.execute { context.getBean(command).onAutoCompleteEvent(event.focusedOption.name, event) } }
        }
    }

}
