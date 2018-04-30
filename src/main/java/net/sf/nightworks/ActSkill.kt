package net.sf.nightworks

import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.SG
import net.sf.nightworks.model.learn
import net.sf.nightworks.util.CABAL_OK
import net.sf.nightworks.util.CLEVEL_OK
import net.sf.nightworks.util.IS_AWAKE
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.RACE_OK
import net.sf.nightworks.util.URANGE
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.util.startsWith
import java.util.Formatter

/* used to converter of prac and train */
fun do_gain(ch: CHAR_DATA, argument: String) {
    ch.pcdata ?: return

    /* find a trainer */
    val trainer = ch.room.people.firstOrNull { it.pcdata == null && (IS_SET(it.act, ACT_PRACTICE) || IS_SET(it.act, ACT_TRAIN) || IS_SET(it.act, ACT_GAIN)) }

    if (trainer == null || !can_see(ch, trainer)) {
        send_to_char("You can't do that here.\n", ch)
        return
    }

    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        do_say(trainer, "You may convert 10 practices into 1 train.")
        do_say(trainer, "You may revert 1 train into 10 practices.")
        do_say(trainer, "Simply type 'gain convert' or 'gain revert'.")
        return
    }

    if (startsWith(arg.toString(), "revert")) {
        if (ch.train < 1) {
            act("\$N tells you 'You are not yet ready.'", ch, null, trainer, TO_CHAR)
            return
        }

        act("\$N helps you apply your training to practice", ch, null, trainer, TO_CHAR)
        ch.practice += 10
        ch.train -= 1
        return
    }

    if (startsWith(arg.toString(), "convert")) {
        if (ch.practice < 10) {
            act("\$N tells you 'You are not yet ready.'", ch, null, trainer, TO_CHAR)
            return
        }

        act("\$N helps you apply your practice to training", ch, null, trainer, TO_CHAR)
        ch.practice -= 10
        ch.train += 1
        return
    }

    act("\$N tells you 'I do not understand...'", ch, null, trainer, TO_CHAR)

}

/* RT spells and skills show the players spells (or skills) */

fun do_spells(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    val spell_list = arrayOfNulls<StringBuilder>(LEVEL_HERO)
    val spell_columns = CharArray(LEVEL_HERO)
    var found = false
    val buf = StringBuilder()
    val f = Formatter(buf)
    for (sn in Skill.skills) {
        if (sn.skill_level[ch.clazz.id] < LEVEL_HERO && sn.isSpell && RACE_OK(ch, sn) &&
                (sn.cabal == ch.cabal || sn.cabal == Clan.None)) {
            buf.setLength(0)
            found = true
            val lev = sn.skill_level[ch.clazz.id]
            if (ch.level < lev) {
                f.format("%-18s  n/a      ", sn.skillName)
            } else {
                val mana = Math.max(sn.min_mana, 100 / (2 + ch.level - lev))
                f.format("%-18s  %3d mana  ", sn.skillName, mana)
            }
            var sb: StringBuilder? = spell_list[lev]
            if (sb == null) {
                sb = StringBuilder()
                spell_list[lev] = sb
            }
            if (sb.isEmpty()) {
                sb.append(Formatter().format("\nLevel %2d: %s", lev, buf).toString())
            } else
            /* append */ {
                if ((++spell_columns[lev]).toInt() % 2 == 0) {
                    sb.append("\n          ")
                }
                sb.append(buf)
            }
            buf.setLength(0)
        }
    }

    /* return results */
    if (!found) {
        send_to_char("You know no spells.\n", ch)
        return
    }

    val output = StringBuilder()
    (0 until LEVEL_HERO).mapNotNull { spell_list[it] }.forEach { output.append(it) }
    output.append("\n")
    page_to_char(output.toString(), ch)
}

fun do_skills(ch: CHAR_DATA) {
    val pcdata = ch.pcdata ?: return

    val skill_list = arrayOfNulls<StringBuilder>(LEVEL_HERO)
    val skill_columns = CharArray(LEVEL_HERO)
    val buf = StringBuilder()
    val f = Formatter(buf)
    var found = false
    val skills = Skill.skills
    for (sn in skills) {
        if (sn.skill_level[ch.clazz.id] < LEVEL_HERO && !sn.isSpell && RACE_OK(ch, sn) && (sn.cabal == ch.cabal || sn.cabal == Clan.None)) {
            found = true
            val lev = sn.skill_level[ch.clazz.id]
            if (ch.level < lev) {
                f.format("%-18s n/a      ", sn.skillName)
            } else {
                f.format("%-18s %3d%%      ", sn.skillName, pcdata.learned[sn.ordinal])
            }

            var sb: StringBuilder? = skill_list[lev]
            if (sb == null) {
                sb = StringBuilder()
                skill_list[lev] = sb
            }
            if (sb.isEmpty()) {
                sb.append(Formatter().format("\nLevel %2d: %s", lev, buf).toString())
            } else
            /* append */ {
                if ((++skill_columns[lev]).toInt() % 2 == 0) {
                    sb.append("\n          ")
                }
                sb.append(buf)
            }
            buf.setLength(0)
        }
    }

    /* return results */

    if (!found) {
        send_to_char("You know no skills.\n", ch)
        return
    }

    val output = StringBuilder()
    (0 until LEVEL_HERO).mapNotNull { skill_list[it] }.forEach { output.append(it) }
    output.append("\n")
    send_to_char(output.toString(), ch)
}


