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

package io.viascom.discord.bot.aluna.bot.handler


import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.exception.AlunaInteractionRepresentationNotFoundException
import io.viascom.discord.bot.aluna.model.TimeMarkStep.*
import io.viascom.discord.bot.aluna.model.at
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction
import org.slf4j.MDC
import kotlin.time.TimeSource.Monotonic.markNow

public abstract class DiscordMessageContextMenuHandler(name: String, localizations: LocalizationFunction? = null) :
    DiscordContextMenuHandler(Command.Type.MESSAGE, name, localizations) {

    internal abstract suspend fun runExecute(event: MessageContextInteractionEvent)

    /**
     * Runs checks for the [DiscordMessageContextMenuHandler] with the given [MessageContextInteractionEvent] that called it.
     *
     * @param event The MessageContextInteractionEvent that triggered this Command
     */

    @JvmSynthetic
    internal suspend fun run(event: MessageContextInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        if (alunaProperties.debug.useTimeMarks) {
            timeMarks = arrayListOf()
        }
        timeMarks?.add(START at markNow())

        if (!discordBot.discordRepresentations.containsKey(event.commandId)) {
            val exception = AlunaInteractionRepresentationNotFoundException(event.commandId)
            try {
                onExecutionException(event, exception)
            } catch (exceptionError: Exception) {
                discordInteractionMetaDataHandler.onGenericExecutionException(this@DiscordMessageContextMenuHandler, exception, exceptionError, event)
            }
            return@withContext
        }

        discordRepresentation = discordBot.discordRepresentations[event.commandId]!!

        MDC.put("interaction", event.fullCommandName)
        MDC.put("uniqueId", uniqueId)

        val integrationType = if (event.integrationOwners.isGuildIntegration) "GUILD" else "USER"
        MDC.put("discord.integration.type", integrationType)

        guild = event.guild

        isGuildIntegration = event.integrationOwners.isGuildIntegration
        isUserIntegration = event.integrationOwners.isUserIntegration

        isInBotDM = event.context == InteractionContextType.BOT_DM

        if (event.integrationOwners.isGuildIntegration) {
            guild?.let { MDC.put("discord.server", "${it.id} (${it.name})") }
            MDC.put("discord.integration.guild", event.integrationOwners.authorizingGuildId)
        } else {
            guild?.let { MDC.put("discord.server", it.id) }
            MDC.put("discord.integration.user", event.integrationOwners.authorizingUserId)
        }

        if (event.integrationOwners.isUserIntegration) {
            MDC.put("discord.integration.user", event.integrationOwners.authorizingUserId)
        }

        author = event.user
        MDC.put("discord.author", "${author.id} (${author.name})")

        userLocale = event.userLocale
        MDC.put("discord.author_locale", userLocale.locale)

        channel = event.channel
        channel?.let { MDC.put("discord.channel", it.id) }

        if (guild != null) {
            if (event.integrationOwners.isGuildIntegration) {
                member = guild!!.getMember(author)
            }
            guildChannel = event.guildChannel
            guildLocale = event.guildLocale
            MDC.put("discord.server_locale", guildLocale.locale)
        }

        val mdcMap = MDC.getCopyOfContextMap()

        timeMarks?.add(INITIALIZED at markNow())

        val missingUserPermissions = async(AlunaDispatchers.Detached) {
            MDC.setContextMap(mdcMap)
            val missingPermissions = discordInteractionConditions.checkForNeededUserPermissions(this@DiscordMessageContextMenuHandler, userPermissions, event)
            mdcMap.putAll(MDC.getCopyOfContextMap())
            missingPermissions
        }.await()
        if (missingUserPermissions.hasMissingPermissions) {
            onMissingUserPermission(event, missingUserPermissions)
            return@withContext
        }
        timeMarks?.add(NEEDED_USER_PERMISSIONS at markNow())

        val missingBotPermissions = async(AlunaDispatchers.Detached) {
            MDC.setContextMap(mdcMap)
            val missingPermissions = discordInteractionConditions.checkForNeededBotPermissions(this@DiscordMessageContextMenuHandler, botPermissions, event)
            mdcMap.putAll(MDC.getCopyOfContextMap())
            missingPermissions
        }.await()
        if (missingBotPermissions.hasMissingPermissions) {
            onMissingBotPermission(event, missingBotPermissions)
            return@withContext
        }
        timeMarks?.add(NEEDED_BOT_PERMISSIONS at markNow())

        async(AlunaDispatchers.Detached) {
            MDC.setContextMap(mdcMap)
            val requirements = discordInteractionLoadAdditionalData.loadDataBeforeAdditionalRequirements(this@DiscordMessageContextMenuHandler, event)
            mdcMap.putAll(MDC.getCopyOfContextMap())
            requirements
        }.await()
        timeMarks?.add(LOAD_DATA_BEFORE_ADDITIONAL_REQUIREMENTS at markNow())

        val additionalRequirements = async(AlunaDispatchers.Detached) {
            MDC.setContextMap(mdcMap)
            val requirements = discordInteractionAdditionalConditions.checkForAdditionalContextRequirements(this@DiscordMessageContextMenuHandler, event)
            mdcMap.putAll(MDC.getCopyOfContextMap())
            requirements
        }.await()
        if (additionalRequirements.failed) {
            onFailedAdditionalRequirements(event, additionalRequirements)
            return@withContext
        }
        timeMarks?.add(CHECK_FOR_ADDITIONAL_COMMAND_REQUIREMENTS at markNow())

        //Load additional data for this interaction
        async(AlunaDispatchers.Detached) {
            MDC.setContextMap(mdcMap)
            discordInteractionLoadAdditionalData.loadData(this@DiscordMessageContextMenuHandler, event)
            mdcMap.putAll(MDC.getCopyOfContextMap())
        }.await()
        timeMarks?.add(LOAD_ADDITIONAL_DATA at markNow())

        //Run onCommandExecution in asyncExecutor to ensure it is not blocking the execution of the interaction itself
        launch(AlunaDispatchers.Detached) {
            MDC.setContextMap(mdcMap)
            discordInteractionMetaDataHandler.onContextMenuExecution(this@DiscordMessageContextMenuHandler, event)
            mdcMap.putAll(MDC.getCopyOfContextMap())
        }
        launch(AlunaDispatchers.Detached) {
            MDC.setContextMap(mdcMap)
            if (alunaProperties.discord.publishDiscordContextEvent) {
                eventPublisher.publishDiscordMessageContextEvent(author, channel, guild, event.fullCommandName, this@DiscordMessageContextMenuHandler)
            }
            mdcMap.putAll(MDC.getCopyOfContextMap())
        }
        timeMarks?.add(ASYNC_TASKS_STARTED at markNow())

        var endedWithException = false

        try {
            logger.info("Run context menu '${event.fullCommandName}'" + if (alunaProperties.debug.showHashCode) " [${this@DiscordMessageContextMenuHandler.hashCode()}]" else "")
            runExecute(event)
            timeMarks?.add(RUN_EXECUTE at markNow())
        } catch (e: Exception) {
            endedWithException = true
            try {
                async(AlunaDispatchers.Detached) {
                    MDC.setContextMap(mdcMap)
                    onExecutionException(event, e)
                }.await()
                timeMarks?.add(ON_EXECUTION_EXCEPTION at markNow())
            } catch (exceptionError: Exception) {
                async(AlunaDispatchers.Detached) {
                    MDC.setContextMap(mdcMap)
                    discordInteractionMetaDataHandler.onGenericExecutionException(
                        this@DiscordMessageContextMenuHandler,
                        e,
                        exceptionError,
                        event
                    )
                }.await()
                timeMarks?.add(ON_EXECUTION_EXCEPTION at markNow())
            }
        } finally {
            exitCommand(event, endedWithException)
        }
    }
}
