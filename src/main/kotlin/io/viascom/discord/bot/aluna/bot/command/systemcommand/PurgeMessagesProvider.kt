package io.viascom.discord.bot.aluna.bot.command.systemcommand

import io.viascom.discord.bot.aluna.bot.Command
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.emotes.AlunaEmote
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.util.createTextInput
import io.viascom.discord.bot.aluna.util.getOptionAsString
import io.viascom.discord.bot.aluna.util.getValueAsString
import net.dv8tion.jda.api.entities.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.stream.Collectors

@Command
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
class PurgeMessagesProvider(
    private val shardManager: ShardManager
) : SystemCommandDataProvider(
    "purge_messages",
    "Purge Messages",
    true,
    false,
    true,
    false
) {

    lateinit var selectedChannel: GuildMessageChannel

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {

        val id = event.getOptionAsString("args", "")!!
        if (id.isEmpty()) {
            event.deferReply().setContent("${AlunaEmote.BOT_CROSS.asMention()} Please specify an ID as argument for this command").queue()
            return
        }

        val channel = shardManager.getChannelById(GuildMessageChannel::class.java, id)
        if (channel == null) {
            event.deferReply().setContent("${AlunaEmote.BOT_CROSS.asMention()} Please specify a valid channel ID as argument for this command").queue()
            return
        }

        selectedChannel = channel

        //Show modal
        val amount: TextInput = createTextInput("amount", "Amount of messages", TextInputStyle.SHORT)

        val modal: Modal = Modal.create("purge", "Purge")
            .addActionRows(ActionRow.of(amount))
            .build()

        event.replyModal(modal).queueAndRegisterInteraction(command)
    }

    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        event.deferReply(true).queue {
            it.editOriginal(
                "${AlunaEmote.BOT_CHECK.asMention()} Removing ${
                    event.getValueAsString(
                        "amount",
                        "0"
                    )
                } messages from ${selectedChannel.asMention}..."
            ).queue()

            val number = event.getValueAsString("amount", "0")!!.toLong()
            selectedChannel.iterableHistory.queue {
                val messages = it.stream().limit(number).collect(Collectors.toList())
                selectedChannel.purgeMessages(messages)
            }
        }
        return true
    }

    override fun onArgsAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        val input = event.getOptionAsString("args", "")!!

        if (input == "") {
            event.replyChoices().queue()
            return
        }

        val possibleChannel = shardManager.getChannelById(GuildMessageChannel::class.java, input)

        if (possibleChannel != null) {
            event.replyChoices(net.dv8tion.jda.api.interactions.commands.Command.Choice(possibleChannel.name, possibleChannel.id)).queue()
        } else {
            event.replyChoices().queue()
        }
    }
}
