package io.viascom.discord.bot.starter.scriptengine.exception

class ScriptMemoryAbuseException(
    message: String?, scriptKilled: Boolean,
    throwable: Throwable?
) : ScriptAbuseException(message, scriptKilled, throwable) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
