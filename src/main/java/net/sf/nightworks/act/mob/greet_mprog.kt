@file:Suppress("FunctionName")

import net.sf.nightworks.OBJ_VNUM_EYED_SWORD
import net.sf.nightworks.OFF_AREA_ATTACK
import net.sf.nightworks.QUEST_EYE
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_NOTVICT
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.act
import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.can_see
import net.sf.nightworks.create_object
import net.sf.nightworks.current_time
import net.sf.nightworks.do_cb
import net.sf.nightworks.do_close
import net.sf.nightworks.do_get
import net.sf.nightworks.do_give
import net.sf.nightworks.do_lock
import net.sf.nightworks.do_murder
import net.sf.nightworks.do_open
import net.sf.nightworks.do_say
import net.sf.nightworks.do_slay
import net.sf.nightworks.do_unlock
import net.sf.nightworks.get_obj_carry
import net.sf.nightworks.get_obj_index_nn
import net.sf.nightworks.interpret
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.EXTRA_DESC_DATA
import net.sf.nightworks.model.MPROG_FUN_GREET
import net.sf.nightworks.obj_to_char
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.util.sprintf

fun create_greet_prog(name: String): MPROG_FUN_GREET = when (name) {
    "greet_prog_armourer" -> ::greet_prog_armourer
    "greet_prog_baker" -> ::greet_prog_baker
    "greet_prog_battle" -> ::greet_prog_battle
    "greet_prog_beggar" -> ::greet_prog_beggar
    "greet_prog_chaos" -> ::greet_prog_chaos
    "greet_prog_drunk" -> ::greet_prog_drunk
    "greet_prog_fireflash" -> ::greet_prog_fireflash
    "greet_prog_grocer" -> ::greet_prog_grocer
    "greet_prog_hunter" -> ::greet_prog_hunter
    "greet_prog_hunter_old" -> ::greet_prog_hunter_old
    "greet_prog_invader" -> ::greet_prog_invader
    "greet_prog_keeper" -> ::greet_prog_keeper
    "greet_prog_knight" -> ::greet_prog_knight
    "greet_prog_lions" -> ::greet_prog_lions
    "greet_prog_ruler" -> ::greet_prog_ruler
    "greet_prog_ruler_pre" -> ::greet_prog_ruler_pre
    "greet_prog_shalafi" -> ::greet_prog_shalafi
    "greet_prog_solamnia" -> ::greet_prog_solamnia
    "greet_prog_templeman" -> ::greet_prog_templeman
    else -> throw IllegalArgumentException("Prog not found: $name")
}

private fun greet_prog_shalafi(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    mob.cabal = Clan.Shalafi
    mob.off_flags = SET_BIT(mob.off_flags, OFF_AREA_ATTACK)

    if (ch.cabal == Clan.Shalafi) {
        do_say(mob, "Greetings, wise one.")
        return
    }

    if (ch.last_death_time != -1L && current_time - ch.last_death_time < 600) {
        do_say(mob, "Ghosts are not allowed in this place.")
        do_slay(mob, ch.name)
        return
    }

    if (IS_IMMORTAL(ch)) {
        return
    }

    do_cb(mob, "Intruder! Intruder!")
    do_say(mob, "You should never disturb my cabal!")
}

private fun greet_prog_invader(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    mob.cabal = Clan.Invader
    mob.off_flags = SET_BIT(mob.off_flags, OFF_AREA_ATTACK)

    if (ch.cabal == Clan.Invader) {
        do_say(mob, "Greetings, dark one.")
        return
    }
    if (ch.last_death_time != -1L && current_time - ch.last_death_time < 600) {
        do_say(mob, "Ghosts are not allowed in this place.")
        do_slay(mob, ch.name)
        return
    }

    if (IS_IMMORTAL(ch)) {
        return
    }

    do_cb(mob, "Intruder! Intruder!")
    if (ch.pcdata != null) {
        do_say(mob, "You should never disturb my cabal!")
    }
}

private fun greet_prog_ruler_pre(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    if (ch.cabal == Clan.Ruler) {
        val buf = TextBuffer()
        buf.sprintf("bow %s", ch.name)
        interpret(mob, buf.toString(), false)
        return
    }

    do_say(mob, "Do not go further and leave the square.")
    do_say(mob, "This place is private.")
}

private fun greet_prog_ruler(mob: CHAR_DATA, ch: CHAR_DATA) {

    if (ch.pcdata == null) {
        return
    }

    mob.cabal = Clan.Ruler
    mob.off_flags = SET_BIT(mob.off_flags, OFF_AREA_ATTACK)

    if (ch.cabal == Clan.Ruler) {
        val buf = TextBuffer()
        buf.sprintf("bow %s", ch.name)
        interpret(mob, buf.toString(), false)
        return
    }
    if (ch.last_death_time != -1L && current_time - ch.last_death_time < 600) {
        do_say(mob, "Ghosts are not allowed in this place.")
        do_slay(mob, ch.name)
        return
    }

    if (IS_IMMORTAL(ch)) {
        return
    }

    do_cb(mob, "Intruder! Intruder!")
    do_say(mob, "You should never disturb my cabal!")
}

