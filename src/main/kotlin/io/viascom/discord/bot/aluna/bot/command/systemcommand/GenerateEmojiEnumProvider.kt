package io.viascom.discord.bot.aluna.bot.command.systemcommand

import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.handler.Command
import io.viascom.discord.bot.aluna.bot.handler.DiscordCommand
import io.viascom.discord.bot.aluna.bot.handler.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnAlunaNotProductionMode
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.util.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.sharding.ShardManager
import java.awt.Color

@Command
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
@ConditionalOnAlunaNotProductionMode
class GenerateEmojiEnumProvider(
    private val shardManager: ShardManager
) : SystemCommandDataProvider(
    "generate_emoji_enum",
    "Generate Emoji-Enum",
    true,
    false,
    false,
    false
) {

    lateinit var latestHook: InteractionHook
    var latestEmbed: EmbedBuilder = EmbedBuilder()

    var selectedServerIds = arrayListOf<String>()
    var selectedType = "kotlin"

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        createOverviewMessage()
        event.replyEmbeds(latestEmbed.build()).setEphemeral(true).addActionRows(getActionRow()).queueAndRegisterInteraction(
            command,
            arrayListOf(DiscordCommand.EventRegisterType.BUTTON, DiscordCommand.EventRegisterType.SELECT, DiscordCommand.EventRegisterType.MODAL),
            true
        ) {
            latestHook = it
        }
    }

    private fun createOverviewMessage() {
        latestEmbed.clearFields()

        latestEmbed = EmbedBuilder()
            .setColor(Color.BLUE)
            .setTitle("Generate Emoji-Enum")
            .setFooter("This system-command is only enabled on no production mode!")
            .setDescription("This command lets you create Emotji-Enums which extend the Aluna DiscordEmote interface. To get started add the servers you want and hit generate.")
            .addField("Selected Servers", selectedServerIds.joinToString("\n") { "- ${shardManager.getServer(it)?.name ?: it}" }, false)
    }

    private fun createRemoveMessage() {
        latestEmbed.clearFields()
        createOverviewMessage()

        latestEmbed.addField("", "⬇️ Select a server you want to remove from the list ⬇️", false)
    }

    private fun getRemoveActionRow(): ArrayList<ActionRow> {
        val rows = arrayListOf<ActionRow>()
        val row1 = arrayListOf<ItemComponent>()
        val serverList = SelectMenu.create("serverList")

        selectedServerIds.mapNotNull { shardManager.getServer(it) }.forEach {
            serverList.addOption(it.name, it.id, it.id)
        }

        row1.add(serverList.build())
        rows.add(ActionRow.of(row1))

        val row2 = arrayListOf<ItemComponent>()
        row2.add(createDangerButton("cancel-remove", "Cancel"))
        rows.add(ActionRow.of(row2))

        return rows
    }

    private fun getActionRow(): ArrayList<ActionRow> {
        val rows = arrayListOf<ActionRow>()

        val row1 = arrayListOf<ItemComponent>()
        row1.add(createPrimaryButton("add", "Add Server"))
        row1.add(createDangerButton("remove", "Remove Server", disabled = selectedServerIds.isEmpty()))
        rows.add(ActionRow.of(row1))

        val row2 = arrayListOf<ItemComponent>()
        row2.add(
            SelectMenu.create("type")
                .addOption("Kotlin", "kotlin", isDefault = selectedType == "kotlin")
                .addOption("Java", "java", isDefault = selectedType == "java")
                .addOption("Plain Text", "text", isDefault = selectedType == "text")
                .build()
        )
        rows.add(ActionRow.of(row2))

        val row3 = arrayListOf<ItemComponent>()
        row3.add(createDangerButton("cancel", "Cancel"))
        row3.add(createSuccessButton("generate", "Generate File"))
        rows.add(ActionRow.of(row3))

        return rows
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
        when (event.componentId) {
            "add" -> {
                event.replyModal(
                    Modal.create("add_server", "Add Server")
                        .addTextField("serverId", "Server-ID")
                        .build()
                ).queue()
            }
            "remove" -> {
                event.deferEdit().queue()

                createRemoveMessage()
                latestHook.editOriginalEmbeds(latestEmbed.build()).setActionRows(getRemoveActionRow()).queue()
            }
            "generate" -> {
                val file = generateFile()
                val name = when (selectedType) {
                    "text" -> "emojis.txt"
                    "kotlin" -> "MyEmotes.kt"
                    "java" -> "MyEmotes.java"
                    else -> "emojis.txt"
                }
                latestHook.editOriginalEmbeds().setContent("⬇️ Generated File: ⬇️").setActionRows(arrayListOf()).addFile(file.encodeToByteArray(), name).queue()
            }
            "cancel-remove" -> {
                event.deferEdit().queue()
                createOverviewMessage()
                latestHook.editOriginalEmbeds(latestEmbed.build()).setActionRows(getActionRow()).queue()
            }
            "cancel" -> {
                event.deferEdit().queue()
                createOverviewMessage()
                latestHook.editOriginalEmbeds(latestEmbed.build()).setActionRows(arrayListOf()).queue()
            }
        }

        return true
    }

    override fun onSelectMenuInteraction(event: SelectMenuInteractionEvent): Boolean {
        when (event.componentId) {
            "type" -> {
                event.deferEdit().queue()
                selectedType = event.getSelection()

                createOverviewMessage()
                latestHook.editOriginalEmbeds(latestEmbed.build()).setActionRows(getActionRow()).queue()
            }
            "serverList" -> {
                event.deferEdit().queue()
                selectedServerIds.remove(event.getSelection())

                createOverviewMessage()
                latestHook.editOriginalEmbeds(latestEmbed.build()).setActionRows(getActionRow()).queue()
            }
        }
        return true
    }

    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        when (event.modalId) {
            "add_server" -> {
                event.deferEdit().queue()

                //Check server id
                val serverID = event.getValueAsString("serverId", "0")
                val newServer = try {
                    shardManager.getServer(serverID!!)
                } catch (e: Exception) {
                    null
                }
                if (newServer != null) {
                    if (!selectedServerIds.contains(newServer.id)) {
                        selectedServerIds.add(newServer.id)
                    }

                    createOverviewMessage()
                    latestHook.editOriginalEmbeds(latestEmbed.build()).setActionRows(getActionRow()).queue()
                } else {
                    createOverviewMessage()

                    //Add error message
                    latestEmbed.setColor(Color.RED)
                    latestEmbed.addField("", "⛔ Server with id `$serverID` does not exist or the bot is not on it!", false)

                    latestHook.editOriginalEmbeds(latestEmbed.build()).setActionRows(getActionRow()).queue()
                }
            }
        }
        return true
    }

    private fun generateFile(): String {
        var content = ""

        when (selectedType) {
            "text" -> {
                selectedServerIds.mapNotNull { shardManager.getServer(it) }.forEach { guild ->
                    content += "\n\n** ${guild.name} (${guild.id})**\n"
                    content += guild.emotes.sortedBy { it.name }.joinToString("\n") { "${it.asMention} `${it.asMention}`" }
                }
            }
            "kotlin" -> {
                content = """
                    import io.viascom.discord.bot.aluna.model.DiscordEmote;
                    
                    /**
                     * My emotes
                     *
                     * @property id Id of the emoji
                     * @property emoteName Name of the emoji
                     * @property animated Is this emoji animated
                     */
                    enum class MyEmotes(override val id: String, override val emoteName: String, override val animated: Boolean = false) : DiscordEmote {
                """.trimIndent()

                selectedServerIds.mapNotNull { shardManager.getServer(it) }.forEach {
                    content += "\n\n    //${it.name} (${it.id})\n"
                    content += it.emotes
                        .sortedBy { it.name }
                        .joinToString("\n") { "    ${it.name.uppercase()}(\"${it.id}\", \"${it.name}\"" + if (it.isAnimated) ", true)," else ")," }
                }

                content = content.dropLast(1)
                content += "\n}"
            }
            "java" -> {
                content = """
                   import io.viascom.discord.bot.aluna.model.DiscordEmote;
                   import org.jetbrains.annotations.NotNull;

                   /**
                    * My emotes
                    */
                   public enum MyEmotes implements DiscordEmote {
                """.trimIndent()

                selectedServerIds.mapNotNull { shardManager.getServer(it) }.forEach {
                    content += "\n\n    //${it.name} (${it.id})\n"
                    content += it.emotes
                        .sortedBy { it.name }
                        .joinToString("\n") { "    ${it.name.uppercase()}(\"${it.id}\", \"${it.name}\"" + if (it.isAnimated) ", true)," else ")," }
                }

                content = content.dropLast(1)
                content += ";\n\n"

                content += """
                    
                       final String id;
                       final String name;
                       final boolean animated;
                   
                       MyEmotes(String id, String name, boolean animated) {
                           this.id = id;
                           this.name = name;
                           this.animated = animated;
                       }
                   
                       MyEmotes(String id, String name) {
                           this.id = id;
                           this.name = name;
                           this.animated = false;
                       }

                       @Override
                       public boolean getAnimated() {
                           return animated;
                       }

                       @NotNull
                       @Override
                       public String getEmoteName() {
                           return name;
                       }

                       @NotNull
                       @Override
                       public String getId() {
                           return id;
                       }

                   }
                """.trimIndent()
            }
        }

        return content
    }
}