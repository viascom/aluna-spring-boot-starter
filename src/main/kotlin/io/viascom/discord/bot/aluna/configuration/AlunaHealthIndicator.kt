package io.viascom.discord.bot.aluna.configuration

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.configuration.scope.CommandScope
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.stereotype.Component

@Component
@ConditionalOnWebApplication
@ConditionalOnMissingBean(HealthIndicator::class)
class AlunaHealthIndicator(
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val alunaProperties: AlunaProperties,
    private val configurableListableBeanFactory: ConfigurableListableBeanFactory
) : HealthIndicator {
    override fun health(): Health {
        val status = Health.unknown()

        if (shardManager.shards.any { it.status != JDA.Status.CONNECTED }) {
            status.down()
        } else {
            status.up()
        }

        val commandScope = configurableListableBeanFactory.getRegisteredScope("command") as CommandScope

        shardManager.shards.first().status
        status.withDetail("clientId", alunaProperties.discord.applicationId)
        status.withDetail("commandsTotal", discordBot.commands.size)
        status.withDetail("commands", discordBot.commands.map { it.key })
        status.withDetail("productionMode", alunaProperties.productionMode)
        status.withDetail("commandThreads", discordBot.commandExecutor.activeCount)
        status.withDetail("asyncThreads", discordBot.asyncExecutor.activeCount)
        status.withDetail("currentActiveInstances", commandScope.getInstanceCount())
        status.withDetail("currentActiveInstanceTimeouts", commandScope.getTimeoutCount())
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
