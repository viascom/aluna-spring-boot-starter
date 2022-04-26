package io.viascom.discord.bot.starter.bot.command.systemcommand

import io.viascom.discord.bot.starter.bot.command.SystemCommand
import io.viascom.discord.bot.starter.bot.emotes.AlunaEmote
import io.viascom.discord.bot.starter.bot.handler.Command
import io.viascom.discord.bot.starter.bot.handler.queueAndRegisterInteraction
import io.viascom.discord.bot.starter.util.createDangerButton
import io.viascom.discord.bot.starter.util.createSuccessButton
import io.viascom.discord.bot.starter.util.getOptionAsString
import io.viascom.discord.bot.starter.util.removeActionRows
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.sharding.ShardManager
import java.awt.Color
import java.time.Duration

@Command
class LeaveServerProvider(
    private val shardManager: ShardManager
) : SystemCommandDataProvider(
    "leave_server",
    "Leave Server",
    true,
    false,
    true
) {

    lateinit var lastHook: InteractionHook
    lateinit var lastEmbed: EmbedBuilder
    lateinit var selectedServer: Guild

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook, command: SystemCommand) {
        lastHook = hook

        val id = event.getOptionAsString("args", "")!!
        if (id.isEmpty()) {
            lastHook.editOriginal("${AlunaEmote.BOT_CROSS.asMention()} Please specify an ID as argument for this command").queue()
            return
        }

        val server = shardManager.getGuildById(id)
        if (server == null) {
            lastHook.editOriginal("${AlunaEmote.BOT_CROSS.asMention()} Please specify a valid server ID as argument for this command").queue()
            return
        }

        selectedServer = server

        lastEmbed = EmbedBuilder()
            .setColor(Color.RED)
            .setDescription("${AlunaEmote.DOT_RED.asMention()} Do you really want that this Bot leaves the server **${server.name}**?")
        lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(
            ActionRow.of(
                createDangerButton("yes", "Yes", AlunaEmote.SMALL_TICK_WHITE.toEmoji()),
                createSuccessButton("no", "No", AlunaEmote.SMALL_CROSS_WHITE.toEmoji())
            )
        ).queueAndRegisterInteraction(lastHook, command, duration = Duration.ofMinutes(2))
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        event.deferEdit().queue { hook ->
            lastHook = hook

            if (event.componentId == "yes") {
                lastEmbed.setDescription("${AlunaEmote.DOT_GREEN.asMention()} Bot left **${selectedServer.name}**")
                selectedServer.leave().queue()
            } else {
                lastEmbed.setDescription("${AlunaEmote.DOT_YELLOW.asMention()} Canceled")
            }
            lastHook.editOriginalEmbeds(lastEmbed.build()).removeActionRows().queue()
        }
        return true
    }

    override fun onButtonInteractionTimeout() {
        lastHook.editOriginalEmbeds(lastEmbed.build()).removeActionRows().queue()
    }

    override fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        val input = event.getOptionAsString("args", "")!!
        val options = shardManager.guilds.filter { it.name.lowercase().contains(input.lowercase()) }.take(25).map {
            net.dv8tion.jda.api.interactions.commands.Command.Choice(it.name, it.id)
        }

        event.replyChoices(options).queue()
    }
}
