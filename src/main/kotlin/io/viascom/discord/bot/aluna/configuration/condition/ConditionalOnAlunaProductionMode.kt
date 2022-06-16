package io.viascom.discord.bot.aluna.configuration.condition

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import java.lang.annotation.Inherited

@ConditionalOnProperty(name = ["production-mode"], prefix = "aluna", matchIfMissing = false)
@Inherited
annotation class ConditionalOnAlunaProductionMode()
