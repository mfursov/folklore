package net.sf.nightworks.util

fun exit(code: Int): Nothing {
    Exception("Exiting with code $code ...").printStackTrace()
    System.exit(1)
    null!!
}

fun currentTimeSeconds() = System.currentTimeMillis() / 1000

fun atoi(num: String) = num.toIntOrNull()?:-1

fun sprintf(tb: TextBuffer, text: String, vararg args: Any) {
    tb.sprintf(text, *args)
}

fun crypt(s1: String, s2: String) = if (s1.isEmpty()) s1 else s1 + s2.hashCode()
