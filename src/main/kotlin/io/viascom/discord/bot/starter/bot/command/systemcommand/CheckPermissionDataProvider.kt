package io.viascom.discord.bot.starter.bot.command.systemcommand

import io.viascom.discord.bot.starter.bot.command.SystemCommand
import io.viascom.discord.bot.starter.bot.emotes.AlunaEmote
import io.viascom.discord.bot.starter.bot.handler.Command
import io.viascom.discord.bot.starter.util.getOptionAsString
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.sharding.ShardManager
import java.awt.Color

@Command
class CheckPermissionDataProvider(
    private val shardManager: ShardManager
) : SystemCommandDataProvider(
    "check_permissions",
    "Check Server Permissions",
    true,
    true,
    true
) {

    lateinit var lastHook: InteractionHook
    lateinit var lastEmbed: EmbedBuilder

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

        lastEmbed = EmbedBuilder()
            .setColor(Color.MAGENTA)
            .setTitle("Check Server Permissions")
            .setDescription("${AlunaEmote.LOADING.asMention()} Checking `${server.name}`...")
        lastHook.editOriginalEmbeds(lastEmbed.build()).complete()
    }

    override fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        val input = event.getOptionAsString("args", "")!!
        val options = shardManager.guilds.filter { it.name.lowercase().contains(input.lowercase()) }.take(25).map {
            net.dv8tion.jda.api.interactions.commands.Command.Choice(it.name, it.id)
        }

        event.replyChoices(options).queue()
    }
}
