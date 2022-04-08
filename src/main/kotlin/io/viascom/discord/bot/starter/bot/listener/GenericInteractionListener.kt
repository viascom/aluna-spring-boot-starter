package io.viascom.discord.bot.starter.bot.listener

import io.viascom.discord.bot.starter.bot.DiscordBot
import io.viascom.discord.bot.starter.configuration.scope.DiscordContext
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
class GenericInteractionListener(
    private val discordBot: DiscordBot,
    private val context: ConfigurableApplicationContext
) : ListenerAdapter() {

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        discordBot.asyncExecutor.execute {
            val commandId = discordBot.commandsWithAutocomplete.firstOrNull { it == event.name }
            if (event.name == commandId) {
                discordBot.commands[event.name]?.let { command ->
                    discordBot.commandExecutor.execute {
                        DiscordContext.setDiscordState(event.user.id, event.guild?.id)
                        context.getBean(command).onAutoCompleteEvent(event.focusedOption.name, event)
                    }
                }
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        discordBot.asyncExecutor.execute {
            if (discordBot.messagesToObserveButton.containsKey(event.message.id)) {
                discordBot.commandExecutor.execute {
                    DiscordContext.setDiscordState(event.user.id, event.guild?.id)
                    val entry = discordBot.messagesToObserveButton[event.message.id]!!
                    if (!entry.third) {
                        discordBot.removeMessageForButtonEvents(event.message.id)
                    }
                    context.getBean(entry.first.java).onButtonInteraction(event)
                }
            }
        }
    }


    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent) {
        discordBot.asyncExecutor.execute {
            if (discordBot.messagesToObserveSelect.containsKey(event.message.id)) {
                discordBot.commandExecutor.execute {
                    DiscordContext.setDiscordState(event.user.id, event.guild?.id)
                    val entry = discordBot.messagesToObserveSelect[event.message.id]!!
                    if (!entry.third) {
                        discordBot.removeMessageForSelectEvents(event.message.id)
                    }
                    context.getBean(entry.first.java).onSelectMenuInteraction(event)
                }
            }
        }
    }
}
