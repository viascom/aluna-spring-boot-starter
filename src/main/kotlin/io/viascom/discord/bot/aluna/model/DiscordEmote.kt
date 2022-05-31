package io.viascom.discord.bot.aluna.model

import net.dv8tion.jda.api.entities.Emoji

interface DiscordEmote {
    val id: String
    val emoteName: String
    val animated: Boolean

    fun asMention(): String {
        val animatedSuffix = if (animated) "a" else ""
        return "<$animatedSuffix:$emoteName:$id>"
    }

    fun toStringShort(spaceAfter: Boolean = false): String {
        val animatedSuffix = if (animated) "a" else ""
        return "<$animatedSuffix:e:$id>" + if (spaceAfter) " " else ""
    }

    fun toReactionCode(): String {
        return "$emoteName:$id"
    }

    fun toEmoji(): Emoji {
        return Emoji.fromEmote(emoteName, id.toLong(), animated)
    }
}
