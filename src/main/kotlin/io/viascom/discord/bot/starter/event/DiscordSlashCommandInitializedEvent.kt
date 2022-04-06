package io.viascom.discord.bot.starter.event

import io.viascom.discord.bot.starter.bot.handler.DiscordCommand
import org.springframework.context.ApplicationEvent
import kotlin.reflect.KClass

class DiscordSlashCommandInitializedEvent(source: Any?, val newUpdatedCommands: List<KClass<out DiscordCommand>>, val removedCommands: List<String>) :
    ApplicationEvent(source)
