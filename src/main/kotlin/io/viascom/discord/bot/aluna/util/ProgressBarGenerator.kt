package io.viascom.discord.bot.aluna.util

object ProgressBarGenerator {

    @JvmStatic
    fun generate(value: Double, max: Int, progressElements: ArrayList<String> = arrayListOf(" ", "▏", "▎", "▍", "▌", "▋", "▊", "▉", "█")): String {
        var bar = ""

        val percent: Double = value / max.toDouble() * 100
        val tenPercent = percent / 10
        for (i in 1..tenPercent.toInt()) {
            bar += progressElements[8]
        }

        val restPercent = percent - (tenPercent.toInt() * 10)
        val elementSelection = if ((restPercent / 10 * 9 - 1) < 1) {
            1.0
        } else {
            restPercent / 10 * 9 - 1
        }
        bar += progressElements[elementSelection.toInt()]

        for (i in 9 downTo tenPercent.toInt()) {
            bar += progressElements[0]
        }

        return bar
    }
}
