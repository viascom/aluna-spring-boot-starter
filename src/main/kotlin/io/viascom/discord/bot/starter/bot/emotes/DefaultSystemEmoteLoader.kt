package io.viascom.discord.bot.starter.bot.emotes

import net.dv8tion.jda.api.entities.Emoji

class DefaultSystemEmoteLoader : SystemEmoteLoader {
    override fun getCross(): Emoji = Emoji.fromMarkdown("❌")

    override fun getCheck(): Emoji = Emoji.fromMarkdown("✅")
}
