package io.viascom.discord.bot.aluna.property

import org.springframework.boot.convert.DurationUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

class AlunaThreadProperties {

    /**
     * Max amount of threads used for command execution.
     */
    var commandExecutorCount: Int = 100

    /**
     * Duration of how long a command thread should be keep inactive before it gets destroyed.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    var commandExecutorTtl: Duration = Duration.ofSeconds(30)

    /**
     * Max amount of async executor threads. These threads are used by Aluna to handle internal async task.
     */
    var asyncExecutorCount: Int = 100

    /**
     * Duration of how long an async thread should be keep inactive before it gets destroyed.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    var asyncExecutorTtl: Duration = Duration.ofSeconds(10)

    /**
     * Max amount of event waiter threads.
     */
    var eventWaiterThreadPoolCount: Int = 100

    /**
     * Duration of how long an event waiter thread should be keep inactive before it gets destroyed.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    var eventWaiterThreadPoolTtl: Duration = Duration.ofSeconds(30)

    /**
     * Max amount of scheduler threads for timeout handling on interaction observers.
     */
    var messagesToObserveScheduledThreadPool: Int = 2

    /**
     * Max amount of scheduler threads for timeout handling on discord scoped objects.
     */
    var scopedObjectsTimeoutScheduler: Int = 2

    /**
     * Max amount of scheduler threads for event waiter timeout handling.
     */
    var eventWaiterTimeoutScheduler: Int = 2
}
