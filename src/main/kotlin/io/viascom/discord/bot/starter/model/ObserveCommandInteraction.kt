package io.viascom.discord.bot.starter.model

import io.viascom.discord.bot.starter.bot.handler.DiscordCommand
import net.dv8tion.jda.api.interactions.InteractionHook
import java.time.Duration
import java.time.LocalDateTime
import kotlin.reflect.KClass

class ObserveCommandInteraction(
    val command: KClass<out DiscordCommand>,
    val startTime: LocalDateTime,
    val duration: Duration,
    val stayActive: Boolean = false,
    val hook: InteractionHook? = null,
    val additionalData: HashMap<String, Any?> = hashMapOf()
) {
}