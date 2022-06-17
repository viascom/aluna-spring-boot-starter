package io.viascom.discord.bot.aluna.event

import io.viascom.discord.bot.aluna.bot.AutoCompleteHandler
import org.springframework.context.ApplicationEvent
import kotlin.reflect.KClass

class DiscordAutoCompleteHandlerInitializedEvent(source: Any?, val handlers: List<KClass<out AutoCompleteHandler>>) :
    ApplicationEvent(source)
