package io.viascom.discord.bot.aluna.bot.command.systemcommand

import net.dv8tion.jda.api.entities.emoji.Emoji

public interface SystemCommandEmojiProvider {

    public fun loadingEmoji(): Emoji
    public fun tickEmoji(): Emoji
    public fun crossEmoji(): Emoji
    public fun channelEmoji(): Emoji
    public fun channelLockedEmoji(): Emoji
    public fun voiceChannelEmoji(): Emoji
    public fun newsEmoji(): Emoji
    public fun stageChannelEmoji(): Emoji
    public fun threadChannelEmoji(): Emoji
    public fun categoryEmoji(): Emoji
    public fun emptyEmoji(): Emoji

}
