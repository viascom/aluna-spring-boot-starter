package io.viascom.discord.bot.starter.bot.command.systemcommand.adminsearch

import io.viascom.discord.bot.starter.bot.command.systemcommand.AdminSearchDataProvider
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*

abstract class AdminSearchPageDataProvider(
    val pageId: String,
    val pageName: String,
    val supportedTypes: ArrayList<AdminSearchDataProvider.AdminSearchType>
) {

    open fun onUserRequest(discordUser: User, embedBuilder: EmbedBuilder) {}
    open fun onServerRequest(discordServer: Guild, embedBuilder: EmbedBuilder) {}
    open fun onRoleRequest(discordRole: Role, embedBuilder: EmbedBuilder) {}
    open fun onChannelRequest(discordChannel: Channel, embedBuilder: EmbedBuilder) {}
    open fun onEmoteRequest(discordEmote: Emote, embedBuilder: EmbedBuilder) {}
}
