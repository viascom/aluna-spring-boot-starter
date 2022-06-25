package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.Command
import io.viascom.discord.bot.aluna.bot.DiscordCommand
import io.viascom.discord.bot.aluna.bot.listener.EventWaiter
import io.viascom.discord.bot.aluna.model.ChannelOption
import io.viascom.discord.bot.aluna.model.DoubleOption
import io.viascom.discord.bot.aluna.model.OptionRange
import io.viascom.discord.bot.aluna.model.StringOption
import io.viascom.discord.bot.aluna.util.createPrimaryButton
import io.viascom.discord.bot.aluna.util.getTypedOption
import io.viascom.discord.bot.aluna.util.removeActionRows
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.sharding.ShardManager
import java.time.Duration


@Command
class PingCommand(
    private val eventWaiter: EventWaiter,
    private val shardManager: ShardManager
) : DiscordCommand(
    "ping",
    "Send a ping"
) {

    init {
        this.beanTimoutDelay = Duration.ofSeconds(20)
    }

    private val mapOption = StringOption("map", "Select a map", isRequired = false, isAutoComplete = true)
    private val channelOption = ChannelOption("channel", "Select a map", isRequired = false, arrayListOf(ChannelType.VOICE))
    private val rangeOption = DoubleOption("range", "Select a map", isRequired = false, isAutoComplete = false, requiredRange = OptionRange(1.2, 10.0))

    override fun initCommandOptions() {
        this.addOptions(mapOption, channelOption, rangeOption)
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        logger.debug("command: " + this.hashCode().toString())
        val selectedMap = event.getTypedOption(mapOption)
        val selectedChannel = event.getTypedOption(channelOption, OptionMapping::getAsAudioChannel)
        val range = event.getTypedOption(rangeOption, 1.2)!!
        val oldWayChannel = event.getOption("channel")

        event.reply("Pong\nYour locale is:${this.userLocale}").addActionRows(ActionRow.of(createPrimaryButton("hi", "Hi"))).queue {
            eventWaiter.waitForInteraction(this.uniqueId,
                ButtonInteractionEvent::class.java,
                hook = it,
                action = {
                    logger.debug(this.hashCode().toString())
                    if (it.componentId == "hi") {
                        it.editMessage("Oh hi :)").removeActionRows().queue()
                    }
                })
        }
    }

//    override fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
//        logger.debug("autocomplete: " + this.hashCode().toString())
//        event.replyChoice("hello", "hello").queue()
//    }


    override fun onDestroy() {
        logger.debug("/ping got destroyed " + this.hashCode().toString())
    }
}
