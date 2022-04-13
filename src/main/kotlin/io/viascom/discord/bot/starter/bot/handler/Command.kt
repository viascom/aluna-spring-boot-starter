package io.viascom.discord.bot.starter.bot.handler

import io.viascom.discord.bot.starter.configuration.scope.CommandScoped
import org.springframework.stereotype.Component

/**
 * Command is an Aluna specific annotation which should be used for all DiscordCommand's as well as DiscordContextMenu's. It includes @Component and @CommandScoped
 *
 * Based on DiscordContext, this scope will create or reuse existing instances. If no DiscordContext is provided for the current thread, This scope will always provide a new instance.
 * Instances are destroyed after a defined amount of time. For instances created with the type COMMAND will aso trigger the onDestroy method.
 * The amount of time can be defined per command and is by default 15 min for commands and 5 min for auto complete events.
 * After the onDestroy method trigger, all observers and event listeners for this instance are also removed if not disabled in the per command configuration.
 *
 * If DiscordContext provides a uniqueId, like it is the case for Button and Select observer, the same instance is returned.
 *
 * If DiscordContext.Type is set to AUTO_COMPLETE, the same instance for this DiscordContext.id (`user.id + ":" + server?.id`) is returned. This means during multiple onAutoCompleteEvent per command, the data is persistent.
 *
 * If DiscordContext.Type is set to COMMAND, a new instance will be created if no auto complete instance exists or this feature is disabled.
 *
 * @author itsmefox
 * @since 0.0.10
 * @see Component
 * @see CommandScoped
 */
@Component
@CommandScoped
annotation class Command
