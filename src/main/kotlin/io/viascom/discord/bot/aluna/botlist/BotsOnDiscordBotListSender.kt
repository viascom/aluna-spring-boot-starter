package io.viascom.discord.bot.aluna.botlist

import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.sharding.ShardManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class BotsOnDiscordBotListSender(
    private val alunaProperties: AlunaProperties,
    private val shardManager: ShardManager
) : BotListSender {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun sendStats(totalServer: Int, totalShards: Int) {
        val botsOnDiscordToken = alunaProperties.botList.botsOnDiscordToken
        if (botsOnDiscordToken == null) {
            logger.debug("Stats are not sent to bots.ondiscord.xyz because token is not set")
            return
        }

        logger.debug("Send stats to bots.ondiscord.xyz")

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url("https://bots.ondiscord.xyz/bot-api/bots/${alunaProperties.discord.applicationId}/guilds").post(
            "{\"guildCount\": ${shardManager.guilds.size}}".toRequestBody("application/json".toMediaType())
        ).header("Authorization", botsOnDiscordToken).build()

        httpClient.newCall(request).execute()
    }
}
