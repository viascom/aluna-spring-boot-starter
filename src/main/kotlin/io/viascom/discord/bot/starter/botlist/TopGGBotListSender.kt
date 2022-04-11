package io.viascom.discord.bot.starter.botlist

import io.viascom.discord.bot.starter.property.AlunaProperties
import net.dv8tion.jda.api.sharding.ShardManager
import org.discordbots.api.client.DiscordBotListAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TopGGBotListSender(
    private val alunaProperties: AlunaProperties,
    private val shardManager: ShardManager
) : BotListSender {

    private var topGGApi: DiscordBotListAPI? = null

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun sendStats(totalServer: Int, totalShards: Int) {
        val topGGToken = alunaProperties.botList.topggToken
        if(topGGToken == null){
            logger.debug("Stats are not sent to top.gg because token is not set")
            return
        }

        logger.debug("Send stats to top.gg")

        if (topGGApi == null) {
            val clientId = alunaProperties.discord.applicationId
            topGGApi = DiscordBotListAPI.Builder()
                .token(topGGToken)
                .botId(clientId)
                .build()
        }

        topGGApi!!.setStats(shardManager.shards.map { it.guilds.size })
    }
}
