package io.viascom.discord.bot.aluna.event

import io.viascom.discord.bot.aluna.bot.DiscordMessageContextMenu
import net.dv8tion.jda.api.entities.Channel
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.springframework.context.ApplicationEvent

/**
 * Discord message context menu event which gets fired before the context menu is executed.
 * This event is fired asynchronously.
 *
 * @param user user of the context menu
 * @param channel channel of the context menu
 * @param server server of the context menu if available
 * @param name name of the context menu
 * @param contextMenu context menu itself
 */
class DiscordMessageContextEvent(source: Any, val user: User, val channel: Channel?, val server: Guild?, val name: String, val contextMenu: DiscordMessageContextMenu) :
    ApplicationEvent(source)
