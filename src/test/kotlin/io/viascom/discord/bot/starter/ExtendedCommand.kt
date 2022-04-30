package io.viascom.discord.bot.starter

import io.viascom.discord.bot.starter.bot.handler.DiscordCommand

abstract class ExtendedCommand(name: String, description: String, observeAutoComplete: Boolean = false) : DiscordCommand(name, description, hashMapOf(),  observeAutoComplete)
