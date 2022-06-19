package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.Command
import io.viascom.discord.bot.aluna.bot.DiscordUserContextMenu
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.util.createPrimaryButton
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

@Command
class CheckUserContextMenu : DiscordUserContextMenu("test") {
    override fun execute(event: UserContextInteractionEvent) {
        event.reply("Hello").addActionRow(arrayListOf(createPrimaryButton("test", "Test"))).queueAndRegisterInteraction(this)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        event.editMessage("Noice").queue()
        return true
    }
}
