package io.viascom.discord.bot.starter.bot.command.systemcommand.adminsearch

import io.viascom.discord.bot.starter.bot.command.systemcommand.AdminSearchDataProvider
import io.viascom.discord.bot.starter.bot.emotes.AlunaEmote
import io.viascom.discord.bot.starter.property.AlunaProperties
import io.viascom.discord.bot.starter.util.DiscordFormatUtil
import io.viascom.discord.bot.starter.util.toDiscordTimestamp
import io.viascom.discord.bot.starter.util.toHex
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component

@Component
class AdminSearchOverviewPage(
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties
) : AdminSearchPageDataProvider(
    "OVERVIEW",
    "Overview",
    arrayListOf(AdminSearchDataProvider.AdminSearchType.USER, AdminSearchDataProvider.AdminSearchType.SERVER)
) {

    override fun onUserRequest(discordUser: User, embedBuilder: EmbedBuilder) {
        val mutualServers = shardManager.getMutualGuilds(discordUser)

        embedBuilder.addField("Discord-ID", discordUser.id, true)
            .addField("Discord-Tag", discordUser.asTag, true)
            .addField("Is Bot", (if (discordUser.isBot) AlunaEmote.SMALL_TICK else AlunaEmote.SMALL_CROSS).asMention(), true)
            .addField("Flags", discordUser.flags.joinToString(", ") { it.getName() }, true)
            .addField("Time Created", discordUser.timeCreated.toDiscordTimestamp(DiscordFormatUtil.TimestampFormat.SHORT_DATE_TIME), true)
            .addField(
                "On Support Server",
                (if (mutualServers.any { it.id == alunaProperties.command.systemCommand.supportServer }) AlunaEmote.SMALL_TICK else AlunaEmote.SMALL_CROSS).asMention(),
                true
            )
            .addField("Avatar-URL", "[Link](${discordUser.avatarUrl})", true)
            .addField("Banner-URL", "[Link](${discordUser.retrieveProfile().complete().bannerUrl})", true)
            .addField("Accent Color", "`${discordUser.retrieveProfile().complete().accentColor?.toHex() ?: "n/a"}`", true)
    }

    override fun onServerRequest(embedBuilder: EmbedBuilder) {

    }
}
