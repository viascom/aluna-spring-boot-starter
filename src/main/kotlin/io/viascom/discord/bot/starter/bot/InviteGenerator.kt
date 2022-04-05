package io.viascom.discord.bot.starter.bot

import io.viascom.discord.bot.starter.property.AlunaProperties
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
class InviteGenerator(private val alunaProperties: AlunaProperties) : ApplicationListener<ApplicationStartedEvent> {

    override fun onApplicationEvent(event: ApplicationStartedEvent) {
        if (!alunaProperties.productionMode) {
            val clientId = alunaProperties.discord.applicationId
            var permission = 0L
            alunaProperties.discord.defaultPermissions.forEach { permission = permission or it.rawValue }
            val invite = "https://discordapp.com/oauth2/authorize?client_id=$clientId&scope=bot%20applications.commands&permissions=$permission"

            println("###############################################")
            println("     Configuration")
            println("-> ownerIds: ${alunaProperties.ownerIds.joinToString { it.toString() }}")
            println("-> botToken: ${alunaProperties.discord.token}")
            println("-> invite:   $invite")
            println("###############################################")
        }
    }

}
