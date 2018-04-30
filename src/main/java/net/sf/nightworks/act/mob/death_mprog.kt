package net.sf.nightworks.act.mob

import net.sf.nightworks.do_cb
import net.sf.nightworks.do_say
import net.sf.nightworks.interpret
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.MPROG_FUN_DEATH
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.util.TextBuffer

fun create_death_prog(name: String): MPROG_FUN_DEATH = when (name) {
    "death_prog_beggar" -> ::death_prog_beggar
    "death_prog_stalker" -> ::death_prog_stalker
    "death_prog_vagabond" -> ::death_prog_vagabond
    else -> throw IllegalArgumentException("Prog not found: $name")
}

private fun death_prog_stalker(mob: CHAR_DATA): Boolean {
    mob.cabal = Clan.Ruler
    val buf = TextBuffer()
    buf.sprintf("I have failed trying to kill %s, I gasp my last breath.", mob.last_fought!!.name)
    do_cb(mob, buf.toString())
    return false
}

private fun death_prog_beggar(mob: CHAR_DATA): Boolean {
    when {
        randomPercent() < 50 -> do_say(mob, "Now I go to a better place.")
        else -> do_say(mob, "Forgive me God for I have sinned...")
    }
    return false
}

private fun death_prog_vagabond(mob: CHAR_DATA): Boolean {
    interpret(mob, "emote throws back his head and cackles with insane glee!", false)
    return false
}