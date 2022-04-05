package io.viascom.discord.bot.starter.property

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(AlunaProperties.PREFIX)
class AlunaProperties {

    companion object {
        const val PREFIX = "aluna"
    }

    /**
     * Discord settings
     */
    @NestedConfigurationProperty
    var discord: AlunaDiscordProperties = AlunaDiscordProperties()

    /**
     * Is in production mode
     */
    var productionMode: Boolean = false

    /**
     * Show time elapsed for commands
     */
    var useStopwatch: Boolean = false

    /**
     * Owner ids
     */
    var ownerIds: ArrayList<Long> = arrayListOf()


}
