package io.viascom.discord.bot.aluna.bot

import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

abstract class DiscordSubCommandGroup(name: String, description: String) : SubcommandGroupData(name, description), InteractionScopedObject,
    DiscordSubCommandElement {

    override lateinit var uniqueId: String
    override var beanTimoutDelay: Duration = Duration.ofMinutes(14)
    override var beanUseAutoCompleteBean: Boolean = true
    override var beanRemoveObserverOnDestroy: Boolean = true
    override var beanCallOnDestroy: Boolean = true

    val subCommands: HashMap<String, DiscordSubCommand> = hashMapOf()

    @JvmSynthetic
    internal fun initSubCommands() {
        if (subCommands.isEmpty()) {
            this::class.primaryConstructor!!.parameters.forEach {
                if (it.findAnnotation<SubCommandElement>() != null && (it.type.classifier as KClass<*>).isSubclassOf(DiscordSubCommandElement::class)) {
                    val field = this::class.memberProperties.firstOrNull { member -> member.name == it.name }
                        ?: throw IllegalArgumentException("Couldn't access ${it.name} parameter because it is not a property. To fix this, make sure that your parameter is defined as property.")
                    field.isAccessible = true
                    registerSubCommands(field.getter.call(this) as DiscordSubCommand)
                }
            }
        }
    }

    fun registerSubCommand(subCommand: DiscordSubCommand) {
        subCommands[subCommand.name] = subCommand
        this.addSubcommands(subCommand)
    }

    fun registerSubCommands(vararg subCommands: DiscordSubCommand) {
        subCommands.forEach {
            registerSubCommand(it)
        }
    }

}