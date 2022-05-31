package io.viascom.discord.bot.aluna.configuration.scope

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import io.viascom.discord.bot.aluna.bot.DiscordBot
import io.viascom.discord.bot.aluna.bot.handler.CommandScopedObject
import io.viascom.discord.bot.aluna.bot.listener.EventWaiter
import io.viascom.discord.bot.aluna.util.AlunaThreadPool
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.config.Scope
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.NamedInheritableThreadLocal
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class CommandScope(private val context: ConfigurableApplicationContext) : Scope {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val scopedObjects = Collections.synchronizedMap(HashMap<BeanName, HashMap<DiscordStateId, HashMap<UniqueId, ScopedObjectData>>>())
    private var scopedObjectsTimeoutScheduler: ScheduledThreadPoolExecutor

    private val scopedObjectsTimeoutScheduledTask = Collections.synchronizedMap(HashMap<UniqueId, ScheduledFuture<*>>())

    init {
        val scopedObjectsTimeoutScheduler = context.environment.getProperty("aluna.thread.scoped-objects-timeout-scheduler", Int::class.java, 2)
        this.scopedObjectsTimeoutScheduler = AlunaThreadPool.getFixedScheduledThreadPool(
            scopedObjectsTimeoutScheduler, "Aluna-Scoped-Objects-Timeout-Pool-%d", true
        )
    }

    override fun get(name: String, objectFactory: ObjectFactory<*>): Any {
        //If state id is not set, a new instance is returned
        if (DiscordContext.discordState?.id == null) {
            val newObj = objectFactory.getObject() as CommandScopedObject
            newObj.uniqueId = ""
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
            val data = scopedObjects[name]!![DiscordContext.discordState!!.id]!![DiscordContext.discordState!!.uniqueId]!!
            val timeout = createTimeoutDestroy(
                name,
                data.obj,
                DiscordContext.discordState!!.uniqueId!!,
                loadTimeoutDelay(data.obj, Duration.ofMinutes(15)),
                loadBeanCallOnDestroy(data.obj, true)
            )
            resetTimeoutDestroy(DiscordContext.discordState!!.uniqueId!!, timeout)
            return data.obj
        }

        //If Autocomplete request, the same instance for this DiscordContext.discordState.id is returned
        val isAutoComplete = DiscordContext.discordState?.type == DiscordContext.Type.AUTO_COMPLETE
        if (isAutoComplete) {
            val hasAutoCompleteBean = scopedObjects[name]!![DiscordContext.discordState!!.id]!!.any { it.value.type == DiscordContext.Type.AUTO_COMPLETE }
            if (hasAutoCompleteBean) {
                //Found existing auto complete bean
                val data = scopedObjects[name]!![DiscordContext.discordState!!.id]!!.entries.first { it.value.type == DiscordContext.Type.AUTO_COMPLETE }
                val timeout = createTimeoutDestroy(
                    name,
                    data.value.obj,
                    data.key,
                    Duration.ofMinutes(5),
                    false
                )
                resetTimeoutDestroy(data.key, timeout)
                return data.value.obj
            } else {
                //No bean exists, so we create one
                DiscordContext.discordState!!.uniqueId = NanoIdUtils.randomNanoId()
                val newObj = objectFactory.getObject() as CommandScopedObject
                newObj.uniqueId = DiscordContext.discordState!!.uniqueId!!
                scopedObjects[name]!![DiscordContext.discordState!!.id]!![DiscordContext.discordState!!.uniqueId!!] =
                    ScopedObjectData(DiscordContext.discordState?.type ?: DiscordContext.Type.AUTO_COMPLETE, newObj)

                val timeout = createTimeoutDestroy(
                    name,
                    newObj,
                    DiscordContext.discordState!!.uniqueId!!,
                    Duration.ofMinutes(5),
                    false
                )
                resetTimeoutDestroy(DiscordContext.discordState!!.uniqueId!!, timeout)

                return newObj
            }
        }

        //If type is Command we create a new instance or use the one from the auto complete if present
        val autoCompleteForThisCommand =
            scopedObjects[name]!![DiscordContext.discordState!!.id]!!.entries.firstOrNull { it.value.type == DiscordContext.Type.AUTO_COMPLETE }
        if (autoCompleteForThisCommand != null && loadBeanUseAutoCompleteBean(autoCompleteForThisCommand.value.obj, false)) {
            //Found existing auto complete bean we can use
            //Change type to COMMAND
            autoCompleteForThisCommand.value.type = DiscordContext.Type.COMMAND
            //Remove old timeout
            scopedObjectsTimeoutScheduledTask[autoCompleteForThisCommand.key]!!.cancel(true)

            val timeout = createTimeoutDestroy(
                name,
                autoCompleteForThisCommand.value.obj,
                autoCompleteForThisCommand.key,
                loadTimeoutDelay(autoCompleteForThisCommand.value.obj, Duration.ofMinutes(15)),
                loadBeanCallOnDestroy(autoCompleteForThisCommand.value.obj, true)
            )

            scopedObjectsTimeoutScheduledTask[autoCompleteForThisCommand.key] = timeout
            return autoCompleteForThisCommand.value.obj
        } else {
            //Remove auto complete if not re-use
            if (autoCompleteForThisCommand?.value?.obj?.let { loadBeanUseAutoCompleteBean(it, true) } == false) {
                scopedObjectsTimeoutScheduledTask[autoCompleteForThisCommand.key]!!.cancel(true)
            }

            //No bean exists, so we create one
            DiscordContext.discordState!!.uniqueId = NanoIdUtils.randomNanoId()
            val newObj = objectFactory.getObject() as CommandScopedObject
            newObj.uniqueId = DiscordContext.discordState!!.uniqueId!!
            scopedObjects[name]!![DiscordContext.discordState!!.id]!![DiscordContext.discordState!!.uniqueId!!] =
                ScopedObjectData(DiscordContext.discordState?.type ?: DiscordContext.Type.OTHER, newObj)

            val timeout = createTimeoutDestroy(
                name,
                newObj,
                DiscordContext.discordState!!.uniqueId!!,
                loadTimeoutDelay(newObj, Duration.ofMinutes(15)),
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
            discordBot.asyncExecutor.execute {
                if (executeOnDestroy) {
                    try {
                        newObj::class.java.getDeclaredMethod("onDestroy").invoke(newObj)
                    } catch (e: NoSuchMethodException) {
                        logger.debug("onDestroy does not exist for $newObj")
                    } catch (e: Exception) {
                        logger.debug("Could not invoke onDestroy for ${newObj}\n${e.stackTraceToString()}")
                    }
                    try {
                        if (loadObserverWaiterOnDestroy(newObj, true)) {
                            logger.debug("Remove observer & eventWaiters if existing for this instance.")
                            val eventWaiter: EventWaiter = context.getBean(EventWaiter::class.java) as EventWaiter

                            val buttonMessage = discordBot.messagesToObserveButton.entries.firstOrNull { it.value.uniqueId == uniqueId }
                            buttonMessage?.let {
                                it.value.timeoutTask?.cancel(true)
                                discordBot.messagesToObserveButton.remove(it.key)
                            }

                            val selectMessage = discordBot.messagesToObserveSelect.entries.firstOrNull { it.value.uniqueId == uniqueId }
                            selectMessage?.let {
                                it.value.timeoutTask?.cancel(true)
                                discordBot.messagesToObserveSelect.remove(it.key)
                            }

                            eventWaiter.removeEvents(uniqueId, true)
                        }
                    } catch (e: Exception) {
                        logger.debug("Could not remove observer for ${newObj}\"\n${e.stackTraceToString()}")
                    }
                }
                //Remove element from scope cache
                scopedObjects.getOrElse(name) { null }?.getOrElse(DiscordContext.discordState!!.id) { null }?.remove(uniqueId)
                if(scopedObjects.getOrElse(name) { null }?.getOrElse(DiscordContext.discordState!!.id) { null }?.isEmpty() == true){
                    scopedObjects.getOrElse(name) { null }?.remove(DiscordContext.discordState!!.id)
                }
                if(scopedObjects.getOrElse(name) { null }?.isEmpty() == true){
                    scopedObjects.remove(name)
                }

                scopedObjectsTimeoutScheduledTask.remove(uniqueId)
            }
        }, delay.seconds, TimeUnit.SECONDS)
    }

    private fun resetTimeoutDestroy(uniqueId: String, newScheduledFuture: ScheduledFuture<*>) {
        scopedObjectsTimeoutScheduledTask[uniqueId]?.cancel(true)
        scopedObjectsTimeoutScheduledTask[uniqueId] = newScheduledFuture
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
        DiscordContext.discordState?.uniqueId?.let { scopedObjectsTimeoutScheduledTask[it]!!.cancel(true) }
        return scopedObjects.getOrElse(name) { null }?.remove(DiscordContext.discordState?.id)
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
        return scopedObjects.entries.sumOf { it.value.entries.sumOf { it.value.size } }
    }

    fun getTimeoutCount(): Int {
        return scopedObjectsTimeoutScheduledTask.size
    }
}

object DiscordContext {
    private val CONTEXT = NamedInheritableThreadLocal<Data>("discord")
    fun setDiscordState(userId: String, serverId: String? = null, type: Type = Type.OTHER, uniqueId: String? = null) {
        CONTEXT.set(Data(userId + ":" + (serverId ?: ""), type, uniqueId))
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
        val id: String,
        val type: Type = Type.OTHER,
        var uniqueId: String? = null
    )

    enum class Type {
        COMMAND, AUTO_COMPLETE, OTHER
    }
}

private class ScopedObjectData(
    var type: DiscordContext.Type,
    val obj: Any,
    var creationDate: LocalDateTime = LocalDateTime.now()
)
private typealias BeanName = String
private typealias DiscordStateId = String
private typealias UniqueId = String
