package io.viascom.discord.bot.aluna

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class AlunaDiscordBot {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val context = runApplication<AlunaDiscordBot>(*args)
        }
    }

}
