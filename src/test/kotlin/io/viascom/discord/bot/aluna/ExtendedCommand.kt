package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.handler.DiscordCommand

abstract class ExtendedCommand(name: String, description: String, observeAutoComplete: Boolean = false) : DiscordCommand(name, description, observeAutoComplete)
