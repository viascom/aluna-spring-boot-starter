package io.viascom.discord.bot.starter.bot.handler

import io.viascom.discord.bot.starter.bot.DiscordBot
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.beans.factory.annotation.Autowired
import kotlin.reflect.KClass

abstract class AutoCompleteHandler(private val command: KClass<out DiscordCommand>, private val option: String) : ListenerAdapter() {

    @Autowired
    lateinit var discordBot: DiscordBot

    abstract fun onRequest(event: CommandAutoCompleteInteractionEvent)

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        val commandId = discordBot.commands.entries.firstOrNull { it.value == command.java }?.key
        if (event.name == commandId && event.focusedOption.name == option) {
            onRequest(event)
        }
    }
}
