package io.viascom.discord.bot.starter.botlist

import io.viascom.discord.bot.starter.property.AlunaProperties
import net.dv8tion.jda.api.sharding.ShardManager
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class DiscordBotsBotListSender(
    private val alunaProperties: AlunaProperties,
    private val shardManager: ShardManager
) : BotListSender {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun sendStats(totalServer: Int, totalShards: Int) {
        val discordBotsToken = alunaProperties.botList.discordBotsToken
        if (discordBotsToken == null) {
            logger.debug("Stats are not sent to discord.bots.gg because token is not set")
            return
        }

        logger.debug("Send stats to discord.bots.gg")

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url("https://discord.bots.gg/api/v1/bots/${alunaProperties.discord.applicationId}/stats").post(
            RequestBody.create(MediaType.get("application/json"), "{\"guildCount\": ${shardManager.guilds.size},\"shardCount\":${shardManager.shardsTotal}}")
        ).header("Authorization", discordBotsToken).build()

        httpClient.newCall(request).execute()
    }
}