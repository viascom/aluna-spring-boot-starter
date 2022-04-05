package io.viascom.discord.bot.starter.bot.handler

import io.viascom.discord.bot.starter.bot.DiscordBot
import io.viascom.discord.bot.starter.event.DiscordReadyEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.internal.interactions.CommandDataImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
open class SlashCommandInteractionInitializer(
    private val commands: List<DiscordCommand>,
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
) : ApplicationListener<DiscordReadyEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordReadyEvent) {
        init()
    }

    private fun init() {
        logger.info("Init slash commands")

        //Create global commands
        shardManager.shards.first().retrieveCommands().queue { currentCommands ->
            commands.forEach {
                it.initCommandOptions()
                it.initSubCommands()
            }

            val commandsToRemove = currentCommands.filter { command ->
                commands.none { compareCommands(it, command) }
            }

            commandsToRemove.forEach {
                logger.info("Removed unneeded command '/${it.name}'")
                shardManager.shards.first().deleteCommandById(it.id).queue()
            }

            val commandsToUpdateOrAdd = commands
                .filter { it.useScope in arrayListOf(DiscordCommand.UseScope.GLOBAL, DiscordCommand.UseScope.GUILD_ONLY) }
                .filter { commandData ->
                    currentCommands.none { compareCommands(commandData, it) }
                }

            commandsToUpdateOrAdd.forEach { discordCommand ->
                printCommand(discordCommand)
                shardManager.shards.first().upsertCommand(discordCommand).queue { command ->
                    discordBot.commands[command.name] = discordCommand.javaClass
                    if (discordCommand.observeAutoComplete) {
                        discordBot.commandsWithAutoComplete[command.name] = discordCommand.javaClass
                    }
                }
            }

            commands.forEach { command ->
                try {
                    discordBot.commands.computeIfAbsent(command.name) { commands.first { it.name == command.name }.javaClass }
                    if (command.observeAutoComplete) {
                        discordBot.commandsWithAutoComplete.computeIfAbsent(command.name) { commands.first { it.name == command.name }.javaClass }
                    }
                } catch (e: Exception) {
                    logger.error("Could not add command '${command.name}' to available commands")
                }
            }

        }

        /*
        //Create per guild Commands
        commands.filter { it.useScope == AleevaCommand.UseScope.PER_GUILD_ONLY }.map {
            logger.info("\t-> init command '${it.name}'")
            it.initCommandOptions()
            it.initSubCommands()
            printCommand(it)
            val server = shardManager.getGuildById(Environment.aleevaServerId)!!
            server.upsertCommand(it).queue { command ->
                Environment.commands[command.name] = it.javaClass
            }
        }
         */

    }

    private fun printCommand(command: DiscordCommand) {
        var commandText = ""
        commandText += "\t-> init command '/${command.name}'"
        when {
            (command.subcommandGroups.isNotEmpty()) -> {
                commandText += "\n" + command.subcommandGroups.joinToString("\n") {
                    "\t\t--> ${it.name}\n" +
                            it.subcommands.joinToString("\n") {
                                "\t\t\t---> ${it.name}"
                            }
                }
            }
            (command.subcommands.isNotEmpty()) -> {
                commandText += "\n" + command.subcommands.joinToString("\n") {
                    "\t\t--> ${it.name}"
                }
            }
        }

        logger.info(commandText)
    }

    private fun compareCommands(commandData: CommandDataImpl, command: Command): Boolean {
        return commandData.name == command.name &&
                commandData.description == command.description &&
                commandData.options.map { Command.Option(it.toData()) } == command.options &&
                commandData.subcommandGroups.map { Command.SubcommandGroup(it.toData()) } == command.subcommandGroups &&
                commandData.subcommands.map { Command.Subcommand(it.toData()) } == command.subcommands
    }

}
