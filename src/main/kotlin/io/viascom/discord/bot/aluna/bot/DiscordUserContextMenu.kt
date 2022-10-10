/*
 * Copyright 2022 Viascom Ltd liab. Co
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

package io.viascom.discord.bot.aluna.bot


import io.viascom.discord.bot.aluna.bot.event.AlunaCoroutinesDispatcher
import io.viascom.discord.bot.aluna.exception.AlunaInteractionRepresentationNotFoundException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction
import org.slf4j.MDC
import org.springframework.util.StopWatch

abstract class DiscordUserContextMenu(name: String, localizations: LocalizationFunction? = null) : DiscordContextMenu(Command.Type.USER, name, localizations) {

    /**
     * The main body method of a [DiscordContextMenu].
     * <br></br>This is the "response" for a successful
     * [#run(DiscordContextMenu)][DiscordContextMenu.execute].
     *
     * @param event The [UserContextInteractionEvent] that triggered this Command
     */

    protected abstract fun execute(event: UserContextInteractionEvent)

    /**
     * Runs checks for the [DiscordUserContextMenu] with the given [UserContextInteractionEvent] that called it.
     *
     * @param event The UserContextInteractionEvent that triggered this Command
     */

    @JvmSynthetic
    internal fun run(event: UserContextInteractionEvent) {
        val command = this
        if (alunaProperties.debug.useStopwatch) {
            stopWatch = StopWatch()
            stopWatch!!.start()
        }

        if (!discordBot.discordRepresentations.containsKey(event.name)) {
            val exception = AlunaInteractionRepresentationNotFoundException(event.name)
            try {
                onExecutionException(event, exception)
            } catch (exceptionError: Exception) {
                discordInteractionMetaDataHandler.onGenericExecutionException(command, exception, exceptionError, event)
            }
            return
        }

        discordRepresentation = discordBot.discordRepresentations[event.name]!!

        MDC.put("interaction", event.commandPath)
        MDC.put("uniqueId", uniqueId)

        guild = event.guild
        guild?.let { MDC.put("discord.server", "${it.id} (${it.name})") }
        channel = event.channel
        channel?.let { MDC.put("discord.channel", it.id) }
        author = event.user
        MDC.put("discord.author", "${author.id} (${author.asTag})")

        userLocale = event.userLocale

        if (guild != null) {
            member = guild!!.getMember(author)
            guildChannel = event.guildChannel
            guildLocale = event.guildLocale
        }

        val missingUserPermissions = discordInteractionConditions.checkForNeededUserPermissions(this, userPermissions, event)
        if (missingUserPermissions.hasMissingPermissions) {
            onMissingUserPermission(event, missingUserPermissions)
            return
        }

        val missingBotPermissions = discordInteractionConditions.checkForNeededBotPermissions(this, botPermissions, event)
        if (missingBotPermissions.hasMissingPermissions) {
            onMissingBotPermission(event, missingBotPermissions)
            return
        }

        discordInteractionLoadAdditionalData.loadDataBeforeAdditionalRequirements(this, event)

        val additionalRequirements = discordInteractionAdditionalConditions.checkForAdditionalContextRequirements(this, event)
        if (additionalRequirements.failed) {
            onFailedAdditionalRequirements(event, additionalRequirements)
            return
        }

        //Load additional data for this interaction
        discordInteractionLoadAdditionalData.loadData(this, event)

        runBlocking(AlunaCoroutinesDispatcher.Default) {
            //Run onCommandExecution in asyncExecutor to ensure it is not blocking the execution of the interaction itself
            async(AlunaCoroutinesDispatcher.IO) { discordInteractionMetaDataHandler.onContextMenuExecution(command, event) }
            async(AlunaCoroutinesDispatcher.IO) {
                if (alunaProperties.discord.publishDiscordContextEvent) {
                    eventPublisher.publishDiscordUserContextEvent(author, channel, guild, event.commandPath, command)
                }
            }
            try {
                logger.info("Run context menu '${event.commandPath}'" + if (alunaProperties.debug.showHashCode) " [${command.hashCode()}]" else "")
                execute(event)
            } catch (e: Exception) {
                try {
                    onExecutionException(event, e)
                } catch (exceptionError: Exception) {
                    discordInteractionMetaDataHandler.onGenericExecutionException(command, e, exceptionError, event)
                }
            } finally {
                exitCommand(event)
            }
        }
    }
}
