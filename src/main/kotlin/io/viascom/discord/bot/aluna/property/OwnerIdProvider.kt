package io.viascom.discord.bot.aluna.property

/**
 * Owner id provider
 *
 */
interface OwnerIdProvider {

    /**
     * Get owner ids.
     * Make sure this method does not run to long as it may be used before an event acknowledgement.
     *
     * @return List of owners (Discord user ids)
     */
    fun getOwnerIds(): ArrayList<Long>

}
