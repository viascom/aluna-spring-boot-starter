/*
 * Copyright 2025 Viascom Ltd liab. Co
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.viascom.discord.bot.aluna

import com.fasterxml.jackson.databind.ObjectMapper
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.command.systemcommand.DefaultSystemCommandEmojiProvider
import io.viascom.discord.bot.aluna.bot.command.systemcommand.SystemCommandEmojiProvider
import io.viascom.discord.bot.aluna.bot.handler.*
import io.viascom.discord.bot.aluna.bot.listener.*
import io.viascom.discord.bot.aluna.bot.shardmanager.*
import io.viascom.discord.bot.aluna.configuration.condition.*
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.property.*
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@AutoConfiguration
@EnableConfigurationProperties(AlunaProperties::class)
@EnableScheduling
@ComponentScan(basePackages = ["io.viascom.discord.bot.aluna.*"])
open class AlunaAutoConfiguration {

    @Autowired(required = false)
    lateinit var discordBot: DiscordBot

    //Has to be AlunaAutoConfiguration::class.java as otherwise it is shown as SpringProxy!
    private val logger: Logger = LoggerFactory.getLogger(AlunaAutoConfiguration::class.java)

    /**
     * Creates a default ShardManager instance using the provided dependencies.
     *
     * @param interactionEventListener the InteractionEventListener dependency
     * @param genericAutoCompleteListener the GenericInteractionListener dependency
     * @param interactionComponentEventListener the InteractionComponentEventListener dependency
     * @param shardStartListener the ShardStartListener dependency
     * @param shardReadyEventListener the ShardReadyEventListener dependency
     * @param shardShutdownEventListener the ShardShutdownEventListener dependency
     * @param eventWaiter the EventWaiter dependency
     * @param genericEventPublisher the GenericEventPublisher dependency
     * @param eventPublisher the EventPublisher dependency
     * @param alunaProperties the AlunaProperties dependency
     * @param customizers the list of ShardManagerBuilderCustomizer dependencies
     * @param objectMapper the ObjectMapper dependency
     * @param discordBot the DiscordBot dependency
     * @return the created ShardManager instance
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean(ShardManagerBuilder::class)
    open fun defaultShardManagerBuilder(
        interactionEventListener: InteractionEventListener,
        genericAutoCompleteListener: GenericInteractionListener,
        interactionComponentEventListener: InteractionComponentEventListener,
        shardStartListener: ShardStartListener,
        shardReadyEventListener: ShardReadyEventListener,
        shardShutdownEventListener: ShardShutdownEventListener,
        eventWaiter: EventWaiter,
        genericEventPublisher: GenericEventPublisher,
        eventPublisher: EventPublisher,
        alunaProperties: AlunaProperties,
        customizers: List<ShardManagerBuilderCustomizer>,
        objectMapper: ObjectMapper,
        discordBot: DiscordBot
    ): ShardManager {
        logger.debug("Enable DefaultShardManagerBuilder")

        val shardManagerBuilder = DefaultShardManagerBuilder(
            interactionEventListener,
            genericAutoCompleteListener,
            interactionComponentEventListener,
            shardStartListener,
            shardReadyEventListener,
            shardShutdownEventListener,
            eventWaiter,
            genericEventPublisher,
            eventPublisher,
            alunaProperties,
            customizers,
            objectMapper,
            discordBot
        )

        discordBot.shardManager = shardManagerBuilder.build()
        discordBot.latchCount = shardManagerBuilder.getLatchCount()

        if (alunaProperties.discord.autoLoginOnStartup) {
            AlunaDispatchers.InternalScope.launch {
                discordBot.login()
            }
        } else {
            logger.debug("AutoLoginOnStartup is disabled. Not awaiting for shards to connect.")
        }

        return discordBot.shardManager!!
    }

    /**
     * Builds a custom ShardManager instance using the provided dependencies.
     * This is used if a custom ShardManagerBuilder is provided.
     *
     * @param shardManagerBuilder the ShardManagerBuilder instance
     * @param alunaProperties the AlunaProperties instance
     * @return the created ShardManager instance
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnBean(ShardManagerBuilder::class)
    open fun customShardManagerBuilder(shardManagerBuilder: ShardManagerBuilder, alunaProperties: AlunaProperties): ShardManager {
        logger.debug("Enable custom ShardManagerBuilder: ${shardManagerBuilder.javaClass.name}")
        discordBot.shardManager = shardManagerBuilder.build()
        discordBot.latchCount = shardManagerBuilder.getLatchCount()

        if (alunaProperties.discord.autoLoginOnStartup) {
            AlunaDispatchers.InternalScope.launch {
                discordBot.login()
            }
        } else {
            logger.debug("AutoLoginOnStartup is disabled. Not awaiting for shards to connect.")
        }

        return discordBot.shardManager!!
    }

    /**
     * Returns the default DiscordInteractionConditions.
     *
     * @return the created instance of DefaultDiscordInteractionConditions.
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun defaultDiscordInteractionConditions(): DiscordInteractionConditions {
        logger.debug("Enable DefaultDiscordInteractionConditions")
        return DefaultDiscordInteractionConditions()
    }

    /**
     * Returns the default implementation of the DiscordInteractionAdditionalConditions interface.
     *
     * @return the default DiscordInteractionAdditionalConditions implementation
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun defaultDiscordInteractionAdditionalConditions(): DiscordInteractionAdditionalConditions {
        logger.debug("Enable DefaultDiscordInteractionAdditionalConditions")
        return DefaultDiscordInteractionAdditionalConditions()
    }

    /**
     * Returns the default implementation of the BotShutdownHook.
     *
     * @param shardManager the ShardManager dependency
     * @param alunaProperties the AlunaProperties dependency
     * @param discordBot the DiscordBot dependency
     * @return the created BotShutdownHook instance
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    @ConditionalOnAlunaShutdownHook
    open fun defaultBotShutdownHook(shardManager: ShardManager, alunaProperties: AlunaProperties, discordBot: DiscordBot): BotShutdownHook {
        logger.debug("Enable DefaultBotShutdownHook")
        return DefaultBotShutdownHook(shardManager, alunaProperties)
    }

    /**
     *  Returns the default implementation of the DiscordInteractionLoadAdditionalData instance.
     *
     * @return the created instance of DiscordInteractionLoadAdditionalData
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun discordInteractionLoadAdditionalData(): DiscordInteractionLoadAdditionalData {
        logger.debug("Enable DefaultDiscordInteractionLoadAdditionalData")
        return DefaultDiscordInteractionLoadAdditionalData()
    }

    /**
     *  Returns the default implementation of the DiscordInteractionMetaDataHandler instance.
     *
     * @return the created instance of DiscordInteractionMetaDataHandler
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun discordInteractionMetaDataHandler(): DiscordInteractionMetaDataHandler {
        logger.debug("Enable DefaultDiscordInteractionMetaDataHandler")
        return DefaultDiscordInteractionMetaDataHandler()
    }

    /**
     *  Returns the default implementation of the OwnerIdProvider instance.
     *
     * @return the created instance of OwnerIdProvider
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun ownerIdProvider(alunaProperties: AlunaProperties): OwnerIdProvider {
        logger.debug("Enable DefaultOwnerIdProvider")
        return DefaultOwnerIdProvider(alunaProperties)
    }

    /**
     *  Returns the default implementation of the ModeratorIdProvider instance.
     *
     * @return the created instance of ModeratorIdProvider
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun moderatorIdProvider(alunaProperties: AlunaProperties): ModeratorIdProvider {
        logger.debug("Enable DefaultModeratorIdProvider")
        return DefaultModeratorIdProvider(alunaProperties)
    }

    /**
     *  Returns the default implementation of the SystemCommandEmojiProvider instance.
     *
     * @return the created instance of SystemCommandEmojiProvider
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnSystemCommandEnabled
    @ConditionalOnMissingBean
    open fun systemCommandEmojiProvider(): SystemCommandEmojiProvider {
        logger.debug("Enable DefaultSystemCommandEmojiProvider")
        return DefaultSystemCommandEmojiProvider()
    }

    /**
     *  Returns the default implementation of the DiscordInteractionLocalization instance.
     *
     * @return the created instance of DiscordInteractionLocalization
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnTranslationEnabled
    @ConditionalOnMissingBean
    open fun defaultLocalizationProvider(alunaProperties: AlunaProperties): DiscordInteractionLocalization {
        logger.debug("Enable DefaultDiscordInteractionLocalization")
        return DefaultDiscordInteractionLocalization(alunaProperties)
    }

    /**
     * Returns the default implementation of the DefaultInteractionInitializerCondition instance.
     *
     * @return the created instance of DefaultInteractionInitializerCondition
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnMissingBean
    open fun defaultInteractionInitializerCondition(): InteractionInitializerCondition {
        logger.debug("Enable DefaultInteractionInitializerCondition")
        return DefaultInteractionInitializerCondition()
    }

    /**
     * Returns the default implementation of the FastMutualGuildsCache interface.
     *
     * @return the created instance of DefaultFastMutualGuildsCache
     */
    @Bean
    @ConditionalOnJdaEnabled
    @ConditionalOnFastMutualGuildCacheEnabled
    @ConditionalOnMissingBean(FastMutualGuildsCache::class)
    open fun defaultFastMutualGuildsCache(shardManager: ShardManager, alunaProperties: AlunaProperties): FastMutualGuildsCache {
        logger.debug("Enable DefaultFastMutualGuildsCache")
        return DefaultFastMutualGuildsCache(shardManager, alunaProperties)
    }

}
