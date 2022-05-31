package io.viascom.discord.bot.aluna.bot.handler

import java.time.Duration

interface CommandScopedObject {
    var uniqueId: String
    var beanTimoutDelay: Duration
    var beanUseAutoCompleteBean: Boolean
    var beanRemoveObserverOnDestroy: Boolean
    var beanCallOnDestroy: Boolean
}
