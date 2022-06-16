package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.AlunaAutoConfiguration
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationPreparedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
@ConditionalOnJdaEnabled
class VersionPrinter : ApplicationListener<ApplicationPreparedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(AlunaAutoConfiguration::class.java)

    override fun onApplicationEvent(event: ApplicationPreparedEvent) {
        val versions = this::class.java.classLoader.getResource("version.txt")?.readText()?.split("\n")
        val internalVersion = this::class.java.getPackage().implementationVersion
        val alunaVersion = versions?.getOrElse(0) { _ -> internalVersion } ?: internalVersion
        val jdaVersion = versions?.getOrElse(1) { _ -> "n/a" } ?: "n/a"

        logger.info("Running with Aluna $alunaVersion, JDA $jdaVersion")


        val productionMode = event.applicationContext.environment.getProperty("aluna.production-mode", Boolean::class.java, false)
        if (!productionMode) {
            logger.warn("Aluna is NOT running in production mode!")
        }
    }

}
