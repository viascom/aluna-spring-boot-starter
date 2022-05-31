package io.viascom.discord.bot.aluna.scriptengine.exception

class ScriptCPUAbuseException(string: String?, scriptKilled: Boolean, throwable: Throwable?) : ScriptAbuseException(string, scriptKilled, throwable)
