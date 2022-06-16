package io.viascom.discord.bot.aluna.botlist

import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnAlunaProductionMode
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
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
@ConditionalOnJdaEnabled
@ConditionalOnAlunaProductionMode
class DiscordBotListEuBotListSender(
    private val alunaProperties: AlunaProperties,
    private val shardManager: ShardManager
) : BotListSender {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun sendStats(totalServer: Int, totalShards: Int) {
        val discordBotListEuToken = alunaProperties.botList.discordBotListEuToken
        if (discordBotListEuToken == null) {
            logger.debug("Stats are not sent to discord-botlist.eu because token is not set")
            return
        }

        logger.debug("Send stats to discord-botlist.eu")

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url("https://api.discord-botlist.eu/v1/update").post(
            "{\"serverCount\": ${shardManager.guilds.size}}".toRequestBody("application/json".toMediaType())
        ).header("Authorization", discordBotListEuToken).build()

        httpClient.newCall(request).execute()
    }
}
