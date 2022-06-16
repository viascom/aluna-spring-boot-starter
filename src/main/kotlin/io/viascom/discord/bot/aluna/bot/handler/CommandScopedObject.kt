package io.viascom.discord.bot.aluna.bot.handler

import java.time.Duration

interface CommandScopedObject {
    /**
     * Unique id for this object.
     * It's recommended to use NanoIdUtils.randomNanoId()
     */
    var uniqueId: String

    /**
     * Bean timout delay before it gets destroyed
     */
    var beanTimoutDelay: Duration

    /**
     * Should command execution use the bean created during auto complete request if present.
     */
    var beanUseAutoCompleteBean: Boolean

    /**
     * Should observers be removed if the bean gets destroyed.
     */
    var beanRemoveObserverOnDestroy: Boolean

    /**
     * Should onDestroy be called if the bean gets destroyed.
     */
    var beanCallOnDestroy: Boolean
}
