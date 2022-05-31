package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.command.systemcommand.SystemCommandDataProvider
import io.viascom.discord.bot.aluna.bot.handler.*
import io.viascom.discord.bot.aluna.bot.listener.*
import io.viascom.discord.bot.aluna.bot.shardmanager.DefaultShardManagerBuilder
import io.viascom.discord.bot.aluna.property.*
import io.viascom.discord.bot.aluna.translation.DefaultMessageService
import io.viascom.discord.bot.aluna.translation.MessageService
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    open fun defaultDiscordCommandConditions(): DiscordCommandConditions {
        logger.debug("Enable DefaultDiscordCommandConditions")
        return DefaultDiscordCommandConditions()
    }

    @Bean
    @ConditionalOnMissingBean
    open fun discordCommandLoadAdditionalData(): DiscordCommandLoadAdditionalData {
        logger.debug("Enable DefaultDiscordCommandLoadAdditionalData")
        return DefaultDiscordCommandLoadAdditionalData()
    }

    @Bean
    @ConditionalOnMissingBean
    open fun discordCommandMetaDataHandler(): DiscordCommandMetaDataHandler {
        logger.debug("Enable DefaultDiscordCommandMetaDataHandler")
        return DefaultDiscordCommandMetaDataHandler()
    }

    @Bean
    @ConditionalOnMissingBean
    open fun ownerIdProvider(alunaProperties: AlunaProperties): OwnerIdProvider {
        logger.debug("Enable DefaultOwnerIdProvider")
        return DefaultOwnerIdProvider(alunaProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    open fun moderatorIdProvider(alunaProperties: AlunaProperties): ModeratorIdProvider {
        logger.debug("Enable DefaultModeratorIdProvider")
        return DefaultModeratorIdProvider(alunaProperties)
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

//    @Bean
//    @ConditionalOnMissingBean
//    @ConditionalOnProperty(name = ["enable-translation"], prefix = "aluna", matchIfMissing = false)
//    open fun alunaLocalizationFunction(messageSource: MessageSource): LocalizationFunction {
//        return AlunaLocalizationFunction(messageSource)
//    }


    @EventListener
    open fun printSystemCommandFeatureOverview(event: ApplicationStartedEvent) {
        val systemCommand = event.applicationContext.environment.getProperty("aluna.command.system-command.enable", Boolean::class.java) ?: false
        //Print enabled /system-command features
        if (systemCommand) {
            val allFunctions = event.applicationContext.getBeansOfType(SystemCommandDataProvider::class.java)
            val enabledFunctionsDefinition = event.applicationContext.environment.getProperty("aluna.command.system-command.enabled-functions", ArrayList::class.java)
                    ?: arrayListOf<String>()

            val  enabledFunctions = allFunctions.values.filter { it.id in enabledFunctionsDefinition || enabledFunctionsDefinition.isEmpty() }

            if (enabledFunctions.size == allFunctions.size) {
                logger.debug("Enabled /system-command functions: [" + allFunctions.values.joinToString(", ") { it.id } + "]")
            } else {
                logger.debug("Enabled /system-command functions: [" + enabledFunctions.joinToString(", ") { it.id } + "]")
                logger.debug("Disabled /system-command functions: [" + allFunctions.values.filter { it.id !in enabledFunctionsDefinition }
                    .joinToString(", ") { it.id } + "]")
            }

            val allowedModFunctionsDefinition = event.applicationContext.environment.getProperty("aluna.command.system-command.allowed-for-moderators-functions", ArrayList::class.java)
                ?: arrayListOf<String>()

            val allowedModFunctions = allFunctions.filter { it.value.id in allowedModFunctionsDefinition || allowedModFunctionsDefinition.isEmpty() }.filter { it.value.allowMods }

            logger.debug("Allowed for moderators /system-command functions: [" + allowedModFunctions.values.joinToString(", ") { it.id } + "]")
        }
    }
}
