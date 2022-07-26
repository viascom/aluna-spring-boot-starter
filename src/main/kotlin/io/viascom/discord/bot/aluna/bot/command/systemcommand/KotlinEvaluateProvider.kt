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

package io.viascom.discord.bot.aluna.bot.command.systemcommand

import io.viascom.discord.bot.aluna.bot.Interaction
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.scriptengine.KotlinScriptV2Service
import io.viascom.discord.bot.aluna.util.addTextField
import io.viascom.discord.bot.aluna.util.getValueAsString
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import kotlin.math.min

@Interaction
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
@ConditionalOnProperty(name = ["command.system-command.enable-kotlin-script-evaluate"], prefix = "aluna", matchIfMissing = false)
class KotlinEvaluateProvider(
    private val kotlinScriptService: KotlinScriptV2Service,
    private val systemCommandEmojiProvider: SystemCommandEmojiProvider
) : SystemCommandDataProvider(
    "evaluate_kotlin",
    "Evaluate Script",
    true,
    false,
    false,
    false
) {

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        //Show modal

        val modal: Modal = Modal.create("script", "Evaluate Script")
            .addTextField("script", "Kotlin Script", TextInputStyle.PARAGRAPH)
            .build()

        event.replyModal(modal).queueAndRegisterInteraction(command)
    }


    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        val script = event.getValueAsString("script", "0")!!.toString()
        event.reply(
            "Script:\n```kotlin\n" +
                    "$script```\n" +
                    "${systemCommandEmojiProvider.loadingEmoji().formatted} Result:\n" +
                    "``` ```"
        ).queue {
            val result = try {
                kotlinScriptService.eval(script)
            } catch (e: Exception) {
                val trace = e.stackTraceToString()
                trace.substring(0, min(trace.length - 1, 2000))

            }

            if (result.toString().length < 1000) {
                it.editOriginal(
                    "Script:\n```kotlin\n" +
                            "$script```\n" +
                            "Result:\n" +
                            "```$result```"
                ).queue()
            } else {
                it.editOriginal(
                    "Script:\n```kotlin\n" +
                            "$script```\n" +
                            "Result:"
                ).addFile(result.toString().encodeToByteArray(), "result.txt").queue()
            }

        }
        return true
    }
}
