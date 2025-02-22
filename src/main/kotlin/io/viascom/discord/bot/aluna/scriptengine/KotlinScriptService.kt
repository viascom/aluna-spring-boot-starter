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

package io.viascom.discord.bot.aluna.scriptengine

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.sharding.ShardManager
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

/**
 * Kotlin script service which can be used to execute kotlin scripts during runtime.
 *
 * !! Attention: This does not provide any security or sandbox like feature. !!
 *
 * By default, all classes from the io.viascom.discord.bot.aluna.* package are imported.
 * You can directly access shardManager, discordBot, alunaProperties. If you need other binding and or imports, you have to implement KotlinScriptBindingProvider
 *
 * Default Bindings:
 * - shardManager: ShardManager
 * - discordBot: DiscordBot
 * - alunaProperties: AlunaProperties
 *
 * Default imports:
 * - io.viascom.discord.bot.aluna.*
 * - java.awt.Color
 * - java.util.*
 * - java.time.OffsetDateTime",
 * - net.dv8tion.jda.api.interactions.commands.*
 * - net.dv8tion.jda.api.interactions.commands.build.*
 * - net.dv8tion.jda.api.interactions.components.*
 * - net.dv8tion.jda.api.interactions.components.buttons.*
 * - net.dv8tion.jda.api.interactions.components.selections.*
 * - net.dv8tion.jda.api.*
 * - net.dv8tion.jda.api.entities.*
 */
@Service
@ConditionalOnJdaEnabled
@ConditionalOnProperty(name = ["command.system-command.enable-kotlin-script-evaluate"], prefix = "aluna", matchIfMissing = false)
class KotlinScriptService(
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val alunaProperties: AlunaProperties,
    private val bindingProviders: List<KotlinScriptBindingProvider>
) {

    private val engine = KotlinJsr223JvmLocalScriptEngineFactory().scriptEngine

    private val imports: String

    init {
        val os = System.getProperty("os.name").lowercase()
        if (os.startsWith("win")) {
            setIdeaIoUseFallback()
        }

        imports = buildString {
            // Import every aluna package
            Package.getPackages().map { it.name }.filter { it.startsWith("io.viascom.discord.bot.aluna") }.forEach { append("import ", it, ".*\n") }
            // Include some miscellaneous imports for quality of life
            additionalImports.forEach { append("import ", it, "\n") }
            append("\n\n")
        }
    }

    fun eval(kotlin: String): Any {

        val bindings = arrayListOf(
            Binding("shardManager", shardManager, ShardManager::class),
            Binding("discordBot", discordBot, DiscordBot::class),
            Binding("alunaProperties", alunaProperties, AlunaProperties::class),
        )

        var providedImports = ""

        bindingProviders.forEach {
            bindings.addAll(it.getBindings())
            providedImports += it.getImports().joinToString("\n", postfix = "\n\n") { it }
        }

        // Insert them into the script engine
        bindings.forEach { engine.put(it.name, it.obj) }

        // Prepend the variables to the script, so we don't have to use bindings["name"]
        val variables = bindings.joinToString("\n", postfix = "\n\n") {
            """val ${it.name} = bindings["${it.name}"] as ${it.type.qualifiedName}"""
        }

        // Before we begin constructing the script, we need to find any additional imports
        val importLines = kotlin.lines().takeWhile { it.startsWith("import") }
        val additionalImports = importLines.joinToString("\n", postfix = "\n\n") + providedImports
        val script = kotlin.lines().filterNot { it in importLines }.joinToString("\n")

        return try {
            engine.eval(imports + additionalImports + variables + script)
        } catch (throwable: Throwable) {
            throw throwable
        } ?: "n/a - no return value"
    }

    private companion object {
        private val additionalImports = arrayOf(
            "java.awt.Color", "java.util.*",
            "java.time.OffsetDateTime",
            "net.dv8tion.jda.api.interactions.commands.*", "net.dv8tion.jda.api.interactions.commands.build.*",
            "net.dv8tion.jda.api.interactions.components.*", "net.dv8tion.jda.api.interactions.components.buttons.*",
            "net.dv8tion.jda.api.interactions.components.selections.*", "net.dv8tion.jda.api.*",
            "net.dv8tion.jda.api.entities.*"
        )
    }

    open class Binding(val name: String, val obj: Any, val type: KClass<out Any>)
}
