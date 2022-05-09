package io.viascom.discord.bot.aluna.scriptengine.internal

object RemoveComments {
    const val DEFAULT = 1
    const val ESCAPE = 2
    const val STRING = 3
    const val ONE_LINE = 4
    const val MULTI_LINE = 5
    fun perform(s: String): String {
        val out = StringBuilder()
        var mod = DEFAULT
        var i = 0
        while (i < s.length) {
            val substring = s.substring(i, Math.min(i + 2, s.length))
            val c = s[i]
            when (mod) {
                DEFAULT -> mod = if (substring == "/*") MULTI_LINE else if (substring == "//") ONE_LINE else if (c == '"' || c == '\'') STRING else DEFAULT
                STRING -> mod = if (c == '"' || c == '\'') DEFAULT else if (c == '\\') ESCAPE else STRING
                ESCAPE -> mod = STRING
                ONE_LINE -> {
                    mod = if (c == '\n') DEFAULT else ONE_LINE
                    i++
                    continue
                }
                MULTI_LINE -> {
                    mod = if (substring == "*/") DEFAULT else MULTI_LINE
                    i += if (mod == DEFAULT) 1 else 0
                    i++
                    continue
                }
            }
            if (mod < 4) {
                out.append(c)
            }
            i++
        }
        return out.toString()
    }
}
