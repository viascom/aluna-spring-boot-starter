package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.handler.*
import io.viascom.discord.bot.aluna.bot.listener.*
import io.viascom.discord.bot.aluna.bot.shardmanager.DefaultShardManagerBuilder
import io.viascom.discord.bot.aluna.bot.shardmanager.ShardManagerBuilder
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnTranslationEnabled
import io.viascom.discord.bot.aluna.property.*
import io.viascom.discord.bot.aluna.translation.DefaultMessageService
import io.viascom.discord.bot.aluna.translation.MessageService
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Duration

@Configuration
@EnableConfigurationProperties(AlunaProperties::class)
@EnableScheduling
@ComponentScan(basePackages = ["io.viascom.discord.bot.aluna.*"])
open class AlunaAutoConfiguration {

    @Autowired(required = false)
    lateinit var discordBot: DiscordBot

    //Has to be AlunaAutoConfiguration::class.java as otherwise it is shown as SpringProxy!
    private val logger: Logger = LoggerFactory.getLogger(AlunaAutoConfiguration::class.java)

    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean(ShardManagerBuilder::class)
    open fun defaultShardManagerBuilder(
        shardReadyEvent: ShardReadyEvent,
        slashCommandInteractionEventListener: SlashCommandInteractionEventListener,
        genericAutoCompleteListener: GenericInteractionListener,
        eventWaiter: EventWaiter,
        genericEventPublisher: GenericEventPublisher,
        alunaProperties: AlunaProperties
    ): ShardManager {
        logger.debug("Enable DefaultShardManagerBuilder")

        discordBot.shardManager = discordBot.shardManager ?: DefaultShardManagerBuilder(
            shardReadyEvent,
            slashCommandInteractionEventListener,
            genericAutoCompleteListener,
            eventWaiter,
            genericEventPublisher,
            alunaProperties
        ).build()

        return discordBot.shardManager!!
    }

    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnBean(ShardManagerBuilder::class)
    open fun customShardManagerBuilder(shardReadyEvent: ShardManagerBuilder): ShardManager {
        logger.debug("Enable custom ShardManagerBuilder: ${shardReadyEvent.javaClass.name}")
        discordBot.shardManager = shardReadyEvent.build()
        return discordBot.shardManager!!
    }

    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun defaultDiscordCommandConditions(): DiscordCommandConditions {
        logger.debug("Enable DefaultDiscordCommandConditions")
        return DefaultDiscordCommandConditions()
    }

    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun defaultDiscordCommandAdditionalConditions(): DiscordCommandAdditionalConditions {
        logger.debug("Enable DefaultDiscordCommandAdditionalConditions")
        return DefaultDiscordCommandAdditionalConditions()
    }

    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun discordCommandLoadAdditionalData(): DiscordCommandLoadAdditionalData {
        logger.debug("Enable DefaultDiscordCommandLoadAdditionalData")
        return DefaultDiscordCommandLoadAdditionalData()
    }

    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun discordCommandMetaDataHandler(): DiscordCommandMetaDataHandler {
        logger.debug("Enable DefaultDiscordCommandMetaDataHandler")
        return DefaultDiscordCommandMetaDataHandler()
    }

    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun ownerIdProvider(alunaProperties: AlunaProperties): OwnerIdProvider {
        logger.debug("Enable DefaultOwnerIdProvider")
        return DefaultOwnerIdProvider(alunaProperties)
    }

    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun moderatorIdProvider(alunaProperties: AlunaProperties): ModeratorIdProvider {
        logger.debug("Enable DefaultModeratorIdProvider")
        return DefaultModeratorIdProvider(alunaProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnTranslationEnabled
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
    @ConditionalOnTranslationEnabled
    open fun messageSource(environment: Environment): MessageSource {
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasename(environment.getProperty("aluna.translation.base-path", String::class.java) ?: "classpath:i18n/messages")
        messageSource.setDefaultEncoding(environment.getProperty("aluna.translation.default-encoding", String::class.java) ?: "UTF-8")
        messageSource.setCacheSeconds(
            (environment.getProperty("aluna.translation.cache-duration", Duration::class.java) ?: Duration.ofSeconds(60)).toSeconds().toInt()
        )
        messageSource.setUseCodeAsDefaultMessage(environment.getProperty("aluna.translation.use-code-as-default-message", Boolean::class.java) ?: true)
        messageSource.setFallbackToSystemLocale(environment.getProperty("aluna.translation.fallback-to-system-locale", Boolean::class.java) ?: false)
        return messageSource
    }

//    @Bean
//    @ConditionalOnMissingBean
//    @ConditionalOnProperty(name = ["enabled-translation"], prefix = "aluna", matchIfMissing = false)
//    open fun alunaLocalizationFunction(messageSource: MessageSource): LocalizationFunction {
//        return AlunaLocalizationFunction(messageSource)
//    }
}
