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
import io.viascom.discord.bot.aluna.bot.handler.*
import io.viascom.discord.bot.aluna.event.EventPublisher
import io.viascom.discord.bot.aluna.model.AdditionalRequirements
import io.viascom.discord.bot.aluna.model.DevelopmentStatus
import io.viascom.discord.bot.aluna.model.MissingPermissions
import io.viascom.discord.bot.aluna.model.UseScope
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StopWatch
import java.time.Duration
import java.util.*

abstract class DiscordContextMenu(
    type: Command.Type,
    name: String,

    /**
     * Define a [LocalizationFunction] for this command. If set no null, Aluna will take the implementation of [DiscordInteractionLocalization].
     */
    var localizations: LocalizationFunction? = null
) : CommandDataImpl(type, name), InteractionScopedObject, DiscordInteractionHandler {

    @Autowired
    lateinit var alunaProperties: AlunaProperties

    @Autowired
    lateinit var discordInteractionConditions: DiscordInteractionConditions

    @Autowired
    lateinit var discordInteractionAdditionalConditions: DiscordInteractionAdditionalConditions

    @Autowired
    lateinit var discordInteractionLoadAdditionalData: DiscordInteractionLoadAdditionalData

    @Autowired
    lateinit var discordInteractionMetaDataHandler: DiscordInteractionMetaDataHandler

    @Autowired
    lateinit var eventPublisher: EventPublisher

    @Autowired
    override lateinit var discordBot: DiscordBot

    @Autowired(required = false)
    lateinit var localizationProvider: DiscordInteractionLocalization

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * This gets set by the CommandContext automatically and should not be changed
     */
    override lateinit var uniqueId: String

    /**
     * If true, this command is only seen by users with the administrator permission on the server by default!
     * Aluna will set `this.defaultPermissions = DefaultMemberPermissions.DISABLED` if true.
     */
    var isAdministratorOnlyCommand = false

    var interactionDevelopmentStatus = DevelopmentStatus.LIVE

    /**
     * Defines the use scope of this command.
     *
     * *This gets mapped to [isGuildOnly] if set to [UseScope.GUILD_ONLY].*
     */
    var useScope = UseScope.GLOBAL

    override var beanTimoutDelay: Duration = Duration.ofMinutes(14)
    override var beanUseAutoCompleteBean: Boolean = false
    override var beanRemoveObserverOnDestroy: Boolean = true
    override var beanCallOnDestroy: Boolean = true

    /**
     * Any [Permission]s a Member must have to use this command.
     * <br></br>These are only checked in a [Guild] environment.
     */
    var userPermissions = arrayListOf<Permission>()

    /**
     * Any [Permission]s the bot must have to use a command.
     * <br></br>These are only checked in a [Guild] environment.
     */
    var botPermissions = arrayListOf<Permission>()

    /**
     * [Channel] in which the command was used in.
     */
    var channel: Channel? = null

    /**
     * [Author][User] of the command
     */
    override lateinit var author: User

    /**
     * [Guild] in which the command was used in. Can be null if the command was used in direct messages.
     */
    var guild: Guild? = null

    /**
     * [GuildChannel] in which the command was used in. Can be null if the command was used in direct messages.
     */
    var guildChannel: GuildChannel? = null

    /**
     * [Member] which used the command. Can be null if the command was used in direct messages.
     */
    var member: Member? = null

    /**
     * User [Locale]
     *
     * *This is set by Aluna based on the information provided by Discord*
     */
    var userLocale: Locale = Locale.ENGLISH

    /**
     * Guild [Locale]
     *
     * *This is set by Aluna based on the information provided by Discord*
     */
    var guildLocale: Locale = Locale.ENGLISH

    /**
     * Stop watch used if enabled by properties
     */
    var stopWatch: StopWatch? = null

    /**
     * This method gets triggered, as soon as a button event for this interaction is called.
     * Make sure that you register your message id: `discordBot.registerMessageForButtonEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [ButtonInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    @Trace
    override fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }

    /**
     * This method gets triggered, as soon as a button event observer duration timeout is reached.
     */
    @Trace
    override fun onButtonInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    /**
     * This method gets triggered, as soon as a select event for this interaction is called.
     * Make sure that you register your message id: `discordBot.registerMessageForSelectEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [SelectMenuInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    @Trace
    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }

    /**
     * This method gets triggered, as soon as a select event observer duration timeout is reached.
     */
    @Trace
    override fun onSelectMenuInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    /**
     * This method gets triggered, as soon as a modal event for this interaction is called.
     * Make sure that you register your message id: `discordBot.registerMessageForModalEvents(it, this)` or `.queueAndRegisterInteraction()`
     *
     * @param event [ModalInteractionEvent] this method is based on
     * @return Returns true if you acknowledge the event. If false is returned, the aluna will wait for the next event.
     */
    @Trace
    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        return true
    }

    /**
     * This method gets triggered, as soon as a modal event observer duration timeout is reached.
     */
    @Trace
    override fun onModalInteractionTimeout(additionalData: HashMap<String, Any?>) {
    }

    /**
     * On destroy gets called, when the object gets destroyed after the defined beanTimoutDelay.
     */
    @Trace
    open fun onDestroy() {
    }

    fun prepareInteraction() {
        if (isAdministratorOnlyCommand) {
            this.defaultPermissions = DefaultMemberPermissions.DISABLED
        }
        this.isGuildOnly = (useScope == UseScope.GUILD_ONLY)

        if (!alunaProperties.productionMode) {
            if ((isAdministratorOnlyCommand || this.defaultPermissions == DefaultMemberPermissions.DISABLED) && !this.isGuildOnly) {
                logger.warn("The interaction '$name' has a default permission for administrator only but is not restricted to guild only. All users will be able to use this interaction in DMs with your bot!")
            }
            if (this.defaultPermissions != DefaultMemberPermissions.ENABLED && this.defaultPermissions != DefaultMemberPermissions.DISABLED && !this.isGuildOnly) {
                logger.warn("The interaction '$name' has a default permission restriction for a specific user permission but is not restricted to guild only. All users will be able to use this interaction in DMs with your bot!")
            }
        }
    }

    fun prepareLocalization() {
        if (alunaProperties.translation.enabled) {
            if (localizations == null) {
                localizations = localizationProvider.getLocalizationFunction()
            }

            this.setLocalizationFunction(localizations!!)
        }
    }

    open fun onMissingUserPermission(event: GenericCommandInteractionEvent, missingPermissions: MissingPermissions) {
        val textChannelPermissions = missingPermissions.textChannel.joinToString("\n") { "└ ${it.getName()}" }
        val voiceChannelPermissions = missingPermissions.voiceChannel.joinToString("\n") { "└ ${it.getName()}" }
        val guildPermissions = missingPermissions.guild.joinToString("\n") { "└ ${it.getName()}" }
        event.deferReply(true).setContent(
            "⛔ You are missing the following permission to execute this interaction:\n" +
                    (if (textChannelPermissions.isNotBlank()) textChannelPermissions + "\n" else "") +
                    (if (voiceChannelPermissions.isNotBlank()) voiceChannelPermissions + "\n" else "") +
                    (if (guildPermissions.isNotBlank()) guildPermissions + "\n" else "")
        ).queue()
    }

    open fun onMissingBotPermission(event: GenericCommandInteractionEvent, missingPermissions: MissingPermissions) {
        when {
            missingPermissions.notInVoice -> {
                event.deferReply(true)
                    .setContent("⛔ You need to be in a voice channel yourself to execute this interaction").queue()

            }
            (missingPermissions.hasMissingPermissions) -> {
                event.deferReply(true).setContent("⛔ I'm missing the following permission to execute this interaction:\n" +
                        missingPermissions.textChannel.joinToString("\n") { "└ ${it.getName()}" } + "\n" +
                        missingPermissions.voiceChannel.joinToString("\n") { "└ ${it.getName()}" } + "\n" +
                        missingPermissions.guild.joinToString("\n") { "└ ${it.getName()}" }
                ).queue()
            }
        }
    }

    open fun onFailedAdditionalRequirements(event: GenericCommandInteractionEvent, additionalRequirements: AdditionalRequirements) {
        event.deferReply(true).setContent("⛔ Additional requirements for this interaction failed.").queue()
    }

    open fun onExecutionException(event: GenericCommandInteractionEvent, exception: Exception) {
        throw exception
    }

    fun exitCommand(event: GenericCommandInteractionEvent) {
        if (alunaProperties.debug.useStopwatch && stopWatch != null) {
            stopWatch!!.stop()
            MDC.put("duration", stopWatch!!.totalTimeMillis.toString())
            logger.info("Context menu '${event.commandPath}' (${this.author.id})${if (alunaProperties.debug.showHashCode) " [${this.hashCode()}]" else ""} took ${stopWatch!!.totalTimeMillis}ms")
            when {
                (stopWatch!!.totalTimeMillis > 3000) -> logger.warn("The execution of the context menu ${event.commandPath} until it got completed took longer than 3 second. Make sure you acknowledge the event as fast as possible. If it got acknowledge at the end of the method, the interaction token was no longer valid.")
                (stopWatch!!.totalTimeMillis > 1500) -> logger.warn("The execution of the context menu ${event.commandPath} until it got completed took longer than 1.5 second. Make sure that you acknowledge the event as fast as possible. Because the initial interaction token is only 3 seconds valid.")
            }
            discordBot.interactionExecutor.execute {
                discordInteractionMetaDataHandler.onExitInteraction(this, stopWatch, event)
            }
        }
    }
}
