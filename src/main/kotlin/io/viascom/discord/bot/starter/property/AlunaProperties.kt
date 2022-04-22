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
     * Show time elapsed for commands
     */
    var useStopwatch: Boolean = false

    /**
     * Owner ids
     */
    var ownerIds: ArrayList<Long> = arrayListOf()

    /**
     * Moderator ids
     */
    var modIds: ArrayList<Long> = arrayListOf()

    /**
     * Enable Translation
     */
    var enableTranslation: Boolean = false

    /**
     * Translation path
     *
     * Format: <code>file:/</code>
     *
     * If not set, Aluna will fall back to <code>classpath:i18n/messages</code>
     */
    var translationPath: String? = null

    /**
     * Use en_GB for en in production
     */
    var useEnGbForEnInProduction: Boolean = false

    /**
     * Should Aluna register commands in production mode which are in commandDevelopmentStatus == IN_DEVELOPMENT
     */
    var includeInDevelopmentCommands: Boolean = false
}
