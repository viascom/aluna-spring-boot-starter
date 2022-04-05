package io.viascom.discord.bot.starter.bot.emotes

import net.dv8tion.jda.api.entities.Emoji

interface SystemEmoteLoader {

    fun getCross(): Emoji
    fun getCheck(): Emoji

}
