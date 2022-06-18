package io.viascom.discord.bot.aluna.bot

import datadog.trace.api.Trace
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import org.slf4j.MDC
import org.springframework.util.StopWatch

abstract class DiscordMessageContextMenu(name: String) : DiscordContextMenu(Command.Type.MESSAGE, name) {

    /**
     * The main body method of a [DiscordContextMenu].
     * <br></br>This is the "response" for a successful
     * [#run(DiscordCommand)][DiscordContextMenu.execute].
     *
     * @param event The [MessageContextInteractionEvent] that triggered this Command
     */
    @Trace
    protected abstract fun execute(event: MessageContextInteractionEvent)

    /**
     * Runs checks for the [DiscordUserContextMenu] with the given [MessageContextInteractionEvent] that called it.
     *
     * @param event The MessageContextInteractionEvent that triggered this Command
     */
    @Trace
    open fun run(event: MessageContextInteractionEvent) {
        if (alunaProperties.debug.useStopwatch) {
            stopWatch = StopWatch()
            stopWatch!!.start()
        }

        MDC.put("command", event.commandPath)

        server = event.guild
        server?.let { MDC.put("discord.server", "${it.id} (${it.name})") }
        channel = event.channel
        channel?.let { MDC.put("discord.channel", it.id) }
        author = event.user
        MDC.put("author", "${author.id} (${author.name})")

        userLocale = event.userLocale

        if (server != null) {
            member = server!!.getMember(author)
            serverChannel = event.guildChannel
            serverLocale = event.guildLocale
        }

        val missingUserPermissions = discordCommandConditions.checkForNeededUserPermissions(this, userPermissions, event)
        if (missingUserPermissions.hasMissingPermissions) {
            onMissingUserPermission(event, missingUserPermissions)
            return
        }

        val missingBotPermissions = discordCommandConditions.checkForNeededBotPermissions(this, botPermissions, event)
        if (missingBotPermissions.hasMissingPermissions) {
            onMissingBotPermission(event, missingBotPermissions)
            return
        }

        val additionalRequirements = discordCommandAdditionalConditions.checkForAdditionalContextRequirements(this, event)
        if (additionalRequirements.failed) {
            onFailedAdditionalRequirements(event, additionalRequirements)
            return
        }

        //Load additional data for this command
        discordCommandLoadAdditionalData.loadData(this, event)

        try {
            //Run onCommandExecution in asyncExecutor to ensure it is not blocking the execution of the command itself
            discordBot.asyncExecutor.execute {
                discordCommandMetaDataHandler.onContextMenuExecution(this, event)
            }
            if (alunaProperties.discord.publishDiscordContextEvent) {
                eventPublisher.publishDiscordMessageContextEvent(author, channel, server, event.commandPath, this)
            }
            logger.info("Run context menu ${event.commandPath}" + if (alunaProperties.debug.showHashCode) " [${this.hashCode()}]" else "")
            execute(event)
        } catch (e: Exception) {
            try {
                onExecutionException(event, e)
            } catch (exceptionError: Exception) {
                discordCommandMetaDataHandler.onGenericExecutionException(this, e, exceptionError, event)
            }
        } finally {
            exitCommand(event)
        }
    }
}
