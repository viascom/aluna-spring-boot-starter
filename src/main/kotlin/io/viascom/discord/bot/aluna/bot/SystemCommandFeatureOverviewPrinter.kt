package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.bot.command.systemcommand.SystemCommandDataProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true)
class SystemCommandFeatureOverviewPrinter : ApplicationListener<ApplicationStartedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ApplicationStartedEvent) {
        val systemCommand = event.applicationContext.environment.getProperty("aluna.command.system-command.enable", Boolean::class.java) ?: false
        //Print enabled /system-command features
        if (systemCommand) {
            val allFunctions = event.applicationContext.getBeansOfType(SystemCommandDataProvider::class.java)
            val enabledFunctionsDefinition =
                event.applicationContext.environment.getProperty("aluna.command.system-command.enabled-functions", ArrayList::class.java)
                    ?: arrayListOf<String>()

            val enabledFunctions = allFunctions.values.filter { it.id in enabledFunctionsDefinition || enabledFunctionsDefinition.isEmpty() }

            if (enabledFunctions.size == allFunctions.size) {
                logger.debug("Enabled /system-command functions: [" + allFunctions.values.joinToString(", ") { it.id } + "]")
            } else {
                logger.debug("Enabled /system-command functions: [" + enabledFunctions.joinToString(", ") { it.id } + "]")
                logger.debug("Disabled /system-command functions: [" + allFunctions.values.filter { it.id !in enabledFunctionsDefinition }
                    .joinToString(", ") { it.id } + "]")
            }

            val allowedModFunctionsDefinition =
                event.applicationContext.environment.getProperty("aluna.command.system-command.allowed-for-moderators-functions", ArrayList::class.java)
                    ?: arrayListOf<String>()

            val allowedModFunctions =
                allFunctions.filter { it.value.id in allowedModFunctionsDefinition || allowedModFunctionsDefinition.isEmpty() }.filter { it.value.allowMods }

            logger.debug("Allowed for moderators /system-command functions: [" + allowedModFunctions.values.joinToString(", ") { it.id } + "]")
        }
    }
}
