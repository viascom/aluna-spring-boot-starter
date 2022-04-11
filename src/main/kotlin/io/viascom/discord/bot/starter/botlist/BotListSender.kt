package io.viascom.discord.bot.starter.botlist

interface BotListSender {

    fun sendStats(totalServer: Int, totalShards: Int)

}
