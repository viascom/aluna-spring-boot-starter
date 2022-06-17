package io.viascom.discord.bot.aluna.bot.command.systemcommand

import io.viascom.discord.bot.aluna.bot.Command
import io.viascom.discord.bot.aluna.bot.command.SystemCommand
import io.viascom.discord.bot.aluna.bot.emotes.AlunaEmote
import io.viascom.discord.bot.aluna.bot.queueAndRegisterInteraction
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnSystemCommandEnabled
import io.viascom.discord.bot.aluna.scriptengine.KotlinScriptService
import io.viascom.discord.bot.aluna.util.createTextInput
import io.viascom.discord.bot.aluna.util.getValueAsString
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import kotlin.math.min

@Command
@ConditionalOnJdaEnabled
@ConditionalOnSystemCommandEnabled
@ConditionalOnProperty(name = ["command.system-command.enable-kotlin-script-evaluate"], prefix = "aluna", matchIfMissing = false)
class KotlinEvaluateProvider(
    private val kotlinScriptService: KotlinScriptService
) : SystemCommandDataProvider(
    "evaluate_kotlin",
    "Evaluate Script",
    true,
    false,
    false,
    false
) {

    override fun execute(event: SlashCommandInteractionEvent, hook: InteractionHook?, command: SystemCommand) {
        //Show modal
        val script: TextInput = createTextInput("script", "Kotlin Script", TextInputStyle.PARAGRAPH)

        val modal: Modal = Modal.create("script", "Evaluate Script")
            .addActionRows(ActionRow.of(script))
            .build()

        event.replyModal(modal).queueAndRegisterInteraction(command)
    }


    override fun onModalInteraction(event: ModalInteractionEvent, additionalData: HashMap<String, Any?>): Boolean {
        val script = event.getValueAsString("script", "0")!!.toString()
        event.reply(
            "Script:\n```kotlin\n" +
                    "$script```\n" +
                    "${AlunaEmote.LOADING.asMention()} Result:\n" +
                    "``` ```"
        ).queue {
            val result = try {
                kotlinScriptService.eval(script)
            } catch (e: Exception) {
                val trace = e.stackTraceToString()
                trace.substring(0, min(trace.length - 1, 2000))

            }

            if (result.toString().length < 1000) {
                it.editOriginal(
                    "Script:\n```kotlin\n" +
                            "$script```\n" +
                            "Result:\n" +
                            "```$result```"
                ).queue()
            } else {
                it.editOriginal(
                    "Script:\n```kotlin\n" +
                            "$script```\n" +
                            "Result:"
                ).addFile(result.toString().encodeToByteArray(), "result.txt").queue()
            }

        }
        return true
    }
}
