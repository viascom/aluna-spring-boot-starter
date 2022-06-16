package io.viascom.discord.bot.aluna.configuration.condition

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class SendServerNotificationCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata) =
        context.environment.getProperty("aluna.notification.server-join.enabled", Boolean::class.java) == true ||
                context.environment.getProperty("aluna.notification.server-leave.enabled", Boolean::class.java) == true ||
                context.environment.getProperty("aluna.notification.bot-ready.enabled", Boolean::class.java) == true
}
