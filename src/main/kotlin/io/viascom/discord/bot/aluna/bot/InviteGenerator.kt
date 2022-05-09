package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.property.AlunaProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["enable-debug-configuration-log"], prefix = "aluna", matchIfMissing = true)
class InviteGenerator(private val alunaProperties: AlunaProperties) : ApplicationListener<ApplicationStartedEvent> {

    val logger: Logger = LoggerFactory.getLogger(javaClass)
    override fun onApplicationEvent(event: ApplicationStartedEvent) {
        if (!alunaProperties.productionMode) {
            val clientId = alunaProperties.discord.applicationId
            var permission = 0L
            alunaProperties.discord.defaultPermissions.forEach { permission = permission or it.rawValue }
            val invite = if(clientId != null) {
                "https://discordapp.com/oauth2/authorize?client_id=$clientId&scope=bot%20applications.commands&permissions=$permission"
            }else{
                "<Please add an applicationId to see this invite link!>"
            }
            logger.info("""
                
                ###############################################
                                Configuration
                -> ownerIds:      ${alunaProperties.ownerIds.joinToString { it.toString() }}
                -> modIds:        ${alunaProperties.modIds.joinToString { it.toString() }}
                -> applicationId: ${alunaProperties.discord.applicationId ?: ""}
                -> token:         ${alunaProperties.discord.token}
                -> invite:        $invite
                ###############################################
            """.trimIndent())
        }
    }

}
