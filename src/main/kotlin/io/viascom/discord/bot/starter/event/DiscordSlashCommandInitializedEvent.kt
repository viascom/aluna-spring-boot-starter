package io.viascom.discord.bot.starter.event

import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.springframework.context.ApplicationEvent
import kotlin.reflect.KClass

class DiscordSlashCommandInitializedEvent(source: Any?, val newUpdatedCommands: List<KClass<out CommandDataImpl>>, val removedCommands: List<String>) :
    ApplicationEvent(source)
