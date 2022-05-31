package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.property.ModeratorIdProvider
import io.viascom.discord.bot.aluna.property.OwnerIdProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["enable-debug-configuration-log"], prefix = "aluna", matchIfMissing = true)
class InviteGenerator(
    private val alunaProperties: AlunaProperties,
    private val ownerIdProvider: OwnerIdProvider,
    private val moderatorIdProvider: ModeratorIdProvider
) : ApplicationListener<ApplicationStartedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ApplicationStartedEvent) {
        if (!alunaProperties.productionMode) {
            var permission = 0L
            alunaProperties.discord.defaultPermissions.forEach { permission = permission or it.rawValue }
            val invite = if (alunaProperties.discord.applicationId != null) {
                "https://discordapp.com/oauth2/authorize?client_id=${alunaProperties.discord.applicationId}&scope=bot%20applications.commands&permissions=$permission"
            } else {
                "<Please add an applicationId to see this invite link!>"
            }
            logger.info(
                """
                
                ###############################################
                                Configuration
                -> ownerIds:      ${ownerIdProvider.getOwnerIds().joinToString { it.toString() }}
                -> modIds:        ${moderatorIdProvider.getModeratorIds().joinToString { it.toString() }}
                -> applicationId: ${alunaProperties.discord.applicationId ?: ""}
                -> token:         ${alunaProperties.discord.token}
                -> invite:        $invite
                ###############################################
            """.trimIndent()
            )
        }
    }

}
