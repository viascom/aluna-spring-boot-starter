package io.viascom.discord.bot.aluna.scriptengine

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.sharding.ShardManager
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Service

/**
 * Kotlin script service which can be used to execute kotlin scripts during runtime.
 *
 * !! Attention: This does not provide any security or sandbox like feature. !!
 *
 * By default all classes from the io.viascom.discord.bot.aluna.* package are imported.
 * You can directly access shardManager, discordBot, alunaProperties. If you need other binding and or imports, you have to implement KotlinScriptBindingProvider
 *
 */
@Service
@ConditionalOnExpression("\${aluna.discord.enable-jda:true} && \${aluna.command.system-command.enable-kotlin-script-evaluate:false}")
class KotlinScriptService(
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val alunaProperties: AlunaProperties,
    private val bindingProviders: List<KotlinScriptBindingProvider>
) {

    private val engine = KotlinJsr223JvmLocalScriptEngineFactory().scriptEngine

    private val imports: String

    init {
        setIdeaIoUseFallback()
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
            Triple("shardManager", shardManager, ShardManager::class),
            Triple("discordBot", discordBot, DiscordBot::class),
            Triple("alunaProperties", alunaProperties, AlunaProperties::class),
        )

        var providedImports = ""

        bindingProviders.forEach {
            bindings.addAll(it.getBindings())
            providedImports += it.getImports().joinToString("\n", postfix = "\n\n") { it }
        }

        // Insert them into the script engine
        bindings.forEach { engine.put(it.first, it.second) }

        // Prepend the variables to the script, so we don't have to use bindings["name"]
        val variables = bindings.joinToString("\n", postfix = "\n\n") {
            """val ${it.first} = bindings["${it.first}"] as ${it.third.qualifiedName}"""
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

}
