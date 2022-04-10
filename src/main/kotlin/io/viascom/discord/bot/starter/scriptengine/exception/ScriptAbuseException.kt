package io.viascom.discord.bot.starter.scriptengine.exception

open class ScriptAbuseException(
    message: String?,
    /**
     * Check if script when asked exited nicely, or not.
     *
     *
     * Note, killint java thread is very dangerous to VM health.
     *
     *
     * @return `true` when evaluator thread was finished by
     * [Thread.stop] method, `false` when only
     * [Thread.interrupt] was used
     */
    val isScriptKilled: Boolean,
    throwable: Throwable?
) : RuntimeException(message, throwable) {
    override val message: String
        get() = if (isScriptKilled) {
            super.message + " The operation could NOT be gracefully interrupted."
        } else {
            super.message!!
        }

    companion object {
        private const val serialVersionUID = 1L
    }
}
