package io.viascom.discord.bot.aluna.botlist

import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
open class BotListHandler(
    private val senders: List<BotListSender>,
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties,
) {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 600_000, initialDelay = 600_000) //Send updates every 10 minutes
    open fun sendStats() {
        senders.forEach { sender ->
            try {
                if (alunaProperties.productionMode) {
                    sender.sendStats(shardManager.guilds.size, shardManager.shardsTotal)
                }
            } catch (e: Exception) {
                logger.error("Was not able to send stats to ${sender::class.qualifiedName}:" + e.stackTraceToString())
            }
        }

    }

}
