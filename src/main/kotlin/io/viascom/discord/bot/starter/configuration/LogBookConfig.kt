package io.viascom.discord.bot.starter.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.logbook.HttpLogFormatter
import org.zalando.logbook.Sink
import org.zalando.logbook.json.JsonHttpLogFormatter
import org.zalando.logbook.logstash.LogstashLogbackSink

@Configuration
open class LogBookConfig {

    @Bean
    open fun logBookSink(): Sink {
        val formatter: HttpLogFormatter = JsonHttpLogFormatter()
        return LogstashLogbackSink(formatter)
    }

}
