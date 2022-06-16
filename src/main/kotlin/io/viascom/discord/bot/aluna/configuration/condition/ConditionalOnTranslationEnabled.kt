package io.viascom.discord.bot.aluna.configuration.condition

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import java.lang.annotation.Inherited

@ConditionalOnProperty(name = ["translation.enabled"], prefix = "aluna", matchIfMissing = false)
@Inherited
annotation class ConditionalOnTranslationEnabled()
