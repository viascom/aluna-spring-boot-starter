package io.viascom.discord.bot.aluna.property

/**
 * Moderator id provider
 *
 */
interface ModeratorIdProvider {

    /**
     * Get moderator ids.
     * Make sure this method does not run to long as it may be used before an event acknowledgement.
     *
     * @return List of moderators (Discord user ids)
     */
    fun getModeratorIds(): ArrayList<Long>

    /**
     * Get moderator ids based on a command path.
     * Make sure this method does not run to long as it may be used before an event acknowledgement.
     *
     * @return List of moderators for the given command path (Discord user ids)
     */
    fun getModeratorIdsForCommandPath(commandPath: String): ArrayList<Long>

}
