package net.sf.nightworks.util

import net.sf.nightworks.flag_type
import java.io.File
import java.io.FileReader
import java.io.IOException

private val MAX_WORD_LENGTH = 256
private val END_OF_STREAM_CHAR = (-1).toChar()

class DikuTextFile @Throws(IOException::class)
constructor(private val file: File) {
    private val data: CharArray
    private var currentPos = 0
    private val tmpBuf = StringBuilder()

    constructor(fileName: String) : this(File(fileName))

    init {
        val len = file.length().toInt()
        data = CharArray(len)
        FileReader(file).use { reader ->
            var pos = 0
            do {
                val dpos = reader.read(data, pos, len - pos)
                assert(dpos >= 0)
                pos += dpos
            } while (pos != len)
        }
    }


    fun long(): Long {
        var number: Long
        var negative = false

        var c: Char
        do c = read() while (isSpace(c))

        if (c == '-') {
            negative = true
            c = read()
        }
        number = 0
        if (!isDigit(c)) {
            while (c in 'A'..'Z' || c in 'a'..'z') {
                number += flag_convert(c)
                c = read()
            }
        }

        while (isDigit(c)) {
            number = number * 10 + c.toLong() - '0'.toLong()
            c = read()
        }

        when {
            c == '|' -> number += long()
            c != ' ' -> ungetc()
        }

        return if (negative) -1 * number else number
    }

    fun read(): Char = if (currentPos == data.size) END_OF_STREAM_CHAR else data[currentPos++]

    private fun flag_convert(letter: Char): Int {
        var bitsum = 0
        var i: Char

        when (letter) {
            in 'A'..'Z' -> {
                bitsum = 1
                i = letter
                while (i > 'A') {
                    bitsum *= 2
                    i--
                }
            }
            in 'a'..'z' -> {
                bitsum = 67108864 /* 2^26 */
                i = letter
                while (i > 'a') {
                    bitsum *= 2
                    i--
                }
            }
        }
        return bitsum
    }

    /**
     * Read and allocate space for a string from a file.
     * These strings are read-only and shared.
     * Strings are hashed:
     * each string prepended with hash pointer to prev string,
     * hash code is simply the string length.
     * this function takes 40% to 50% of boot-up time.
     */
    fun string(): String {
        tmpBuf.setLength(0)

        var c: Char
        /* Skip blanks.* Read first char. */
        do c = read() while (isSpace(c))
        do {
            /*
             * Back off the char type lookup,
             *   it was too dirty for portability.
             *   -- Furey
             */
            if (c == '~') {
                return tmpBuf.toString()
            }
            if (c != '\r') {
                tmpBuf.append(c)
            }
            c = read()
        } while (true)
    }

    fun buildCurrentStateInfo(): String {
        var line = 0
        var lineStr = ""
        if (data.isNotEmpty()) {
            val pos = currentPos
            currentPos = 0
            var end: Int
            while (true) {
                line++
                end = currentPos
                while (end < data.size) {
                    if (data[end] == '\n') {
                        break
                    }
                    end++
                }
                if (end >= pos - 1) {
                    break
                }
                currentPos = end + 1//new line char
            }
            lineStr = String(data, currentPos, end - currentPos)
        }
        return "File: ${file.canonicalPath}: $line:'$lineStr'"
    }

    fun string_eol()  {
        var c: Char
        /* Skip blanks.* Read first char. */
        do c = read() while (isSpace(c))
        // int pos = currentPos - 1;
        fread_to_eol()
        // return new String(data, pos, currentPos - pos).trim();
    }

    /**
     * Read to end of line (for comments).
     */
    fun fread_to_eol() {
        var c: Char
        do c = read() while (c != '\n' && c != '\r' && c != END_OF_STREAM_CHAR)
        do c = read() while (c == '\n' || c == '\r')
        ungetc()
    }


    /** reads until space */
    fun word(): String {
        tmpBuf.setLength(0)
        var cEnd: Char
        do cEnd = read() while (isSpace(cEnd))

        if (cEnd != '\'' && cEnd != '"') {
            tmpBuf.append(cEnd)
            cEnd = ' '
        }

        while (tmpBuf.length < MAX_WORD_LENGTH) {
            val c = read()
            if (c == END_OF_STREAM_CHAR || (if (cEnd == ' ') isSpace(c) else c == cEnd)) {
                if (isSpace(c)) {
                    ungetc()
                }
                return tmpBuf.toString()
            }
            tmpBuf.append(c)
        }
        throw RuntimeException("Fread_word: word too long." + buildCurrentStateInfo())
    }

    /**
     * Read a letter from a file.
     */
    fun fread_letter(): Char {
        while (!feof()) {
            val c = read()
            if (!isSpace(c)) {
                return c
            }
        }
        return END_OF_STREAM_CHAR
    }

    /**
     * Read a number from a file.
     */
    fun int(): Int {
        var c: Char
        do {
            c = read()
        } while (isSpace(c))

        var number = 0
        var sign = false
        when (c) {
            '+' -> c = read()
            '-' -> {
                sign = true
                c = read()
            }
        }

        if (!isDigit(c)) {
            throw RuntimeException("fread_number: bad format." + buildCurrentStateInfo())

        }
        while (isDigit(c)) {
            number = number * 10 + c.toInt() - '0'.toInt()
            c = read()
        }

        if (sign) {
            number = 0 - number
        }

        when {
            c == '|' -> number += int()
            c != ' ' -> ungetc()
        }

        return number
    }

    fun ungetc() {
        assert(currentPos > 0)
        currentPos--
    }


    fun feof() = currentPos >= data.size

    fun flag(table: Array<flag_type>): Long {
        val str = word()
        return if (is_number(str)) str.toLong() else flag_type.parseFlagsValue(str, table)
    }

    fun flags(table: Array<flag_type>) = flag_type.parseFlagsValue(string(), table)
}
