package io.viascom.discord.bot.aluna.scriptengine.internal

object InterruptTest {
    @Throws(InterruptedException::class)
    fun test() {
        if (Thread.interrupted()) {
            throw InterruptedException()
        }
    }
}
