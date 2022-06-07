package io.viascom.discord.bot.aluna.bot.command.systemcommand.adminsearch

import io.viascom.discord.bot.aluna.bot.command.systemcommand.AdminSearchDataProvider
import io.viascom.discord.bot.aluna.bot.emotes.AlunaEmote
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true, havingValue = "true")
class AdminSearchServerPermissionsPage(
    private val shardManager: ShardManager,
    private val alunaProperties: AlunaProperties
) : AdminSearchPageDataProvider(
    "PERMISSIONS",
    "Permissions",
    arrayListOf(AdminSearchDataProvider.AdminSearchType.SERVER)
) {

    override fun onServerRequest(discordServer: Guild, embedBuilder: EmbedBuilder) {
        embedBuilder.clearFields()

        embedBuilder.addField("Assigned Roles", discordServer.selfMember.roles.joinToString("\n") { "- ${it.name} (${it.id})" }, false)
        embedBuilder.addField("${AlunaEmote.SMALL_TICK.asMention()} Permissions", discordServer.selfMember.permissions.joinToString("\n") {
            if (it in alunaProperties.discord.defaultPermissions) {
                "- **${it.getName()}**"
            } else {
                "- ${it.getName()}"
            }
        }, true)

        val missingPermissions = alunaProperties.discord.defaultPermissions.filter { it !in discordServer.selfMember.permissions }

        embedBuilder.addField(
            "${AlunaEmote.SMALL_CROSS.asMention()} Missing Permissions",
            if (missingPermissions.isNotEmpty()) {
                missingPermissions.joinToString("\n") {
                    "- **${it.getName()}**"
                }
            } else {
                "*nothing missing*"
            },
            true
        )
        embedBuilder.addBlankField(false)
        embedBuilder.addField(
            "${AlunaEmote.SMALL_TICK.asMention()} @everyone Permissions",
            discordServer.roles.first { it.isPublicRole }.permissions.joinToString("\n") {
                if (it in arrayListOf(Permission.USE_APPLICATION_COMMANDS, Permission.MESSAGE_EXT_EMOJI)) {
                    "- **${it.getName()}**"
                } else {
                    "- ${it.getName()}"
                }
            },
            true
        )

        val missingEveryonePermissions = arrayListOf(
            Permission.USE_APPLICATION_COMMANDS,
            Permission.MESSAGE_EXT_EMOJI
        ).filter { it !in discordServer.roles.first { it.isPublicRole }.permissions }

        embedBuilder.addField(
            "${AlunaEmote.SMALL_CROSS.asMention()} Missing @everyone Permissions",
            if (missingEveryonePermissions.isNotEmpty()) {
                missingEveryonePermissions.joinToString("\n") {
                    "- **${it.getName()}**"
                }
            } else {
                "*nothing missing*"
            },
            true
        )
    }

}
