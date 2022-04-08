package io.viascom.discord.bot.starter.configuration

import io.viascom.discord.bot.starter.bot.DiscordBot
import io.viascom.discord.bot.starter.property.AlunaProperties
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class AlunaHealthIndicator(
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val alunaProperties: AlunaProperties
) : HealthIndicator {
    override fun health(): Health {
        val status = Health.unknown()

        if (shardManager.shards.any { it.status != JDA.Status.CONNECTED }) {
            status.down()
        } else {
            status.up()
        }

        shardManager.shards.first().status
        status.withDetail("clientId", alunaProperties.discord.applicationId)
        status.withDetail("commandsTotal", discordBot.commands.size)
        status.withDetail("commands", discordBot.commands.map { it.key })
        status.withDetail("productionMode", alunaProperties.productionMode)
        status.withDetail("commandThreads", discordBot.commandExecutor.activeCount)
        status.withDetail("asyncThreads", discordBot.asyncExecutor.activeCount)
        status.withDetail("shardsTotal", shardManager.shardsTotal)

        val shards = arrayListOf<ShardDetail>()

        shardManager.shards.forEachIndexed { index, jda ->
            shards.add(ShardDetail(index, jda.status, jda.guilds.size))
        }

        status.withDetail("shards", shards)

        return status.build()
    }

    class ShardDetail(
        val id: Int,
        val status: JDA.Status,
        val serverCount: Int
    )
}
