package io.viascom.discord.bot.starter.util

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

class DiscordFormatUtil {

    enum class TimestampFormat(val format: String) {
        SHORT_TIME("t"),
        LONG_TIME("T"),
        SHORT_DATE("d"),
        LONG_DATE("D"),
        SHORT_DATE_TIME("f"),
        LONG_DATE_TIME("F"),
        RELATIVE_TIME("R")
    }

}


fun LocalDateTime.toDiscordTimestamp(format: DiscordFormatUtil.TimestampFormat = DiscordFormatUtil.TimestampFormat.SHORT_DATE_TIME): String =
    "<t:${this.toUnixTimestamp()}:${format.format}>"

fun OffsetDateTime.toDiscordTimestamp(format: DiscordFormatUtil.TimestampFormat = DiscordFormatUtil.TimestampFormat.SHORT_DATE_TIME): String =
    "<t:${this.toEpochSecond()}:${format.format}>"

fun LocalDateTime.toUnixTimestamp(): Long = this.atZone(ZoneId.systemDefault()).toEpochSecond()
