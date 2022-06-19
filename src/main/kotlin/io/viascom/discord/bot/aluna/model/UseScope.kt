package io.viascom.discord.bot.aluna.model

import io.viascom.discord.bot.aluna.configuration.Experimental

enum class UseScope {
    GLOBAL,
    GUILD_ONLY,

    @Experimental("This UseScope is currently not in use")
    GUILD_SPECIFIC
}
