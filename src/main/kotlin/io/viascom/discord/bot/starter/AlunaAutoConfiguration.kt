package io.viascom.discord.bot.starter

import io.viascom.discord.bot.starter.bot.DiscordBot
import io.viascom.discord.bot.starter.bot.emotes.DefaultSystemEmoteLoader
import io.viascom.discord.bot.starter.bot.emotes.SystemEmoteLoader
import io.viascom.discord.bot.starter.bot.handler.DefaultDiscordCommandConditions
import io.viascom.discord.bot.starter.bot.handler.DiscordCommandConditions
import io.viascom.discord.bot.starter.bot.listener.EventWaiter
import io.viascom.discord.bot.starter.bot.listener.GenericAutoCompleteListener
import io.viascom.discord.bot.starter.bot.listener.ShardReadyEvent
import io.viascom.discord.bot.starter.bot.listener.SlashCommandInteractionEventListener
import io.viascom.discord.bot.starter.bot.shardmanager.DefaultShardManagerBuilder
import io.viascom.discord.bot.starter.property.AlunaProperties
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AlunaProperties::class)
@ComponentScan(basePackages = ["io.viascom.discord.bot.starter.*"])
open class AlunaAutoConfiguration(
    private val discordBot: DiscordBot
) {

    @Bean
    @ConditionalOnMissingBean
    open fun defaultShardManagerBuilder(
        shardReadyEvent: ShardReadyEvent,
        slashCommandInteractionEventListener: SlashCommandInteractionEventListener,
        genericAutoCompleteListener: GenericAutoCompleteListener,
        eventWaiter: EventWaiter,
        alunaProperties: AlunaProperties
    ): ShardManager {
        return discordBot.shardManager ?: DefaultShardManagerBuilder(
            shardReadyEvent,
            slashCommandInteractionEventListener,
            genericAutoCompleteListener,
            eventWaiter,
            alunaProperties
        ).build()
    }

    @Bean
    @ConditionalOnMissingBean
    open fun defaultDiscordCommandConditions(
        alunaProperties: AlunaProperties,
        systemEmoteLoader: SystemEmoteLoader
    ): DiscordCommandConditions {
        return DefaultDiscordCommandConditions(alunaProperties, systemEmoteLoader)
    }

    @Bean
    @ConditionalOnMissingBean
    open fun defaultSystemEmoteLoader(): SystemEmoteLoader {
        return DefaultSystemEmoteLoader()
    }
}
