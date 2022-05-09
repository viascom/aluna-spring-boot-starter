package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.handler.DiscordMessageContextMenu
import io.viascom.discord.bot.aluna.bot.handler.DiscordUserContextMenu
import io.viascom.discord.bot.aluna.configuration.scope.DiscordContext
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
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
            DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.COMMAND)
            discordBot.commands[event.commandId]?.let { command -> discordBot.commandExecutor.execute { context.getBean(command).run(event) } }
        }
    }

    override fun onUserContextInteraction(event: UserContextInteractionEvent) {
        discordBot.asyncExecutor.execute {
            DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.COMMAND)
            discordBot.contextMenus[event.commandId]?.let { command ->
                discordBot.commandExecutor.execute {
                    (context.getBean(command) as DiscordUserContextMenu).run(
                        event
                    )
                }
            }
        }
    }

    override fun onMessageContextInteraction(event: MessageContextInteractionEvent) {
        discordBot.asyncExecutor.execute {
            DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.COMMAND)
            discordBot.contextMenus[event.name]?.let { command ->
                discordBot.commandExecutor.execute {
                    (context.getBean(command) as DiscordMessageContextMenu).run(
                        event
                    )
                }
            }
        }
    }
}
