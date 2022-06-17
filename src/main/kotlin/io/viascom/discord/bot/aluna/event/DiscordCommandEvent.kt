package io.viascom.discord.bot.aluna.event

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import net.dv8tion.jda.api.entities.Channel
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.springframework.context.ApplicationEvent

class DiscordCommandEvent(source: Any?, user: User, channel: Channel, server: Guild?, commandPath: String, command: DiscordCommand) : ApplicationEvent(source)
