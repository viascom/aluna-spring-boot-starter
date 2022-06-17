package io.viascom.discord.bot.aluna.model

import io.viascom.discord.bot.aluna.bot.DiscordCommand
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ScheduledFuture
import kotlin.reflect.KClass

class ObserveCommandInteraction(
    val command: KClass<out DiscordCommand>,
    val uniqueId: String?,
    val startTime: LocalDateTime,
    val duration: Duration,
    val stayActive: Boolean = false,
    val additionalData: HashMap<String, Any?> = hashMapOf(),
    val authorIds: ArrayList<String>? = null,
    val commandUserOnly: Boolean = false,
    var timeoutTask: ScheduledFuture<*>? = null
) {
}
