package io.viascom.discord.bot.aluna.bot.command.systemcommand.adminsearch

import io.viascom.discord.bot.aluna.bot.command.systemcommand.AdminSearchDataProvider
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import org.springframework.stereotype.Component

@Component
class AdminSearchServerEmotesPage : AdminSearchPageDataProvider(
    "EMOTES",
    "Emotes",
    arrayListOf(AdminSearchDataProvider.AdminSearchType.SERVER)
) {

    override fun onServerRequest(discordServer: Guild, embedBuilder: EmbedBuilder) {
        embedBuilder.clearFields()

        var text = ""
        var isFirst = true

        discordServer.retrieveEmotes().complete()

        discordServer.emotes.forEach {
            val newElement = "${it.asMention}  `${it.asMention}`"
            if (text.length + newElement.length >= 1000) {
                embedBuilder.addField(if (isFirst) "Emotes (${discordServer.emotes.size})" else "", text, false)
                text = ""
                isFirst = false
            }
            text += "\n" + newElement
        }

        if (text.isNotEmpty()) {
            embedBuilder.addField(if (isFirst) "Emotes (${discordServer.emotes.size})" else "", text, false)
        }

    }

}
