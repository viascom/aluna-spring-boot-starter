package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.configuration.Experimental

@Experimental("Cooldowns are currently not supported")
enum class CooldownScope(private val format: String, internal val errorSpecification: String) {
    /**
     * Applies the cooldown to the calling [User][net.dv8tion.jda.core.entities.User] across all locations on this instance (IE: TextChannels, PrivateChannels, etc).
     *
     *
     * The key for this is generated in the format
     *
     * `<command-name>|U:<userID>`
     *
     */
    USER("U:%d", ""),

    /**
     * Applies the cooldown to the [MessageChannel][net.dv8tion.jda.core.entities.MessageChannel] the command is called in.
     *
     *
     * The key for this is generated in the format
     *
     * `<command-name>|C:<channelID>`
     *
     */
    CHANNEL("C:%d", "in this channel"),

    /**
     * Applies the cooldown to the calling [User][net.dv8tion.jda.core.entities.User] local to the [MessageChannel][net.dv8tion.jda.core.entities.MessageChannel] the
     * command is called in.
     *
     *
     * The key for this is generated in the format
     *
     * `<command-name>|U:<userID>|C:<channelID>`
     *
     */
    USER_CHANNEL("U:%d|C:%d", "in this channel"),

    /**
     * Applies the cooldown to the [Guild][net.dv8tion.jda.core.entities.Guild] the command is called in.
     *
     *
     * The key for this is generated in the format
     *
     * `<command-name>|G:<guildID>`
     *
     *
     *
     * **NOTE:** This will automatically default back to [CooldownScope.CHANNEL][Command.CooldownScope.CHANNEL]
     * when called in a private channel.  This is done in order to prevent internal [NullPointerException]s from being thrown while generating
     * cooldown keys!
     */
    GUILD("G:%d", "in this server"),

    /**
     * Applies the cooldown to the calling [User][net.dv8tion.jda.core.entities.User] local to the [Guild][net.dv8tion.jda.core.entities.Guild] the command is called
     * in.
     *
     *
     * The key for this is generated in the format
     *
     * `<command-name>|U:<userID>|G:<guildID>`
     *
     *
     *
     * **NOTE:** This will automatically default back to [CooldownScope.CHANNEL][Command.CooldownScope.CHANNEL]
     * when called in a private channel. This is done in order to prevent internal [NullPointerException]s from being thrown while generating
     * cooldown keys!
     */
    USER_GUILD("U:%d|G:%d", "in this server"),

    /**
     * Applies the cooldown to the calling Shard the command is called on.
     *
     *
     * The key for this is generated in the format
     *
     * `<command-name>|S:<shardID>`
     *
     *
     *
     * **NOTE:** This will automatically default back to [CooldownScope.GLOBAL][Command.CooldownScope.GLOBAL]
     * when [JDA#getShardInfo()][net.dv8tion.jda.core.JDA.getShardInfo] returns `null`. This is done in order to prevent internal [ NullPointerException][NullPointerException]s from being thrown while generating cooldown keys!
     */
    SHARD("S:%d", "on this shard"),

    /**
     * Applies the cooldown to the calling [User][net.dv8tion.jda.core.entities.User] on the Shard the command is called on.
     *
     *
     * The key for this is generated in the format
     *
     * `<command-name>|U:<userID>|S:<shardID>`
     *
     *
     *
     * **NOTE:** This will automatically default back to [CooldownScope.USER][Command.CooldownScope.USER]
     * when [JDA#getShardInfo()][net.dv8tion.jda.core.JDA.getShardInfo] returns `null`. This is done in order to prevent internal [ NullPointerException][NullPointerException]s from being thrown while generating cooldown keys!
     */
    USER_SHARD("U:%d|S:%d", "on this shard"),

    /**
     * Applies this cooldown globally.
     *
     *
     * As this implies: the command will be unusable on the instance of JDA in all types of
     * [MessageChannel][net.dv8tion.jda.core.entities.MessageChannel]s until the cooldown has ended.
     *
     *
     * The key for this is `<command-name>|globally`
     */
    GLOBAL("Global", "globally");

    @JvmSynthetic
    internal fun genKey(name: String, id: Long): String {
        return genKey(name, id, -1)
    }

    @JvmSynthetic
    internal fun genKey(name: String, idOne: Long, idTwo: Long): String {
        return when {
            this == GLOBAL -> "$name|$format"
            idTwo == -1L -> name + "|" + String.format(format, idOne)
            else -> name + "|" + String.format(format, idOne, idTwo)
        }
    }
}
