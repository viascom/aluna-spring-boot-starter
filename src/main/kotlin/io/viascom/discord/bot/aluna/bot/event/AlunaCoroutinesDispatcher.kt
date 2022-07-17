package io.viascom.discord.bot.aluna.bot.event

import io.viascom.discord.bot.aluna.util.AlunaThreadPool
import kotlinx.coroutines.asCoroutineDispatcher

object AlunaCoroutinesDispatcher {

    val executor = AlunaThreadPool.getDynamicThreadPool(
        2,
        (Runtime.getRuntime().availableProcessors()).coerceAtLeast(2),
        java.time.Duration.ofSeconds(30),
        false,
        "Aluna-Coroutines-%d"
    ).asCoroutineDispatcher()

}