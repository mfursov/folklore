package net.sf.nightworks.act.info

import net.sf.nightworks.AC_BASH
import net.sf.nightworks.AC_EXOTIC
import net.sf.nightworks.AC_PIERCE
import net.sf.nightworks.AC_SLASH
import net.sf.nightworks.COMM_SHOW_AFFECTS
import net.sf.nightworks.COND_BLOODLUST
import net.sf.nightworks.COND_DESIRE
import net.sf.nightworks.COND_DRUNK
import net.sf.nightworks.COND_HUNGER
import net.sf.nightworks.COND_THIRST
import net.sf.nightworks.Clazz
import net.sf.nightworks.FIGHT_DELAY_TIME
import net.sf.nightworks.LEVEL_HERO
import net.sf.nightworks.PLR_HOLYLIGHT
import net.sf.nightworks.can_carry_items
import net.sf.nightworks.can_carry_weight
import net.sf.nightworks.current_time
import net.sf.nightworks.exp_to_level
import net.sf.nightworks.get_age
import net.sf.nightworks.get_trust
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Align
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.Religion
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.model.get_stat_alias
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.GET_AC
import net.sf.nightworks.util.GET_DAMROLL
import net.sf.nightworks.util.GET_HITROLL
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.get_carry_weight

fun do_score(ch: CHAR_DATA) {
    val buf = TextBuffer()
    val pcdata = ch.pcdata

    buf.sprintf("You are {Y%s{x%s, level {Y%d{x, {W%d{x years old (%d hours).\n",
            ch.name, pcdata?.title ?: "", ch.level, get_age(ch),
            (ch.played + (current_time - ch.logon).toInt()) / 3600)
    send_to_char(buf, ch)

    if (get_trust(ch) != ch.level) {
        buf.sprintf("You are trusted at level %d.\n", get_trust(ch))
        send_to_char(buf, ch)
    }

    buf.sprintf("Race: {B%s{x  Sex: {B%s{x  Class: {B%s{x  Hometown: {B%s{x\n",
            ch.race.name, ch.sex.title, if (pcdata == null) "mobile" else ch.clazz.name, ch.hometown.displayName)
    send_to_char(buf, ch)

    buf.sprintf("You have %d/{w%d{x hit, %d/{w%d{x mana, %d/{w%d{x movement.\n",
            ch.hit, ch.max_hit, ch.mana, ch.max_mana, ch.move, ch.max_move)

    send_to_char(buf, ch)

    buf.sprintf("You have {B%d{x practices and {B%d{x training sessions.\n",
            ch.practice, ch.train)
    send_to_char(buf, ch)

    buf.sprintf("You are carrying {c%d{x/{B%d{x items with weight {c%d{x/{B%d{x pounds.\n",
            ch.carry_number, can_carry_items(ch), get_carry_weight(ch), can_carry_weight(ch))
    send_to_char(buf, ch)

    if (ch.level > 20 || pcdata == null) {
        buf.sprintf("Str: {Y%d{b({Y%d{B)  Int: {Y%d{b({Y%d{b)  Wis: {Y%d{b({Y%d{b)  Dex: {Y%d{b({Y%d{b)  Con: {Y%d{b({Y%d{b) Cha: {Y%d{b({Y%d{b)\n",
                ch.perm_stat[PrimeStat.Strength], ch.curr_stat(PrimeStat.Strength), ch.perm_stat[PrimeStat.Intelligence], ch.curr_stat(PrimeStat.Intelligence), ch.perm_stat[PrimeStat.Wisdom], ch.curr_stat(PrimeStat.Wisdom), ch.perm_stat[PrimeStat.Dexterity], ch.curr_stat(PrimeStat.Dexterity), ch.perm_stat[PrimeStat.Constitution], ch.curr_stat(PrimeStat.Constitution), ch.perm_stat[PrimeStat.Charisma], ch.curr_stat(PrimeStat.Charisma))
        send_to_char(buf, ch)
    } else {
        buf.sprintf(
                "Str: {Y%-9s{x Wis: {Y%-9s{x Con: {Y%-9s{x\nInt: {Y%-9s{x Dex: {Y%-9s{x Cha: {Y%-11s{x\n",
                get_stat_alias(ch, PrimeStat.Strength),
                get_stat_alias(ch, PrimeStat.Wisdom),
                get_stat_alias(ch, PrimeStat.Constitution),
                get_stat_alias(ch, PrimeStat.Intelligence),
                get_stat_alias(ch, PrimeStat.Dexterity),
                get_stat_alias(ch, PrimeStat.Charisma))

        send_to_char(buf, ch)
    }

    buf.sprintf("You have scored {m%d{x exp, and have {B%s%s%s{x.\n",
            ch.exp, if (ch.gold + ch.silver == 0) "no money" else if (ch.gold != 0) "%d gold " else "",
            if (ch.silver != 0) "%d silver " else "", if (ch.gold + ch.silver != 0) if (ch.gold + ch.silver == 1) "coin" else "coins" else "")
    val buf2 = TextBuffer()
    if (ch.gold != 0) {
        buf2.sprintf(buf.toString(), ch.gold, ch.silver)
    } else {
        buf2.sprintf(buf.toString(), ch.silver)
    }

    send_to_char(buf2, ch)

    /* KIO shows exp to level */
    if (pcdata != null && ch.level < LEVEL_HERO) {
        buf.sprintf("You need {c%d{x exp to level.\n", exp_to_level(ch))
        send_to_char(buf, ch)
    }

    if (pcdata != null) {
        buf.sprintf("Quest Points: {B%d{x.  Next Quest Time: {B%d{x.\n", pcdata.questpoints, pcdata.nextquest)
        send_to_char(buf, ch)
    }

    if (ch.clazz !== Clazz.SAMURAI) {
        buf.sprintf("Wimpy set to {r%d{x hit points.  ", ch.wimpy)
        send_to_char(buf, ch)
    } else {
        if (pcdata != null) {
            buf.sprintf("Total {r%d{x deaths up to now.", pcdata.death)
            send_to_char(buf, ch)
        }
    }
    val guarding = ch.guarding
    if (guarding != null) {
        buf.sprintf("You are guarding: {g%s{x  ", guarding.name)
        send_to_char(buf, ch)
    }

    val guarded_by = ch.guarded_by
    if (guarded_by != null) {
        buf.sprintf("You are guarded by: {g%s{x", guarded_by.name)
        send_to_char(buf, ch)
    }

    send_to_char("\n", ch)
    if (pcdata != null) {
        if (pcdata.condition[COND_DRUNK] > 10) {
            send_to_char("You are drunk.\n", ch)
        }
        if (pcdata.condition[COND_THIRST] <= 0) {
            send_to_char("You are thirsty.\n", ch)
        }
        /*    if ( !IS_NPC(ch) && ch.pcdata.condition[COND_FULL]   ==  0 ) */
        if (pcdata.condition[COND_HUNGER] <= 0) {
            send_to_char("You are hungry.\n", ch)
        }
        if (pcdata.condition[COND_BLOODLUST] <= 0) {
            send_to_char("You are hungry for blood.\n", ch)
        }
        if (pcdata.condition[COND_DESIRE] <= 0) {
            send_to_char("You are desiring your home.\n", ch)
        }
    }

    when (ch.position) {
        Pos.Dead -> send_to_char("{rYou are DEAD!!\n", ch)
        Pos.Mortal -> send_to_char("{rYou are mortally wounded.\n", ch)
        Pos.Incap -> send_to_char("{rYou are incapacitated.\n", ch)
        Pos.Stunned -> send_to_char("{rYou are stunned.\n", ch)
        Pos.Sleeping -> {
            send_to_char("{yYou are sleeping.", ch)
            if (ch.last_fight_time != -1L && !IS_IMMORTAL(ch) && current_time - ch.last_fight_time < FIGHT_DELAY_TIME) {
                send_to_char("Your adrenalin is gushing!\n", ch)
            } else {
                send_to_char("\n", ch)
            }
        }
        Pos.Resting -> {
            send_to_char("{bYou are resting.", ch)
            if (ch.last_fight_time != -1L && !IS_IMMORTAL(ch) && current_time - ch.last_fight_time < FIGHT_DELAY_TIME) {
                send_to_char("Your adrenalin is gushing!\n", ch)
            } else {
                send_to_char("\n", ch)
            }
        }
        Pos.Standing -> {
            send_to_char("{cYou are standing.", ch)
            if (ch.last_fight_time != -1L && !IS_IMMORTAL(ch) &&
                    current_time - ch.last_fight_time < FIGHT_DELAY_TIME) {
                send_to_char("Your adrenalin is gushing!\n", ch)
            } else {
                send_to_char("\n", ch)
            }
        }
        Pos.Fighting -> {
            send_to_char("{rYou are fighting.", ch)
            if (ch.last_fight_time != -1L && !IS_IMMORTAL(ch) &&
                    current_time - ch.last_fight_time < FIGHT_DELAY_TIME) {
                send_to_char("Your adrenalin is gushing!\n", ch)
            } else {
                send_to_char("\n", ch)
            }
        }
        else -> {
        }
    }
    send_to_char("{x", ch)

    /* print AC values */
    if (ch.level >= 25) {
        buf.sprintf("Armor: pierce: {B%d{x  bash: {B%d{x  slash: {B%d{x  magic: {B%d{x\n",
                GET_AC(ch, AC_PIERCE), GET_AC(ch, AC_BASH), GET_AC(ch, AC_SLASH), GET_AC(ch, AC_EXOTIC))
        send_to_char(buf, ch)
    }

    val temp = TextBuffer()
    for (i in 0..3) {
        when (i) {
            AC_PIERCE -> temp.sprintf("{rpiercing{x")
            AC_BASH -> temp.sprintf("{rbashing{x")
            AC_SLASH -> temp.sprintf("{rslashing{x")
            AC_EXOTIC -> temp.sprintf("{rmagic{x")
            else -> temp.sprintf("{rerror{x")
        }

        send_to_char("You are ", ch)

        when {
            GET_AC(ch, i) >= 101 -> buf.sprintf("hopelessly vulnerable to %s.\n", temp)
            GET_AC(ch, i) >= 80 -> buf.sprintf("defenseless against %s.\n", temp)
            GET_AC(ch, i) >= 60 -> buf.sprintf("barely protected from %s.\n", temp)
            GET_AC(ch, i) >= 40 -> buf.sprintf("slightly armored against %s.\n", temp)
            GET_AC(ch, i) >= 20 -> buf.sprintf("somewhat armored against %s.\n", temp)
            GET_AC(ch, i) >= 0 -> buf.sprintf("armored against %s.\n", temp)
            GET_AC(ch, i) >= -20 -> buf.sprintf("well-armored against %s.\n", temp)
            GET_AC(ch, i) >= -40 -> buf.sprintf("very well-armored against %s.\n", temp)
            GET_AC(ch, i) >= -60 -> buf.sprintf("heavily armored against %s.\n", temp)
            GET_AC(ch, i) >= -80 -> buf.sprintf("superbly armored against %s.\n", temp)
            GET_AC(ch, i) >= -100 -> buf.sprintf("almost invulnerable to %s.\n", temp)
            else -> buf.sprintf("divinely armored against %s.\n", temp)
        }

        send_to_char(buf, ch)
    }

    /* RT wizinvis and holy light */
    if (IS_IMMORTAL(ch)) {
        send_to_char("{gHoly Light: ", ch)
        if (IS_SET(ch.act, PLR_HOLYLIGHT)) {
            send_to_char("on", ch)
        } else {
            send_to_char("off", ch)
        }
        send_to_char("{x", ch)

        if (ch.invis_level != 0) {
            buf.sprintf("  Invisible: level %d", ch.invis_level)
            send_to_char(buf, ch)
        }

        if (ch.incog_level != 0) {
            buf.sprintf("  Incognito: level %d", ch.invis_level)
            send_to_char(buf, ch)
        }
        send_to_char("\n", ch)

    }
    if (ch.level >= 20) {
        buf.sprintf("Hitroll: {Y%d{x  Damroll: {Y%d{x.\n", GET_HITROLL(ch), GET_DAMROLL(ch))
        send_to_char(buf, ch)
    }

    send_to_char("You are ", ch)
    when (ch.align) {
        Align.Good -> send_to_char("{Wgood.  ", ch)
        Align.Evil -> send_to_char("{revil.  ", ch)
        else -> send_to_char("{cneutral.  ", ch)
    }
    send_to_char("{x", ch)

    send_to_char("You have a ${ch.ethos.title} ethos.", ch)
    if (pcdata == null) {
        ch.religion = Religion.None
    }
    if (ch.religion.isNone()) {
        send_to_char("You don't believe any religion.\n", ch)
    } else {
        buf.sprintf("Your religion is the way of {B%s{x.\n", ch.religion.leader)
        send_to_char(buf, ch)
    }
    if (ch.affected != null && IS_SET(ch.comm, COMM_SHOW_AFFECTS)) {
        send_to_char("You are affected by:\n", ch)
        var paf: Affect? = ch.affected
        while (paf != null) {
            buf.sprintf("{rSpell{x: '{Y%s{x'", paf.type.skillName)
            send_to_char(buf, ch)

            if (ch.level >= 20) {
                if (paf.duration != -1 && paf.duration != -2) {
                    buf.sprintf(" modifies {m%s{x by {m%d{x for {m%d{x hours", paf.location.locName, paf.modifier, paf.duration)
                } else {
                    buf.sprintf(" modifies {m%s{x by {m%d{x {cpermanently{x", paf.location.locName, paf.modifier)
                    send_to_char(buf, ch)
                }

                send_to_char(".\n", ch)
            }
            paf = paf.next
        }

    }
}