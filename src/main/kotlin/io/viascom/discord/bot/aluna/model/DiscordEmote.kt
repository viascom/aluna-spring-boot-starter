package io.viascom.discord.bot.aluna.model

import net.dv8tion.jda.api.entities.Emoji

/**
 * Discord emote interface.
 * By implementing this interface, you get some QOL functions for your defined emotes.
 *
 * Example on how to use this interface:
 * <code>
 * enum class MyEmote(override val id: String, override val emoteName: String, override val animated: Boolean = false) : DiscordEmote {
 *     ASPARKLES("963462938822344794", "asparkles", true),
 *     AUTHORIZED("963463099317362728", "authorized"),
 * }
 * </code>
 *
 */
interface DiscordEmote {
    val id: String
    val emoteName: String
    val animated: Boolean

    /**
     * Get emote as mention
     *
     * @return Emote as mention (<[a]:name:12345>)
     */
    fun asMention(): String {
        val animatedSuffix = if (animated) "a" else ""
        return "<$animatedSuffix:$emoteName:$id>"
    }

    /**
     * Get emote as mention in short form (with a single character name).
     * This should always be used on large embeds as the emote counts to the total size of the embed.
     *
     * @param spaceAfter Add addition space after the emote
     * @return Emote as mention without name (<[a]:e:12345>)
     */
    fun toStringShort(spaceAfter: Boolean = false): String {
        val animatedSuffix = if (animated) "a" else ""
        return "<$animatedSuffix:e:$id>" + if (spaceAfter) " " else ""
    }

    /**
     * Get emote as reaction code
     *
     * @return Reaction code of the Emote
     */
    fun toReactionCode(): String {
        return "$emoteName:$id"
    }

    /**
     * Get emote as Emoji
     *
     * @return Emoji of the Emote.
     */
    fun toEmoji(): Emoji {
        return Emoji.fromEmote(emoteName, id.toLong(), animated)
    }
}
