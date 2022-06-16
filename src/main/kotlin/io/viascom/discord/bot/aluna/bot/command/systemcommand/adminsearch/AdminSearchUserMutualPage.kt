package io.viascom.discord.bot.aluna.bot.command.systemcommand.adminsearch

import io.viascom.discord.bot.aluna.bot.command.systemcommand.AdminSearchDataProvider
import io.viascom.discord.bot.aluna.bot.emotes.AlunaEmote
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component

@Component
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
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
