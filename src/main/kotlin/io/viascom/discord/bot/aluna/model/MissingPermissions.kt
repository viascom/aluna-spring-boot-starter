package io.viascom.discord.bot.aluna.model

import net.dv8tion.jda.api.Permission

class MissingPermissions(
    val textChannel: ArrayList<Permission> = arrayListOf(),
    val voiceChannel: ArrayList<Permission> = arrayListOf(),
    val server: ArrayList<Permission> = arrayListOf(),
    var notInVoice: Boolean = false,
) {
    val hasMissingPermissions: Boolean
        get() = textChannel.isNotEmpty() || voiceChannel.isNotEmpty() || server.isNotEmpty()
}
