package io.viascom.discord.bot.starter.exception

class AlunaPropertiesException(val description: String, val property: String, val value: String, val reason: String) : RuntimeException()
