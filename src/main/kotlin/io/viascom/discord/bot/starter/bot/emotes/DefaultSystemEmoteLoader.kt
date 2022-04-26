package io.viascom.discord.bot.starter.bot.emotes

import net.dv8tion.jda.api.entities.Emoji

class DefaultSystemEmoteLoader : SystemEmoteLoader {
    override fun getCross(): Emoji = AlunaEmote.SMALL_CROSS.toEmoji()

    override fun getCheck(): Emoji = AlunaEmote.SMALL_TICK.toEmoji()
}
