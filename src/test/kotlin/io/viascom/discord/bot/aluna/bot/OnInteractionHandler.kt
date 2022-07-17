package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.bot.handler.DiscordInteractionMetaDataHandler
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.util.concurrent.TimeUnit

@Service
class OnInteractionHandler : DiscordInteractionMetaDataHandler {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onCommandExecution(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent) {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()


        httpClient.newCall(Request.Builder().get().url("http://httpbin.org/delay/10").build()).execute()
        logger.info("After rest :)")
    }

    override fun onContextMenuExecution(contextMenu: DiscordContextMenu, event: GenericCommandInteractionEvent) {

    }

    override fun onExitInteraction(discordCommand: DiscordCommand, stopWatch: StopWatch?, event: SlashCommandInteractionEvent) {

    }

    override fun onExitInteraction(contextMenu: DiscordContextMenu, stopWatch: StopWatch?, event: GenericCommandInteractionEvent) {

    }

    override fun onGenericExecutionException(
        discordCommand: DiscordCommand,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        event: GenericCommandInteractionEvent
    ) {
    }

    override fun onGenericExecutionException(
        contextMenu: DiscordContextMenu,
        throwableOfExecution: Exception,
        exceptionOfSpecificHandler: Exception,
        event: GenericCommandInteractionEvent
    ) {
    }
}