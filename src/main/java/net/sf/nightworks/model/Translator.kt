package net.sf.nightworks.model

object Translator {

    fun tr(s: String): String {
        val sb = StringBuffer()
        s.toCharArray().forEach { sb.append(tr(it)) }
        return sb.toString()
    }

    private fun tr(c: Char) = when (c) {
        'A' -> 'E'
        'a' -> 'e'
        'b' -> 'c'
        'B' -> 'C'
        'c' -> 'd'
        'C' -> 'D'
        'd' -> 'f'
        'D' -> 'F'
        'e' -> 'i'
        'E' -> 'I'
        'f' -> 'g'
        'F' -> 'G'
        'g' -> 'h'
        'G' -> 'H'
        'h' -> 'j'
        'H' -> 'J'
        'i' -> 'o'
        'I' -> 'O'
        'j' -> 'k'
        'J' -> 'K'
        'k' -> 'l'
        'K' -> 'L'
        'l' -> 'm'
        'L' -> 'M'
        'm' -> 'n'
        'M' -> 'N'
        'n' -> 'p'
        'N' -> 'P'
        'o' -> 'u'
        'O' -> 'U'
        'p' -> 'q'
        'P' -> 'Q'
        'q' -> 'r'
        'Q' -> 'R'
        'r' -> 's'
        'R' -> 'S'
        's' -> 't'
        'S' -> 'T'
        't' -> 'v'
        'T' -> 'V'
        'u' -> 'y'
        'U' -> 'Y'
        'v' -> 'w'
        'V' -> 'W'
        'w' -> 'x'
        'W' -> 'X'
        'x' -> 'z'
        'X' -> 'Z'
        'y' -> 'a'
        'Y' -> 'A'
        'z' -> 'b'
        'Z' -> 'B'
        else -> c
    }
}
