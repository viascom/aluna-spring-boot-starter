package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.configuration.condition.SendServerNotificationCondition
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.getServerTextChannel
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component
import java.awt.Color

@Component
@Conditional(SendServerNotificationCondition::class)
open class ServerNotificationEvent(private val discordBot: DiscordBot, private val shardManager: ShardManager, private val alunaProperties: AlunaProperties) :
    ListenerAdapter() {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onGuildJoin(event: GuildJoinEvent) {
        discordBot.asyncExecutor.execute {
            if (!alunaProperties.notification.serverJoin.enable) {
                return@execute
            }

            val server = event.guild
            val embedMessage = EmbedBuilder()
                .setTitle("\uD83D\uDFE2 New server **${server.name}** joined")
                .setColor(Color.GREEN)
                .setDescription("")
                .setThumbnail(server.iconUrl)
                .addField("» Server", "Name: ${server.name}\nId: ${server.id}", false)
                .addField("» Owner", "Name: ${server.owner?.effectiveName}\nId: ${server.ownerId}", false)
                .addField("» Locale", "Name: ${server.locale.displayName}", false)
                .addField("» Members", "Total: ${server.memberCount}", false)
                .addField("» Other Bots", server.loadMembers().get().filter { it.user.isBot }.joinToString(", ") { it.user.asTag }, false)

            val channel = shardManager.getServerTextChannel(
                alunaProperties.notification.serverJoin.server.toString(),
                alunaProperties.notification.serverJoin.channel.toString()
            )

            if (channel == null) {
                logger.warn("Aluna was not able to send a GuildJoinEvent to the defined channel.")
                return@execute
            }

            channel.sendMessageEmbeds(embedMessage.build()).queue()
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        discordBot.asyncExecutor.execute {
            if (!alunaProperties.notification.serverLeave.enable) {
                return@execute
            }

            val server = event.guild
            val embedMessage = EmbedBuilder()
                .setTitle("\uD83D\uDD34 Server **${server.name}** left")
                .setColor(Color.RED)
                .setDescription("")
                .setThumbnail(server.iconUrl)
                .addField("» Server", "Name: ${server.name}\nId: ${server.id}", false)
                .addField("» Owner", "Name: ${server.owner?.effectiveName}\nId: ${server.ownerId}", false)
                .addField("» Locale", "Name: ${server.locale.displayName}", false)
                .addField("» Members", "Total: ${server.memberCount}", false)
                .addField(
                    "» Other Bots",
                    server.loadMembers().get().filter { it.user.isBot && it.user.id != server.jda.selfUser.id }.joinToString(", ") { it.user.asTag },
                    false
                )

            val channel = shardManager.getServerTextChannel(
                alunaProperties.notification.serverLeave.server.toString(),
                alunaProperties.notification.serverLeave.channel.toString()
            )

            if (channel == null) {
                logger.warn("Aluna was not able to send a GuildLeaveEvent to the defined channel.")
                return@execute
            }

            channel.sendMessageEmbeds(embedMessage.build()).queue()
        }
    }
}
