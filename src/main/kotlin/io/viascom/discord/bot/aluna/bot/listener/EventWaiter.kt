/*
 * Copyright 2025 Viascom Ltd liab. Co
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.viascom.discord.bot.aluna.bot.listener

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.event.CoroutineEventListener
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.scope.DiscordContext
import io.viascom.discord.bot.aluna.property.AlunaProperties
import io.viascom.discord.bot.aluna.util.AlunaThreadPool
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GatewayPingEvent
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.internal.utils.Checks
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.reflect.full.isSuperclassOf

@Service
@ConditionalOnJdaEnabled
public class EventWaiter(
    alunaProperties: AlunaProperties
) : CoroutineEventListener {

    private val logger = LoggerFactory.getLogger(javaClass)

    @get:JvmSynthetic
    internal val waitingEvents: ConcurrentHashMap<Class<*>, ConcurrentHashMap<String, CopyOnWriteArrayList<WaitingEvent<GenericEvent>>>> = ConcurrentHashMap()

    @JvmSynthetic
    internal val scheduledThreadPool = AlunaThreadPool.getScheduledThreadPool(
        1,
        alunaProperties.thread.eventWaiterTimeoutScheduler,
        Duration.ofSeconds(30),
        "Aluna-Waiter-Timeout-Pool-%d",
        true
    )

    override suspend fun onEvent(event: GenericEvent) {
        var eventClass: Class<*>? = event.javaClass

        while (eventClass != null) {
            val workClass = eventClass

            //GatewayPingEvent is filtered out because this happens to often and generates unnecessary load
            if (workClass == GatewayPingEvent::class.java) {
                return
            }

            if (waitingEvents.containsKey(key = workClass)) {
                waitingEvents[workClass]!!.forEach {
                    val waitingEventElements = it.value
                    val elementsToRemove = arrayListOf<Int>()
                    waitingEventElements.forEach { waitingEvent ->
                        val remove = if (waitingEvent.attempt(event)) {
                            AlunaDispatchers.InternalScope.launch {
                                try {
                                    if (SlashCommandInteractionEvent::class.isSuperclassOf(event::class)) {
                                        event as SlashCommandInteractionEvent
                                        DiscordContext.setDiscordState(event.user.id, event.guild?.id, DiscordContext.Type.INTERACTION)
                                    }
                                    launch(AlunaDispatchers.Interaction) { waitingEvent.execute(event) }
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

                    //Cleanup waitingEvents hash map
                    elementsToRemove.forEach { codes ->
                        waitingEvents[workClass]?.get(it.key)?.removeIf { it.hashCode() == codes }
                    }

                    //Cleanup waitingEvents hash map
                    if (waitingEvents[workClass]?.get(it.key)?.isEmpty() == true) {
                        waitingEvents[workClass]?.remove(it.key)
                    }

                }

                //Cleanup waitingEvents hash map
                if (waitingEvents[workClass]?.isEmpty() == true) {
                    waitingEvents.remove(workClass)
                }

            }

            eventClass = eventClass.superclass
        }
    }

    /**
     * Remove event waiters by id
     *
     * @param id ID of the waiter
     * @param isTimeout Defines if the timeout action should be called
     */
    @JvmOverloads
    public fun removeWaiter(id: String, isTimeout: Boolean = false) {
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
                                logger.debug("Could not run timeout action for event wait $id\n" + e.stackTraceToString())
                            }
                        }
                    }
                    it.value.remove(id)
                }
            }
        }
    }

    /**
     * Suspend event waiters by id. During suspension, the waiter will not be triggered in case of a matching event.
     *
     * @param id ID of the waiter
     */
    public fun suspendWaiter(id: String) {
        waitingEvents.filter { it.value.containsKey(id) }.forEach {
            it.value[id]?.forEach { it.suspended = true }
        }
    }

    /**
     * Unsuspend event waiters by id.
     *
     * @param id ID of the waiter
     */
    public fun unsuspendWaiter(id: String) {
        waitingEvents.filter { it.value.containsKey(id) }.forEach {
            it.value[id]?.forEach { it.suspended = false }
        }
    }

    /**
     * Override timeout of event waiters.
     *
     * @param id ID of the waiter
     * @param timeout New timeout duration. If null, the old duration of the waiter timeout will be used.
     */
    @JvmOverloads
    public fun overrideTimeout(id: String, timeout: Duration? = null) {
        waitingEvents.filter { it.value.containsKey(id) }.forEach { entry ->
            entry.value[id]?.forEach {
                //Cancel current task
                it.timeoutTask?.cancel(false)

                //Create new task
                it.timeoutTask = scheduledThreadPool.schedule({
                    AlunaDispatchers.InternalScope.launch {
                        if (waitingEvents.containsKey(it.type) && waitingEvents[it.type]!!.containsKey(id) &&
                            waitingEvents[it.type]!![id]!!.remove(it) && it.timeoutAction != null
                        ) {
                            launch(AlunaDispatchers.Interaction) { it.timeoutAction.invoke() }
                        }
                    }

                }, timeout?.seconds ?: it.timeout?.seconds ?: Duration.ofMinutes(14).seconds, TimeUnit.SECONDS)

            }
        }
    }

    /**
     * Remove timeout of event waiters.
     *
     * @param id ID of the waiter
     */
    public fun removeTimeout(id: String) {
        waitingEvents.filter { it.value.containsKey(id) }.forEach { entry ->
            entry.value[id]?.forEach {
                //Cancel current task
                it.timeoutTask?.cancel(false)
            }
        }
    }

    public fun <T : GenericEvent> hasWaiter(id: String, type: Class<in T>? = null): Boolean =
        waitingEvents.filter { (it.key == type || type == null) && it.value.containsKey(id) }.isNotEmpty()

    public inner class WaitingEvent<in T : GenericEvent>(
        private val condition: Predicate<T>,
        private val action: Consumer<T>,
        public val stayActive: Boolean = false,
        public val type: Class<in T>,
        public val timeout: Duration?,
        public val timeoutAction: (() -> Unit)?,
        public var timeoutTask: ScheduledFuture<*>?
    ) {

        public var suspended: Boolean = false

        public fun attempt(event: T): Boolean {
            return !suspended && condition.test(event)
        }

        public fun execute(event: T) {
            action.accept(event)
        }
    }

    /**
     * Wait for interaction
     *
     * @param T extension of GenericComponentInteractionCreateEvent
     * @param id ID of this interaction wait. Can be used to remove it if needed.
     * @param type Type of the interaction
     * @param message Message to which this interaction is bound to.
     * @param action Action to execute for this interaction.
     * @param condition Additional condition to match before executing the action.
     * @param timeout Duration before the waiter gets canceled.
     * @param timeoutAction Action to execute if the timeout is exceeded..
     * @param stayActive Defines if the waiter should stay active after the execution of the action.
     */
    @JvmOverloads
    public fun <T : GenericComponentInteractionCreateEvent> waitForInteraction(
        id: String = NanoId.generate(), type: Class<T>, message: Message, action: Consumer<T>, condition: Predicate<T>? = null,
        timeout: Duration? = Duration.ofMinutes(14), timeoutAction: (() -> (Unit))? = {}, stayActive: Boolean = false
    ) {
        waitForEvent(
            id,
            type,
            action,
            { !it.user.isBot && it.messageIdLong == message.idLong && (condition == null || condition.test(it)) },
            timeout,
            timeoutAction,
            stayActive
        )
    }

    /**
     * Wait for interaction
     *
     * @param T extension of ModalInteractionEvent
     * @param id ID of this interaction wait. Can be used to remove it if needed.
     * @param type Type of the interaction
     * @param action Action to execute for this interaction.
     * @param condition Additional condition to match before executing the action.
     * @param timeout Duration before the waiter gets canceled.
     * @param timeoutAction Action to execute if the timeout is exceeded..
     * @param stayActive Defines if the waiter should stay active after the execution of the action.
     */
    @JvmOverloads
    public fun <T : ModalInteractionEvent> waitForInteraction(
        id: String = NanoId.generate(), type: Class<T>, action: Consumer<T>, condition: Predicate<T>? = null,
        timeout: Duration? = Duration.ofMinutes(14), timeoutAction: (() -> (Unit))? = {}, stayActive: Boolean = false
    ) {
        waitForEvent(
            id,
            type,
            action,
            { !it.user.isBot && (condition == null || condition.test(it)) },
            timeout,
            timeoutAction,
            stayActive
        )
    }

    /**
     * Wait for interaction
     *
     * @param T extension of GenericComponentInteractionCreateEvent
     * @param id ID of this interaction wait. Can be used to remove it if needed.
     * @param type Type of the interaction
     * @param hook Hook to which this interaction is bound to.
     * @param action Action to execute for this interaction.
     * @param condition Additional condition to match before executing the action.
     * @param timeout Duration before the waiter gets canceled.
     * @param timeoutAction Action to execute if the timeout is exceeded..
     * @param stayActive Defines if the waiter should stay active after the execution of the action.
     */
    @JvmOverloads
    public fun <T : GenericComponentInteractionCreateEvent> waitForInteraction(
        id: String = NanoId.generate(), type: Class<T>, hook: InteractionHook, action: Consumer<T>, condition: Predicate<T>? = null,
        timeout: Duration? = Duration.ofMinutes(14), timeoutAction: (() -> (Unit))? = {}, stayActive: Boolean = false
    ) {
        waitForEvent(
            id,
            type,
            action,
            { !it.user.isBot && it.messageIdLong == hook.callbackResponse.message?.idLong && (condition == null || condition.test(it)) },
            timeout,
            timeoutAction,
            stayActive
        )
    }

    /**
     * Wait for event
     *
     * @param T extension of Event
     * @param id ID of this interaction wait. Can be used to remove it if needed.
     * @param type Type of the interaction
     * @param action Action to execute for this interaction.
     * @param condition Additional condition to match before executing the action.
     * @param timeout Duration before the waiter gets canceled.
     * @param timeoutAction Action to execute if the timeout is exceeded..
     * @param stayActive Defines if the waiter should stay active after the execution of the action.
     */
    @JvmOverloads
    public fun <T : Event> waitForEvent(
        id: String = NanoId.generate(),
        type: Class<T>,
        action: Consumer<T>,
        condition: Predicate<T>,
        timeout: Duration? = Duration.ofMinutes(14),
        timeoutAction: (() -> Unit)? = null,
        stayActive: Boolean = false
    ) {
        Checks.notNull(type, "The provided type")
        Checks.notNull(condition, "The provided condition predicate")
        Checks.notNull(action, "The provided action consumer")

        val we = WaitingEvent(condition, action, stayActive, type, timeout, timeoutAction, null)

        if (timeout != null) {
            we.timeoutTask = scheduledThreadPool.schedule({
                AlunaDispatchers.InternalScope.launch {
                    if (waitingEvents.containsKey(type) && waitingEvents[type]!!.containsKey(id) && waitingEvents[type]!![id]!!.remove(we)
                    ) {
                        launch(AlunaDispatchers.Interaction) { timeoutAction?.invoke() }
                    }
                }

            }, timeout.seconds, TimeUnit.SECONDS)
        }
        @Suppress("UNCHECKED_CAST") waitingEvents.computeIfAbsent(type) { ConcurrentHashMap() }.computeIfAbsent(id) { CopyOnWriteArrayList() }.add(we as WaitingEvent<GenericEvent>)
    }
}
