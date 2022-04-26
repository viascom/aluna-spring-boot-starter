package io.viascom.discord.bot.starter.property

import org.springframework.boot.context.properties.NestedConfigurationProperty

class AlunaCommandProperties {

    @NestedConfigurationProperty
    var systemCommand: SystemCommandProperties = SystemCommandProperties()

    @NestedConfigurationProperty
    var helpCommand: CommandProperties = CommandProperties()
}

class SystemCommandProperties {
    var enable: Boolean = true

    /**
     * Server id on which this command can be used. If set to 0 the command will be removed completely.
     */
    var server: String? = null

    var supportServer: String? = null
}

class CommandProperties {
    var enable: Boolean = true

    /**
     * Server id on which this command can be used. If set to 0 the command will be removed completly.
     */
    var server: String? = null
}
