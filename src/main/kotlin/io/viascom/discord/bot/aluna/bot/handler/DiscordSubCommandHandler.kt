/*
 * Copyright 2023 Viascom Ltd liab. Co
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
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.DiscordSubCommandElement
import io.viascom.discord.bot.aluna.bot.InteractionScopedObject
import io.viascom.discord.bot.aluna.model.DevelopmentStatus
import io.viascom.discord.bot.aluna.model.UseScope
import io.viascom.discord.bot.aluna.util.TimestampFormat
import io.viascom.discord.bot.aluna.util.toDiscordTimestamp
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

abstract class DiscordSubCommandHandler(
    name: String,
    description: String
) : SubcommandData(name, description), InteractionScopedObject, DiscordSubCommandElement {

    @Autowired
    lateinit var discordBot: DiscordBot

    /**
     * This gets set by the CommandContext automatically and should not be changed
     */
    override lateinit var uniqueId: String

    override var beanTimoutDelay: Duration = Duration.ofMinutes(14)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = true
    override var beanResetObserverTimeoutOnBeanExtend: Boolean = true
    override var beanCallOnDestroy: Boolean = true
    override var freshInstance: Boolean = true

    /**
     * The [CooldownScope][CooldownScope] of the command.
     */
    var cooldownScope = CooldownScope.NO_COOLDOWN

    var cooldown: Duration = Duration.ZERO

    var useScope = UseScope.GLOBAL

    @set:JvmSynthetic
    lateinit var parentCommand: DiscordCommandHandler
        internal set

    /**
     * Discord representation of this interaction
     */
    @set:JvmSynthetic
    lateinit var discordRepresentation: Command.Subcommand
        internal set

    /**
     * Interaction development status
     */
    var interactionDevelopmentStatus = DevelopmentStatus.LIVE

    @JvmSynthetic
    internal suspend fun initialize(currentSubFullCommandName: String, parentCommand: DiscordCommandHandler, parentDiscordRepresentation: Command) =
        withContext(AlunaDispatchers.Internal) {
            this@DiscordSubCommandHandler.parentCommand = parentCommand

            val elements = currentSubFullCommandName.split(" ")
            discordRepresentation = when (elements.size) {
                2 -> parentDiscordRepresentation.subcommands.firstOrNull { it.name == elements[1] }
                3 -> parentDiscordRepresentation.subcommandGroups.firstOrNull { it.name == elements[1] }?.subcommands?.firstOrNull { it.name == elements[2] }
                else -> null
            } ?: throw IllegalArgumentException("Could not find Discord Representation of this command based on: $currentSubFullCommandName")
        }

    @JvmSynthetic
    internal suspend fun run(event: SlashCommandInteractionEvent) = withContext(AlunaDispatchers.Interaction) {
        //Check use scope of this command
        if (!event.isFromGuild && useScope == UseScope.GUILD_ONLY) {
            onWrongUseScope(event)
            return@withContext
        }

        //Check for cooldown
        val cooldownKey = discordBot.getCooldownKey(cooldownScope, discordRepresentation.id, parentCommand.author.id, parentCommand.channel.id, parentCommand.guild?.id)
        if (cooldownScope != CooldownScope.NO_COOLDOWN) {
            if (discordBot.isCooldownActive(cooldownKey, cooldown)) {
                onCooldownStillActive(event, discordBot.cooldowns[cooldownKey]!!)
                return@withContext
            }
        }
        discordBot.cooldowns[cooldownKey] = LocalDateTime.now(ZoneOffset.UTC)

        runExecute(event)
    }

    @JvmSynthetic
    internal abstract suspend fun runExecute(event: SlashCommandInteractionEvent)

    @JvmSynthetic
    internal abstract suspend fun runInitCommandOptions()

    @JvmSynthetic
    internal abstract suspend fun runOnDestroy()

    @JvmSynthetic
    internal abstract suspend fun runOnButtonInteraction(event: ButtonInteractionEvent): Boolean

    @JvmSynthetic
    internal abstract suspend fun runOnButtonInteractionTimeout()

    @JvmSynthetic
    internal abstract suspend fun runOnStringSelectInteraction(event: StringSelectInteractionEvent): Boolean

    @JvmSynthetic
    internal abstract suspend fun runOnStringSelectInteractionTimeout()

    @JvmSynthetic
    internal abstract suspend fun runOnEntitySelectInteraction(event: EntitySelectInteractionEvent): Boolean

    @JvmSynthetic
    internal abstract suspend fun runOnEntitySelectInteractionTimeout()

    @JvmSynthetic
    internal abstract suspend fun runOnModalInteraction(event: ModalInteractionEvent): Boolean

    @JvmSynthetic
    internal abstract suspend fun runOnModalInteractionTimeout()

    @JvmSynthetic
    internal abstract suspend fun runOnAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent)

    open fun onCooldownStillActive(
        event: SlashCommandInteractionEvent,
        lastUse: LocalDateTime
    ) {
        event.deferReply(true)
            .setContent("⛔ This interaction is still on cooldown and will be usable ${lastUse.plusNanos(cooldown.toNanos()).toDiscordTimestamp(TimestampFormat.RELATIVE_TIME)}.")
            .queue()
    }

    open fun onWrongUseScope(event: SlashCommandInteractionEvent) {
        event.deferReply(true).setContent("⛔ This command can only be used on a server directly.").queue()
    }

    fun updateMessageIdForScope(messageId: String) {
        parentCommand.updateMessageIdForScope(messageId)
    }

    /**
     * Destroy this bean instance. This will remove the bean from the interaction scope as well as remove the bean timout.
     *
     * @param removeObservers Remove all observers
     * @param removeObserverTimeouts Remove all observer timeouts
     * @param callOnDestroy Call onDestroy of this bean
     * @param callButtonTimeout Call onButtonInteractionTimeout of this bean
     * @param callStringSelectTimeout Call onStringSelectInteractionTimeout of this bean
     * @param callEntitySelectTimeout Call onEntitySelectInteractionTimeout of this bean
     * @param callModalTimeout Call onModalInteractionTimeout of this bean
     */
    suspend fun destroyThisInstance(
        removeObservers: Boolean = true,
        removeObserverTimeouts: Boolean = true,
        callOnDestroy: Boolean = false,
        callButtonTimeout: Boolean = false,
        callStringSelectTimeout: Boolean = false,
        callEntitySelectTimeout: Boolean = false,
        callModalTimeout: Boolean = false
    ) = withContext(AlunaDispatchers.Interaction) {
        parentCommand.destroyThisInstance(
            removeObservers,
            removeObserverTimeouts,
            callOnDestroy,
            callButtonTimeout,
            callStringSelectTimeout,
            callEntitySelectTimeout,
            callModalTimeout
        )
    }
}