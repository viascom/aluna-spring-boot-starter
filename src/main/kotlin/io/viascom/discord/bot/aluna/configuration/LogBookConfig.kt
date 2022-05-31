package io.viascom.discord.bot.aluna.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.logbook.HttpLogFormatter
import org.zalando.logbook.Sink
import org.zalando.logbook.json.JsonHttpLogFormatter
import org.zalando.logbook.logstash.LogstashLogbackSink

@Configuration
@ConditionalOnClass(Sink::class)
open class LogBookConfig {

    @Bean
    open fun logBookSink(): Sink {
        val formatter: HttpLogFormatter = JsonHttpLogFormatter()
        return LogstashLogbackSink(formatter)
    }

}
