package net.sf.nightworks.act.mob

import net.sf.nightworks.Clazz
import net.sf.nightworks.DICE_BONUS
import net.sf.nightworks.DICE_NUMBER
import net.sf.nightworks.DICE_TYPE
import net.sf.nightworks.Index
import net.sf.nightworks.Skill
import net.sf.nightworks.TARGET_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.util.bug
import net.sf.nightworks.do_murder
import net.sf.nightworks.do_rescue
import net.sf.nightworks.do_say
import net.sf.nightworks.find_path
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.MPROG_FUN_FIGHT
import net.sf.nightworks.model.Pos
import net.sf.nightworks.move_char
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.util.number_range
import net.sf.nightworks.say_spell
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.random

fun create_fight_prog(name: String): MPROG_FUN_FIGHT = when (name) {
    "fight_prog_beggar" -> { mob, _ -> fight_prog_beggar(mob) }
    "fight_prog_diana" -> ::fight_prog_diana
    "fight_prog_golem" -> { mob, _ -> fight_prog_golem(mob) }
    "fight_prog_ofcol_guard" -> ::fight_prog_ofcol_guard
    else -> throw IllegalArgumentException("Prog not found: $name")
}

private fun fight_prog_diana(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (randomPercent() < 25) {
        return
    }
    if (mob.room.area != mob.room.area) {
        return
    }

    do_yell(mob, "Help me guards!")
    for (ach in Index.CHARS) {
        if (ach.room.area != ch.room.area || ach.pcdata != null) {
            continue
        }
        if (ach.pIndexData.vnum == 600 || ach.pIndexData.vnum == 603) {
            if (ach.fighting != null || ach.last_fought != null) {
                continue
            }
            if (mob.room == ach.room) {

                act("{b\$n call the gods for help.{x", ach, null, null, TO_ROOM, Pos.Sleeping)
                act("{gGods advance \$n to help Diana.{x", ach, null, null, TO_ROOM, Pos.Sleeping)
                ach.max_hit = 6000
                ach.hit = 6000
                ach.level = 60
                ach.timer = 0
                ach.damage[DICE_NUMBER] = number_range(3, 5)
                ach.damage[DICE_TYPE] = number_range(12, 22)
                ach.damage[DICE_BONUS] = number_range(6, 8)
                ach.perm_stat.fill(23)
                do_say(ach, "Diana, I came.")
                do_murder(ach, ch.name)
                continue
            }
            val door = find_path(ach.room.vnum, mob.room.vnum, ach, -40, true)
            if (door == -1) {
                bug("Couldn't find a path with -40")
            } else {
                when {
                    randomPercent() < 25 -> do_yell(ach, " Keep on Diana!.I am coming.")
                    else -> do_say(ach, "I must find Diana to help.")
                }
                move_char(ach, door)
            }
        }
    }
}

private fun fight_prog_ofcol_guard(mob: CHAR_DATA, ch: CHAR_DATA) {
    if (randomPercent() < 25) {
        return
    }
    val buf = TextBuffer()
    buf.sprintf("Help guards. %s is fighting with me.", ch.name)
    do_yell(mob, buf.toString())
    for (ach in Index.CHARS) {
        if (ach.room.area != ch.room.area || ach.pcdata != null) {
            continue
        }
        if (ach.pIndexData.vnum == 600) {
            if (ach.fighting != null) {
                continue
            }
            if (mob.room == ach.room) {
                buf.sprintf("Now %s , you will pay for attacking a guard.", ch.name)
                do_say(ach, buf.toString())
                do_murder(ach, ch.name)
                continue
            }
            val door = find_path(ach.room.vnum, mob.room.vnum, ach, -40, true)
            if (door == -1) {
                bug("Couldn't find a path with -40")
            } else {
                if (randomPercent() < 25) {
                    do_yell(ach, " Keep on Guard! I am coming.")
                } else {
                    do_say(ach, "I must go the guard to help.")
                }
                move_char(ach, door)
            }
        }
    }
}

private fun fight_prog_beggar(mob: CHAR_DATA) {
    if (mob.hit < mob.max_hit * 0.45 && mob.hit > mob.max_hit * 0.55) {
        do_say(mob, "Halfway to death...")
    }
}

private fun fight_prog_golem(mob: CHAR_DATA) {
    val master = mob.room.people.firstOrNull { it.pcdata != null && mob.master == it && it.clazz === Clazz.NECROMANCER }
    val fighting = master?.fighting ?: return
    val buf = TextBuffer()
    if (fighting == master) {
        buf.sprintf("%s", master.name)
        do_rescue(mob, buf.toString())
    }

    val spell = when (random(16)) {
        0 -> "curse"
        1 -> "weaken"
        2 -> "chill touch"
        3 -> "blindness"
        4 -> "poison"
        5 -> "energy drain"
        6 -> "harm"
        7 -> "teleport"
        8 -> "plague"
        else -> return
    }
    val sn = Skill.lookup(spell) ?: return
    val m_next = if (mob.fighting != null) mob.fighting else master.fighting
    if (m_next != null) {
        say_spell(mob, sn)
        sn.spell_fun(mob.level, mob, m_next, TARGET_CHAR)
    }
}