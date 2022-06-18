package io.viascom.discord.bot.aluna.event

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import net.dv8tion.jda.api.entities.Channel
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.springframework.context.ApplicationEvent

/**
 * Discord command event which gets fired before the command is executed.
 * This event is fired asynchronously.
 *
 * @param user user of the command
 * @param channel channel of the command
 * @param server server of the command if available
 * @param commandPath path of the command
 * @param command command itself
 */
class DiscordCommandEvent(source: Any, val user: User, val channel: Channel, val server: Guild?, val commandPath: String, val command: DiscordCommand) :
    ApplicationEvent(source)
