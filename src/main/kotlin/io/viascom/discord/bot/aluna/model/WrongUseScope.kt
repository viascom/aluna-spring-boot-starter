package io.viascom.discord.bot.aluna.model

class WrongUseScope(var serverOnly: Boolean = false, var subCommandServerOnly: Boolean = false) {
    val wrongUseScope: Boolean
        get() = serverOnly || subCommandServerOnly
}
