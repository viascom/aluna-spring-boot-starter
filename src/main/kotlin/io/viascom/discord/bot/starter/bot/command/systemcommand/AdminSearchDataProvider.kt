package io.viascom.discord.bot.starter.bot.command.systemcommand

import io.viascom.discord.bot.starter.bot.command.SystemCommand
import io.viascom.discord.bot.starter.bot.command.systemcommand.adminsearch.AdminSearchPageDataProvider
import io.viascom.discord.bot.starter.bot.emotes.AlunaEmote
import io.viascom.discord.bot.starter.bot.handler.Command
import io.viascom.discord.bot.starter.bot.handler.DiscordCommand
import io.viascom.discord.bot.starter.bot.handler.queueAndRegisterInteraction
import io.viascom.discord.bot.starter.util.getOptionAsString
import io.viascom.discord.bot.starter.util.getSelection
import io.viascom.discord.bot.starter.util.removeActionRows
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.sharding.ShardManager
import java.awt.Color

@Command
class AdminSearchDataProvider(
    private val shardManager: ShardManager,
    private val adminSearchPageDataProviders: List<AdminSearchPageDataProvider>
) : SystemCommandDataProvider(
    "adminsearch",
    "Admin Search",
    true,
    false,
    true
) {

    lateinit var lastHook: InteractionHook
    lateinit var lastEmbed: EmbedBuilder
    lateinit var selectedType: AdminSearchType
    lateinit var discordUser: User

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook, command: SystemCommand) {
        lastHook = hook

        val id = event.getOptionAsString("args", "")!!
        if (id.isEmpty()) {
            lastHook.editOriginal("${AlunaEmote.BOT_CROSS.asMention()} Please specify an ID as argument for this command").queue()
            return
        }

        lastEmbed = EmbedBuilder()
            .setColor(Color.MAGENTA)
            .setTitle("Admin Search")
            .setDescription("${AlunaEmote.LOADING.asMention()} Searching for `${id}`...")
        lastHook.editOriginalEmbeds(lastEmbed.build()).complete()

        //======= User =======
        val optionalDiscordUser = checkForUser(id)
        if (optionalDiscordUser != null) {
            discordUser = optionalDiscordUser
            selectedType = AdminSearchType.USER
            generateDiscordUser(discordUser)
            lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(getDiscordMenu(AdminSearchType.USER))
                .queueAndRegisterInteraction(lastHook, command, arrayListOf(DiscordCommand.EventRegisterType.SELECT), true)
            return
        }

        //======= Server =======
    }

    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent): Boolean {
        lastHook = event.deferEdit().complete()
        val page = event.getSelection()

        when (selectedType) {
            AdminSearchType.USER -> generateDiscordUser(discordUser, page)
            AdminSearchType.SERVER -> TODO()
        }

        val actionRows = getDiscordMenu(selectedType, page)
        lastHook.editOriginalEmbeds(lastEmbed.build()).setActionRows(actionRows).queue()

        return true
    }

    override fun onSelectMenuInteractionTimeout() {
        lastHook.editOriginalEmbeds(lastEmbed.build()).removeActionRows().queue()
    }

    private fun checkForUser(id: String) = try {
        shardManager.retrieveUserById(id).complete()
    } catch (e: Exception) {
        try {
            shardManager.getUserByTag(id)
        } catch (e: Exception) {
            null
        }
    }

    private fun checkForServer(id: String): Guild? {
        return if(id.isEmpty()){
            null
        } else {
            try {
                shardManager.getGuildById(id)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun checkForRole(id: String): Role? {
        return if(id.isEmpty()){
            null
        } else {
            try {
                shardManager.getRoleById(id)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun checkForChannel(id: String): GuildChannel? {
        return if(id.isEmpty()){
            null
        } else {
            try {
                shardManager.getChannelById(GuildChannel::class.java, id)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun generateDiscordUser(discordUser: User, page: String = "OVERVIEW") {
        lastEmbed.clearFields()
        lastEmbed.setDescription("Found Discord User **${discordUser.asTag}**\nwith ID: ``${discordUser.id}``")
        lastEmbed.setThumbnail(discordUser.avatarUrl)

        adminSearchPageDataProviders.firstOrNull { it.supportedTypes.contains(AdminSearchType.USER) && it.pageId == page }
            ?.onUserRequest(discordUser, lastEmbed)
    }

    private fun getDiscordMenu(type: AdminSearchType, page: String = "OVERVIEW"): ActionRow {
        val menu = SelectMenu.create("menu:type")
            .setRequiredRange(1, 1)

        adminSearchPageDataProviders.filter { it.supportedTypes.contains(type) }.forEach {
            menu.addOptions(SelectOption.of(it.pageName, it.pageId).withDefault(it.pageId == page))
        }
        return ActionRow.of(menu.build())
    }

    override fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        val arg = event.getOptionAsString("args", "")!!

        val user = checkForUser(arg)
        if(user != null){
            event.replyChoices(net.dv8tion.jda.api.interactions.commands.Command.Choice(user.asTag + " (User)", arg)).queue()
            return
        }

        val server = checkForServer(arg)
        if(server != null){
            event.replyChoices(net.dv8tion.jda.api.interactions.commands.Command.Choice(server.name + " (Server)", arg)).queue()
            return
        }

        val role = checkForRole(arg)
        if(role != null){
            event.replyChoices(net.dv8tion.jda.api.interactions.commands.Command.Choice(role.name + " (Role)", arg)).queue()
            return
        }

        val channel = checkForChannel(arg)
        if(channel != null){
            event.replyChoices(net.dv8tion.jda.api.interactions.commands.Command.Choice(channel.name + " (Channel)", arg)).queue()
            return
        }

        event.replyChoices().queue()
    }

    enum class AdminSearchType {
        USER, SERVER
    }
}