private fun greet_prog_chaos(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    mob.cabal = Clan.Chaos
    mob.off_flags = SET_BIT(mob.off_flags, OFF_AREA_ATTACK)

    if (ch.cabal == Clan.Chaos) {
        do_say(mob, "Greetings, chaotic one.")
        return
    }
    if (ch.last_death_time != -1L && current_time - ch.last_death_time < 600) {
        do_say(mob, "Ghosts are not allowed in this place.")
        do_slay(mob, ch.name)
        return
    }

    if (IS_IMMORTAL(ch)) {
        return
    }

    do_cb(mob, "Intruder! Intruder!")
    do_say(mob, "You should never disturb my cabal!")
}

private fun greet_prog_battle(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    mob.cabal = Clan.BattleRager
    mob.off_flags = SET_BIT(mob.off_flags, OFF_AREA_ATTACK)

    if (ch.cabal == Clan.BattleRager) {
        do_say(mob, "Welcome, great warrior.")
        return
    }
    if (ch.last_death_time != -1L && current_time - ch.last_death_time < 600) {
        do_say(mob, "Ghosts are not allowed in this place.")
        do_slay(mob, ch.name)
        return
    }

    if (IS_IMMORTAL(ch)) {
        return
    }

    do_cb(mob, "Intruder! Intruder!")
    do_say(mob, "You should never disturb my cabal!")
}

private fun greet_prog_fireflash(mob: CHAR_DATA, ch: CHAR_DATA) {

    if (!can_see(mob, ch) || ch.pcdata == null || IS_IMMORTAL(ch)) {
        return
    }

    if (get_obj_carry(ch, "rug") == null) {
        do_say(mob, "I don't want to see that worthless rug anywhere near me.")
        do_say(mob, "Why don't you give it to that silly Green sister from Tear.")
        do_unlock(mob, "box")
        do_open(mob, "box")
        do_get(mob, "papers box")
        do_say(mob, "These papers might help you.")
        act("\$n sneers at you.", mob, null, ch, TO_VICT)
        act("You sneer at \$N.", mob, null, ch, TO_CHAR)
        act("\$n sneers at \$N.", mob, null, ch, TO_NOTVICT)
        val buf = TextBuffer()
        buf.sprintf("papers %s", ch.name)
        do_give(mob, buf.toString())
        do_close(mob, "box")
        do_lock(mob, "box")
    }
}

private fun greet_prog_solamnia(mob: CHAR_DATA, ch: CHAR_DATA) {

    if (!can_see(mob, ch) || ch.pcdata == null || IS_IMMORTAL(ch)) {
        return
    }

    if (get_obj_carry(ch, "xxx") != null) {
        do_say(mob, "I think you bring something for me....")
        interpret(mob, "smile", false)
    }
}

private fun greet_prog_knight(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    mob.cabal = Clan.Knight
    mob.off_flags = SET_BIT(mob.off_flags, OFF_AREA_ATTACK)

    if (ch.cabal == Clan.Knight) {
        do_say(mob, "Welcome, honorable one.")
        return
    }
    if (ch.last_death_time != -1L && current_time - ch.last_death_time < 600) {
        do_say(mob, "Ghosts are not allowed in this place.")
        do_slay(mob, ch.name)
        return
    }

    if (IS_IMMORTAL(ch)) {
        return
    }

    do_cb(mob, "Intruder! Intruder!")
    do_say(mob, "You should never disturb my cabal!")
}

private fun greet_prog_keeper(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }
    if (!can_see(mob, ch)) {
        return
    }
    do_say(mob, "What business do you have here?  Is it that dress I ordered?")
}

private fun greet_prog_templeman(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (!can_see(mob, ch) || ch.pcdata == null || IS_IMMORTAL(ch)) {
        return
    }
    val buf = TextBuffer()
    sprintf(buf, "smile %s", ch.name)
    interpret(mob, buf.toString(), false)
}

private fun greet_prog_lions(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    mob.cabal = Clan.Lion
    mob.off_flags = SET_BIT(mob.off_flags, OFF_AREA_ATTACK)

    if (ch.cabal == Clan.Lion) {
        do_say(mob, "Welcome, my Lions.")
        return
    }
    if (ch.last_death_time != -1L && current_time - ch.last_death_time < 600) {
        do_say(mob, "Ghosts are not allowed in this place.")
        do_slay(mob, ch.name)
        return
    }

    if (IS_IMMORTAL(ch)) {
        return
    }

    do_cb(mob, "Intruder! Intruder!")
    do_say(mob, "You should never disturb my cabal!")
}

