package io.viascom.discord.bot.aluna.event

import net.dv8tion.jda.api.events.ReadyEvent
import org.springframework.context.ApplicationEvent

class DiscordFirstShardReadyEvent(source: Any?, val jdaEvent: ReadyEvent) : ApplicationEvent(source)