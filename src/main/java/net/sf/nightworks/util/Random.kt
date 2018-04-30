package net.sf.nightworks.util

import java.util.Random

private val rnd: Random = Random(System.currentTimeMillis())

/** Stick a little fuzz on a number.*/
fun number_fuzzy(numberArg: Int): Int {
    var number = numberArg
    when (random(4)) {
        0 -> number -= 1
        3 -> number += 1
    }
    return Math.max(1, number)
}

/** Generate a random number in [from, to] range */
fun number_range(from: Int, to: Int): Int {
    if (from >= to) {
        return from
    }
    val range = to - from
    return from + Math.round(rnd.nextDouble() * range).toInt()
}

/** Generate a percentile roll: [0, 100] */
fun randomPercent(): Int = rnd.nextInt(101)

/** Generate a random door [0, 5] */
fun randomDir(): Int = rnd.nextInt(6)

fun random(bound: Int) = rnd.nextInt(bound)

/** Return true or false with 50% chance for each */
fun flipCoin() = rnd.nextBoolean()

fun oneChanceOf(c: Int) = random(c) == 0

fun chance(percent: Int) = percent >= number_range(1, 100)


/**
 * Roll some dice.
 * TODO: find all toInt and fix usin separate data structure to hold int values
 */
fun dice(number: Int, size: Int): Int {
    when (size) {
        0 -> return 0
        1 -> return number
    }

    var sum = 0
    for (idice in 0 until number) {
        sum += number_range(1, size)
    }

    return sum
}
