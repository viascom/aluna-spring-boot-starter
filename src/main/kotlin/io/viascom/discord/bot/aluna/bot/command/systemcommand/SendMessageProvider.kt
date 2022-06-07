package io.viascom.discord.bot.aluna.bot.command.systemcommand

import com.google.gson.Gson
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.handler.Command
import io.viascom.discord.bot.aluna.bot.handler.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.model.Webhook
import io.viascom.discord.bot.aluna.util.createTextInput
import io.viascom.discord.bot.aluna.util.getPrivateChannelByUser
import io.viascom.discord.bot.aluna.util.getServerTextChannel
import io.viascom.discord.bot.aluna.util.getValueAsString
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Command
@ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true, havingValue = "true")
class SendMessageProvider(
    private val shardManager: ShardManager,
    private val gson: Gson
) : SystemCommandDataProvider(
    "send_message",
    "Send Message",
    true,
    false,
    false,
    false
) {

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        //Show modal
        val serverId: TextInput = createTextInput("serverId", "Server ID (0 = current or if DM)", TextInputStyle.SHORT)
        val channelId: TextInput = createTextInput("channelId", "Channel ID (0 = current, @<id> = for DM)", TextInputStyle.SHORT)
        val message: TextInput = createTextInput("message", "Message", TextInputStyle.PARAGRAPH)

        val modal: Modal = Modal.create("send_message", "Send Message")
            .addActionRows(ActionRow.of(serverId), ActionRow.of(channelId), ActionRow.of(message))
            .build()

        event.replyModal(modal).queueAndRegisterInteraction(command)
    }

    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        var serverId = event.getValueAsString("serverId", "0")!!
        var channelId = event.getValueAsString("channelId", "0")!!
        var isDM = false
        val message = event.getValueAsString("message")!!
        val messageObj = if(message.startsWith("{")){
            gson.fromJson(message, Webhook::class.java).toMessage()
        } else {
            Webhook(message).toMessage()
        }

        //Set correct values for current if in DM
        if(!event.isFromGuild && serverId == "0" && channelId == "0"){
            channelId = "@${event.user.id}"
        }

        //Set serverId to current server if channel is not set to dm
        if (serverId == "0" && !channelId.startsWith("@")) {
            serverId = event.guild?.id ?: "0"
        }

        //Set channelId to current channel or dm channel
        if (channelId.startsWith("@")) {
            channelId = channelId.substring(1)
            serverId = "0"
            isDM = true
        }
        if (channelId == "0") {
            channelId = event.channel?.id ?: "0"
        }

        when {
            (isDM) -> {
                val dmChannel = shardManager.getPrivateChannelByUser(channelId)
                if (dmChannel == null) {
                    event.deferReply(true).queue {
                        it.editOriginal("Could not find user: $channelId").queue()
                    }
                    return true
                }

                dmChannel.sendMessage(messageObj).queue()
                event.deferReply(true).queue {
                    it.editOriginal("Message Sent!").queue()
                }
                return true
            }
            (serverId != "0" && channelId != "0") -> {
                val channel = shardManager.getServerTextChannel(serverId, channelId)
                if (channel == null) {
                    event.deferReply(true).queue {
                        it.editOriginal("Could not find channel $channelId on $serverId!").queue()
                    }
                    return true
                }

                channel.sendMessage(messageObj).queue()
                event.deferReply(true).queue {
                    it.editOriginal("Message Sent!").queue()
                }
                return true
            }
            else -> {
                event.deferReply(true).queue {
                    it.editOriginal("Could not find channel $channelId on $serverId!").queue()
                }
                return true
            }
        }
    }
}
