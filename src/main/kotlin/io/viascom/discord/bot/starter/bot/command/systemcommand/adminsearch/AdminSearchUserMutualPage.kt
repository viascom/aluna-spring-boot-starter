package io.viascom.discord.bot.starter.bot.command.systemcommand.adminsearch

import io.viascom.discord.bot.starter.bot.command.systemcommand.AdminSearchDataProvider
import io.viascom.discord.bot.starter.bot.emotes.AlunaEmote
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component

@Component
class AdminSearchUserMutualPage(
    private val shardManager: ShardManager
) : AdminSearchPageDataProvider(
    "MUTUAL",
    "Mutual Servers",
    arrayListOf(AdminSearchDataProvider.AdminSearchType.USER)
) {

    override fun onUserRequest(discordUser: User, embedBuilder: EmbedBuilder) {
        val mutualServers = shardManager.getMutualGuilds(discordUser)

        embedBuilder.clearFields()

        var text = ""
        var isFirst = true

        mutualServers.forEach {
            val newElement = "- ${it.name}${if (it.ownerId == discordUser.id) " ${AlunaEmote.SMALL_TICK.asMention()}" else ""} (`${it.id}`)"
            if (text.length + newElement.length >= 1000) {
                embedBuilder.addField(if (isFirst) "Mutual Servers (${mutualServers.size})" else "", text, false)
                text = ""
                isFirst = false
            }
            text += "\n" + newElement
        }

        if (text.isNotEmpty()) {
            embedBuilder.addField(if (isFirst) "Mutual Servers (${mutualServers.size})" else "", text, false)
        }
    }

}