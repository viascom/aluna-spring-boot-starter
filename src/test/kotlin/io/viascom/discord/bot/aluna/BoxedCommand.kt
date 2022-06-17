package io.viascom.discord.bot.aluna

import io.viascom.discord.bot.aluna.bot.Command
import io.viascom.discord.bot.aluna.configuration.scope.CommandScope
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory

@Command
class BoxedCommand(
    private val boxedSubCommand: BoxedSubCommand,
    private val boxedSubCommand1: BoxedSubCommand1,
    private val boxedSubCommand2: BoxedSubCommand2,
    private val configurableListableBeanFactory: ConfigurableListableBeanFactory
) : ExtendedCommand(
    "boxed",
    "Noice boxed command",
    observeAutoComplete = true
) {

    override fun initSubCommands() {
        this.addSubcommands(SubcommandData("info", "Details").addOption(OptionType.STRING, "name", "name", true, true))
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        boxedSubCommand.execute(event)
        logger.debug((configurableListableBeanFactory.getRegisteredScope("command") as CommandScope).scopedObjects.toString())
    }

    override fun onAutoCompleteEvent(option: String, event: CommandAutoCompleteInteractionEvent) {
        super.onAutoCompleteEvent(option, event)
        event.replyChoiceStrings("test", "test2", "test3").queue()
    }
}
