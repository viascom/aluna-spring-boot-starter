package io.viascom.discord.bot.aluna.bot.handler

import datadog.trace.api.Trace
import io.viascom.discord.bot.aluna.configuration.Experimental
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import org.slf4j.MDC
import org.springframework.util.StopWatch

@Experimental("This is still in development")
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

        try {
            writeToStats()
            logger.info("Run context menu ${event.commandPath}" + if (alunaProperties.debug.showHashCode) " [${this.hashCode()}]" else "")
            execute(event)
            exitCommand(event)
        } catch (t: Throwable) {
            exitCommand(event)
            throw t
        }
    }
}
