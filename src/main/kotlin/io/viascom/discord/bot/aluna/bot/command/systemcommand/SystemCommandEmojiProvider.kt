package io.viascom.discord.bot.aluna.bot.command.systemcommand

import net.dv8tion.jda.api.entities.emoji.Emoji

interface SystemCommandEmojiProvider {

    fun loadingEmoji(): Emoji
    fun tickEmoji(): Emoji
    fun crossEmoji(): Emoji
    fun channelEmoji(): Emoji
    fun channelLockedEmoji(): Emoji
    fun voiceChannelEmoji(): Emoji
    fun newsEmoji(): Emoji
    fun stageChannelEmoji(): Emoji
    fun threadChannelEmoji(): Emoji
    fun emptyEmoji(): Emoji

}
