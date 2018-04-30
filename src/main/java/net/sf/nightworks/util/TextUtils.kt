package net.sf.nightworks.util

fun isSpace(c: Char) = c == ' ' || c == '\n' || c == '\t' || c == '\r'

fun isDigit(c: Char) = c in '0'..'9'

/**
 * Removes the tildes from a string.
 * Used for player-entered strings that go into disk files.
 */
fun smash_tilde(str: String) = str.replace('~', '-')


/**
 * Compare strings, case insensitive, for prefix matching.
 */
fun startsWith(p: String, str: String): Boolean {
    var prefix = !p.isEmpty() && p.length <= str.length
    prefix = prefix && LOWER(p[0]) == LOWER(str[0]) && eq(str.substring(0, p.length), p)
    return prefix
}

fun eq(s1: String, s2: String) = s1.equals(s2, ignoreCase = true)

fun endsWith(s: String, str: String): Boolean {
    val sLen = s.length
    val strLen = str.length
    return sLen <= strLen && eq(s, str.substring(strLen - sLen))
}

/**
 * Returns an initial-capped string.
 */
fun capitalize(str: String) = when {
    str.isEmpty() -> str
    else -> Character.toUpperCase(str[0]) + if (str.length > 1) str.substring(1).toLowerCase() else ""
}

fun capitalize_nn(str: String) = if (str.isEmpty()) str else UPPER(str[0]) + if (str.length > 1) str.substring(1).toLowerCase() else ""

fun LOWER(c: Char) = c.toLowerCase()

fun UPPER(c: Char) = c.toUpperCase()

/**
 * Return true if an argument is completely numeric.
 */
fun is_number(arg: CharSequence?): Boolean {
    if (arg == null || arg.isEmpty()) {
        return false
    }
    var i = 0
    if (arg[i] == '+' || arg[i] == '-') {
        i++
    }
    while (i < arg.length) {
        if (!isDigit(arg[i])) {
            return false
        }
        i++
    }
    return true
}

/**
 * Pick off one argument from a string and return the rest.
 * Understands quotes.
 */
fun one_argument(argument: String, arg0: StringBuilder): String {
    var res = argument
    if (res.isEmpty()) {
        return res
    }
    res = trimSpaces(res, 0)
    if (res.isEmpty()) {
        return res
    }

    var cEnd = res[0]
    if (cEnd != '\'' && cEnd != '"') {
        arg0.append(cEnd)
        cEnd = ' '
    }

    var pos = 1
    while (pos < res.length) {
        val c = res[pos]
        if (c == cEnd) {
            pos++
            break
        }
        arg0.append(c)
        pos++
    }
    res = if (pos >= res.length) "" else trimSpaces(res, pos)
    return res
}

fun trimSpaces(argument: String, fromPos: Int): String {
    var res = argument
    var pos = fromPos
    val len = res.length
    while (pos < len && res[pos] <= ' ') {
        pos++
    }
    if (pos > 0) {
        res = res.substring(pos)
    }
    return res
}

