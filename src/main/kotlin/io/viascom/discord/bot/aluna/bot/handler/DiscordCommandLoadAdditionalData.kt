package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

/**
 * Interface to implement if you need to load additional data before the command gets executed.
 */
interface DiscordCommandLoadAdditionalData {

    /**
     * This method get called before the command is executed.
     * Make sure to not block the execution for to long as the command needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param event Slash command event
     */
    fun loadData(discordCommand: DiscordCommand, event: SlashCommandInteractionEvent)

    /**
     * This method get called before the command is executed.
     * Make sure to not block the execution for to long as the auto complete interaction needs to be acknowledged in 3 seconds.
     *
     * @param discordCommand Discord command instance
     * @param event Auto complete interaction event
     */
    fun loadData(discordCommand: DiscordCommand, event: CommandAutoCompleteInteractionEvent)

    /**
     * This method get called before the auto-complete is executed.
     * Make sure to not block the execution for to long as the auto complete interaction needs to be acknowledged in 3 seconds.
     *
     * @param event Auto complete interaction event
     */
    fun loadData(event: CommandAutoCompleteInteractionEvent)

}
