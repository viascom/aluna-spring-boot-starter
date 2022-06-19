package io.viascom.discord.bot.aluna.bot

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent

interface DiscordInteractionHandler {

    var uniqueId: String
    var author: User
    var discordBot: DiscordBot

    val interactionName: String

    fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean
    fun onButtonInteractionTimeout(additionalData: HashMap<String, Any?>)

    fun onSelectMenuInteraction(event: SelectMenuInteractionEvent, additionalData: HashMap<String, Any?>): Boolean
    fun onSelectMenuInteractionTimeout(additionalData: HashMap<String, Any?>)

    fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean
    fun onModalInteractionTimeout(additionalData: HashMap<String, Any?>)
}
