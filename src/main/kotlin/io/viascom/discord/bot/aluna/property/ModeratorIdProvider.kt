package io.viascom.discord.bot.aluna.property

interface ModeratorIdProvider {

    fun getModeratorIds(): ArrayList<Long>
    fun getModeratorIdsForCommandPath(commandPath: String): ArrayList<Long>

}
