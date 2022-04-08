package io.viascom.discord.bot.starter.property

import org.springframework.boot.context.properties.NestedConfigurationProperty

class AlunaNotificationProperties {

    /**
     * Configuration for server join event notification
     */
    @NestedConfigurationProperty
    var serverJoin: Notification = Notification()

    /**
     * Configuration for server leave event notification
     */
    @NestedConfigurationProperty
    var serverLeave: Notification = Notification()

    /**
     * Configuration for bot ready event notification
     */
    @NestedConfigurationProperty
    var botReady: Notification = Notification()

}

class Notification {
    /**
     * Enable this notification
     */
    var enable: Boolean = false

    /**
     * Server where it gets posted
     */
    var server: Long? = null

    /**
     * Channel where it gets posted
     */
    var channel: Long? = null
}
