package io.viascom.discord.bot.aluna.configuration

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.listener.EventWaiter
import io.viascom.discord.bot.aluna.configuration.scope.CommandScope
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.stereotype.Component

@Component
@ConditionalOnWebApplication
@ConditionalOnClass(HealthIndicator::class)
@ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true, havingValue = "true")
class AlunaHealthIndicator(
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val eventWaiter: EventWaiter,
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
        status.withDetail("commands", discordBot.commands.mapValues { it.value.name })
        status.withDetail("contextMenuTotal", discordBot.contextMenus.size)
        status.withDetail("contextMenus", discordBot.contextMenus.mapValues { it.value.name })
        status.withDetail("productionMode", alunaProperties.productionMode)

        val threads = hashMapOf<String, Any>()
        threads["commandThreads"] = discordBot.commandExecutor.activeCount
        threads["asyncThreads"] = discordBot.asyncExecutor.activeCount
        threads["messagesToObserveTimeoutThreads"] = discordBot.messagesToObserveScheduledThreadPool.activeCount
        threads["eventWaiterExecutorThreads"] = eventWaiter.executorThreadPool.activeCount
        threads["eventWaiterExecutorTimeoutThreads"] = eventWaiter.scheduledThreadPool.activeCount

        status.withDetail("threads", threads)
        status.withDetail("currentActiveCommands", commandScope.getInstanceCount())
        status.withDetail("currentActiveCommandTimeouts", commandScope.getTimeoutCount())

        val interactionObserver = hashMapOf<String, Any>()
        interactionObserver["buttons"] =
            discordBot.messagesToObserveButton.size +
                    eventWaiter.waitingEvents.entries.filter { it.key == ButtonInteractionEvent::class.java }.count { it.value.isNotEmpty() }
        interactionObserver["select"] =
            discordBot.messagesToObserveSelect.size +
                    eventWaiter.waitingEvents.entries.filter { it.key == SelectMenuInteractionEvent::class.java }.count { it.value.isNotEmpty() }
        interactionObserver["modal"] =
            discordBot.messagesToObserveModal.size +
                    eventWaiter.waitingEvents.entries.filter { it.key == ModalInteractionEvent::class.java }.count { it.value.isNotEmpty() }

        status.withDetail("interactionObserver", interactionObserver)
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
