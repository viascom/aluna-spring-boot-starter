package io.viascom.discord.bot.starter.bot.listener

import io.viascom.discord.bot.starter.bot.DiscordBot
import io.viascom.discord.bot.starter.bot.handler.DiscordCommand
import io.viascom.discord.bot.starter.event.DiscordReadyEvent
import io.viascom.discord.bot.starter.property.AlunaProperties
import io.viascom.discord.bot.starter.util.getServerTextChannel
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import java.awt.Color

@Service
open class DiscordReadyEventListener(
    private val commands: List<DiscordCommand>,
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val alunaProperties: AlunaProperties
) : ApplicationListener<DiscordReadyEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordReadyEvent) {
        discordBot.asyncExecutor.execute {
            if (alunaProperties.notification.botReady.enable) {
                val embedMessage = EmbedBuilder()
                    .setTitle("⚡ Bot Ready")
                    .setColor(Color.GREEN)
                    .setDescription("Bot is up and ready to answer commands.")
                    .addField("» Client-Id", alunaProperties.discord.applicationId, false)
                    .addField("» Total Commands", commands.size.toString(), true)
                    .addField("» Production Mode", alunaProperties.productionMode.toString(), true)

                val channel = shardManager.getServerTextChannel(
                    alunaProperties.notification.botReady.server.toString(),
                    alunaProperties.notification.botReady.channel.toString()
                )

                if (channel == null) {
                    logger.warn("Aluna was not able to send a DiscordReadyEvent to the defined channel.")
                    return@execute
                }

                channel.sendMessageEmbeds(embedMessage.build()).queue()
            }
        }
    }

}