private fun greet_prog_hunter_old(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    mob.cabal = Clan.Hunter
    mob.off_flags = SET_BIT(mob.off_flags, OFF_AREA_ATTACK)

    if (ch.cabal == Clan.Hunter) {
        do_say(mob, "Welcome, my dear hunter.")
        return
    }
    if (ch.last_death_time != -1L && current_time - ch.last_death_time < 600) {
        do_say(mob, "Ghosts are not allowed in this place.")
        do_slay(mob, ch.name)
        return
    }

    if (IS_IMMORTAL(ch)) {
        return
    }

    do_cb(mob, "Intruder! Intruder!")
    do_say(mob, "You should never disturb my cabal!")
}

private fun greet_prog_hunter(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    mob.cabal = Clan.Hunter
    mob.off_flags = SET_BIT(mob.off_flags, OFF_AREA_ATTACK)

    if (ch.cabal == Clan.Hunter) {

        do_say(mob, "Welcome, my dear hunter.")
        if (IS_SET(ch.quest, QUEST_EYE)) {
            return
        }

        ch.quest = SET_BIT(ch.quest, QUEST_EYE)

        val i = when {
            ch.isGood() -> 0
            ch.isEvil() -> 2
            else -> 1
        }

        val eyed = create_object(get_obj_index_nn(OBJ_VNUM_EYED_SWORD), 0)
        eyed.owner = ch.name
        eyed.from = ch.name
        eyed.altar = ch.hometown.altar[i]
        eyed.pit = ch.hometown.pit[i]
        eyed.level = ch.level
        val buf = TextBuffer()
        buf.sprintf(eyed.short_desc, ch.name)
        eyed.short_desc = buf.toString()

        buf.sprintf(eyed.pIndexData.extra_descr!!.description, ch.name)
        eyed.extra_desc = EXTRA_DESC_DATA()
        eyed.extra_desc.keyword = eyed.pIndexData.extra_descr!!.keyword
        eyed.extra_desc.description = buf.toString()
        eyed.extra_desc.next = null

        eyed.value[2] = (ch.level / 10 + 3).toLong()
        eyed.level = ch.level
        eyed.cost = 0
        obj_to_char(eyed, mob)
        interpret(mob, "emote creates the Hunter's Sword.", false)
        do_say(mob, "I gave you the hunter's sword to you.")
        buf.sprintf("give eyed %s", ch.name)
        interpret(mob, buf.toString(), false)
        do_say(mob, "Remember that if you lose that, you can want it from cabal cleric!")
        do_say(mob, "Simple say to him that 'trouble'")
        return
    }
    if (ch.last_death_time != -1L && current_time - ch.last_death_time < 600) {
        do_say(mob, "Ghosts are not allowed in this place.")
        do_slay(mob, ch.name)
        return
    }

    if (IS_IMMORTAL(ch)) {
        return
    }

    do_cb(mob, "Intruder! Intruder!")
    do_say(mob, "You should never disturb my cabal!")
}

private fun greet_prog_baker(mob: CHAR_DATA, ch: CHAR_DATA) {

    if (!can_see(mob, ch) || ch.pcdata == null || IS_IMMORTAL(ch)) {
        return
    }
    interpret(mob, "smile", false)
    val buf = TextBuffer()
    val name = if (eq(mob.room.area.name, ch.hometown.displayName)) ch.name else "traveler"
    buf.sprintf("Welcome to my Bakery, %s", name)
    do_say(mob, buf.toString())
}

private fun greet_prog_beggar(mob: CHAR_DATA, ch: CHAR_DATA) {

    if (!can_see(mob, ch) || ch.pcdata == null || IS_IMMORTAL(ch)) {
        return
    }
    val buf = TextBuffer()
    buf.sprintf("Beg %s", if (!eq(mob.room.area.name, ch.hometown.displayName)) "traveler" else ch.name)
    do_say(mob, buf.toString())
    do_say(mob, "Spare some gold?")
}

private fun greet_prog_drunk(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (!can_see(mob, ch) || ch.pcdata == null || IS_IMMORTAL(ch)) {
        return
    }
    if (randomPercent() < 5) {
        do_yell(mob, "Monster! I found a monster! Kill! Banzai!")
        do_murder(mob, ch.name)
    }
}

private fun greet_prog_grocer(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (!can_see(mob, ch) || ch.pcdata == null || IS_IMMORTAL(ch)) {
        return
    }
    val name = if (eq(mob.room.area.name, ch.hometown.displayName)) ch.name else "traveler"
    do_say(mob, "Welcome to my Store, $name")
}

private fun greet_prog_armourer(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (!can_see(mob, ch) || ch.pcdata == null || IS_IMMORTAL(ch)) {
        return
    }
    interpret(mob, "smile", false)
    val buf = TextBuffer()
    val name = if (eq(mob.room.area.name, ch.hometown.displayName)) ch.name else "traveler"
    buf.sprintf("Welcome to my Armoury, %s", name)
    do_say(mob, buf.toString())
    do_say(mob, "What can I interest you in?")
    do_say(mob, "I have only the finest armor in my store.")
    interpret(mob, "emote beams with pride.", false)
}
