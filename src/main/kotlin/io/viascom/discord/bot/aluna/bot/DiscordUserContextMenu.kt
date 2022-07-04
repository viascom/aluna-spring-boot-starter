package io.viascom.discord.bot.aluna.bot

import datadog.trace.api.Trace
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction
import org.slf4j.MDC
import org.springframework.util.StopWatch

abstract class DiscordUserContextMenu(name: String, localizations: LocalizationFunction? = null) : DiscordContextMenu(Command.Type.USER, name, localizations) {

    /**
     * The main body method of a [DiscordContextMenu].
     * <br></br>This is the "response" for a successful
     * [#run(DiscordContextMenu)][DiscordContextMenu.execute].
     *
     * @param event The [UserContextInteractionEvent] that triggered this Command
     */
    @Trace
    protected abstract fun execute(event: UserContextInteractionEvent)

    /**
     * Runs checks for the [DiscordUserContextMenu] with the given [UserContextInteractionEvent] that called it.
     *
     * @param event The UserContextInteractionEvent that triggered this Command
     */
    @Trace
    fun run(event: UserContextInteractionEvent) {
        if (alunaProperties.debug.useStopwatch) {
            stopWatch = StopWatch()
            stopWatch!!.start()
        }

        MDC.put("interaction", event.commandPath)
        MDC.put("uniqueId", uniqueId)

        guild = event.guild
        guild?.let { MDC.put("discord.server", "${it.id} (${it.name})") }
        channel = event.channel
        channel?.let { MDC.put("discord.channel", it.id) }
        author = event.user
        MDC.put("author", "${author.id} (${author.name})")

        userLocale = event.userLocale

        if (guild != null) {
            member = guild!!.getMember(author)
            guildChannel = event.guildChannel
            guildLocale = event.guildLocale
        }

        val missingUserPermissions = discordInteractionConditions.checkForNeededUserPermissions(this, userPermissions, event)
        if (missingUserPermissions.hasMissingPermissions) {
            onMissingUserPermission(event, missingUserPermissions)
            return
        }

        val missingBotPermissions = discordInteractionConditions.checkForNeededBotPermissions(this, botPermissions, event)
        if (missingBotPermissions.hasMissingPermissions) {
            onMissingBotPermission(event, missingBotPermissions)
            return
        }

        val additionalRequirements = discordInteractionAdditionalConditions.checkForAdditionalContextRequirements(this, event)
        if (additionalRequirements.failed) {
            onFailedAdditionalRequirements(event, additionalRequirements)
            return
        }

        //Load additional data for this interaction
        discordInteractionLoadAdditionalData.loadData(this, event)

        try {
            //Run onCommandExecution in asyncExecutor to ensure it is not blocking the execution of the interaction itself
            discordBot.interactionExecutor.execute {
                discordInteractionMetaDataHandler.onContextMenuExecution(this, event)
            }
            if (alunaProperties.discord.publishDiscordContextEvent) {
                eventPublisher.publishDiscordUserContextEvent(author, channel, guild, event.commandPath, this)
            }
            logger.info("Run context menu '${event.commandPath}'" + if (alunaProperties.debug.showHashCode) " [${this.hashCode()}]" else "")
            execute(event)
        } catch (e: Exception) {
            try {
                onExecutionException(event, e)
            } catch (exceptionError: Exception) {
                discordInteractionMetaDataHandler.onGenericExecutionException(this, e, exceptionError, event)
            }
        } finally {
            exitCommand(event)
        }
    }
}
