package net.sf.nightworks.act.mob

import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.do_say
import net.sf.nightworks.interpret
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.MPROG_FUN_AREA
import net.sf.nightworks.util.randomPercent

fun create_area_prog(name: String): MPROG_FUN_AREA {
    return when (name) {
        "area_prog_baker" -> ::area_prog_baker
        "area_prog_drunk" -> ::area_prog_drunk
        "area_prog_grocer" -> ::area_prog_grocer
        "area_prog_janitor" -> ::area_prog_janitor
        "area_prog_vagabond" -> ::area_prog_vagabond
        else -> throw IllegalArgumentException("Prog not found: $name")
    }
}

private fun area_prog_drunk(mob: CHAR_DATA) {
    val roll = randomPercent()
    when {
        roll < 5 -> interpret(mob, "dance", false)
        roll < 10 -> interpret(mob, "sing", false)
    }
}

private fun area_prog_janitor(mob: CHAR_DATA) {
    if (randomPercent() < 20) {
        interpret(mob, "grumble", false)
        do_say(mob, "Litterbugs")
        if (randomPercent() < 20) {
            do_say(mob, "All I do each day is cleanup other people's messes.")
            if (randomPercent() < 20) {
                do_say(mob, "I do not get paid enough.")
            } else if (randomPercent() < 20) {
                do_say(mob, "Day in. Day out. This is all I do in 24 hours a day.")
                if (randomPercent() < 10) {
                    do_yell(mob, "I want a vacation!")
                }
            }
        }
    }
}

private fun area_prog_vagabond(mob: CHAR_DATA) {
    if (randomPercent() < 10) {
        do_say(mob, "Kill! Blood! Gore!")
    }
}

private fun area_prog_baker(mob: CHAR_DATA) {
    if (randomPercent() < 5) {
        do_say(mob, "Would you like to try some tasty pies?")
    }
}

private fun area_prog_grocer(mob: CHAR_DATA) {
    if (randomPercent() < 5) {
        do_say(mob, "Can I interest you in a lantern today?")
    }
}