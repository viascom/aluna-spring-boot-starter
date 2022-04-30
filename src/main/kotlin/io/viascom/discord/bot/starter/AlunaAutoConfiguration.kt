package io.viascom.discord.bot.starter

import io.viascom.discord.bot.starter.bot.DiscordBot
import io.viascom.discord.bot.starter.bot.emotes.DefaultSystemEmoteLoader
import io.viascom.discord.bot.starter.bot.emotes.SystemEmoteLoader
import io.viascom.discord.bot.starter.bot.handler.DefaultDiscordCommandConditions
import io.viascom.discord.bot.starter.bot.handler.DefaultDiscordCommandLoadAdditionalData
import io.viascom.discord.bot.starter.bot.handler.DiscordCommandConditions
import io.viascom.discord.bot.starter.bot.handler.DiscordCommandLoadAdditionalData
import io.viascom.discord.bot.starter.bot.listener.*
import io.viascom.discord.bot.starter.bot.shardmanager.DefaultShardManagerBuilder
import io.viascom.discord.bot.starter.property.AlunaProperties
import io.viascom.discord.bot.starter.translation.DefaultMessageService
import io.viascom.discord.bot.starter.translation.MessageService
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.env.Environment

@Configuration
@EnableConfigurationProperties(AlunaProperties::class)
@ComponentScan(basePackages = ["io.viascom.discord.bot.starter.*"])
open class AlunaAutoConfiguration(
    private val discordBot: DiscordBot
) {

    //Has to be AlunaAutoConfiguration::class.java as otherwise it is shown as SpringProxy!
    private val logger: Logger = LoggerFactory.getLogger(AlunaAutoConfiguration::class.java)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ["enable-jda"], prefix = "aluna", matchIfMissing = true)
    open fun defaultShardManagerBuilder(
        shardReadyEvent: ShardReadyEvent,
        slashCommandInteractionEventListener: SlashCommandInteractionEventListener,
        genericAutoCompleteListener: GenericInteractionListener,
        eventWaiter: EventWaiter,
        genericEventPublisher: GenericEventPublisher,
        alunaProperties: AlunaProperties
    ): ShardManager {
        logger.debug("Enable DefaultShardManagerBuilder")
        return discordBot.shardManager ?: DefaultShardManagerBuilder(
            shardReadyEvent,
            slashCommandInteractionEventListener,
            genericAutoCompleteListener,
            eventWaiter,
            genericEventPublisher,
            alunaProperties
        ).build()
    }

    @Bean
    @ConditionalOnMissingBean
    open fun defaultDiscordCommandConditions(
        alunaProperties: AlunaProperties,
        systemEmoteLoader: SystemEmoteLoader
    ): DiscordCommandConditions {
        logger.debug("Enable DefaultDiscordCommandConditions")
        return DefaultDiscordCommandConditions(alunaProperties, systemEmoteLoader)
    }

    @Bean
    @ConditionalOnMissingBean
    open fun discordCommandLoadAdditionalData(): DiscordCommandLoadAdditionalData {
        logger.debug("Enable DefaultDiscordCommandLoadAdditionalData")
        return DefaultDiscordCommandLoadAdditionalData()
    }

    @Bean
    @ConditionalOnMissingBean
    open fun defaultSystemEmoteLoader(): SystemEmoteLoader {
        logger.debug("Enable DefaultSystemEmoteLoader")
        return DefaultSystemEmoteLoader()
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ["enable-translation"], prefix = "aluna", matchIfMissing = false)
    open fun defaultMessageService(
        alunaProperties: AlunaProperties,
        reloadableMessageSource: ReloadableResourceBundleMessageSource,
        messageSource: MessageSource
    ): MessageService {
        logger.debug("Enable DefaultMessageService")
        return DefaultMessageService(messageSource, reloadableMessageSource, alunaProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ["enable-translation"], prefix = "aluna", matchIfMissing = false)
    open fun messageSource(environment: Environment): MessageSource {
        val translationPath = environment.getProperty("aluna.translation-path") ?: "classpath:i18n/messages"
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasename(translationPath)
        messageSource.setDefaultEncoding("UTF-8")
        messageSource.setCacheSeconds(60)
        messageSource.setUseCodeAsDefaultMessage(true)
        messageSource.setFallbackToSystemLocale(false)
        return messageSource
    }
}
