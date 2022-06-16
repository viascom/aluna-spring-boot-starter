package io.viascom.discord.bot.aluna.property

class AlunaDebugProperties {

    /**
     * Show time elapsed for commands
     */
    var useStopwatch: Boolean = true

    /**
     * Show hash code for commands
     */
    var showHashCode: Boolean = false

    /**
     * Enable Debug Configuration Log.
     * If enabled and not production mode, Aluna will print a configuration block in the log which contains some selected settings and an invitation link for the bot itself.
     */
    var enableDebugConfigurationLog: Boolean = true
}
