package io.viascom.discord.bot.aluna.bot.event

import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object AlunaCoroutinesDispatcher {

    val Default: CoroutineContext
        get() = getDefaultScope().coroutineContext

    val DefaultScope: CoroutineScope
        get() = getDefaultScope()

    val IO: CoroutineContext
        get() = getDefaultIOScope().coroutineContext

    val IOScope: CoroutineScope
        get() = getDefaultIOScope()

    private fun getDefaultScope(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        parent: Job = SupervisorJob(),
        errorHandler: CoroutineExceptionHandler? = null,
        context: CoroutineContext = EmptyCoroutineContext
    ): CoroutineScope {
        val handler = errorHandler ?: CoroutineExceptionHandler { _, throwable ->
            val logger: Logger = LoggerFactory.getLogger(AlunaCoroutinesDispatcher::class.java)
            logger.error("Uncaught exception from coroutine", throwable)
            if (throwable is Error) {
                parent.cancel()
                throw throwable
            }
        }

        return CoroutineScope(dispatcher + parent + handler + context + MDCContext())
    }

    private fun getDefaultIOScope(
        context: CoroutineContext = EmptyCoroutineContext
    ): CoroutineScope = getDefaultScope(
        Dispatchers.IO, context = context
    )

}