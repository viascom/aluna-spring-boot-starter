package io.viascom.discord.bot.starter.bot.handler

import java.util.concurrent.TimeUnit

interface CommandScopedObject{
    var uniqueId: String
    var beanTimoutDelay: Long
    var beanTimoutDelayUnit: TimeUnit
    var beanUseAutoCompleteBean: Boolean
    var beanRemoveObserverOnDestroy: Boolean
}
