package io.viascom.discord.bot.aluna.event

import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.handler.DiscordCommand
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.entities.Channel
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import kotlin.reflect.KClass


@Service
class EventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val discordBot: DiscordBot,
    private val alunaProperties: AlunaProperties
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    internal fun publishDiscordReadyEvent(jdaEvent: ReadyEvent) {
        discordBot.asyncExecutor.execute {
            logger.debug("Publishing DiscordReadyEvent")
            val discordReadyEvent = DiscordReadyEvent(this, jdaEvent)
            applicationEventPublisher.publishEvent(discordReadyEvent)
        }
    }

    internal fun publishDiscordFirstShardReadyEvent(jdaEvent: ReadyEvent) {
        discordBot.asyncExecutor.execute {
            logger.debug("Publishing DiscordFirstShardReadyEvent")
            val discordReadyEvent = DiscordFirstShardReadyEvent(this, jdaEvent)
            applicationEventPublisher.publishEvent(discordReadyEvent)
        }
    }

    internal fun publishDiscordSlashCommandInitializedEvent(
        newCommands: List<KClass<out CommandDataImpl>>,
        updatedCommands: List<KClass<out CommandDataImpl>>,
        removedCommands: List<String>
    ) {
        discordBot.asyncExecutor.execute {
            logger.debug("Publishing DiscordSlashCommandInitializedEvent")
            val discordSlashCommandInitializedEvent = DiscordSlashCommandInitializedEvent(this, newCommands, updatedCommands, removedCommands)
            applicationEventPublisher.publishEvent(discordSlashCommandInitializedEvent)
        }
    }

    internal fun publishDiscordCommandEvent(user: User, channel: Channel, server: Guild?, commandPath: String, command: DiscordCommand) {
        discordBot.asyncExecutor.execute {
            logger.debug("Publishing DiscordCommandEvent")
            val discordCommandEvent = DiscordCommandEvent(this, user, channel, server, commandPath, command)
            applicationEventPublisher.publishEvent(discordCommandEvent)
        }
    }

    internal fun publishDiscordEvent(event: GenericEvent) {
        if (alunaProperties.discord.publishEvents) {
            discordBot.asyncExecutor.execute {
                try {
                    var eventClass: Class<*>? = event::class.java

                    if (eventClass?.simpleName == "GatewayPingEvent" && !alunaProperties.discord.publishGatePingEvent) {
                        return@execute
                    }
                    if (eventClass?.simpleName == "GuildReadyEvent" && !alunaProperties.discord.publishGuildReadyEvent) {
                        return@execute
                    }

                    while (eventClass != null) {
                        val workClass = eventClass
                        if (workClass.simpleName !in arrayListOf("Class", "Object", "HttpRequestEvent")) {
                            val specificEvent = Class.forName("On${workClass.simpleName}")
                            logger.debug("Publishing ${workClass.canonicalName}")
                            applicationEventPublisher.publishEvent(specificEvent.constructors.first().newInstance(this, event))
                        }

                        eventClass = if (alunaProperties.discord.publishOnlyFirstEvent) {
                            null
                        } else {
                            eventClass.superclass
                        }

                    }
                } catch (e: Exception) {
                    logger.debug("Could not publish event ${event::class.simpleName}\n" + e.printStackTrace())
                }
            }
        }
    }

}