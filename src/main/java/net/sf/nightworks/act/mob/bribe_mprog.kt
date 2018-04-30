package net.sf.nightworks.act.mob

import net.sf.nightworks.do_murder
import net.sf.nightworks.do_say
import net.sf.nightworks.do_sleep
import net.sf.nightworks.interpret
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.MPROG_FUN_BRIBE
import net.sf.nightworks.util.eq

fun create_bribe_prog(name: String): MPROG_FUN_BRIBE {
    return when (name) {
        "bribe_prog_beggar" -> ::bribe_prog_beggar
        "bribe_prog_cityguard" -> ::bribe_prog_cityguard
        "bribe_prog_drunk" -> { mob, _, _ -> bribe_prog_drunk(mob) }
        else -> throw IllegalArgumentException("Prog not found: $name")
    }
}

private fun bribe_prog_beggar(mob: CHAR_DATA, ch: CHAR_DATA, amount: Int) {
    when {
        amount < 10 -> {
            val name = if (!eq(mob.room.area.name, ch.hometown.displayName)) "traveler" else ch.name
            interpret(mob, "thanks $name", false)
        }
        amount < 100 -> do_say(mob, "Wow! Thank you! Thank you!")
        amount < 500 -> {
            do_say(mob, "Oh my God! Thank you! Thank you!")
            interpret(mob, "french ${ch.name}", false)
        }
        else -> {
            interpret(mob, "dance ${ch.name}", false)
            interpret(mob, "french ${ch.name}", false)
        }
    }
}

private fun bribe_prog_drunk(mob: CHAR_DATA) {
    do_say(mob, "Ahh! More Spirits!  Good Spirits!")
    interpret(mob, "sing", false)
}

private fun bribe_prog_cityguard(mob: CHAR_DATA, ch: CHAR_DATA, amount: Int) {
    when {
        amount < 100 -> {
            do_say(mob, "You cheapskate!!!")
            do_murder(mob, ch.name)
        }
        amount >= 5000 -> {
            interpret(mob, "smile", false)
            do_sleep(mob, "")
        }
        else -> do_say(mob, "Trying to bribe me, eh? It'll cost ya more than that.")
    }
}
