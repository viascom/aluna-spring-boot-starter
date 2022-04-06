package io.viascom.discord.bot.starter.property

import org.springframework.boot.context.properties.NestedConfigurationProperty

class AlunaNotificationProperties {

    /**
     *
     */
    @NestedConfigurationProperty
    var serverJoin: Notification = Notification()

    @NestedConfigurationProperty
    var serverLeave: Notification = Notification()

    @NestedConfigurationProperty
    var botReady: Notification = Notification()

}

class Notification {
    var enable: Boolean = false
    var server: Long? = null
    var channel: Long? = null
}
