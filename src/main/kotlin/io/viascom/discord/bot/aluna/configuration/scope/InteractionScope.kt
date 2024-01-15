/*
 * Copyright 2024 Viascom Ltd liab. Co
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

package io.viascom.discord.bot.aluna.configuration.scope

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.InteractionScopedObject
import io.viascom.discord.bot.aluna.bot.handler.DiscordInteractionHandler
import io.viascom.discord.bot.aluna.bot.listener.EventWaiter
import io.viascom.discord.bot.aluna.model.ObserveInteraction
import io.viascom.discord.bot.aluna.util.AlunaThreadPool
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.config.Scope
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.NamedInheritableThreadLocal
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class InteractionScope(private val context: ConfigurableApplicationContext) : Scope {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @JvmSynthetic
    internal val scopedObjects = Collections.synchronizedMap(HashMap<BeanName, HashMap<DiscordStateId, HashMap<UniqueId, ScopedObjectData>>>())

    @JvmSynthetic
    internal var scopedObjectsTimeoutScheduler: ScheduledThreadPoolExecutor

    private val scopedObjectsTimeoutScheduledTask = Collections.synchronizedMap(HashMap<UniqueId, ScheduledFuture<*>>())

    init {
        val scopedObjectsTimeoutScheduler = context.environment.getProperty("aluna.thread.scoped-objects-timeout-scheduler", Int::class.java, 2)
        this.scopedObjectsTimeoutScheduler = AlunaThreadPool.getScheduledThreadPool(
            1,
            scopedObjectsTimeoutScheduler,
            Duration.ofSeconds(30),
            "Aluna-Scoped-Objects-Timeout-Pool-%d",
            true
        )
    }

    override fun get(name: String, objectFactory: ObjectFactory<*>): Any {
        //If state id is not set, a new instance is returned
        if (DiscordContext.discordState?.id == null) {
            logger.debug("[$name] - ${DiscordContext.discordState} -> new instance (because id is null)")
            val newObj = objectFactory.getObject() as InteractionScopedObject
            newObj.uniqueId = ""
            newObj.freshInstance = true
            return newObj
        }

        //If bean was never seen
        val beanData = scopedObjects.getOrElse(name) { null }
        if (beanData == null) {
            scopedObjects[name] = hashMapOf()
        }

        //If discordState.id was never seen
        val discordStateIdData = scopedObjects[name]!!.getOrElse(DiscordContext.discordState!!.id) { null }
        if (discordStateIdData == null) {
            scopedObjects[name]!![DiscordContext.discordState!!.id] = hashMapOf()
        }

        //If uniqueId is present we return the corresponding bean and reset the timeout
        if (DiscordContext.discordState?.uniqueId != null && scopedObjects[name]!![DiscordContext.discordState!!.id]!!.containsKey(DiscordContext.discordState?.uniqueId)) {
            logger.debug("[$name] - ${DiscordContext.discordState} -> found uniqueId and return")
            val data = scopedObjects[name]!![DiscordContext.discordState!!.id]!![DiscordContext.discordState!!.uniqueId]!!
            val timeout = createTimeoutDestroy(
                name,
                data.obj,
                DiscordContext.discordState!!.uniqueId!!,
                loadTimeoutDelay(data.obj, Duration.ofMinutes(14)),
                loadBeanCallOnDestroy(data.obj, true)
            )
            resetTimeoutDestroy(name, DiscordContext.discordState!!.uniqueId!!, timeout)
            resetObserverTimeouts(name, DiscordContext.discordState!!.uniqueId!!, data.obj)
            (data.obj as InteractionScopedObject).freshInstance = false

            if (DiscordContext.discordState?.messageId != null) {
                data.messageId = DiscordContext.discordState!!.messageId
            }

            return data.obj
        }

        //If messageId is present we return the corresponding bean and reset the timeout
        if (DiscordContext.discordState?.messageId != null && scopedObjects[name]!![DiscordContext.discordState!!.id]!!.any { it.value.messageId == DiscordContext.discordState?.messageId }) {
            logger.debug("[$name] - ${DiscordContext.discordState} -> found messageId and return")
            val data = scopedObjects[name]!![DiscordContext.discordState!!.id]!!.values.first { it.messageId == DiscordContext.discordState?.messageId }
            val timeout = createTimeoutDestroy(
                name,
                data.obj,
                DiscordContext.discordState!!.uniqueId!!,
                loadTimeoutDelay(data.obj, Duration.ofMinutes(14)),
                loadBeanCallOnDestroy(data.obj, true)
            )
            resetTimeoutDestroy(name, DiscordContext.discordState!!.uniqueId!!, timeout)
            resetObserverTimeouts(name, DiscordContext.discordState!!.uniqueId!!, data.obj)
            (data.obj as InteractionScopedObject).freshInstance = false
            return data.obj
        }

        //If Autocomplete request, the same instance for this DiscordContext.discordState.id is returned
        val isAutoComplete = DiscordContext.discordState?.type == DiscordContext.Type.AUTO_COMPLETE
        if (isAutoComplete) {
            val hasAutoCompleteBean = scopedObjects[name]!![DiscordContext.discordState!!.id]!!.any { it.value.type == DiscordContext.Type.AUTO_COMPLETE }
            if (hasAutoCompleteBean) {

                //Found existing auto complete bean
                val data = scopedObjects[name]!![DiscordContext.discordState!!.id]!!.entries.first { it.value.type == DiscordContext.Type.AUTO_COMPLETE }

                DiscordContext.discordState!!.uniqueId = data.key
                logger.debug("[$name] - ${DiscordContext.discordState} -> found auto-complete instance by uniqueId and return")

                val timeout = createTimeoutDestroy(
                    name,
                    data.value.obj,
                    data.key,
                    Duration.ofMinutes(5),
                    false
                )
                resetTimeoutDestroy(name, data.key, timeout)
                resetObserverTimeouts(name, DiscordContext.discordState!!.uniqueId!!, data.value.obj)
                (data.value.obj as InteractionScopedObject).freshInstance = false
                return data.value.obj
            } else {
                //No bean exists, so we create one
                DiscordContext.discordState!!.uniqueId = DiscordContext.discordState!!.uniqueId ?: NanoId.generate()
                val newObj = objectFactory.getObject() as InteractionScopedObject
                newObj.uniqueId = DiscordContext.discordState!!.uniqueId!!
                newObj.freshInstance = true
                logger.debug("[$name] - ${DiscordContext.discordState} -> new instance (for auto-complete)")
                scopedObjects[name]!![DiscordContext.discordState!!.id]!![newObj.uniqueId] =
                    ScopedObjectData(DiscordContext.discordState?.type ?: DiscordContext.Type.AUTO_COMPLETE, newObj)

                val timeout = createTimeoutDestroy(
                    name,
                    newObj,
                    DiscordContext.discordState!!.uniqueId!!,
                    Duration.ofMinutes(5),
                    false
                )
                resetTimeoutDestroy(name, DiscordContext.discordState!!.uniqueId!!, timeout)
                resetObserverTimeouts(name, DiscordContext.discordState!!.uniqueId!!, newObj)
                return newObj
            }
        }

        //If type is Command we create a new instance or use the one from the auto complete if present
        val autoCompleteForThisCommand = scopedObjects[name]!![DiscordContext.discordState!!.id]!!.entries.firstOrNull { it.value.type == DiscordContext.Type.AUTO_COMPLETE }
        if (autoCompleteForThisCommand != null && loadBeanUseAutoCompleteBean(autoCompleteForThisCommand.value.obj, false)) {
            DiscordContext.discordState!!.uniqueId = autoCompleteForThisCommand.key
            logger.debug("[$name] - ${DiscordContext.discordState} -> found auto-complete (use for command)")
            //Found existing auto complete bean we can use

            //Change type to COMMAND
            val newScopedObjectData = ScopedObjectData(
                DiscordContext.Type.INTERACTION,
                scopedObjects[name]!![DiscordContext.discordState!!.id]!![autoCompleteForThisCommand.key]!!.obj,
                scopedObjects[name]!![DiscordContext.discordState!!.id]!![autoCompleteForThisCommand.key]!!.messageId,
                scopedObjects[name]!![DiscordContext.discordState!!.id]!![autoCompleteForThisCommand.key]!!.creationDate
            )
            scopedObjects[name]!![DiscordContext.discordState!!.id]!![autoCompleteForThisCommand.key] = newScopedObjectData

            //Search for other beans with same uniqueId and AUTO_COMPLETE
            scopedObjects.filter { scopedObject -> scopedObject.value.any { it.value.containsKey(autoCompleteForThisCommand.key) } }.forEach { beanEntry ->
                beanEntry.value.filter { it.value.containsKey(autoCompleteForThisCommand.key) }.forEach { stateEntry ->
                    stateEntry.value.forEach { scopedObject ->
                        val newSubScopedObjectData = ScopedObjectData(
                            DiscordContext.Type.INTERACTION,
                            scopedObjects[beanEntry.key]!![stateEntry.key]!![scopedObject.key]!!.obj,
                            scopedObjects[beanEntry.key]!![stateEntry.key]!![scopedObject.key]!!.messageId,
                            scopedObjects[beanEntry.key]!![stateEntry.key]!![scopedObject.key]!!.creationDate
                        )
                        scopedObjects[beanEntry.key]!![stateEntry.key]!![scopedObject.key] = newSubScopedObjectData
                    }
                }
            }

            //Remove old timeout
            scopedObjectsTimeoutScheduledTask[autoCompleteForThisCommand.key]!!.cancel(true)

            val timeout = createTimeoutDestroy(
                name,
                autoCompleteForThisCommand.value.obj,
                autoCompleteForThisCommand.key,
                loadTimeoutDelay(autoCompleteForThisCommand.value.obj, Duration.ofMinutes(14)),
                loadBeanCallOnDestroy(autoCompleteForThisCommand.value.obj, true)
            )

            scopedObjectsTimeoutScheduledTask[autoCompleteForThisCommand.key] = timeout
            (autoCompleteForThisCommand.value.obj as InteractionScopedObject).freshInstance = true
            return autoCompleteForThisCommand.value.obj
        } else {
            //Remove auto complete if not re-use
            if (autoCompleteForThisCommand?.value?.obj?.let { loadBeanUseAutoCompleteBean(it, true) } == false) {
                scopedObjectsTimeoutScheduledTask[autoCompleteForThisCommand.key]!!.cancel(true)
            }

            //No bean exists, so we create one
            DiscordContext.discordState!!.uniqueId = DiscordContext.discordState!!.uniqueId ?: NanoId.generate()
            logger.debug("[$name] - ${DiscordContext.discordState} -> new instance")
            val newObj = objectFactory.getObject() as InteractionScopedObject
            newObj.uniqueId = DiscordContext.discordState!!.uniqueId!!
            newObj.freshInstance = true
            scopedObjects[name]!![DiscordContext.discordState!!.id]!![DiscordContext.discordState!!.uniqueId!!] =
                ScopedObjectData(DiscordContext.discordState?.type ?: DiscordContext.Type.OTHER, newObj)

            val timeout = createTimeoutDestroy(
                name,
                newObj,
                DiscordContext.discordState!!.uniqueId!!,
                loadTimeoutDelay(newObj, Duration.ofMinutes(14)),
                loadBeanCallOnDestroy(newObj, true)
            )

            scopedObjectsTimeoutScheduledTask[DiscordContext.discordState!!.uniqueId!!] = timeout
            return newObj
        }
    }

    private fun createTimeoutDestroy(
        name: String,
        newObj: Any,
        uniqueId: String,
        delay: Duration,
        executeOnDestroy: Boolean
    ): ScheduledFuture<*> {
        return scopedObjectsTimeoutScheduler.schedule({
            val discordBot: DiscordBot = context.getBean(DiscordBot::class.java) as DiscordBot
            runBlocking(AlunaDispatchers.Internal) {
                if (executeOnDestroy) {
                    try {
                        (newObj as InteractionScopedObject).runOnDestroy()
                    } catch (e: Exception) {
                        logger.debug("[$name] - ${DiscordContext.discordState} -> could not invoke onDestroy for ${newObj}\n${e.stackTraceToString()}")
                    }
                    try {
                        if (loadObserverWaiterOnDestroy(newObj, true)) {
                            logger.debug("[$name] - ${DiscordContext.discordState} -> remove observer & eventWaiters if existing for this instance.")
                            val eventWaiter: EventWaiter = context.getBean(EventWaiter::class.java) as EventWaiter

                            val buttonMessage = discordBot.messagesToObserveButton.entries.firstOrNull { it.value.uniqueId == uniqueId }
                            buttonMessage?.let {
                                it.value.timeoutTask?.cancel(true)
                                discordBot.messagesToObserveButton.remove(it.key)
                            }

                            val stringSelectMessage = discordBot.messagesToObserveStringSelect.entries.firstOrNull { it.value.uniqueId == uniqueId }
                            stringSelectMessage?.let {
                                it.value.timeoutTask?.cancel(true)
                                discordBot.messagesToObserveStringSelect.remove(it.key)
                            }

                            val entitySelectMessage = discordBot.messagesToObserveEntitySelect.entries.firstOrNull { it.value.uniqueId == uniqueId }
                            entitySelectMessage?.let {
                                it.value.timeoutTask?.cancel(true)
                                discordBot.messagesToObserveEntitySelect.remove(it.key)
                            }

                            val modalMessage = discordBot.messagesToObserveModal.entries.firstOrNull { it.value.uniqueId == uniqueId }
                            modalMessage?.let {
                                it.value.timeoutTask?.cancel(true)
                                discordBot.messagesToObserveModal.remove(it.key)
                            }

                            eventWaiter.removeWaiter(uniqueId, true)
                        }
                    } catch (e: Exception) {
                        logger.debug("[$name] - ${DiscordContext.discordState} -> could not remove observer for ${newObj}\"\n${e.stackTraceToString()}")
                    }
                }
                //Remove element from scope cache
                scopedObjects.getOrElse(name) { null }?.getOrElse(DiscordContext.discordState!!.id) { null }?.remove(uniqueId)
                if (scopedObjects.getOrElse(name) { null }?.getOrElse(DiscordContext.discordState!!.id) { null }?.isEmpty() == true) {
                    scopedObjects.getOrElse(name) { null }?.remove(DiscordContext.discordState!!.id)
                }
                if (scopedObjects.getOrElse(name) { null }?.isEmpty() == true) {
                    scopedObjects.remove(name)
                }

                scopedObjectsTimeoutScheduledTask.remove(uniqueId)
            }
        }, delay.seconds, TimeUnit.SECONDS)
    }

    private fun resetTimeoutDestroy(name: String, uniqueId: String, newScheduledFuture: ScheduledFuture<*>) {
        logger.debug("[$name] - ${DiscordContext.discordState} -> reset bean timeout")
        scopedObjectsTimeoutScheduledTask[uniqueId]?.cancel(true)
        scopedObjectsTimeoutScheduledTask[uniqueId] = newScheduledFuture
    }

    private fun resetObserverTimeouts(name: String, uniqueId: String, newObj: Any) {
        if (newObj !is DiscordInteractionHandler) {
            logger.debug("[$name] - ${DiscordContext.discordState} -> object is not a DiscordInteractionHandler -> skip observer reset")
            return
        }

        if (!loadResetObserverTimeoutOnBeanExtend(newObj, true)) {
            logger.debug("[$name] - ${DiscordContext.discordState} -> observer reset on bean reset is disabled -> skip observer reset")
            return
        }

        //Set observers if needed
        val discordBot: DiscordBot = context.getBean(DiscordBot::class.java) as DiscordBot
        val buttonMessage = discordBot.messagesToObserveButton.entries.firstOrNull { it.value.uniqueId == uniqueId }
        buttonMessage?.let { observer ->
            logger.debug("[$name] - ${DiscordContext.discordState} -> reset button interaction timeout for message ${observer.key}")
            observer.value.timeoutTask?.cancel(true)
            observer.value.timeoutTask = ObserveInteraction.scheduleButtonTimeout(newObj, observer.value.duration, observer.key, discordBot, logger)
        }

        val stringSelectMessage = discordBot.messagesToObserveStringSelect.entries.firstOrNull { it.value.uniqueId == uniqueId }
        stringSelectMessage?.let { observer ->
            logger.debug("[$name] - ${DiscordContext.discordState} -> reset string select interaction timeout for message ${observer.key}")
            observer.value.timeoutTask?.cancel(true)
            observer.value.timeoutTask =
                ObserveInteraction.scheduleStringSelectTimeout(newObj, observer.value.duration, observer.key, discordBot, logger)
        }

        val entitySelectMessage = discordBot.messagesToObserveEntitySelect.entries.firstOrNull { it.value.uniqueId == uniqueId }
        entitySelectMessage?.let { observer ->
            logger.debug("[$name] - ${DiscordContext.discordState} -> reset entity select interaction timeout for message ${observer.key}")
            observer.value.timeoutTask?.cancel(true)
            observer.value.timeoutTask =
                ObserveInteraction.scheduleEntitySelectTimeout(newObj, observer.value.duration, observer.key, discordBot, logger)
        }

        val modalMessage = discordBot.messagesToObserveModal.entries.firstOrNull { it.value.uniqueId == uniqueId }
        modalMessage?.let { observer ->
            if (observer.value.authorIds?.firstOrNull() == null) {
                logger.debug("[$name] - ${DiscordContext.discordState} -> can not reset modal timeout as author id is no set in observer.")
                return@let
            }
            logger.debug("[$name] - ${DiscordContext.discordState} -> reset modal interaction timeout for message ${observer.key}")
            observer.value.timeoutTask?.cancel(true)
            observer.value.timeoutTask =
                ObserveInteraction.scheduleModalTimeout(newObj, observer.value.duration, observer.value.authorIds!!.first(), discordBot, logger)
        }
    }

    private fun loadTimeoutDelay(obj: Any, default: Duration, clazz: Class<*> = obj::class.java): Duration {
        return try {
            if (clazz.declaredFields.none { it.name == "beanTimoutDelay" } && clazz != CommandDataImpl::class.java) {
                loadTimeoutDelay(obj, default, clazz.superclass)
            } else {
                val declaredField = clazz.getDeclaredField("beanTimoutDelay")
                declaredField.isAccessible = true
                declaredField.get(obj) as Duration
            }
        } catch (e: Exception) {
            default
        }
    }

    private fun loadObserverWaiterOnDestroy(obj: Any, default: Boolean, clazz: Class<*> = obj::class.java): Boolean {
        return try {
            if (clazz.declaredFields.none { it.name == "beanRemoveObserverOnDestroy" } && clazz != CommandDataImpl::class.java) {
                loadObserverWaiterOnDestroy(obj, default, clazz.superclass)
            } else {
                val declaredField = clazz.getDeclaredField("beanRemoveObserverOnDestroy")
                declaredField.isAccessible = true
                declaredField.get(obj) as Boolean
            }
        } catch (e: Exception) {
            default
        }
    }

    private fun loadResetObserverTimeoutOnBeanExtend(obj: Any, default: Boolean, clazz: Class<*> = obj::class.java): Boolean {
        return try {
            if (clazz.declaredFields.none { it.name == "beanResetObserverTimeoutOnBeanExtend" } && clazz != CommandDataImpl::class.java) {
                loadResetObserverTimeoutOnBeanExtend(obj, default, clazz.superclass)
            } else {
                val declaredField = clazz.getDeclaredField("beanResetObserverTimeoutOnBeanExtend")
                declaredField.isAccessible = true
                declaredField.get(obj) as Boolean
            }
        } catch (e: Exception) {
            default
        }
    }

    private fun loadBeanUseAutoCompleteBean(obj: Any, default: Boolean, clazz: Class<*> = obj::class.java): Boolean {
        return try {
            if (clazz.declaredFields.none { it.name == "beanUseAutoCompleteBean" } && clazz != CommandDataImpl::class.java) {
                loadBeanUseAutoCompleteBean(obj, default, clazz.superclass)
            } else {
                val declaredField = clazz.getDeclaredField("beanUseAutoCompleteBean")
                declaredField.isAccessible = true
                declaredField.get(obj) as Boolean
            }
        } catch (e: Exception) {
            default
        }
    }

    private fun loadBeanCallOnDestroy(obj: Any, default: Boolean, clazz: Class<*> = obj::class.java): Boolean {
        return try {
            if (clazz.declaredFields.none { it.name == "beanCallOnDestroy" } && clazz != CommandDataImpl::class.java) {
                loadBeanCallOnDestroy(obj, default, clazz.superclass)
            } else {
                val declaredField = clazz.getDeclaredField("beanCallOnDestroy")
                declaredField.isAccessible = true
                declaredField.get(obj) as Boolean
            }
        } catch (e: Exception) {
            default
        }
    }

    override fun remove(name: String): Any? {
        logger.debug("[$name] - ${DiscordContext.discordState} -> remove bean")
        DiscordContext.discordState?.uniqueId?.let { scopedObjectsTimeoutScheduledTask[it]!!.cancel(true) }
        return scopedObjects.getOrElse(name) { null }?.remove(DiscordContext.discordState?.id)
    }

    fun removeByUniqueId(uniqueId: String) {
        logger.debug("[$uniqueId] -> remove beans by unique id")
        scopedObjectsTimeoutScheduledTask[uniqueId]!!.cancel(true)

        //Remove all scopedObjects for a given unique id
        scopedObjects.filter { (_, innerMap) ->
            innerMap.values.any { innerInnerMap ->
                innerInnerMap.keys.any { keys ->
                    keys == uniqueId
                }
            }
        }.forEach { scopedObjects.remove(it.key) }
    }

    fun setMessageIdForInstance(uniqueId: UniqueId, messageId: String) {
        try {
            val obj = scopedObjects.values.firstOrNull { it.containsKey(DiscordContext.discordState!!.id) }?.get(DiscordContext.discordState!!.id)?.get(uniqueId)
            if (obj == null) {
                logger.debug("[$uniqueId] -> id does not exist")
                return
            }

            logger.debug("[$uniqueId] -> set message id to $messageId")
            obj.messageId = messageId
        } catch (e: Exception) {
            logger.debug("[$uniqueId] -> set message id to $messageId failed")
        }
    }

    override fun registerDestructionCallback(name: String, callback: Runnable) {
    }

    override fun resolveContextualObject(key: String): Any? {
        return null
    }

    override fun getConversationId(): String {
        return DiscordContext.discordState?.id ?: ""
    }

    fun getInstanceCount(): Int {
        return scopedObjects.entries.sumOf { scopedObject -> scopedObject.value.entries.sumOf { it.value.size } }
    }

    fun getTimeoutCount(): Int {
        return scopedObjectsTimeoutScheduledTask.size
    }
}

object DiscordContext {
    private val CONTEXT = NamedInheritableThreadLocal<Data>("discord")
    fun setDiscordState(userId: String, serverId: String? = null, type: Type = Type.OTHER, uniqueId: String? = null, messageId: String? = null) {
        CONTEXT.set(Data(userId + ":" + (serverId ?: ""), type, uniqueId, messageId))
    }

    var discordState: Data?
        get() = CONTEXT.get()
        set(discordStateId) {
            CONTEXT.set(discordStateId)
        }

    fun clear() {
        CONTEXT.remove()
    }

    class Data(
        val id: DiscordStateId,
        val type: Type = Type.OTHER,
        var uniqueId: String? = null,
        var messageId: String? = null
    ) {
        override fun toString(): String {
            return "[id='$id', uniqueId='$uniqueId', messageId='$messageId', type='$type']"
        }
    }

    enum class Type {
        INTERACTION, AUTO_COMPLETE, OTHER
    }
}

internal class ScopedObjectData(
    var type: DiscordContext.Type,
    val obj: Any,
    var messageId: String? = null,
    var creationDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
) {
    override fun toString(): String {
        return "[type='$type', creationDate='$creationDate', obj='$obj']"
    }
}
private typealias BeanName = String
private typealias DiscordStateId = String
private typealias UniqueId = String
