/*
 * Copyright 2024 Viascom Ltd liab. Co
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction
import org.slf4j.MDC
import kotlin.time.TimeSource.Monotonic.markNow

abstract class DiscordMessageContextMenuHandler(name: String, localizations: LocalizationFunction? = null) :
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

        if (!discordBot.discordRepresentations.containsKey(event.name)) {
            val exception = AlunaInteractionRepresentationNotFoundException(event.name)
            try {
                onExecutionException(event, exception)
            } catch (exceptionError: Exception) {
                discordInteractionMetaDataHandler.onGenericExecutionException(this@DiscordMessageContextMenuHandler, exception, exceptionError, event)
            }
            return@withContext
        }

        discordRepresentation = discordBot.discordRepresentations[event.name]!!

        MDC.put("interaction", event.fullCommandName)
        MDC.put("uniqueId", uniqueId)

        guild = event.guild
        guild?.let { MDC.put("discord.server", "${it.id} (${it.name})") }
        channel = event.channel
        channel?.let { MDC.put("discord.channel", it.id) }
        author = event.user
        MDC.put("discord.author", "${author.id} (${author.name})")


        userLocale = event.userLocale

        if (guild != null) {
            member = guild!!.getMember(author)
            guildChannel = event.guildChannel
            guildLocale = event.guildLocale
        }
        timeMarks?.add(INITIALIZED at markNow())

        val missingUserPermissions = discordInteractionConditions.checkForNeededUserPermissions(this@DiscordMessageContextMenuHandler, userPermissions, event)
        if (missingUserPermissions.hasMissingPermissions) {
            onMissingUserPermission(event, missingUserPermissions)
            return@withContext
        }
        timeMarks?.add(NEEDED_USER_PERMISSIONS at markNow())

        val missingBotPermissions = discordInteractionConditions.checkForNeededBotPermissions(this@DiscordMessageContextMenuHandler, botPermissions, event)
        if (missingBotPermissions.hasMissingPermissions) {
            onMissingBotPermission(event, missingBotPermissions)
            return@withContext
        }
        timeMarks?.add(NEEDED_BOT_PERMISSIONS at markNow())

        discordInteractionLoadAdditionalData.loadDataBeforeAdditionalRequirements(this@DiscordMessageContextMenuHandler, event)
        timeMarks?.add(LOAD_DATA_BEFORE_ADDITIONAL_REQUIREMENTS at markNow())

        val additionalRequirements = discordInteractionAdditionalConditions.checkForAdditionalContextRequirements(this@DiscordMessageContextMenuHandler, event)
        if (additionalRequirements.failed) {
            onFailedAdditionalRequirements(event, additionalRequirements)
            return@withContext
        }
        timeMarks?.add(CHECK_FOR_ADDITIONAL_COMMAND_REQUIREMENTS at markNow())

        //Load additional data for this interaction
        discordInteractionLoadAdditionalData.loadData(this@DiscordMessageContextMenuHandler, event)
        timeMarks?.add(LOAD_ADDITIONAL_DATA at markNow())

        //Run onCommandExecution in asyncExecutor to ensure it is not blocking the execution of the interaction itself
        launch(AlunaDispatchers.Detached) { discordInteractionMetaDataHandler.onContextMenuExecution(this@DiscordMessageContextMenuHandler, event) }
        launch(AlunaDispatchers.Detached) {
            if (alunaProperties.discord.publishDiscordContextEvent) {
                eventPublisher.publishDiscordMessageContextEvent(author, channel, guild, event.fullCommandName, this@DiscordMessageContextMenuHandler)
            }
        }
        timeMarks?.add(ASYNC_TASKS_STARTED at markNow())

        try {
            logger.info("Run context menu '${event.fullCommandName}'" + if (alunaProperties.debug.showHashCode) " [${this@DiscordMessageContextMenuHandler.hashCode()}]" else "")
            runExecute(event)
            timeMarks?.add(RUN_EXECUTE at markNow())
        } catch (e: Exception) {
            try {
                onExecutionException(event, e)
                timeMarks?.add(ON_EXECUTION_EXCEPTION at markNow())
            } catch (exceptionError: Exception) {
                discordInteractionMetaDataHandler.onGenericExecutionException(this@DiscordMessageContextMenuHandler, e, exceptionError, event)
            }
        } finally {
            exitCommand(event)
        }
    }
}
