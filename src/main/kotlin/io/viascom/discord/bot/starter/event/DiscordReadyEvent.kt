package io.viascom.discord.bot.starter.event

import net.dv8tion.jda.api.events.ReadyEvent
import org.springframework.context.ApplicationEvent

class DiscordReadyEvent(source: Any?, val jdaEvent: ReadyEvent) : ApplicationEvent(source)
