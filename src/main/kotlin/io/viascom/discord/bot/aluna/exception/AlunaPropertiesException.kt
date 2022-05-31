package io.viascom.discord.bot.aluna.exception

internal class AlunaPropertiesException(val description: String, val property: String, val value: String, val reason: String) : RuntimeException()
