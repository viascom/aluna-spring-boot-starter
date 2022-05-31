package io.viascom.discord.bot.aluna.property

class DefaultOwnerIdProvider(private val alunaProperties: AlunaProperties) : OwnerIdProvider {
    override fun getOwnerIds(): ArrayList<Long> = alunaProperties.ownerIds
}
