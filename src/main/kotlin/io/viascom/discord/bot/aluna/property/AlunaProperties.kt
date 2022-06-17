package io.viascom.discord.bot.aluna.property

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
     * Notification settings
     */
    @NestedConfigurationProperty
    var notification: AlunaNotificationProperties = AlunaNotificationProperties()

    /**
     * BotLists settings
     */
    @NestedConfigurationProperty
    var botList: AlunaBotListProperties = AlunaBotListProperties()

    /**
     * BotLists settings
     */
    @NestedConfigurationProperty
    var thread: AlunaThreadProperties = AlunaThreadProperties()

    /**
     * Command settings
     */
    @NestedConfigurationProperty
    var command: AlunaCommandProperties = AlunaCommandProperties()

    /**
     * Is in production mode
     */
    var productionMode: Boolean = false

    /**
     * Owner ids. This is used by the DefaultOwnerIdProvider.
     */
    var ownerIds: ArrayList<Long> = arrayListOf()

    /**
     * Moderator ids. This is used by the DefaultModeratorIdProvider.
     */
    var modIds: ArrayList<Long> = arrayListOf()

    @NestedConfigurationProperty
    var debug: AlunaDebugProperties = AlunaDebugProperties()

    @NestedConfigurationProperty
    var translation: AlunaTranslationProperties = AlunaTranslationProperties()

    /**
     * Should Aluna register commands in production mode which are in commandDevelopmentStatus == IN_DEVELOPMENT
     */
    var includeInDevelopmentCommands: Boolean = false
}
