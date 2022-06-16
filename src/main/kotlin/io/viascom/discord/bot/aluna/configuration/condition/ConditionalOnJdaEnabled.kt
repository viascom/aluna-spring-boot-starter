package io.viascom.discord.bot.aluna.configuration.condition

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import java.lang.annotation.Inherited

@ConditionalOnProperty(name = ["discord.enable-jda"], prefix = "aluna", matchIfMissing = true)
@Inherited
annotation class ConditionalOnJdaEnabled()
