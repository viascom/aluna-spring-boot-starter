package io.viascom.discord.bot.aluna.model

class AdditionalRequirements(val failedRequirements: HashMap<String, Any> = hashMapOf()) {
    val failed: Boolean
        get() = failedRequirements.isNotEmpty()
}
