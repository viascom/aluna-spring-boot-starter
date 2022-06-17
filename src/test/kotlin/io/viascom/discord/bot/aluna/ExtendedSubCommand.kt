package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.CommandScopedObject
import java.time.Duration

abstract class ExtendedSubCommand : CommandScopedObject {

    override lateinit var uniqueId: String
    override var beanTimoutDelay: Duration = Duration.ofMinutes(15)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = true
    override var beanCallOnDestroy: Boolean = true

}

