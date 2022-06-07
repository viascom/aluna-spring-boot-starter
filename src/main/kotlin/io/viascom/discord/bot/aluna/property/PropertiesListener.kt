package io.viascom.discord.bot.aluna.property

import io.viascom.discord.bot.aluna.exception.AlunaPropertiesException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationContextInitializedEvent
import org.springframework.context.ApplicationListener


/**
 * Executes checks against the configuration present on startup to ensure that all needed parameters are set.
 */
class PropertiesListener : ApplicationListener<ApplicationContextInitializedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ApplicationContextInitializedEvent) {
        //Check if jda is disabled
        if((event.applicationContext.environment.getProperty("aluna.discord.enable-jda", Boolean::class.java) ?: true) == false){
            return
        }

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


        //Check owners if system-command is enabled
        val ownerIds = event.applicationContext.environment.getProperty("aluna.owner-ids", ArrayList::class.java) ?: arrayListOf<Long>()
        val systemCommand = event.applicationContext.environment.getProperty("aluna.command.system-command.enable", Boolean::class.java) ?: false
        if (ownerIds.isEmpty() && systemCommand) {
            logger.info("/system-command is enabled but no owner-ids are defined! If you use the DefaultOwnerIdProvider, you may not be able to use this command.")
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
