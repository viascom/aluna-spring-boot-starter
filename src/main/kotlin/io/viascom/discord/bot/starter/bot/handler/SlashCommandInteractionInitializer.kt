package io.viascom.discord.bot.starter.bot.handler

import io.viascom.discord.bot.starter.bot.DiscordBot
import io.viascom.discord.bot.starter.event.DiscordFirstShardReadyEvent
import io.viascom.discord.bot.starter.event.EventPublisher
import io.viascom.discord.bot.starter.property.AlunaProperties
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
    private val contextMenus: List<DiscordContextMenu>,
    private val shardManager: ShardManager,
    private val discordBot: DiscordBot,
    private val eventPublisher: EventPublisher,
    private val alunaProperties: AlunaProperties
) : ApplicationListener<DiscordFirstShardReadyEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: DiscordFirstShardReadyEvent) {
        initSlashCommands()
    }

    private fun initSlashCommands() {
        //Create global commands
        shardManager.shards.first().retrieveCommands().queue { currentCommands ->
            logger.debug("Update slash commands if needed")

            val commandDataList = arrayListOf<CommandDataImpl>()

            val filteredCommands = commands.filter {
                when {
                    (alunaProperties.includeInDevelopmentCommands) -> true
                    (alunaProperties.productionMode && it.commandDevelopmentStatus == DiscordCommand.DevelopmentStatus.IN_DEVELOPMENT) -> false
                    else -> true
                }
            }.map {
                it.initCommandOptions()
                it.initSubCommands()
                it.prepareCommand()
                it
            }.toCollection(arrayListOf())

            val filteredContext = contextMenus.filter {
                when {
                    (alunaProperties.includeInDevelopmentCommands) -> true
                    (alunaProperties.productionMode && it.commandDevelopmentStatus == DiscordCommand.DevelopmentStatus.IN_DEVELOPMENT) -> false
                    else -> true
                }
            }.toCollection(arrayListOf())

            commandDataList.addAll(filteredCommands)
            commandDataList.addAll(filteredContext)

            val commandsToRemove = currentCommands.filter { command ->
                commandDataList.none { compareCommands(it, command) }
            }

            commandsToRemove.forEach {
                logger.debug("Removed unneeded command '/${it.name}'")
                shardManager.shards.first().deleteCommandById(it.id).queue()
            }

            val commandsToUpdateOrAdd = commandDataList
                //.filter { it.useScope in arrayListOf(DiscordCommand.UseScope.GLOBAL, DiscordCommand.UseScope.GUILD_ONLY) }
                .filter { commandData ->
                    currentCommands.none { compareCommands(commandData, it) }
                }

            commandsToUpdateOrAdd.forEach { discordCommand ->
                shardManager.shards.first().upsertCommand(discordCommand).queue { command ->
                    if (discordCommand.type == Command.Type.SLASH) {
                        printCommand((discordCommand as DiscordCommand))
                        discordBot.commands[command.name] = (discordCommand as DiscordCommand).javaClass
                    }
                    if (discordCommand.type != Command.Type.SLASH) {
                        logger.debug("Register context menu ${(discordCommand as DiscordContextMenu).name}")
                        discordBot.contextMenus[command.name] = discordCommand.javaClass
                    }
                    if (discordCommand.type == Command.Type.SLASH && (discordCommand as DiscordCommand).observeAutoComplete && command.name !in discordBot.commandsWithAutocomplete) {
                        discordBot.commandsWithAutocomplete.add(command.name)
                    }
                }
            }

            commands.forEach { command ->
                try {
                    discordBot.commands.computeIfAbsent(command.name) { commands.first { it.name == command.name }.javaClass }
                    if (command.observeAutoComplete && command.name !in discordBot.commandsWithAutocomplete) {
                        discordBot.commandsWithAutocomplete.add(command.name)
                    }
                } catch (e: Exception) {
                    logger.error("Could not add command '${command.name}' to available commands")
                }
            }

            contextMenus.forEach { command ->
                try {
                    discordBot.contextMenus.computeIfAbsent(command.name) { contextMenus.first { it.name == command.name }.javaClass }
                } catch (e: Exception) {
                    logger.error("Could not add context menu '${command.name}' to available menus")
                }
            }

            eventPublisher.publishDiscordSlashCommandInitializedEvent(
                commandsToUpdateOrAdd.filter { it.type == Command.Type.SLASH }.map { it::class },
                commandsToRemove.map { it.name })
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
