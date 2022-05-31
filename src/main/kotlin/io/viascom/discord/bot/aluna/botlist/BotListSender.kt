package io.viascom.discord.bot.aluna.botlist

interface BotListSender {

    fun sendStats(totalServer: Int, totalShards: Int)

}
