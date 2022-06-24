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

import datadog.trace.api.Trace
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import org.slf4j.MDC
import org.springframework.util.StopWatch

abstract class DiscordMessageContextMenu(name: String) : DiscordContextMenu(Command.Type.MESSAGE, name) {

    /**
     * The main body method of a [DiscordContextMenu].
     * <br></br>This is the "response" for a successful
     * [#run(DiscordCommand)][DiscordContextMenu.execute].
     *
     * @param event The [MessageContextInteractionEvent] that triggered this Command
     */
    @Trace
    protected abstract fun execute(event: MessageContextInteractionEvent)

    /**
     * Runs checks for the [DiscordMessageContextMenu] with the given [MessageContextInteractionEvent] that called it.
     *
     * @param event The MessageContextInteractionEvent that triggered this Command
     */
    @Trace
    open fun run(event: MessageContextInteractionEvent) {
        if (alunaProperties.debug.useStopwatch) {
            stopWatch = StopWatch()
            stopWatch!!.start()
        }

        MDC.put("command", event.commandPath)
        MDC.put("uniqueId", uniqueId)

        guild = event.guild
        guild?.let { MDC.put("discord.server", "${it.id} (${it.name})") }
        channel = event.channel
        channel?.let { MDC.put("discord.channel", it.id) }
        author = event.user
        MDC.put("author", "${author.id} (${author.name})")


        userLocale = event.userLocale

        if (guild != null) {
            member = guild!!.getMember(author)
            guildChannel = event.guildChannel
            guildLocale = event.guildLocale
        }

        val missingUserPermissions = discordCommandConditions.checkForNeededUserPermissions(this, userPermissions, event)
        if (missingUserPermissions.hasMissingPermissions) {
            onMissingUserPermission(event, missingUserPermissions)
            return
        }

        val missingBotPermissions = discordCommandConditions.checkForNeededBotPermissions(this, botPermissions, event)
        if (missingBotPermissions.hasMissingPermissions) {
            onMissingBotPermission(event, missingBotPermissions)
            return
        }

        val additionalRequirements = discordCommandAdditionalConditions.checkForAdditionalContextRequirements(this, event)
        if (additionalRequirements.failed) {
            onFailedAdditionalRequirements(event, additionalRequirements)
            return
        }

        //Load additional data for this command
        discordCommandLoadAdditionalData.loadData(this, event)

        try {
            //Run onCommandExecution in asyncExecutor to ensure it is not blocking the execution of the command itself
            discordBot.asyncExecutor.execute {
                discordCommandMetaDataHandler.onContextMenuExecution(this, event)
            }
            if (alunaProperties.discord.publishDiscordContextEvent) {
                eventPublisher.publishDiscordMessageContextEvent(author, channel, guild, event.commandPath, this)
            }
            logger.info("Run context menu '${event.commandPath}'" + if (alunaProperties.debug.showHashCode) " [${this.hashCode()}]" else "")
            execute(event)
        } catch (e: Exception) {
            try {
                onExecutionException(event, e)
            } catch (exceptionError: Exception) {
                discordCommandMetaDataHandler.onGenericExecutionException(this, e, exceptionError, event)
            }
        } finally {
            exitCommand(event)
        }
    }
}
