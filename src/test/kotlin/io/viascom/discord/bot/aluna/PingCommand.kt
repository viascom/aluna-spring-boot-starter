package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.handler.Command
import io.viascom.discord.bot.aluna.bot.handler.DiscordCommand
import io.viascom.discord.bot.aluna.bot.listener.EventWaiter
import io.viascom.discord.bot.aluna.scriptengine.KotlinScriptService
import io.viascom.discord.bot.aluna.util.createPrimaryButton
import io.viascom.discord.bot.aluna.util.getOptionAsString
import io.viascom.discord.bot.aluna.util.removeActionRows
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit


@Command
class PingCommand(
    private val eventWaiter: EventWaiter,
    private val shardManager: ShardManager,
    private val kotlinScriptService: KotlinScriptService
) : DiscordCommand(
    "ping",
    "Send a ping",
    observeAutoComplete = true
) {

    init {
        this.beanTimoutDelay = 20L
        this.beanTimoutDelayUnit = TimeUnit.SECONDS
    }

    override fun initCommandOptions() {
        val mapOption = OptionData(OptionType.STRING, "map", "Select a map", false, true)

        this.addOptions(mapOption)
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        logger.debug("command: " + this.hashCode().toString())
        val selectedMap = event.getOptionAsString("map", "all")

        var eval = kotlinScriptService.eval("shardManager.shards.first().guilds.size") as Int
        logger.info("Guilds: $eval")

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

    override fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
        super.onAutoCompleteEvent(option, event)

        logger.debug("autocomplete: " + this.hashCode().toString())
        event.replyChoice("hello", "hello").queue()
    }

    override fun onDestroy() {
        logger.debug("/ping got destroyed " + this.hashCode().toString())
    }
}