fun base_exp(ch: CHAR_DATA): Int {
    val exp = 1000 + ch.race.points + ch.clazz.points
    val mod = ch.race.getClassModifier(ch.clazz) ?: return exp
    return exp * mod.expMult / 100
}

fun exp_to_level(ch: CHAR_DATA): Int = base_exp(ch) - exp_this_level(ch)

fun exp_this_level(ch: CHAR_DATA): Int = ch.exp - ch.level * base_exp(ch)


fun exp_per_level(ch: CHAR_DATA): Int {
    val exp = 1000 + ch.race.points + ch.clazz.points
    val mod = ch.race.getClassModifier(ch.clazz) ?: return exp
    return exp * mod.expMult / 100
}

/* checks for skill improvement */
fun check_improve(ch: CHAR_DATA, sn: Skill, success: Boolean, multiplier: Int) {
    val pcdata = ch.pcdata ?: return

    if (ch.level < sn.skill_level[ch.clazz.id]
            || sn.rating[ch.clazz.id] == 0
            || pcdata.learned[sn.ordinal] == 0
            || pcdata.learned[sn.ordinal] == 100) {
        return   /* skill is not known */
    }

    /* check to see if the character has a chance to learn */
    var chance = 10 * ch.learn
    chance /= multiplier * sn.rating[ch.clazz.id] * 4
    chance += ch.level

    if (number_range(1, 1000) > chance) return

    /* now that the character has a CHANCE to learn, see if they really have */

    if (success) {
        chance = URANGE(5, 100 - pcdata.learned[sn.ordinal], 95)
        if (randomPercent() < chance) {
            act("{gYou have become better at " + sn.skillName + "!{x", ch, null, null, TO_CHAR, Pos.Dead)
            pcdata.learned[sn.ordinal]++
            gain_exp(ch, 2 * sn.rating[ch.clazz.id])
        }
    } else {
        chance = URANGE(5, pcdata.learned[sn.ordinal] / 2, 30)
        if (randomPercent() < chance) {
            act("{gYou learn from your mistakes, and your " + sn.skillName + " skill improves.{x", ch, null, null, TO_CHAR, Pos.Dead)
            pcdata.learned[sn.ordinal] += number_range(1, 3)
            pcdata.learned[sn.ordinal] = Math.min(pcdata.learned[sn.ordinal], 100)
            gain_exp(ch, 2 * sn.rating[ch.clazz.id])
        }
    }
}

fun do_slist(ch: CHAR_DATA, argument: String) {
    if (ch.pcdata == null) {
        return
    }
    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        send_to_char("syntax: slist <class name>.\n", ch)
        return
    }
    val clazz = Clazz.lookup(arg.toString())
    if (clazz == null) {
        send_to_char("That is not a valid class.\n", ch)
        return
    }
    val skill_list = arrayOfNulls<StringBuilder>(LEVEL_HERO)
    val skill_columns = CharArray(LEVEL_HERO)
    val buf = StringBuilder()
    val f = Formatter(buf)
    var found = false
    val skills = Skill.skills
    for (sn in skills) {

        if (sn.skill_level[clazz.id] < LEVEL_HERO && sn.cabal == Clan.None && sn.race == null) {
            found = true
            val lev = sn.skill_level[clazz.id]
            f.format("%-18s          ", sn.skillName)
            var sb: StringBuilder? = skill_list[lev]
            if (sb == null) {
                sb = StringBuilder()
                skill_list[lev] = sb
            }

            if (sb.isEmpty()) {
                sb.append(Formatter().format("\nLevel %2d: %s", lev, buf).toString())
            } else
            /* append */ {
                if ((++skill_columns[lev]).toInt() % 2 == 0) {
                    sb.append("\n          ")
                }
                sb.append(buf)
            }
            buf.setLength(0)
        }
    }

    /* return results */

    if (!found) {
        send_to_char("That class know no skills.\n", ch)
        return
    }

    val output = StringBuilder()
    (0 until LEVEL_HERO).mapNotNull { skill_list[it] }.forEach { output.append(it) }
    output.append("\n")
    page_to_char(output.toString(), ch)
}

