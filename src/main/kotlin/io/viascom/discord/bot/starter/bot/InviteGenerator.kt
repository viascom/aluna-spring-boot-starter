package io.viascom.discord.bot.starter.bot

import io.viascom.discord.bot.starter.property.AlunaProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
class InviteGenerator(private val alunaProperties: AlunaProperties) : ApplicationListener<ApplicationStartedEvent> {

    val logger: Logger = LoggerFactory.getLogger(javaClass)
    override fun onApplicationEvent(event: ApplicationStartedEvent) {
        if (!alunaProperties.productionMode) {
            val clientId = alunaProperties.discord.applicationId
            var permission = 0L
            alunaProperties.discord.defaultPermissions.forEach { permission = permission or it.rawValue }
            val invite = "https://discordapp.com/oauth2/authorize?client_id=$clientId&scope=bot%20applications.commands&permissions=$permission"

            logger.info("""
                
                ###############################################
                                Configuration
                -> ownerIds:      ${alunaProperties.ownerIds.joinToString { it.toString() }}
                -> modIds:        ${alunaProperties.modIds.joinToString { it.toString() }}
                -> applicationId: ${alunaProperties.discord.applicationId}
                -> token:         ${alunaProperties.discord.token}
                -> invite:        $invite
                ###############################################
            """.trimIndent())
        }
    }

}
