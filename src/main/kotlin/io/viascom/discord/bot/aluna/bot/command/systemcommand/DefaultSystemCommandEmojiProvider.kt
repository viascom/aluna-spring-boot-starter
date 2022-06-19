package io.viascom.discord.bot.aluna.bot.command.systemcommand

import net.dv8tion.jda.api.entities.Emoji

class DefaultSystemCommandEmojiProvider : SystemCommandEmojiProvider {
    override fun loadingEmoji(): Emoji = Emoji.fromUnicode("\uD83D\uDD04")

    override fun tickEmoji(): Emoji = Emoji.fromUnicode("✅")

    override fun crossEmoji(): Emoji = Emoji.fromUnicode("⛔")

    override fun channelEmoji(): Emoji = Emoji.fromUnicode("\uD83D\uDCC1")

    override fun channelLockedEmoji(): Emoji = Emoji.fromUnicode("\uD83D\uDD12")

    override fun voiceChannelEmoji(): Emoji = Emoji.fromUnicode("\uD83D\uDD08")

    override fun newsEmoji(): Emoji = Emoji.fromUnicode("\uD83D\uDCF0")

    override fun stageChannelEmoji(): Emoji = Emoji.fromUnicode("\uD83D\uDCE3")

    override fun threadChannelEmoji(): Emoji = Emoji.fromUnicode("\uD83D\uDCC1")

    override fun emptyEmoji(): Emoji = Emoji.fromUnicode("\uD83D\uDD33")
}
