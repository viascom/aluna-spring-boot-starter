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

package io.viascom.discord.bot.aluna.bot.command.systemcommand

import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.model.EventRegisterType
import io.viascom.discord.bot.aluna.util.getTypedOption
import io.viascom.discord.bot.aluna.util.modalTextField
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.modals.Modal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.logging.LoggersEndpoint
import org.springframework.boot.actuate.logging.LoggersEndpoint.LoggerLevelsDescriptor
import org.springframework.boot.logging.LogLevel
import org.springframework.stereotype.Component
import java.awt.Color

/**
 * System command provider for managing log levels.
 *
 * This class is designed to work with Spring Boot 3.x only.
 * It uses the LoggersEndpoint API to manage logger configurations.
 *
 * The class provides a Discord command interface for:
 * - Viewing logger configurations
 * - Setting log levels
 * - Removing logger configurations
 *
 * It directly interacts with Spring Boot's LoggersEndpoint to retrieve and modify
 * logger configurations with minimal use of reflection.
 */
@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
public class LogLevelProvider(
    private val loggersEndpoint: LoggersEndpoint,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : SystemCommandDataProvider(
    "log_level",
    "Manage log levels",
    true,
    false,
    true,
    false
) {
    private var currentLogger: String? = null
    private var currentLevel: String? = null
    private lateinit var systemCommand: SystemCommand
    private var isAddOperation: Boolean = false

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * Gets all logger names from the LoggersEndpoint.
     *
     * @return Set of logger names
     */
    @Suppress("UNCHECKED_CAST")
    private fun LoggersEndpoint.safeLoggers(): Set<String> {
        val loggersDescriptor = this.loggers()
        return loggersDescriptor.loggers.keys
    }

    /**
     * Gets the configured level for a logger.
     *
     * @param obj The LoggerLevelsDescriptor object
     * @return The configured level as a String, or null if not configured
     */
    private fun getConfiguredLevel(obj: Any?): String? {
        return (obj as? LoggerLevelsDescriptor)?.configuredLevel
    }

    /**
     * Gets the effective level for a logger.
     *
     * Uses minimal reflection to access the getEffectiveLevel method
     * on the LoggerLevelsDescriptor class.
     *
     * @param obj The LoggerLevelsDescriptor object
     * @return The effective level as a String, or null if not available
     */
    private fun getEffectiveLevel(obj: Any?): String? {
        if (obj !is LoggerLevelsDescriptor) {
            return null
        }

        return obj.javaClass.methods
            .firstOrNull { it.name == "getEffectiveLevel" }
            ?.invoke(obj) as? String
    }

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        val args = event.getTypedOption(command.argsOption, "")
        systemCommand = command

        if (args.isNullOrEmpty()) {
            // Show UI to add a new logger configuration
            isAddOperation = true
            showAddLoggerUI(event)
        } else {
            // Show management UI for the specified logger
            isAddOperation = false
            currentLogger = args
            displayLoggerManagementUI(event, args)
        }
    }

    private fun showAddLoggerUI(event: SlashCommandInteractionEvent) {
        val textInput = modalTextField("logger-name", "Logger Name", TextInputStyle.SHORT, "e.g., io.viascom.discord.bot.aluna")
        val modal = Modal.create("add-logger-modal", "Add Logger Configuration")
            .addComponents(textInput)
            .build()

        event.replyModal(modal).queueAndRegisterInteraction(systemCommand, arrayListOf(EventRegisterType.MODAL))
    }

    private fun displayLoggerManagementUI(event: Any, loggerName: String, showSuccessMessage: Boolean = false) {
        val loggerInfo = loggersEndpoint.loggerLevels(loggerName)

        if (loggerInfo == null) {
            when (event) {
                is SlashCommandInteractionEvent -> event.reply("Logger not found: $loggerName").setEphemeral(true).queue()
                is ModalInteractionEvent -> event.reply("Logger not found: $loggerName").setEphemeral(true).queue()
                is StringSelectInteractionEvent -> event.reply("Logger not found: $loggerName").setEphemeral(true).queue()
                else -> logger.error("Unsupported event type: ${event.javaClass.name}")
            }
            return
        }

        val configuredLevel = getConfiguredLevel(loggerInfo) ?: "Not configured"
        val effectiveLevel = getEffectiveLevel(loggerInfo) ?: "Not configured"
        currentLevel = configuredLevel

        val embedBuilder = EmbedBuilder()
            .setTitle("Logger Configuration: $loggerName")
            .setColor(if (showSuccessMessage) Color.GREEN else Color.BLUE)

        // Add success message if needed
        if (showSuccessMessage) {
            embedBuilder.setDescription(
                if (isAddOperation) {
                    "${systemCommandEmojiProvider.tickEmoji().formatted} Logger configuration added successfully!"
                } else {
                    "${systemCommandEmojiProvider.tickEmoji().formatted} Log level updated successfully!"
                }
            )
        }

        embedBuilder.addField("Configured Level", configuredLevel ?: "Not configured", true)
            .addField("Effective Level", effectiveLevel, true)
        
        val embed = embedBuilder.build()

        val logLevels = listOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF")
        val defaultLevel = configuredLevel ?: effectiveLevel
        val selectOptions = logLevels.map { level ->
            SelectOption.of(level, level)
                .withDefault(level == defaultLevel)
                .withEmoji(getLevelEmoji(level))
        }

        val selectMenu = StringSelectMenu.create("log-level-select")
            .setPlaceholder("Select Log Level")
            .addOptions(selectOptions)
            .build()

        val removeButton = Button.danger("remove-logger", "Remove Configuration")
            .withEmoji(Emoji.fromUnicode("🗑️"))

        val actionRow = ActionRow.of(selectMenu)
        val buttonRow = ActionRow.of(removeButton)

        when (event) {
            is SlashCommandInteractionEvent -> {
                event.replyEmbeds(embed)
                    .addComponents(actionRow, buttonRow)
                    .setEphemeral(true)
                    .queueAndRegisterInteraction(systemCommand, arrayListOf(EventRegisterType.STRING_SELECT, EventRegisterType.BUTTON))
            }

            is ModalInteractionEvent -> {
                event.replyEmbeds(embed)
                    .addComponents(actionRow, buttonRow)
                    .setEphemeral(true)
                    .queueAndRegisterInteraction(systemCommand, arrayListOf(EventRegisterType.STRING_SELECT, EventRegisterType.BUTTON))
            }

            is StringSelectInteractionEvent -> {
                event.editMessageEmbeds(embed)
                    .setComponents(actionRow, buttonRow)
                    .queue()
            }

            else -> logger.error("Unsupported event type: ${event.javaClass.name}")
        }
    }

    private fun getLevelEmoji(level: String): Emoji {
        return when (level) {
            "TRACE" -> Emoji.fromUnicode("🔍")
            "DEBUG" -> Emoji.fromUnicode("🐛")
            "INFO" -> Emoji.fromUnicode("ℹ️")
            "WARN" -> Emoji.fromUnicode("⚠️")
            "ERROR" -> Emoji.fromUnicode("❌")
            "OFF" -> Emoji.fromUnicode("🔇")
            else -> Emoji.fromUnicode("❓")
        }
    }

    override fun onStringSelectMenuInteraction(event: StringSelectInteractionEvent): Boolean {
        if (event.componentId == "log-level-select" && currentLogger != null) {
            val selectedLevel = event.values.first()

            // Update the log level
            loggersEndpoint.configureLogLevel(currentLogger!!, LogLevel.valueOf(selectedLevel))
            currentLevel = selectedLevel

            // Log the action with appropriate message based on operation type
            if (isAddOperation) {
                logger.info("Log level for '$currentLogger' set to '$selectedLevel' (new configuration) by ${event.user.name} (${event.user.id})")
            } else {
                logger.info("Log level for '$currentLogger' changed to '$selectedLevel' by ${event.user.name} (${event.user.id})")
            }

            // Update the UI using the centralized displayLoggerManagementUI method
            displayLoggerManagementUI(event, currentLogger!!, true)
            return false
        }
        return true
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        if (event.componentId == "remove-logger" && currentLogger != null) {
            // Remove the logger configuration by setting it to null
            loggersEndpoint.configureLogLevel(currentLogger!!, null)

            // Log the action with appropriate message based on operation type
            if (isAddOperation) {
                logger.info("Logger configuration for '$currentLogger' was removed during creation by ${event.user.name} (${event.user.id})")
            } else {
                logger.info("Logger configuration for '$currentLogger' was removed by ${event.user.name} (${event.user.id})")
            }

            val embed = EmbedBuilder()
                .setTitle("Logger Configuration Removed")
                .setColor(Color.ORANGE)
                .setDescription("${systemCommandEmojiProvider.tickEmoji().formatted} Configuration for logger `$currentLogger` has been removed.")
                .build()

            event.editMessageEmbeds(embed)
                .setComponents()
                .queue()

            return false
        }
        return true
    }

    override fun onModalInteraction(event: ModalInteractionEvent): Boolean {
        if (event.modalId == "add-logger-modal") {
            val loggerName = event.getValue("logger-name")?.asString

            if (loggerName != null) {
                currentLogger = loggerName
                // We're adding a new logger configuration
                isAddOperation = true
                displayLoggerManagementUI(event, loggerName)
                return false
            } else {
                event.reply("Invalid logger name").setEphemeral(true).queue()
                return false
            }
        }
        return true
    }

    override fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent, command: SystemCommand) {
        val input = event.getTypedOption(command.argsOption, "")!!
        val loggers = loggersEndpoint.safeLoggers()

        val filteredLoggers = if (input.isEmpty()) {
            // If no input, prioritize loggers with configured levels
            loggers.filter { loggersEndpoint.loggerLevels(it).configuredLevel != null }
                .take(25)
        } else {
            // Filter by input
            loggers.filter { it.contains(input, ignoreCase = true) }
                .take(25)
        }

        val options = filteredLoggers.map { loggerName ->
            val configuredLevel = loggersEndpoint.loggerLevels(loggerName).configuredLevel
            val displayName = if (configuredLevel != null) {
                "$loggerName [$configuredLevel]"
            } else {
                loggerName
            }
            Command.Choice(displayName, loggerName)
        }

        event.replyChoices(options).queue()
    }
}
