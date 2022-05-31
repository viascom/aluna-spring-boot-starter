package io.viascom.discord.bot.aluna.property

import org.springframework.boot.context.properties.NestedConfigurationProperty

class AlunaCommandProperties {

    @NestedConfigurationProperty
    var systemCommand: SystemCommandProperties = SystemCommandProperties()
}

class SystemCommandProperties {
    /**
     * Enable /system-command
     */
    var enable: Boolean = false

    /**
     * Server id on which this command can be used.
     * If set to 0 the command will be removed completely.
     * If set to null, the command can be used on every server and in DMs.
     */
    var server: String? = null

    /**
     * Defines the support server which will be used for certain information..
     */
    var supportServer: String? = null

    /**
     * Define which system command features should be enabled. If not defined, all implementations of SystemCommandDataProvider are available.
     * Functions: admin_search, extract_message, evaluate_kotlin, leave_server, purge_messages, send_message
     */
    var enabledFunctions: ArrayList<String>? = null

    /**
     * Define which system command features are allowed for moderators. If not defined, Aluna will use what is defined in the feature or the default which is false
     */
    var allowedForModeratorsFunctions: ArrayList<String>? = null
}
