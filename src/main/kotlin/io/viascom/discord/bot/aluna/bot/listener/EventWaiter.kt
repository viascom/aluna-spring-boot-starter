package io.viascom.discord.bot.aluna.bot.listener

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.configuration.scope.DiscordContext
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.AlunaThreadPool
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GatewayPingEvent
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ShutdownEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.internal.utils.Checks
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.reflect.full.isSuperclassOf

@Service
class EventWaiter(
    private val discordBot: DiscordBot,
    private val alunaProperties: AlunaProperties
) : EventListener {

    private var logger = LoggerFactory.getLogger(javaClass)

    internal val waitingEvents: ConcurrentHashMap<Class<*>, ConcurrentHashMap<String, ArrayList<WaitingEvent<GenericEvent>>>> = ConcurrentHashMap()

    internal val executorThreadPool = AlunaThreadPool.getDynamicThreadPool(
        alunaProperties.thread.eventWaiterThreadPoolCount,
        alunaProperties.thread.eventWaiterThreadPoolTtl,
        "Aluna-Waiter-Pool-%d"
    )
    internal val scheduledThreadPool =
        AlunaThreadPool.getFixedScheduledThreadPool(alunaProperties.thread.eventWaiterTimeoutScheduler, "Aluna-Waiter-Timeout-Pool-%d", true)

    override fun onEvent(event: GenericEvent) {
        var eventClass: Class<*>? = event.javaClass

        // Runs at least once for the fired Event, at most
        // once for each superclass (excluding Object) because
        // Class#getSuperclass() returns null when the superclass
        // is primitive, void, or (in this case) Object.
        while (eventClass != null) {
            val workClass = eventClass

            if (workClass == GatewayPingEvent::class.java) {
                return
            }

            if (waitingEvents.containsKey(key = workClass)) {
                // WaitingEvent#attempt invocations that return true have passed their condition tests
                // and executed the action. We filter the ones that return false out of the toRemove and
                // remove them all from the set.
                waitingEvents[workClass]!!.forEach {
                    val waitingEventElements = it.value
                    val elementsToRemove = arrayListOf<Int>()
                    waitingEventElements.forEach { waitingEvent ->
                        val remove = if (waitingEvent.attempt(event)) {
                            executorThreadPool.submit {
                                try {
                                    if (SlashCommandInteractionEvent::class.isSuperclassOf(event::class)) {
                                        event as SlashCommandInteractionEvent
                                        DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.COMMAND)
                                    }
                                    waitingEvent.execute(event)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    logger.error(e.message)
                                }
                            }
                            !waitingEvent.stayActive
                        } else {
                            false
                        }

                        if (remove) {
                            elementsToRemove.add(waitingEvent.hashCode())
                        }
                    }

                    elementsToRemove.forEach { codes ->
                        waitingEvents[workClass]?.get(it.key)?.removeIf { it.hashCode() == codes }
                    }

                    if (waitingEvents[workClass]?.get(it.key)?.isEmpty() == true) {
                        waitingEvents[workClass]?.remove(it.key)
                    }

                }

                if (waitingEvents[workClass]?.isEmpty() == true) {
                    waitingEvents.remove(workClass)
                }

            }

            if (event is ShutdownEvent) {
                executorThreadPool.shutdown()
            }
            eventClass = eventClass.superclass
        }
    }

    fun removeEvents(id: String, isTimeout: Boolean = false) {
        waitingEvents.forEach {
            run {
                if (it.value.containsKey(id)) {
                    if (isTimeout) {
                        it.value[id]?.forEach { entry ->
                            try {
                                if (entry.timeoutTask?.cancel(false) == true) {
                                    entry.timeoutAction?.invoke()
                                }
                            } catch (e: Exception) {
                                logger.debug("Could not run timeout action for event wait $id\"\n" + e.stackTraceToString())
                            }
                        }
                    }
                    it.value.remove(id)
                }
            }
        }
    }

    fun suspendEvents(id: String) {
        waitingEvents.forEach {
            run {
                if (it.value.containsKey(id)) {
                    it.value[id]?.forEach { it.suspended = true }
                }
            }
        }
    }

    fun unsuspendEvents(id: String) {
        waitingEvents.forEach {
            run {
                if (it.value.containsKey(id)) {
                    it.value[id]?.forEach { it.suspended = false }
                }
            }
        }
    }

    private fun <T : GenericComponentInteractionCreateEvent> overrideTimeOut(uniqueId: String, type: Class<T>) {
        TODO()
    }

    fun isShutdown(): Boolean = executorThreadPool.isShutdown

    inner class WaitingEvent<in T : GenericEvent>(
        private val condition: Predicate<T>,
        private val action: Consumer<T>,
        val stayActive: Boolean = false,
        val user: User? = null,
        val server: Guild? = null,
        val timeoutAction: (() -> Unit)?,
        var timeoutTask: ScheduledFuture<*>?
    ) {

        var suspended = false

        fun attempt(event: T): Boolean {
            if (!suspended && condition.test(event)) {
                return true
            }
            return false
        }

        fun execute(event: T) {
            action.accept(event)
        }
    }

    fun <T : GenericComponentInteractionCreateEvent> waitForInteraction(
        id: String = NanoIdUtils.randomNanoId(), type: Class<T>, message: Message, action: Consumer<T>, condition: Predicate<T>? = null,
        timeout: Duration? = Duration.ofMinutes(15), timeoutAction: (() -> (Unit))? = {}, stayActive: Boolean = false, user: User? = null, server: Guild? = null
    ) {
        waitForEvent(
            id,
            type,
            action,
            { !it.user.isBot && it.messageIdLong == message.idLong && (condition == null || condition.test(it)) },
            timeout,
            timeoutAction,
            stayActive,
            user,
            server
        )
    }

    fun <T : ModalInteractionEvent> waitForInteraction(
        id: String = NanoIdUtils.randomNanoId(), type: Class<T>, action: Consumer<T>, condition: Predicate<T>? = null,
        timeout: Duration? = Duration.ofMinutes(15), timeoutAction: (() -> (Unit))? = {}, stayActive: Boolean = false, user: User? = null, server: Guild? = null
    ) {
        waitForEvent(
            id,
            type,
            action,
            { !it.user.isBot && (condition == null || condition.test(it)) },
            timeout,
            timeoutAction,
            stayActive,
            user,
            server
        )
    }

    fun <T : GenericComponentInteractionCreateEvent> waitForInteraction(
        id: String = NanoIdUtils.randomNanoId(), type: Class<T>, hook: InteractionHook, action: Consumer<T>, condition: Predicate<T>? = null,
        timeout: Duration? = Duration.ofMinutes(15), timeoutAction: (() -> (Unit))? = {}, stayActive: Boolean = false, user: User? = null, server: Guild? = null
    ) {
        waitForEvent(
            id,
            type,
            action,
            { !it.user.isBot && it.messageIdLong == hook.retrieveOriginal().complete().idLong && (condition == null || condition.test(it)) },
            timeout,
            timeoutAction,
            stayActive,
            user,
            server
        )
    }

    fun <T : Event> waitForEvent(
        id: String = NanoIdUtils.randomNanoId(), classType: Class<T>, action: Consumer<T>, condition: Predicate<T>,
        timeout: Duration? = Duration.ofMinutes(15), timeoutAction: (() -> (Unit))? = {}, stayActive: Boolean = false, user: User? = null, server: Guild? = null
    ) {
        Checks.check(!isShutdown(), "Attempted to register a WaitingEvent while the EventWaiter's thread pool was already shut down!")
        Checks.notNull(classType, "The provided class type")
        Checks.notNull(condition, "The provided condition predicate")
        Checks.notNull(action, "The provided action consumer")

        val we = WaitingEvent(condition, action, stayActive, user, server, timeoutAction, null)

        if (timeout != null) {
            we.timeoutTask = scheduledThreadPool.schedule({
                discordBot.asyncExecutor.execute {
                    if (waitingEvents.containsKey(classType) && waitingEvents[classType]!!.containsKey(id)) {
                        if (waitingEvents[classType]!![id]!!.remove(we) && timeoutAction != null) {
                            timeoutAction.invoke()
                        }
                    }
                }

            }, timeout.seconds, TimeUnit.SECONDS)
        }
        @Suppress("UNCHECKED_CAST")
        waitingEvents.computeIfAbsent(classType) { ConcurrentHashMap() }.computeIfAbsent(id) { arrayListOf() }.add(we as WaitingEvent<GenericEvent>)
    }
}
