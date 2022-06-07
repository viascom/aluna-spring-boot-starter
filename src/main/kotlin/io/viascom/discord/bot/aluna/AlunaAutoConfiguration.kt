package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.handler.*
import io.viascom.discord.bot.aluna.bot.listener.*
import io.viascom.discord.bot.aluna.bot.shardmanager.DefaultShardManagerBuilder
import io.viascom.discord.bot.aluna.property.*
import io.viascom.discord.bot.aluna.translation.DefaultMessageService
import io.viascom.discord.bot.aluna.translation.MessageService
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.env.Environment

@Configuration
@EnableConfigurationProperties(AlunaProperties::class)
@ComponentScan(basePackages = ["io.viascom.discord.bot.aluna.*"])
open class AlunaAutoConfiguration {

    @Autowired(required = false)
    lateinit var discordBot: DiscordBot

    //Has to be AlunaAutoConfiguration::class.java as otherwise it is shown as SpringProxy!
    private val logger: Logger = LoggerFactory.getLogger(AlunaAutoConfiguration::class.java)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true)
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
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true)
    open fun defaultDiscordCommandConditions(): DiscordCommandConditions {
        logger.debug("Enable DefaultDiscordCommandConditions")
        return DefaultDiscordCommandConditions()
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true)
    open fun discordCommandLoadAdditionalData(): DiscordCommandLoadAdditionalData {
        logger.debug("Enable DefaultDiscordCommandLoadAdditionalData")
        return DefaultDiscordCommandLoadAdditionalData()
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true)
    open fun discordCommandMetaDataHandler(): DiscordCommandMetaDataHandler {
        logger.debug("Enable DefaultDiscordCommandMetaDataHandler")
        return DefaultDiscordCommandMetaDataHandler()
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true)
    open fun ownerIdProvider(alunaProperties: AlunaProperties): OwnerIdProvider {
        logger.debug("Enable DefaultOwnerIdProvider")
        return DefaultOwnerIdProvider(alunaProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true)
    open fun moderatorIdProvider(alunaProperties: AlunaProperties): ModeratorIdProvider {
        logger.debug("Enable DefaultModeratorIdProvider")
        return DefaultModeratorIdProvider(alunaProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("\${aluna.discord.enable-jda:true} && \${aluna.enable-translation:false}")
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
    @ConditionalOnExpression("\${aluna.discord.enable-jda:true} && \${aluna.enable-translation:false}")
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

//    @Bean
//    @ConditionalOnMissingBean
//    @ConditionalOnProperty(name = ["enable-translation"], prefix = "aluna", matchIfMissing = false)
//    open fun alunaLocalizationFunction(messageSource: MessageSource): LocalizationFunction {
//        return AlunaLocalizationFunction(messageSource)
//    }


    @EventListener
    open fun printVersion(event: ApplicationStartedEvent) {
        logger.info("Running with Aluna 0.0.21_5.0.0-alpha.12, JDA 5.0.0-alpha.12")
    }
}
