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
import io.viascom.discord.bot.aluna.util.getTypedOption
import io.viascom.discord.bot.aluna.util.removeComponents
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.config.ScheduledTaskHolder
import org.springframework.stereotype.Component
import java.awt.Color
import java.lang.reflect.Method

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class SpringSchedulerProvider(
    private val applicationContext: ApplicationContext,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : SystemCommandDataProvider(
    "trigger_scheduler",
    "Trigger Scheduler",
    true,
    false,
    true,
    true
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private var selectedScheduler: SchedulerInfo? = null
    private var embedMessage: EmbedBuilder? = null

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        val schedulers = getSpringSchedulers()

        if (schedulers.isEmpty()) {
            hook!!.editOriginal("${systemCommandEmojiProvider.crossEmoji().formatted} No Spring schedulers found in the application.").queue()
            return
        }

        val selectedId = event.getTypedOption(command.argsOption, "")!!
        if (selectedId.isBlank()) {
            hook!!.editOriginal("${systemCommandEmojiProvider.crossEmoji().formatted} Please specify a valid scheduler").queue()
            return
        }
        val scheduler = getSpringSchedulers().find { it.id == selectedId }

        selectedScheduler = scheduler

        if (scheduler == null) {
            hook!!.editOriginal("${systemCommandEmojiProvider.crossEmoji().formatted} Selected scheduler not found.").queue()
            return
        }

        val confirmButton = Button.success("trigger_scheduler", "Trigger Scheduler")
        val cancelButton = Button.danger("cancel_trigger", "Cancel")

        embedMessage = EmbedBuilder()
            .setTitle("Trigger Scheduler")
            .setColor(Color.GREEN)
            .addField("**Name:**", scheduler.name, false)
            .addField("**Class:**", "`${scheduler.className}`", false)
            .addField("**Method:**", "`${scheduler.methodName}`", true)

        scheduler.cronExpression?.let { embedMessage!!.addField("**Cron Expression:**", it, false) }
        scheduler.fixedDelay?.let {
            embedMessage!!.addField(
                "**Fixed Delay:** ",
                "$it ms\n" +
                        "-# The fixedDelay property makes sure that there is a delay of n millisecond between the finish time of an execution of a task and the start time of the next execution of the task.",
                false
            )
        }
        scheduler.fixedRate?.let {
            embedMessage!!.addField(
                "**Fixed Rate:**", "$it ms\n" +
                        "-# The fixedRate property runs the scheduled task at every n millisecond. It doesn’t check for any previous executions of the task.", false
            )
        }
        scheduler.initialDelay?.let {
            embedMessage!!.addField(
                "**Initial Delay:**", "$it ms\n" +
                        "-# The initialDelay property makes sure that the first execution of the task is delayed by n millisecond.", false
            )
        }

        embedMessage!!.addBlankField(false)
            .addField("", "Do you want to trigger this scheduler?", false)

        hook!!.editOriginalEmbeds(embedMessage!!.build())
            .setActionRow(confirmButton, cancelButton)
            .queueAndRegisterInteraction(hook, command)
    }


    override fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent, command: SystemCommand) {
        val schedulers = getSpringSchedulers()
        val input = event.getTypedOption(command.argsOption, "")!!
        val options = schedulers
            .filter { it.shortName.contains(input, true) || it.className.contains(input, true) || it.methodName.contains(input, true) }
            .sortedBy { it.shortName }
            .take(25)
            .map { Choice(it.shortName, it.id) }
        event.replyChoices(options).queue()
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        when (event.componentId) {
            "trigger_scheduler" -> {
                val scheduler = selectedScheduler
                if (scheduler == null) {
                    event.deferEdit().queue { hook ->
                        hook.editOriginalEmbeds(embedMessage!!.build()).removeComponents().queue()
                        hook.sendMessage("${systemCommandEmojiProvider.crossEmoji().formatted} No scheduler selected.").setEphemeral(true).queue()
                    }
                    return true
                }

                event.deferEdit().queue { hook ->
                    try {
                        val result = triggerScheduler(scheduler)
                        if (result) {
                            hook.editOriginalEmbeds(embedMessage!!.build()).removeComponents().queue()
                            hook.sendMessage("${systemCommandEmojiProvider.tickEmoji().formatted} Successfully triggered scheduler: ${scheduler.name}").setEphemeral(true).queue()
                            logger.info("Scheduler ${scheduler.name} was manually triggered by ${event.user.name} (${event.user.id})")
                        } else {
                            hook.editOriginalEmbeds(embedMessage!!.build()).removeComponents().queue()
                            hook.sendMessage("${systemCommandEmojiProvider.crossEmoji().formatted} Failed to trigger scheduler: ${scheduler.name}").setEphemeral(true).queue()
                        }
                    } catch (e: Exception) {
                        hook.editOriginalEmbeds(embedMessage!!.build()).removeComponents().queue()
                        hook.sendMessage("${systemCommandEmojiProvider.crossEmoji().formatted} Error triggering scheduler: ${e.message}").setEphemeral(true).queue()
                        logger.error("Error triggering scheduler ${scheduler.name}: ${e.stackTraceToString()}")
                    }
                }
                return true
            }

            "cancel_trigger" -> {
                event.deferEdit().queue { hook ->
                    hook.editOriginalEmbeds(embedMessage!!.build()).removeComponents().queue()
                    hook.sendMessage("${systemCommandEmojiProvider.tickEmoji().formatted} Scheduler trigger cancelled.").setEphemeral(true).queue()
                }
                return true
            }

            else -> return false
        }
    }

    private fun getSpringSchedulers(): List<SchedulerInfo> {
        val schedulers = mutableListOf<SchedulerInfo>()

        return try {
            val scheduledTaskHolderBeans = applicationContext.getBeansOfType(ScheduledTaskHolder::class.java)
            val beans = applicationContext.beanDefinitionNames.mapNotNull {
                runCatching { applicationContext.getBean(it) }.getOrNull()
            }

            beans.forEach { bean ->
                bean.javaClass.methods.filter { it.isAnnotationPresent(Scheduled::class.java) }.forEach { method ->
                    val annotation = method.getAnnotation(Scheduled::class.java)
                    val cronExpression = annotation.cron.takeIf { it.isNotBlank() }
                    val fixedDelay = annotation.fixedDelay.takeIf { it > 0 }
                    val fixedRate = annotation.fixedRate.takeIf { it > 0 }
                    val initialDelay = annotation.initialDelay.takeIf { it > 0 }

                    schedulers.add(
                        SchedulerInfo(
                            id = "${bean.javaClass.name}_${method.name}",
                            name = method.name,
                            shortName = "${bean.javaClass.simpleName}.${method.name}",
                            className = bean.javaClass.name,
                            methodName = method.name,
                            cronExpression = cronExpression,
                            fixedDelay = fixedDelay,
                            fixedRate = fixedRate,
                            initialDelay = initialDelay,
                            bean = bean,
                            method = method
                        )
                    )
                }
            }

            schedulers
        } catch (e: Exception) {
            logger.error("Error getting Spring schedulers", e)
            emptyList()
        }
    }

    private fun triggerScheduler(scheduler: SchedulerInfo): Boolean {
        return runCatching {
            scheduler.method.apply { isAccessible = true }.invoke(scheduler.bean)
            true
        }.onFailure { e ->
            logger.error("Error triggering scheduler ${'$'}{scheduler.name}", e)
        }.getOrDefault(false)
    }

    data class SchedulerInfo(
        val id: String,
        val name: String,
        val shortName: String,
        val className: String,
        val methodName: String,
        val cronExpression: String?,
        val fixedDelay: Long?,
        val fixedRate: Long?,
        val initialDelay: Long?,
        val bean: Any,
        val method: Method
    )
}
