package io.viascom.discord.bot.aluna.property

interface OwnerIdProvider {

    fun getOwnerIds(): ArrayList<Long>

}
