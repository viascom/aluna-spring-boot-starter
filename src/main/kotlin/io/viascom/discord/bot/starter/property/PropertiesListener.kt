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

        //Check notification
        checkNotification("aluna.notification.server-join", event)
        checkNotification("aluna.notification.server-leave", event)
        checkNotification("aluna.notification.bot-ready", event)
    }

    private fun checkNotification(base: String, event: ApplicationContextInitializedEvent) {
        val sendJoin = event.applicationContext.environment.getProperty("$base.enable", Boolean::class.java) ?: false
        if (sendJoin) {
            val sendJoinServer = event.applicationContext.environment.getProperty("$base.server", Long::class.java) ?: 0L
            if (sendJoinServer == 0L) {
                throw AlunaPropertiesException(
                    "Aluna configuration is missing a needed parameter",
                    "$base.server",
                    "",
                    "$base.enable is enabled, $base.server has to be defined"
                )
            }

            val sendJoinChannel = event.applicationContext.environment.getProperty("$base.channel", Long::class.java) ?: 0L
            if (sendJoinChannel == 0L) {
                throw AlunaPropertiesException(
                    "Aluna configuration is missing a needed parameter",
                    "$base.channel",
                    "",
                    "$base.enable is enabled, $base.channel has to be defined"
                )
            }
        }
    }

}
