package io.viascom.discord.bot.aluna.property

class AlunaThreadProperties {

    var commandExecutorCount: Int = 100
    var commandExecutorTtl: Long = 30
    var asyncExecutorCount: Int = 100
    var asyncExecutorTtl: Long = 10
    var eventWaiterThreadPoolCount: Int = 100
    var eventWaiterThreadPoolTtl: Long = 30

    var messagesToObserveScheduledThreadPool: Int = 2
    var scopedObjectsTimeoutScheduler: Int = 2
    var eventWaiterTimeoutScheduler: Int = 2
}
