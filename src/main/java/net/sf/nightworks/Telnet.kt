package net.sf.nightworks

/** interpret as command */
const val IAC: Byte = 255.toByte()

/** you are not to use option */
const val DONT = 254.toByte()

/** please, you use option */
const val DO = 253.toByte()

/** I won't use option */
const val WONT = 252.toByte()

/** I will use option */
const val WILL = 251.toByte()

/** you may reverse the line */
const val GA = 249.toByte()

/** echo */
const val TELOPT_ECHO = 1.toByte()


fun toColor(colorCode: Char): String {
    return when (colorCode) {
        'x' -> "\u001b[m"
        'd' -> "\u001b[0;30m"
        'r' -> "\u001b[0;31m"
        'g' -> "\u001b[0;32m"
        'y' -> "\u001b[0;33m"
        'b' -> "\u001b[0;34m"
        'm' -> "\u001b[0;35m"
        'c' -> "\u001b[0;36m"
        'w' -> "\u001b[0;37m"
    // brighter
        'D' -> "\u001b[1;30m"
        'R' -> "\u001b[1;31m"
        'G' -> "\u001b[1;32m"
        'Y' -> "\u001b[1;33m"
        'B' -> "\u001b[1;34m"
        'M' -> "\u001b[1;35m"
        'C' -> "\u001b[1;36m"
        'W' -> "\u001b[1;37m"
    // special
        '*' -> "\u0007"
        else -> "" //todo: warn
    }
}
