package io.viascom.discord.bot.aluna.exception

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer
import org.springframework.boot.diagnostics.FailureAnalysis

internal class AlunaPropertyExceptionFailureAnalyzer : AbstractFailureAnalyzer<AlunaPropertiesException>() {
    override fun analyze(rootFailure: Throwable, cause: AlunaPropertiesException): FailureAnalysis {
        val description = "${cause.description}\n\n" +
                "\tProperty: ${cause.property}\n" +
                "\tValue: ${cause.value}\n" +
                "\tReason: ${cause.reason}"
        return FailureAnalysis(description, "Update your application configuration", cause)
    }
}