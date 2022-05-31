package io.viascom.discord.bot.aluna.property

class DefaultModeratorIdProvider(private val alunaProperties: AlunaProperties) : ModeratorIdProvider {
    override fun getModeratorIds(): ArrayList<Long> = alunaProperties.modIds
    override fun getModeratorIdsForCommandPath(commandPath: String): ArrayList<Long> = alunaProperties.modIds
}