/* Returns group number */
fun do_glist(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Syntax : glist <group>\n", ch)
        return
    }

    val group = SG.of(arg.toString())
    if (group == null) {
        send_to_char("That is not a valid group.\n", ch)
        return
    }

    send_to_char("Now listing group ${group.shortName} :\n", ch)
    val buf = StringBuilder()
    val f = Formatter(buf)
    val skills = Skill.skills
    skills
            .filter { !(group == SG.None && !CLEVEL_OK(ch, it) && it.group == SG.None) && group == it.group && CABAL_OK(ch, it) }
            .forEach {
                if (buf.isNotEmpty()) {
                    f.format("%-18s%-18s\n", buf, it.skillName)
                    send_to_char(buf, ch)
                    buf.setLength(0)
                } else {
                    f.format("%-18s", it.skillName)
                }
            }

}

fun do_slook(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        send_to_char("Syntax : slook <skill or spell name>.\n", ch)
        return
    }
    val sn = Skill.lookup(arg.toString())
    if (sn == null) {
        send_to_char("That is not a spell or skill.\n", ch)
        return
    }
    send_to_char("Skill :${sn.skillName} in group ${sn.group.shortName}.\n", ch)
}

private val PC_PRACTICER = 123

fun do_learn(ch: CHAR_DATA, argument: String) {
    val adept = ch.clazz.skill_adept
    val pcdata = ch.pcdata ?: return

    if (!IS_AWAKE(ch)) {
        send_to_char("In your dreams, or what?\n", ch)
        return
    }

    if (argument.isEmpty()) {
        send_to_char("Syntax: learn <skill | spell> <player>.\n", ch)
        return
    }

    if (ch.practice <= 0) {
        send_to_char("You have no practice sessions left.\n", ch)
        return
    }

    val arg = StringBuilder()
    val rest = one_argument(argument, arg)

    val sn = Skill.find_spell(ch, arg.toString())
    if (sn == null || (ch.level < sn.skill_level[ch.clazz.id] || !RACE_OK(ch, sn) || sn.cabal != ch.cabal && sn.cabal != Clan.None)) {
        send_to_char("You can't practice that.\n", ch)
        return
    }

    if (sn == Skill.vampire) {
        send_to_char("You can't practice that, only available at questor.\n", ch)
        return
    }

    one_argument(rest, arg)
    val mob = get_char_room(ch, arg.toString())
    if (mob == null) {
        send_to_char("Your hero is not here.\n", ch)
        return
    }

    if (mob.pcdata == null || mob.level != HERO) {
        send_to_char("You must find a hero , not an ordinary one.\n", ch)
        return
    }

    if (mob.status != PC_PRACTICER) {
        send_to_char("Your hero doesn't want to teach you anything.\n", ch)
        return
    }

    if (get_skill(mob, sn) < 100) {
        send_to_char("Your hero doesn't know that skill enough to teach you.\n", ch)
        return
    }

    if (pcdata.learned[sn.ordinal] >= adept) {
        send_to_char("You are already learned at " + sn.skillName + ".\n", ch)
        return
    }
    if (pcdata.learned[sn.ordinal] == 0) {
        pcdata.learned[sn.ordinal] = 1
    }
    ch.practice--
    pcdata.learned[sn.ordinal] += ch.learn / Math.max(sn.rating[ch.clazz.id], 1)
    mob.status = 0
    act("You teach \$T.", mob, null, sn.skillName, TO_CHAR)
    act("\$n teachs \$T.", mob, null, sn.skillName, TO_ROOM)
    if (pcdata.learned[sn.ordinal] < adept) {
        act("You learn \$T.", ch, null, sn.skillName, TO_CHAR)
        act("\$n learn \$T.", ch, null, sn.skillName, TO_ROOM)
    } else {
        pcdata.learned[sn.ordinal] = adept
        act("You are now learned at \$T.", ch, null, sn.skillName, TO_CHAR)
        act("\$n is now learned at \$T.", ch, null, sn.skillName, TO_ROOM)
    }
}


fun do_teach(ch: CHAR_DATA) {
    if (ch.pcdata == null || ch.level != LEVEL_HERO) {
        send_to_char("You must be a hero.\n", ch)
        return
    }
    ch.status = PC_PRACTICER
    send_to_char("Now , you can teach youngsters your 100% skills.\n", ch)
}

