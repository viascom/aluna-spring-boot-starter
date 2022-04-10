package io.viascom.discord.bot.starter.scriptengine.exception

class ScriptCPUAbuseException(string: String?, scriptKilled: Boolean, throwable: Throwable?) : ScriptAbuseException(string, scriptKilled, throwable)
