package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.handler.CommandScopedObject
import java.util.concurrent.TimeUnit

abstract class ExtendedSubCommand : CommandScopedObject {

    override lateinit var uniqueId: String
    override var beanTimoutDelay: Long = 15
    override var beanTimoutDelayUnit: TimeUnit = TimeUnit.MINUTES
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = true

}

