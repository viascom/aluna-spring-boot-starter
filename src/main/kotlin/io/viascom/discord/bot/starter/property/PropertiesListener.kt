package io.viascom.discord.bot.starter.property

import io.viascom.discord.bot.starter.exception.AlunaPropertiesException
import org.springframework.boot.context.event.ApplicationContextInitializedEvent
import org.springframework.context.ApplicationListener

/**
 * Executes checks against the configuration present on startup to ensure that all needed parameters are set.
 */
class PropertiesListener : ApplicationListener<ApplicationContextInitializedEvent> {
    override fun onApplicationEvent(event: ApplicationContextInitializedEvent) {
        //check bot token
        val token = event.applicationContext.environment.getProperty("aluna.discord.token") ?: ""
        if (token.isEmpty()) {
            throw AlunaPropertiesException(
                "Aluna configuration is missing a needed parameter",
                "aluna.discord.token",
                "",
                "A valid discord token is needed"
            )
        }


        //Check owners
        val ownerIds = event.applicationContext.environment.getProperty("aluna.owner-ids", ArrayList::class.java) ?: arrayListOf<Long>()
        if (ownerIds == null || ownerIds.isEmpty() == true) {
            throw AlunaPropertiesException(
                "Aluna configuration is missing a needed parameter",
                "aluna.owner-ids",
                "",
                "At least one owner id needs to be defined"
            )
        }

    }

}
