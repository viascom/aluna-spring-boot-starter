package io.viascom.discord.bot.aluna.botlist

import com.google.gson.Gson
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.sharding.ShardManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true, havingValue = "true")
class TopGGBotListSender(
    private val alunaProperties: AlunaProperties,
    private val shardManager: ShardManager,
    private val gson: Gson
) : BotListSender {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun sendStats(totalServer: Int, totalShards: Int) {
        val topGGToken = alunaProperties.botList.topggToken
        if (topGGToken == null) {
            logger.debug("Stats are not sent to top.gg because token is not set")
            return
        }

        logger.debug("Send stats to top.gg")

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url("https://top.gg/api/bots/${alunaProperties.discord.applicationId}/stats").post(
            gson.toJson(TopGGData(shardManager.shards.map { it.guilds.size })).toRequestBody("application/json".toMediaType())
        ).header("Authorization", topGGToken).build()

        httpClient.newCall(request).execute()
    }

    class TopGGData(
        val shards: List<Int>
    )
}
