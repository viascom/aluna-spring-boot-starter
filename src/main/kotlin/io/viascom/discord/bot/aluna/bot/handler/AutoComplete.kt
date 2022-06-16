package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.configuration.Experimental
import io.viascom.discord.bot.aluna.configuration.scope.CommandScoped
import org.springframework.stereotype.Component

@Component
@CommandScoped
@Experimental("This is still in development")
annotation class AutoComplete
