package io.viascom.discord.bot.aluna.property

import org.springframework.boot.context.properties.NestedConfigurationProperty

class AlunaCommandProperties {

    @NestedConfigurationProperty
    var systemCommand: SystemCommandProperties = SystemCommandProperties()
}

class SystemCommandProperties {
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
}
