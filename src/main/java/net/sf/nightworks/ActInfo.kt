package net.sf.nightworks

import net.sf.nightworks.act.info.do_help
import net.sf.nightworks.act.info.do_look
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.HomeTown.Midgaard
import net.sf.nightworks.model.HomeTown.NewOfcol
import net.sf.nightworks.model.HomeTown.NewThalos
import net.sf.nightworks.model.HomeTown.OldMidgaard
import net.sf.nightworks.model.HomeTown.Titans
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.SG
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.model.learn
import net.sf.nightworks.util.CANT_CHANGE_TITLE
import net.sf.nightworks.util.GET_AC
import net.sf.nightworks.util.GET_DAMROLL
import net.sf.nightworks.util.GET_HITROLL
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_AWAKE
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_OBJ_STAT
import net.sf.nightworks.util.IS_OUTSIDE
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_TRUSTED
import net.sf.nightworks.util.IS_VAMPIRE
import net.sf.nightworks.util.IS_WATER
import net.sf.nightworks.util.LEFT_HANDER
import net.sf.nightworks.util.MOUNTED
import net.sf.nightworks.util.PERS
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.RIDDEN
import net.sf.nightworks.util.RIGHT_HANDER
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.atoi
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.crypt
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.endsWith
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.get_carry_weight
import net.sf.nightworks.util.interpolate
import net.sf.nightworks.util.is_number
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.util.smash_tilde
import net.sf.nightworks.util.startsWith
import net.sf.nightworks.util.trimSpaces
import net.sf.nightworks.util.upfirst
import java.util.Date

/* for do_count. TODO: rework */
var max_on = 0

fun show_vwear_to_char(ch: CHAR_DATA, obj: Obj): Boolean {
    if (!can_see_obj(ch, obj)) {
        return false
    }
    send_to_char(obj.wear_loc.where.format(' '), ch)
    send_to_char(format_obj_to_char(obj, ch, true), ch)
    send_to_char("\n", ch)
    return true
}


fun show_cwear_to_char(ch: CHAR_DATA, obj: Obj): Boolean {
    val buf = when {
        obj.wear_loc == Wear.Left && LEFT_HANDER(ch) || obj.wear_loc == Wear.Right && RIGHT_HANDER(ch) -> obj.wear_loc.where.format('*')
        else -> obj.wear_loc.where.format(' ')
    }
    send_to_char(buf, ch)
    if (can_see_obj(ch, obj)) {
        send_to_char(format_obj_to_char(obj, ch, true), ch)
    } else {
        send_to_char("something.\n", ch)
    }
    send_to_char("\n", ch)

    return true
}


fun format_obj_to_char(obj: Obj, ch: CHAR_DATA, fShort: Boolean): String {
    if (fShort && obj.short_desc.isEmpty() || obj.description.isEmpty()) {
        return ""
    }

    val buf_con: String

    /* money, gold, etc */
    buf_con = when {
        obj.pIndexData.vnum > 5 -> "[{g" + get_cond_alias(obj) + "{x]"
        else -> ""
    }


    val buf = StringBuilder()

    if (IS_OBJ_STAT(obj, ITEM_BURIED)) {
        buf.append("{W(Buried) ")
    }
    if (IS_OBJ_STAT(obj, ITEM_INVIS)) {
        buf.append("{W(Invis) ")
    }
    if (IS_AFFECTED(ch, AFF_DETECT_EVIL) && IS_OBJ_STAT(obj, ITEM_EVIL)) {
        buf.append("{r(Red Aura) ")
    }
    if (IS_AFFECTED(ch, AFF_DETECT_GOOD) && IS_OBJ_STAT(obj, ITEM_BLESS)) {
        buf.append("{b(Blue Aura) ")
    }
    if (IS_AFFECTED(ch, AFF_DETECT_MAGIC) && IS_OBJ_STAT(obj, ITEM_MAGIC)) {
        buf.append("{Y(Magical) ")
    }
    if (IS_OBJ_STAT(obj, ITEM_GLOW)) {
        buf.append("{c(Glowing) ")
    }
    if (IS_OBJ_STAT(obj, ITEM_HUM)) {
        buf.append("{y(Humming) ")
    }
    buf.append("{x")
    if (fShort) {
        if (!obj.short_desc.isEmpty()) {
            buf.append(obj.short_desc)
            buf.append(buf_con)
        }
    } else {
        if (!obj.description.isEmpty()) {
            val in_room = obj.in_room
            if (in_room != null) {
                if (IS_WATER(in_room)) {
                    buf.append(upfirst(obj.short_desc))
                    when (dice(1, 3)) {
                        1 -> buf.append(" is floating gently on the water.")
                        2 -> buf.append(" is making it's way on the water.")
                        3 -> buf.append(" is getting wet by the water.")
                    }
                } else {
                    buf.append(obj.description)
                }
            } else {
                buf.append(obj.description)
            }
        }
    }

    return buf.toString()
}

/** Show a list to a character. Can coalesce duplicated items. */
fun show_list_to_char(list: List<Obj>, ch: CHAR_DATA, fShort: Boolean, fShowNothing: Boolean) {
    get_pc(ch) ?: return

    var nShow = 0

    /* Format the list of objects.*/
    val prgnShow = IntArray(list.count())
    val prgpstrShow = arrayOfNulls<String>(list.count())

    for (obj in list) {
        if (obj.wear_loc.isNone() && can_see_obj(ch, obj)) {
            val pstrShow = format_obj_to_char(obj, ch, fShort)
            var fCombine = false

            if (ch.pcdata == null || IS_SET(ch.comm, COMM_COMBINE)) {
                /*
                    * Look for duplicates, case sensitive.
                    * Matches tend to be near end so run loop backwords.
                    */
                for (iShow in nShow - 1 downTo 0) {
                    if (prgpstrShow[iShow] == pstrShow) {
                        prgnShow[iShow]++
                        fCombine = true
                        break
                    }
                }
            }

            /*
                * Couldn't combine, or didn't want to.
                */
            if (!fCombine) {
                prgpstrShow[nShow] = pstrShow
                prgnShow[nShow] = 1
                nShow++
            }
        }
    }

    /* Output the formatted list. */
    val buf = TextBuffer()
    val output = StringBuilder()
    for (iShow in 0 until nShow) {
        if (prgpstrShow[iShow]!!.isEmpty()) {
            continue
        }

        if (ch.pcdata == null || IS_SET(ch.comm, COMM_COMBINE)) {
            if (prgnShow[iShow] != 1) {
                buf.sprintf("(%2d) ", prgnShow[iShow])
                output.append(buf.toString())
            } else {
                output.append("     ")
            }
        }
        output.append(prgpstrShow[iShow])
        output.append("\n")
    }

    if (fShowNothing && nShow == 0) {
        if (ch.pcdata == null || IS_SET(ch.comm, COMM_COMBINE)) {
            send_to_char("     ", ch)
        }
        send_to_char("Nothing.\n", ch)
    }
    page_to_char(output.toString(), ch)

}


fun show_char_to_char_0(victim: CHAR_DATA, ch: CHAR_DATA) {
    val buf = StringBuilder()

    /** Quest staff */
    val pcdata = ch.pcdata
    val v_pcdata = victim.pcdata
    if (pcdata != null && v_pcdata == null && pcdata.questmob > 0 && victim.pIndexData.vnum == pcdata.questmob) {
        buf.append("[TARGET] ")
    }
    /*
    sprintf(message,"(%s) ",race_table[RACE(victim)].name);
    message[1] = UPPER( message[1]);
    buf.append(message);
*/
    if (RIDDEN(victim) != null) {
        buf.append("(Ridden) ")
    }
    if (IS_AFFECTED(victim, AFF_INVISIBLE)) {
        buf.append("(Invis) ")
    }
    if (IS_AFFECTED(victim, AFF_IMP_INVIS)) {
        buf.append("(Improved) ")
    }
    if (victim.invis_level >= LEVEL_HERO) {
        buf.append("(Wizi) ")
    }
    if (IS_AFFECTED(victim, AFF_HIDE)) {
        buf.append("(Hide) ")
    }
    if (IS_AFFECTED(victim, AFF_FADE)) {
        buf.append("(Fade) ")
    }
    if (IS_AFFECTED(victim, AFF_CAMOUFLAGE)) {
        buf.append("(Camf) ")
    }
    if (IS_AFFECTED(victim, AFF_EARTHFADE)) {
        buf.append("(Earth) ")
    }
    if (IS_AFFECTED(victim, AFF_CHARM) && victim.master == ch) {
        buf.append("(Charmed) ")
    }
    if (IS_AFFECTED(victim, AFF_PASS_DOOR)) {
        buf.append("(Translucent) ")
    }
    if (IS_AFFECTED(victim, AFF_FAERIE_FIRE)) {
        buf.append("(Pink Aura) ")
    }
    if (v_pcdata == null && IS_SET(victim.act, ACT_UNDEAD) && IS_AFFECTED(ch, AFF_DETECT_UNDEAD)) {
        buf.append("(Undead) ")
    }
    if (victim.isEvil() && IS_AFFECTED(ch, AFF_DETECT_EVIL)) {
        buf.append("(Red Aura) ")
    }
    if (victim.isGood() && IS_AFFECTED(ch, AFF_DETECT_GOOD)) {
        buf.append("(Golden Aura) ")
    }
    if (IS_AFFECTED(victim, AFF_SANCTUARY)) {
        buf.append("(White Aura) ")
    }
    if (v_pcdata != null && IS_SET(victim.act, PLR_WANTED)) {
        buf.append("(CRIMINAL) ")
    }

    if (victim.position == victim.start_pos && victim.long_desc.isNotEmpty()) {
        buf.append(victim.long_desc)
        send_to_char(buf, ch)
        return
    }

    if (IS_SET(ch.act, PLR_HOLYLIGHT) && is_affected(victim, Skill.doppelganger)) {
        buf.append("{")
        buf.append(PERS(victim, ch))
        buf.append("} ")
    }
    val dopel = victim.doppel
    if (dopel != null && dopel.long_desc.isNotEmpty()) {
        buf.append(dopel.long_desc)
        send_to_char(buf, ch)
        return
    }

    if (victim.long_desc.isNotEmpty() && !is_affected(victim, Skill.doppelganger)) {
        buf.append(victim.long_desc)
        send_to_char(buf, ch)
        return
    }

    if (dopel != null) {
        buf.append(PERS(dopel, ch))
        val d_pcdata = dopel.pcdata
        if (d_pcdata != null && !IS_SET(ch.comm, COMM_BRIEF)) {
            buf.append(d_pcdata.title)
        }
    } else {
        buf.append(PERS(victim, ch))
        if (v_pcdata != null && !IS_SET(ch.comm, COMM_BRIEF) && victim.position == Pos.Standing && ch.on == null) {
            buf.append(v_pcdata.title)
        }
    }

    val vOn = victim.on
    val vDesc = vOn?.short_desc
    when (victim.position) {
        Pos.Dead -> buf.append(" is DEAD!!")
        Pos.Mortal -> buf.append(" is mortally wounded.")
        Pos.Incap -> buf.append(" is incapacitated.")
        Pos.Stunned -> buf.append(" is lying here stunned.")
        Pos.Sleeping -> buf.append(when {
            vOn != null && IS_SET(vOn.value[2], SLEEP_AT) -> " is sleeping at $vDesc."
            vOn != null && IS_SET(vOn.value[2], SLEEP_ON) -> " is sleeping on $vDesc."
            vOn != null -> " is sleeping in $vDesc."
            else -> " is sleeping here."
        })
        Pos.Resting -> buf.append(when {
            vOn != null && IS_SET(vOn.value[2], REST_AT) -> " is resting at $vDesc."
            vOn != null && IS_SET(vOn.value[2], REST_ON) -> " is resting on $vDesc."
            vOn != null -> " is resting in $vDesc."
            else -> " is resting here."
        })
        Pos.Sitting -> buf.append(when {
            vOn != null && IS_SET(vOn.value[2], SIT_AT) -> " is sitting at $vDesc."
            vOn != null && IS_SET(vOn.value[2], SIT_ON) -> " is sitting on $vDesc."
            vOn != null -> " is sitting in $vDesc."
            else -> " is sitting here."
        })
        Pos.Standing -> buf.append(when {
            vOn != null && IS_SET(vOn.value[2], STAND_AT) -> " is standing at $vDesc."
            vOn != null && IS_SET(vOn.value[2], STAND_ON) -> " is standing on $vDesc."
            vOn != null -> " is standing in $vDesc."
            MOUNTED(victim) != null -> " is here, riding ${PERS(MOUNTED(victim), ch)}."
            else -> " is here."
        })
        Pos.Fighting -> {
            buf.append(" is here, fighting ")
            val v_fighting = victim.fighting
            buf.append(when {
                v_fighting == null -> "thin air??"
                v_fighting == ch -> "YOU!"
                victim.room == v_fighting.room -> PERS(v_fighting, ch) + "."
                else -> "someone who left??"
            })
        }
    }
    buf.append("\n")
    send_to_char(upfirst(buf.toString()), ch)
}


fun show_char_to_char_1(victim: CHAR_DATA, ch: CHAR_DATA) {
    var obj: Obj?

    val vict = victim.doppel ?: victim
    if (can_see(victim, ch)) {
        if (ch == victim) {
            act("\$n looks at \$mself.", ch, null, null, TO_ROOM)
        } else {
            act("\$n looks at you.", ch, null, victim, TO_VICT)
            act("\$n looks at \$N.", ch, null, victim, TO_NOTVICT)
        }
    }

    if (vict.description.isNotEmpty()) {
        send_to_char(vict.description, ch)
    } else {
        act("You see nothing special about \$M.", ch, null, victim, TO_CHAR)
    }

    val buf = TextBuffer()
    if (MOUNTED(victim) != null) {
        buf.sprintf("%s is riding %s.\n", PERS(victim, ch), PERS(MOUNTED(victim), ch))
        send_to_char(buf, ch)
    }
    if (RIDDEN(victim) != null) {
        buf.sprintf("%s is being ridden by %s.\n", PERS(victim, ch), PERS(RIDDEN(victim), ch))
        send_to_char(buf, ch)
    }

    val percent = if (victim.max_hit > 0) 100 * victim.hit / victim.max_hit else -1

    buf.sprintf("(%s) %s", vict.race.name, PERS(vict, ch))

    when {
        percent >= 100 -> buf.append(" is in perfect health.\n")
        percent >= 90 -> buf.append(" has a few scratches.\n")
        percent >= 75 -> buf.append(" has some small but disgusting cuts.\n")
        percent >= 50 -> buf.append(" is covered with bleeding wounds.\n")
        percent >= 30 -> buf.append(" is gushing blood.\n")
        percent >= 15 -> buf.append(" is writhing in agony.\n")
        percent >= 0 -> buf.append(" is convulsing on the ground.\n")
        else -> buf.append(" is nearly dead.\n")
    }

    /* vampire ... */
    if (percent < 90 && ch.clazz === Clazz.VAMPIRE && ch.level > 10) {
        gain_condition(ch, COND_BLOODLUST, -1)
    }

    send_to_char(upfirst(buf.toString()), ch)

    var found = false
    for (w in Wear.values()) {
        if (w == Wear.Finger || w == Wear.Neck || w == Wear.Wrist || w == Wear.Tattoo || w == Wear.StuckIn) {
            for (o in vict.carrying) {
                if (o.wear_loc == w) {
                    if (!found) {
                        act("\$N is using:", ch, null, victim, TO_CHAR)
                        send_to_char("\n", ch)
                        found = true
                    }
                    show_vwear_to_char(ch, o)
                }
            }
        } else {
            obj = get_eq_char(vict, w)
            if (obj != null) {
                if (!found) {
                    act("\$N is using:", ch, null, victim, TO_CHAR)
                    send_to_char("\n", ch)
                    found = true
                }
                show_vwear_to_char(ch, obj)
            }
        }
    }

    if (victim != ch && ch.pcdata != null && randomPercent() < get_skill(ch, Skill.peek)) {
        send_to_char("\nYou peek at the inventory:\n", ch)
        check_improve(ch, Skill.peek, true, 4)
        show_list_to_char(vict.carrying, ch, true, true)
    }

}

/** list - chars in room */
fun show_char_to_char(list: List<CHAR_DATA>, ch: CHAR_DATA) {
    var life_count = 0

    list
            .filter { it != ch && get_trust(ch) >= it.invis_level }
            .forEach {
                when {
                    can_see(ch, it) -> show_char_to_char_0(it, ch)
                    room_is_dark(ch) && IS_AFFECTED(it, AFF_INFRARED) -> {
                        send_to_char("You see glowing red eyes watching YOU!\n", ch)
                        if (!IS_IMMORTAL(it)) {
                            life_count++
                        }
                    }
                    !IS_IMMORTAL(it) -> life_count++
                }
            }

    if (life_count != 0 && IS_AFFECTED(ch, AFF_DETECT_LIFE)) {
        val buf = TextBuffer()
        buf.sprintf("You feel %d more life %s in the room.\n", life_count, if (life_count == 1) "form" else "forms")
        send_to_char(buf, ch)
    }
}


fun check_blind(ch: CHAR_DATA): Boolean {

    if (ch.pcdata != null && IS_SET(ch.act, PLR_HOLYLIGHT)) {
        return true
    }

    if (IS_AFFECTED(ch, AFF_BLIND)) {
        send_to_char("You can't see a thing!\n", ch)
        return false
    }

    return true
}

fun do_clear(ch: CHAR_DATA) {
    if (ch.pcdata != null) {
        send_to_char("\u001b[0;0H\u001b[2J", ch)
    }
}

/* changes your scroll */

fun do_scroll(ch: CHAR_DATA, argument: String) {
    val lines: Int
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        if (ch.lines == 0) {
            send_to_char("You do not page long messages.\n", ch)
        } else {
            val buf = TextBuffer()
            buf.sprintf("You currently display %d lines per page.\n", ch.lines + 2)
            send_to_char(buf, ch)
        }
        return
    }

    if (!is_number(arg)) {
        send_to_char("You must provide a number.\n", ch)
        return
    }

    lines = atoi(arg.toString())

    if (lines == 0) {
        send_to_char("Paging disabled.\n", ch)
        ch.lines = 0
        return
    }

    if (lines < 10 || lines > 100) {
        send_to_char("You must provide a reasonable number.\n", ch)
        return
    }
    val buf = TextBuffer()
    buf.sprintf("Scroll set to %d lines.\n", lines)
    send_to_char(buf, ch)
    ch.lines = lines - 2
}

/* RT does socials */

fun do_socials(ch: CHAR_DATA) {
    var col = 0
    val buf = TextBuffer()
    for (soc in Index.SOCIALS) {
        buf.sprintf("%-12s", soc.name)
        send_to_char(buf, ch)
        if (++col % 6 == 0) {
            send_to_char("\n", ch)
        }
    }

    if (col % 6 != 0) {
        send_to_char("\n", ch)
    }
}

/* RT Commands to replace news, motd, imotd, etc from ROM */

fun do_motd(ch: CHAR_DATA) {
    do_help(ch, "motd")
}

fun do_imotd(ch: CHAR_DATA) {
    do_help(ch, "imotd")
}

fun do_rules(ch: CHAR_DATA) {
    do_help(ch, "rules")
}

fun do_story(ch: CHAR_DATA) {
    do_help(ch, "story")
}

fun do_wizlist(ch: CHAR_DATA) {
    do_help(ch, "wizlist")
}

/* RT this following section holds all the auto commands from ROM, as well as
   replacements for config */

fun do_autolist(ch: CHAR_DATA) {
    /* lists most player flags */
    if (ch.pcdata == null) {
        return
    }

    if (IS_SET(ch.act, PLR_COLOR)) {
        do_autolist_col(ch)
        return
    }

    send_to_char("   action     status\n", ch)
    send_to_char("---------------------\n", ch)

    send_to_char("color          ", ch)
    if (IS_SET(ch.act, PLR_COLOR)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    send_to_char("autoassist     ", ch)
    if (IS_SET(ch.act, PLR_AUTO_ASSIST)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    send_to_char("autoexit       ", ch)
    if (IS_SET(ch.act, PLR_AUTO_EXIT)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    send_to_char("autogold       ", ch)
    if (IS_SET(ch.act, PLR_AUTO_GOLD)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    send_to_char("autoloot       ", ch)
    if (IS_SET(ch.act, PLR_AUTO_LOOT)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    send_to_char("autosac        ", ch)
    if (IS_SET(ch.act, PLR_AUTO_SAC)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    send_to_char("autosplit      ", ch)
    if (IS_SET(ch.act, PLR_AUTO_SPLIT)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    send_to_char("compact mode   ", ch)
    if (IS_SET(ch.comm, COMM_COMPACT)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    send_to_char("prompt         ", ch)
    if (IS_SET(ch.comm, COMM_PROMPT)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    send_to_char("combine items  ", ch)
    if (IS_SET(ch.comm, COMM_COMBINE)) {
        send_to_char("ON\n", ch)
    } else {
        send_to_char("OFF\n", ch)
    }

    if (IS_SET(ch.act, PLR_NOSUMMON)) {
        send_to_char("You can only be summoned players within your PK range.\n", ch)
    } else {
        send_to_char("You can be summoned by anyone.\n", ch)
    }

    if (IS_SET(ch.act, PLR_NOFOLLOW)) {
        send_to_char("You do not welcome followers.\n", ch)
    } else {
        send_to_char("You accept followers.\n", ch)
    }

    if (IS_SET(ch.act, PLR_NOCANCEL)) {
        send_to_char("You do not welcome others' cancellation spells.\n", ch)
    } else {
        send_to_char("You accept others' cancellation spells.\n", ch)
    }
}

fun do_autolist_col(ch: CHAR_DATA) {
    /* lists most player flags */
    if (ch.pcdata == null) {
        return
    }

    send_to_char("  [1;33maction           status\n[0;37m", ch)
    send_to_char("[1;36m-------------------------\n[0;37m", ch)

    send_to_char("[1;34m|[0;37m [0;36mcolor          [0;37m", ch)
    if (IS_SET(ch.act, PLR_COLOR)) {
        send_to_char("[1;34m|  [1;32mON  [1;34m|\n[0;37m", ch)
    } else {
        send_to_char("[1;34m|  [1;31mOFF [1;34m|\n[0;37m", ch)
    }

    send_to_char("[1;34m|[0;37m [0;36mautoassist     ", ch)
    if (IS_SET(ch.act, PLR_AUTO_ASSIST)) {
        send_to_char("[1;34m|  [1;32mON  [1;34m|\n[0;37m", ch)
    } else {
        send_to_char("[1;34m|  [1;31mOFF [1;34m|\n[0;37m", ch)
    }

    send_to_char("[1;34m|[0;37m [0;36mautoexit       ", ch)
    if (IS_SET(ch.act, PLR_AUTO_EXIT)) {
        send_to_char("[1;34m|  [1;32mON  [1;34m|\n[0;37m", ch)
    } else {
        send_to_char("[1;34m|  [1;31mOFF [1;34m|\n[0;37m", ch)
    }

    send_to_char("[1;34m|[0;37m [0;36mautogold       ", ch)
    if (IS_SET(ch.act, PLR_AUTO_GOLD)) {
        send_to_char("[1;34m|  [1;32mON  [1;34m|\n[0;37m", ch)
    } else {
        send_to_char("[1;34m|  [1;31mOFF [1;34m|\n[0;37m", ch)
    }

    send_to_char("[1;34m|[0;37m [0;36mautoloot       ", ch)
    if (IS_SET(ch.act, PLR_AUTO_LOOT)) {
        send_to_char("[1;34m|  [1;32mON  [1;34m|\n[0;37m", ch)
    } else {
        send_to_char("[1;34m|  [1;31mOFF [1;34m|\n[0;37m", ch)
    }

    send_to_char("[1;34m|[0;37m [0;36mautosac        ", ch)
    if (IS_SET(ch.act, PLR_AUTO_SAC)) {
        send_to_char("[1;34m|  [1;32mON  [1;34m|\n[0;37m", ch)
    } else {
        send_to_char("[1;34m|  [1;31mOFF [1;34m|\n[0;37m", ch)
    }

    send_to_char("[1;34m|[0;37m [0;36mautosplit      ", ch)
    if (IS_SET(ch.act, PLR_AUTO_SPLIT)) {
        send_to_char("[1;34m|  [1;32mON  [1;34m|\n[0;37m", ch)
    } else {
        send_to_char("[1;34m|  [1;31mOFF [1;34m|\n[0;37m", ch)
    }

    send_to_char("[1;34m|[0;37m [0;36mcompact mode   ", ch)
    if (IS_SET(ch.comm, COMM_COMPACT)) {
        send_to_char("[1;34m|  [1;32mON  [1;34m|\n[0;37m", ch)
    } else {
        send_to_char("[1;34m|  [1;31mOFF [1;34m|\n[0;37m", ch)
    }

    send_to_char("[1;34m|[0;37m [0;36mprompt         ", ch)
    if (IS_SET(ch.comm, COMM_PROMPT)) {
        send_to_char("[1;34m|  [1;32mON  [1;34m|\n[0;37m", ch)
    } else {
        send_to_char("[1;34m|  [1;31mOFF [1;34m|\n[0;37m", ch)
    }

    send_to_char("[1;34m|[0;37m [0;36mcombine items  ", ch)
    if (IS_SET(ch.comm, COMM_COMBINE)) {
        send_to_char("[1;34m|  [1;32mON  [1;34m|\n[0;37m", ch)
    } else {
        send_to_char("[1;34m|  [1;31mOFF [1;34m|\n[0;37m", ch)
    }
    send_to_char("[1;36m-------------------------\n[0;37m", ch)


    if (IS_SET(ch.act, PLR_NOSUMMON)) {
        send_to_char("You can only be summoned players within your PK range.\n", ch)
    } else {
        send_to_char("You can be summoned by anyone.\n", ch)
    }

    if (IS_SET(ch.act, PLR_NOFOLLOW)) {
        send_to_char("You do not welcome followers.\n", ch)
    } else {
        send_to_char("You accept followers.\n", ch)
    }

    if (IS_SET(ch.act, PLR_NOCANCEL)) {
        send_to_char("You do not welcome others' cancellation spells.\n", ch)
    } else {
        send_to_char("You accept others' cancellation spells.\n", ch)
    }
}

fun do_autoassist(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    if (IS_SET(ch.act, PLR_AUTO_ASSIST)) {
        send_to_char("Autoassist removed.\n", ch)
        ch.act = REMOVE_BIT(ch.act, PLR_AUTO_ASSIST)
    } else {
        send_to_char("You will now assist when needed.\n", ch)
        ch.act = SET_BIT(ch.act, PLR_AUTO_ASSIST)
    }
}

fun do_autoexit(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    if (IS_SET(ch.act, PLR_AUTO_EXIT)) {
        send_to_char("Exits will no longer be displayed.\n", ch)
        ch.act = REMOVE_BIT(ch.act, PLR_AUTO_EXIT)
    } else {
        send_to_char("Exits will now be displayed.\n", ch)
        ch.act = SET_BIT(ch.act, PLR_AUTO_EXIT)
    }
}

fun do_autogold(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    if (IS_SET(ch.act, PLR_AUTO_GOLD)) {
        send_to_char("Autogold removed.\n", ch)
        ch.act = REMOVE_BIT(ch.act, PLR_AUTO_GOLD)
    } else {
        send_to_char("Automatic gold looting set.\n", ch)
        ch.act = SET_BIT(ch.act, PLR_AUTO_GOLD)
    }
}

fun do_autoloot(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    if (IS_SET(ch.act, PLR_AUTO_LOOT)) {
        send_to_char("Autolooting removed.\n", ch)
        ch.act = REMOVE_BIT(ch.act, PLR_AUTO_LOOT)
    } else {
        send_to_char("Automatic corpse looting set.\n", ch)
        ch.act = SET_BIT(ch.act, PLR_AUTO_LOOT)
    }
}

fun do_autosac(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    if (IS_SET(ch.act, PLR_AUTO_SAC)) {
        send_to_char("Autosacrificing removed.\n", ch)
        ch.act = REMOVE_BIT(ch.act, PLR_AUTO_SAC)
    } else {
        send_to_char("Automatic corpse sacrificing set.\n", ch)
        ch.act = SET_BIT(ch.act, PLR_AUTO_SAC)
    }
}

fun do_autosplit(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    if (IS_SET(ch.act, PLR_AUTO_SPLIT)) {
        send_to_char("Autosplitting removed.\n", ch)
        ch.act = REMOVE_BIT(ch.act, PLR_AUTO_SPLIT)
    } else {
        send_to_char("Automatic gold splitting set.\n", ch)
        ch.act = SET_BIT(ch.act, PLR_AUTO_SPLIT)
    }
}

fun do_color(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    if (IS_SET(ch.act, PLR_COLOR)) {
        send_to_char("Your color is now OFF.\n", ch)
        ch.act = REMOVE_BIT(ch.act, PLR_COLOR)
    } else {
        send_to_char("Your color is now ON.\n", ch)
        ch.act = SET_BIT(ch.act, PLR_COLOR)
    }
}

fun do_brief(ch: CHAR_DATA) {
    if (IS_SET(ch.comm, COMM_BRIEF)) {
        send_to_char("Full descriptions activated.\n", ch)
        ch.comm = REMOVE_BIT(ch.comm, COMM_BRIEF)
    } else {
        send_to_char("Short descriptions activated.\n", ch)
        ch.comm = SET_BIT(ch.comm, COMM_BRIEF)
    }
}

fun do_compact(ch: CHAR_DATA) {
    if (IS_SET(ch.comm, COMM_COMPACT)) {
        send_to_char("Compact mode removed.\n", ch)
        ch.comm = REMOVE_BIT(ch.comm, COMM_COMPACT)
    } else {
        send_to_char("Compact mode set.\n", ch)
        ch.comm = SET_BIT(ch.comm, COMM_COMPACT)
    }
}

fun do_show(ch: CHAR_DATA) {
    if (IS_SET(ch.comm, COMM_SHOW_AFFECTS)) {
        send_to_char("Affects will no longer be shown in score.\n", ch)
        ch.comm = REMOVE_BIT(ch.comm, COMM_SHOW_AFFECTS)
    } else {
        send_to_char("Affects will now be shown in score.\n", ch)
        ch.comm = SET_BIT(ch.comm, COMM_SHOW_AFFECTS)
    }
}

fun do_prompt(ch: CHAR_DATA, argument: String) {
    var rest = argument
    if (rest.isEmpty()) {
        if (IS_SET(ch.comm, COMM_PROMPT)) {
            send_to_char("You will no longer see prompts.\n", ch)
            ch.comm = REMOVE_BIT(ch.comm, COMM_PROMPT)
        } else {
            send_to_char("You will now see prompts.\n", ch)
            ch.comm = SET_BIT(ch.comm, COMM_PROMPT)
        }
        return
    }
    val buf = when (rest) {
        "all" -> DEFAULT_PROMPT
        else -> {
            if (rest.length > 50) {
                rest = rest.substring(0, 50)
            }
            val res = smash_tilde(rest)
            when {
                !endsWith("%c", res) -> res + " "
                else -> res
            }
        }
    }

    ch.prompt = buf
    send_to_char("Prompt set to ${ch.prompt}\n", ch)
}

fun do_combine(ch: CHAR_DATA) {
    if (IS_SET(ch.comm, COMM_COMBINE)) {
        send_to_char("Long inventory selected.\n", ch)
        ch.comm = REMOVE_BIT(ch.comm, COMM_COMBINE)
    } else {
        send_to_char("Combined inventory selected.\n", ch)
        ch.comm = SET_BIT(ch.comm, COMM_COMBINE)
    }
}

fun do_noloot(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    if (IS_SET(ch.act, PLR_CANLOOT)) {
        send_to_char("Your corpse is now safe from thieves.\n", ch)
        ch.act = REMOVE_BIT(ch.act, PLR_CANLOOT)
    } else {
        send_to_char("Your corpse may now be looted.\n", ch)
        ch.act = SET_BIT(ch.act, PLR_CANLOOT)
    }
}

fun do_nofollow(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }
    if (IS_AFFECTED(ch, AFF_CHARM)) {
        send_to_char("You don't want to leave your beloved master.\n", ch)
        return
    }

    if (IS_SET(ch.act, PLR_NOFOLLOW)) {
        send_to_char("You now accept followers.\n", ch)
        ch.act = REMOVE_BIT(ch.act, PLR_NOFOLLOW)
    } else {
        send_to_char("You no longer accept followers.\n", ch)
        ch.act = SET_BIT(ch.act, PLR_NOFOLLOW)
        die_follower(ch)
    }
}

fun do_nosummon(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        if (IS_SET(ch.imm_flags, IMM_SUMMON)) {
            send_to_char("You are no longer immune to summoning.\n", ch)
            ch.imm_flags = REMOVE_BIT(ch.imm_flags, IMM_SUMMON)
        } else {
            send_to_char("You are now immune to summoning.\n", ch)
            ch.imm_flags = SET_BIT(ch.imm_flags, IMM_SUMMON)
        }
    } else {
        if (IS_SET(ch.act, PLR_NOSUMMON)) {
            send_to_char("You may now be summoned by anyone.\n", ch)
            ch.act = REMOVE_BIT(ch.act, PLR_NOSUMMON)
        } else {
            send_to_char("You may only be summoned by players within your PK range.\n", ch)
            ch.act = SET_BIT(ch.act, PLR_NOSUMMON)
        }
    }
}

/* RT added back for the hell of it */
fun do_read(ch: CHAR_DATA, argument: String) {
    do_look(ch, argument)
}

fun do_examine(ch: CHAR_DATA, argument: String) {
    val obj: Obj?

    val argb = StringBuilder()

    one_argument(argument, argb)

    if (argb.isEmpty()) {
        send_to_char("Examine what?\n", ch)
        return
    }
    val arg = argb.toString()
    do_look(ch, arg)

    obj = get_obj_here(ch, arg)
    if (obj != null) {
        when (obj.item_type) {
            ItemType.Money -> {
                val msg = when {
                    obj.value[0] == 0L -> when {
                        obj.value[1] == 0L -> "Odd...there's no coins in the pile.\n"
                        obj.value[1] == 1L -> "Wow. One gold coin.\n"
                        else -> "There are ${obj.value[1]} gold coins in the pile.\n"
                    }
                    obj.value[1] == 0L -> when {
                        obj.value[0] == 1L -> "Wow. One silver coin.\n"
                        else -> "There are ${obj.value[0]} silver coins in the pile.\n"
                    }
                    else -> "There are ${obj.value[1]} gold and ${obj.value[0]} silver coins in the pile.\n"
                }
                send_to_char(msg, ch)
            }
            ItemType.DrinkContainer, ItemType.Container, ItemType.CorpseNpc, ItemType.CorpsePc -> do_look(ch, "in $argument")
            else -> {
            }
        }
    }
}

fun do_worth(ch: CHAR_DATA) {
    val buf = TextBuffer()
    val pcdata = ch.pcdata
    if (pcdata == null) {
        buf.sprintf("You have %d gold and %d silver.\n", ch.gold, ch.silver)
        send_to_char(buf, ch)
        return
    }

    buf.sprintf("You have %d gold, %d silver, and %d experience (%d exp to level).\n", ch.gold, ch.silver, ch.exp, exp_to_level(ch))

    send_to_char(buf, ch)
    buf.sprintf("You have killed %d %s and %d %s.\n",
            pcdata.has_killed,
            if (ch.isGood()) "non-goods" else if (ch.isEvil()) "non-evils" else "non-neutrals",
            pcdata.anti_killed,
            if (ch.isGood()) "goods" else if (ch.isEvil()) "evils" else "neutrals")
    send_to_char(buf, ch)

    val total_played = get_total_played(ch)
    buf.sprintf("Within last %d days, you have played %d hour(s) and %d minute(s).\n" + "In order to save limited objects, you need minimum %d hours and %d minute(s).\n",
            nw_config.max_time_log,
            total_played / 60,
            total_played % 60,
            nw_config.min_time_limit / 60,
            nw_config.min_time_limit % 60)
    send_to_char(buf, ch)

    if (IS_IMMORTAL(ch)) {
        var l = 0
        while (l < nw_config.max_time_log) {
            val today = get_played_day(l)
            val d_time = get_played_time(ch, l)
            send_to_char("  Day: $today Playing Time: $d_time min(s)\n", ch)
            l++
        }
    }

}


val day_name = arrayOf("the Moon", "the Bull", "Deception", "Thunder", "Freedom", "the Great Gods", "the Sun")

val month_name = arrayOf("Winter", "the Winter Wolf", "the Frost Giant", "the Old Forces", "the Grand Struggle", "the Spring", "Nature", "Futility", "the Dragon", "the Sun", "the Heat", "the Battle", "the Dark Shades", "the Shadows", "the Long Shadows", "the Ancient Darkness", "the Great Evil")


val COLOR_DAWN = "{b"
val COLOR_MORNING = "{W"
val COLOR_DAY = "{Y"
val COLOR_EVENING = "{r"
val COLOR_NIGHT = "{w"

fun do_time(ch: CHAR_DATA) {
    val day = time_info.day + 1
    val suf = when {
        day in 5..19 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }

    val buf = TextBuffer()
    buf.sprintf("It is %d o'clock %s, Day of %s, %d%s the Month of %s.\n",
            if (time_info.hour % 12 == 0) 12 else time_info.hour % 12,
            if (time_info.hour >= 12) "pm" else "am",
            day_name[day % 7],
            day, suf,
            month_name[time_info.month])

    send_to_char(buf, ch)

    if (!IS_SET(ch.room.room_flags, ROOM_INDOORS) || IS_IMMORTAL(ch)) {
        val time = when (time_info.hour) {
            in 5..8 -> "dawn"
            in 9..11 -> "morning"
            in 12..17 -> "mid-day"
            in 18..20 -> "evening"
            else -> "night"
        }
        val color = when (time_info.hour) {
            in 5..8 -> COLOR_DAWN
            in 9..11 -> COLOR_MORNING
            in 12..17 -> COLOR_DAY
            in 18..20 -> COLOR_EVENING
            else -> COLOR_NIGHT
        }

        buf.sprintf("It's %s%s. {x", color, time)
        act(buf.toString(), ch, null, null, TO_CHAR, Pos.Resting)
    }

    if (!IS_IMMORTAL(ch)) {
        return
    }
    buf.sprintf("NIGHTWORKS started up at %s\nThe system time is %s.\n", Date(boot_time * 1000L), Date(current_time * 1000L))
    send_to_char(buf, ch)
}


val sky_look = arrayOf("cloudless", "cloudy", "rainy", "lit by flashes of lightning")

fun do_weather(ch: CHAR_DATA) {

    if (!IS_OUTSIDE(ch)) {
        send_to_char("You can't see the weather indoors.\n", ch)
        return
    }
    val buf = TextBuffer()
    buf.sprintf("The sky is %s and %s.\n",
            sky_look[weather.sky],
            if (weather.change >= 0)
                "a warm southerly breeze blows"
            else
                "a cold northern gust blows"
    )
    send_to_char(buf, ch)
}


/* whois command */

fun do_whois(ch: CHAR_DATA, argument: String) {
    var found = false
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("You must provide a name.\n", ch)
        return
    }

    val output = StringBuilder()
    val buf = TextBuffer()

    for (pc in Index.PLAYERS) {
        val pcdata = pc.o_ch.pcdata!!
        if (!can_see(ch, pc.ch)) {
            continue
        }
        if (IS_VAMPIRE(pc.ch) && !IS_IMMORTAL(ch) && ch != pc.ch) {
            continue
        }
        if (!can_see(ch, pc.o_ch)) {
            continue
        }

        if (startsWith(arg.toString(), pc.o_ch.name)) {
            found = true

            /* work out the printing */
            val clazz = when (pc.o_ch.level) {
                MAX_LEVEL -> "IMP"
                MAX_LEVEL - 1 -> "CRE"
                MAX_LEVEL - 2 -> "SUP"
                MAX_LEVEL - 3 -> "DEI"
                MAX_LEVEL - 4 -> "GOD"
                MAX_LEVEL - 5 -> "IMM"
                MAX_LEVEL - 6 -> "DEM"
                MAX_LEVEL - 7 -> "ANG"
                MAX_LEVEL - 8 -> "AVA"
                else -> pc.o_ch.clazz.who_name
            }

            val cabalbuf = if (pc.o_ch.cabal != Clan.None && ch.cabal == pc.o_ch.cabal || IS_IMMORTAL(ch)
                    || IS_SET(pc.o_ch.act, PLR_CANINDUCT) && pc.o_ch.cabal == Clan.Ruler
                    || pc.o_ch.cabal == Clan.Hunter
                    || pc.o_ch.cabal == Clan.Ruler && is_equiped_n_char(pc.o_ch, OBJ_VNUM_RULER_BADGE, Wear.Neck)) {
                "[{c" + pc.o_ch.cabal.short_name + "{x] "
            } else ""

            val pk_buf = if (!(ch == pc.o_ch && ch.level < PK_MIN_LEVEL || is_safe_nomessage(ch, pc.o_ch))) "{r(PK){x" else ""
            val act_buf = if (IS_SET(pc.o_ch.act, PLR_WANTED)) "{W(WANTED) {x" else ""

            val titlebuf: String
            titlebuf = when {
                pc.ch.pcdata == null -> "Believer of Chronos."
                else -> pcdata.title
            }
            /* Format it up. */
            val level_buf = TextBuffer()
            level_buf.sprintf("{c%2d{x", pc.o_ch.level)
            val classbuf = "{Y$clazz{x"
            /* a little formatting */
            if (IS_TRUSTED(ch, LEVEL_IMMORTAL) || ch == pc.o_ch || pc.o_ch.level >= LEVEL_HERO) {
                buf.sprintf("[%2d %s %s] %s%s%s%s%s\n",
                        pc.o_ch.level,
                        pc.o_ch.race.who_name,
                        classbuf,
                        pk_buf,
                        cabalbuf,
                        act_buf,
                        pc.o_ch.name,
                        titlebuf)
            } else {
                buf.sprintf("[%s %s    ] %s%s%s%s%s\n",
                        if (pc.o_ch.curr_stat(PrimeStat.Charisma) < 18) level_buf else "  ",
                        pc.o_ch.race.who_name,
                        if (ch == pc.o_ch && ch.level < PK_MIN_LEVEL || is_safe_nomessage(ch, pc.o_ch)) "" else "(PK) ",
                        cabalbuf,
                        if (IS_SET(pc.o_ch.act, PLR_WANTED)) "(WANTED) " else "",
                        pc.o_ch.name,
                        titlebuf)
            }
            output.append(buf)
        }
    }

    if (!found) {
        send_to_char("No one of that name is playing.\n", ch)
        return
    }

    page_to_char(output.toString(), ch)
}

/*
* New 'who' command originally by Alander of Rivers of Mud.
*/
fun do_count(ch: CHAR_DATA) {
    val count = Index.PLAYERS.count { can_see(ch, it.ch) }
    max_on = Math.max(count, max_on)
    val buf = when (max_on) {
        count -> "There are $count characters on, the most so far today.\n"
        else -> "There are $count characters on, the most on today was $max_on.\n"
    }
    send_to_char(buf, ch)
}

fun do_inventory(ch: CHAR_DATA) {
    send_to_char("You are carrying:\n", ch)
    show_list_to_char(ch.carrying, ch, true, true)
}


fun do_equipment(ch: CHAR_DATA) {
    send_to_char("You are using:\n", ch)
    var found = false
    for (w in Wear.values()) {
        if (w === Wear.Finger || w === Wear.Neck || w === Wear.Wrist || w === Wear.Tattoo || w === Wear.StuckIn) {
            found = ch.carrying.any { it.wear_loc === w && show_cwear_to_char(ch, it) }
        } else {
            val obj = get_eq_char(ch, w)
            if (obj != null && show_cwear_to_char(ch, obj)) {
                found = true
            }
        }
    }

    if (!found) {
        send_to_char("Nothing.\n", ch)
    }
}


fun do_credits(ch: CHAR_DATA) {
    do_help(ch, "diku")
}


fun do_consider(ch: CHAR_DATA, argument: String) {
    val argb = StringBuilder()
    one_argument(argument, argb)

    if (argb.isEmpty()) {
        send_to_char("Consider killing whom?\n", ch)
        return
    }

    val arg = argb.toString()
    val victim = get_char_room(ch, arg)
    if (victim == null) {
        send_to_char("They're not here.\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        send_to_char("Don't even think about it.\n", ch)
        return
    }

    val diff = victim.level - ch.level

    val msg = when {
        diff <= -10 -> "You can kill \$N naked and weaponless."
        diff <= -5 -> "\$N is no match for you."
        diff <= -2 -> "\$N looks like an easy kill."
        diff <= 1 -> "The perfect match!"
        diff <= 4 -> "\$N says 'Do you feel lucky, punk?'."
        diff <= 9 -> "\$N laughs at you mercilessly."
        else -> "Death will thank you for your gift."
    }

    val align = when {
        ch.isEvil() && victim.isEvil() -> "\$N grins evilly with you."
        ch.isGood() && victim.isGood() -> "\$N greets you warmly."
        ch.isEvil() && victim.isGood() -> "\$N smiles at you, hoping you will turn from your evil path."
        ch.isGood() && victim.isEvil() -> "\$N grins evilly at you."
        ch.isNeutralA() && victim.isEvil() -> "\$N grins evilly."
        ch.isNeutralA() && victim.isGood() -> "\$N smiles happily."
        ch.isNeutralA() && victim.isNeutralA() -> "\$N looks just as disinterested as you."
        else -> "\$N looks very disinterested."
    }

    act(msg, ch, null, victim, TO_CHAR)
    act(align, ch, null, victim, TO_CHAR)
}


fun set_title(ch: CHAR_DATA, title: String) {
    var result = title
    val pcdata = ch.pcdata
    if (pcdata == null) {
        bug("set_title: NPC.")
        return
    }

    val c = result[0]
    if (c == '.' || c == ',' || c == '!' || c == '?') {
        result = " " + result.substring(1)
    }
    if (result.length > 45) {
        result = result.substring(0, 45)
    }
    pcdata.title = result
}


fun do_title(ch: CHAR_DATA, argument: String) {
    var rest = argument
    if (ch.pcdata == null) {
        return
    }

    if (CANT_CHANGE_TITLE(ch)) {
        send_to_char("You can't change your title.\n", ch)
        return
    }

    if (rest.isEmpty()) {
        send_to_char("Change your title to what?\n", ch)
        return
    }

    if (rest.length > 45) {
        rest = rest.substring(0, 45)
    }

    rest = smash_tilde(rest)
    set_title(ch, rest)
    send_to_char("Ok.\n", ch)
}


fun do_description(ch: CHAR_DATA, argument: String) {
    var res = argument

    val buf = StringBuilder()
    if (res.isNotEmpty()) {
        res = smash_tilde(res)

        if (res[0] == '-') {
            var found = false

            if (ch.description.isEmpty()) {
                send_to_char("No lines left to remove.\n", ch)
                return
            }

            buf.append(ch.description)

            var len = buf.length
            while (-len >= 0) {
                if (buf[len] == '\r') {
                    if (!found)
                    /* back it up */ {
                        if (len > 0) {
                            len--
                        }
                        found = true
                    } else
                    /* found the second one */ {
                        ch.description = buf.substring(0, len + 1)
                        send_to_char("Your description is:\n", ch)
                        send_to_char(if (ch.description.isNotEmpty()) ch.description else "(None).\n", ch)
                        return
                    }
                }
            }
            ch.description = ""
            send_to_char("Description cleared.\n", ch)
            return
        }
        if (res[0] == '+') {
            if (!ch.description.isEmpty()) {
                buf.append(ch.description)
            }
            res = res.substring(1).trim { it <= ' ' }
        }

        buf.append(res)
        buf.append("\n")
        ch.description = buf.toString()
    }

    send_to_char("Your description is:\n", ch)
    send_to_char(if (ch.description.isNotEmpty()) ch.description else "(None).\n", ch)
}


fun do_report(ch: CHAR_DATA) {
    val buf = TextBuffer()
    buf.sprintf("I have %d/%d hp %d/%d mana %d/%d mv",
            ch.hit, ch.max_hit,
            ch.mana, ch.max_mana,
            ch.move, ch.max_move)
    do_say(ch, buf.toString())

}


fun do_practice(ch: CHAR_DATA, argument: String) {
    val pcdata = ch.pcdata ?: return

    val buf = TextBuffer()
    val buf2 = StringBuilder()
    if (argument.isEmpty()) {
        var col = 0
        for (sn in Skill.skills) {
            if (skill_failure_nomessage(ch, sn, 0) != 0) {
                continue
            }

            buf.sprintf("%-18s %3d%%  ", sn.skillName, pcdata.learned[sn.ordinal])
            buf.append(buf2)
            if (++col % 3 == 0) {
                buf2.append("\n")
            }
        }

        if (col % 3 != 0) {
            buf2.append("\n")
        }

        buf.sprintf("You have {w%d{x practice sessions left.\n", ch.practice)
        buf2.append(buf)

        if (IS_IMMORTAL(ch)) {
            page_to_char(buf2, ch)
        } else {
            send_to_char(buf2, ch)
        }
    } else {
        if (!IS_AWAKE(ch)) {
            send_to_char("In your dreams, or what?\n", ch)
            return
        }

        val mob: CHAR_DATA? = ch.room.people.firstOrNull { it.pcdata == null && IS_SET(it.act, ACT_PRACTICE) }
        if (mob == null) {
            send_to_char("You can't do that here.\n", ch)
            return
        }

        if (ch.practice <= 0) {
            send_to_char("You have no practice sessions left.\n", ch)
            return
        }
        val sn = Skill.find_spell(ch, argument)
        if (sn == null || skill_failure_nomessage(ch, sn, 0) != 0) {
            send_to_char("You can't practice that.\n", ch)
            return
        }

        if (eq("vampire", sn.skillName)) {
            send_to_char("You can't practice that, only available at questor.\n", ch)
            return
        }

        val adept = ch.clazz.skill_adept

        if (pcdata.learned[sn.ordinal] >= adept) {
            buf.sprintf("You are already learned at %s.\n", sn.skillName)
            send_to_char(buf, ch)
        } else {
            if (pcdata.learned[sn.ordinal] == 0) {
                pcdata.learned[sn.ordinal] = 1
            }
            ch.practice--
            pcdata.learned[sn.ordinal] += ch.learn / Math.max(sn.rating[ch.clazz.id], 1)
            if (pcdata.learned[sn.ordinal] < adept) {
                act("You practice \$T.", ch, null, sn.skillName, TO_CHAR)
                act("\$n practices \$T.", ch, null, sn.skillName, TO_ROOM)
            } else {
                pcdata.learned[sn.ordinal] = adept
                act("You are now learned at \$T.", ch, null, sn.skillName, TO_CHAR)
                act("\$n is now learned at \$T.", ch, null, sn.skillName, TO_ROOM)
            }
        }
    }
}

/* 'Wimpy' originally by Dionysos. */

fun do_wimpy(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)


    if (ch.clazz === Clazz.SAMURAI && ch.level >= 10) {
        send_to_char("You don't deal with wimpies, or such feary things.\n", ch)
        if (ch.wimpy != 0) {
            ch.wimpy = 0
        }
        return
    }

    val wimpy = if (arg.isEmpty()) ch.max_hit / 5 else atoi(arg.toString())

    if (wimpy < 0) {
        send_to_char("Your courage exceeds your wisdom.\n", ch)
        return
    }

    if (wimpy > ch.max_hit / 2) {
        send_to_char("Such cowardice ill becomes you.\n", ch)
        return
    }

    ch.wimpy = wimpy
    send_to_char("Wimpy set to $wimpy hit points.\n", ch)
}


fun do_password(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val pcdata = ch.pcdata ?: return

    /* Can't use one_argument here because it smashes case. So we just steal all its code.  Bleagh. */
    val arg_first = StringBuilder()
    rest = trimSpaces(rest, 0)
    val len = rest.length
    var cEnd = if (len != 0) rest[0] else ' '
    if (cEnd != '\'' && cEnd != '"') {
        arg_first.append(cEnd)
        cEnd = ' '
    }

    var pos = 1
    while (pos < len) {
        val c = rest[pos]
        if (c == cEnd) {
            pos++
            break
        }
        arg_first.append(c)
        pos++
    }
    rest = trimSpaces(rest, pos)

    val arg1 = arg_first.toString()
    val arg2 = rest
    if (arg1.isEmpty() || arg2.isEmpty()) {
        send_to_char("Syntax: password <old> <new>.\n", ch)
        return
    }

    if (crypt(arg1, ch.name) != pcdata.pwd) {
        WAIT_STATE(ch, 40)
        send_to_char("Wrong password.  Wait 10 seconds.\n", ch)
        return
    }

    if (arg2.length < 5) {
        send_to_char("New password must be at least five characters long.\n", ch)
        return
    }

    /*
         * No tilde allowed because of player file format.
         */
    val pwdnew = crypt(arg2, ch.name)
    if (pwdnew.contains("~")) {
        send_to_char("New password not acceptable, try again.\n", ch)
        return
    }

    pcdata.pwd = pwdnew
    save_char_obj(ch)
    send_to_char("Ok.\n", ch)
}

fun do_request(ch: CHAR_DATA, argument: String) {
    var rest = argument

    if (is_affected(ch, Skill.reserved)) {
        send_to_char("Wait for a while to request again.\n", ch)
        return
    }

    val arg1b = StringBuilder()
    val arg2b = StringBuilder()
    rest = one_argument(rest, arg1b)
    one_argument(rest, arg2b)

    if (ch.pcdata == null) {
        return
    }

    if (arg1b.isEmpty() || arg2b.isEmpty()) {
        send_to_char("Request what from whom?\n", ch)
        return
    }

    val arg2 = arg2b.toString()
    val victim = get_char_room(ch, arg2)
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim.pcdata != null) {
        send_to_char("Why don't you just ask the player?\n", ch)
        return
    }

    if (!ch.isGood()) {
        do_say(victim, "I will not give anything to someone so impure.")
        return
    }

    if (ch.move < 50 + ch.level) {
        do_say(victim, "You look rather tired, why don't you rest a bit first?")
        return
    }

    WAIT_STATE(ch, PULSE_VIOLENCE)
    ch.move -= 10
    ch.move = Math.max(ch.move, 0)

    if (victim.level >= ch.level + 10 || victim.level >= ch.level * 2) {
        do_say(victim, "In good time, my child")
        return
    }
    val arg1 = arg1b.toString()
    var obj = get_obj_carry(victim, arg1)
    if (obj == null) {
        obj = get_obj_wear(victim, arg1)
        if (obj == null || IS_SET(obj.extra_flags, ITEM_INVENTORY)) {
            do_say(victim, "Sorry, I don't have that.")
            return
        }
    }

    if (!victim.isGood()) {
        do_say(victim, "I'm not about to give you anything!")
        do_murder(victim, ch.name)
        return
    }

    if (obj.wear_loc.isOn()) {
        unequip_char(victim, obj)
    }

    if (!can_drop_obj(ch, obj)) {
        do_say(victim, "Sorry, I can't let go of it.  It's cursed.")
        return
    }

    if (ch.carry_number + get_obj_number(obj) > can_carry_items(ch)) {
        send_to_char("Your hands are full.\n", ch)
        return
    }

    if (ch.carry_weight + get_obj_weight(obj) > can_carry_weight(ch)) {
        send_to_char("You can't carry that much weight.\n", ch)
        return
    }

    if (!can_see_obj(ch, obj)) {
        act("You don't see that.", ch, null, victim, TO_CHAR)
        return
    }

    obj_from_char(obj)
    obj_to_char(obj, ch)
    act("\$n requests \$p from \$N.", ch, obj, victim, TO_NOTVICT)
    act("You request \$p from \$N.", ch, obj, victim, TO_CHAR)
    act("\$n requests \$p from you.", ch, obj, victim, TO_VICT)


    if (IS_SET(obj.progtypes, OPROG_GIVE)) {
        obj.pIndexData.oprogs.give_prog!!(obj, ch, victim)
    }

    if (IS_SET(victim.progtypes, MPROG_GIVE)) {
        victim.pIndexData.mprogs.give_prog!!(victim, ch, obj)
    }


    ch.move -= 50 + ch.level
    ch.move = Math.max(ch.move, 0)
    ch.hit -= 3 * (ch.level / 2)
    ch.hit = Math.max(ch.hit, 0)

    act("You feel grateful for the trust of \$N.", ch, null, victim, TO_CHAR)
    send_to_char("and for the goodness you have seen in the world.\n", ch)

    val af = Affect()
    af.type = Skill.reserved
    af.level = ch.level
    af.duration = ch.level / 10
    affect_to_char(ch, af)
}

// TODO: add race restrictions to Hometown enum
fun do_hometown(ch: CHAR_DATA, argument: String) {
    if (ch.pcdata == null) {
        send_to_char("You can't change your hometown!\n", ch)
        return
    }
    val race = ch.race
    if (race === Race.STORM_GIANT || race === Race.CLOUD_GIANT || race === Race.FIRE_GIANT || race === Race.FROST_GIANT) {
        send_to_char("Your hometown is permanently Titan Valley!\n", ch)
        return
    }

    if (ch.clazz === Clazz.VAMPIRE || ch.clazz === Clazz.NECROMANCER) {
        send_to_char("Your hometown is permanently Old Midgaard!\n", ch)
        return
    }

    if (!IS_SET(ch.room.room_flags, ROOM_REGISTRY)) {
        send_to_char("You have to be in the Registry to change your hometown.\n", ch)
        return
    }

    val amount = ch.level * ch.level * 250 + 1000

    if (argument.isEmpty()) {
        send_to_char("It will cost you %d gold.\n".format(amount), ch)
        return
    }

    if (ch.gold < amount) {
        send_to_char("You don't have enough money to change hometowns!\n", ch)
        return
    }

    val newHomeTown = when {
        startsWith(argument, "midgaard") -> Midgaard
        startsWith(argument, "newthalos") -> NewThalos
        startsWith(argument, "titans") -> Titans
        startsWith(argument, "ofcol") -> NewOfcol
        startsWith(argument, "oldmidgaard") -> OldMidgaard
        else -> null
    }

    if (newHomeTown == null) {
        send_to_char("That is not a valid choice.\n", ch)
        send_to_char("Choose from Midgaard, Newthalos, Titans, Ofcol and Old Midgaard.\n", ch)
        return
    }

    if (ch.hometown == newHomeTown) {
        send_to_char("But you already live in ${newHomeTown.displayName}!\n", ch)
        return
    }

    ch.gold -= amount
    send_to_char("Your hometown is changed to ${newHomeTown.displayName}.\n", ch)
    ch.hometown = newHomeTown
}


fun do_detect_hidden(ch: CHAR_DATA) {

    if (skill_failure_check(ch, Skill.detect_hidden, true, 0, null)) {
        return
    }

    if (IS_AFFECTED(ch, AFF_DETECT_HIDDEN)) {
        send_to_char("You are already as alert as you can be. \n", ch)
        return
    }
    if (randomPercent() > get_skill(ch, Skill.detect_hidden)) {
        send_to_char(
                "You peer intently at the shadows but they are unrevealing.\n",
                ch)
        return
    }
    val af = Affect()
    af.type = Skill.detect_hidden
    af.level = ch.level
    af.duration = ch.level
    af.bitvector = AFF_DETECT_HIDDEN
    affect_to_char(ch, af)
    send_to_char("Your awareness improves.\n", ch)
}


//todo: spell or skill?
fun do_bear_call(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.bear_call, true, 0, null)) {
        return
    }

    send_to_char("You call for bears help you.\n", ch)
    act("\$n shouts a bear call.", ch, null, null, TO_ROOM)

    if (is_affected(ch, Skill.bear_call)) {
        send_to_char("You cannot summon the strength to handle more bears right now.\n", ch)
        return
    }
    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_BEAR) {
            send_to_char("What's wrong with the bear you've got?", ch)
            return
        }
    }

    if (IS_SET(ch.room.room_flags, ROOM_NO_MOB)) {
        send_to_char("No bears listen you.\n", ch)
        return
    }

    if (randomPercent() > get_skill(ch, Skill.bear_call)) {
        send_to_char("No bears listen you.\n", ch)
        check_improve(ch, Skill.bear_call, true, 1)
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_SAFE) ||
            IS_SET(ch.room.room_flags, ROOM_PRIVATE) ||
            IS_SET(ch.room.room_flags, ROOM_SOLITARY) ||
            ch.room.exit[0] == null &&
                    ch.room.exit[1] == null &&
                    ch.room.exit[2] == null &&
                    ch.room.exit[3] == null &&
                    ch.room.exit[4] == null &&
                    ch.room.exit[5] == null ||

            ch.room.sector_type != SECT_FIELD &&
                    ch.room.sector_type != SECT_FOREST &&
                    ch.room.sector_type != SECT_MOUNTAIN &&
                    ch.room.sector_type != SECT_HILLS) {
        send_to_char("No bears come to your rescue.\n", ch)
        return
    }

    if (ch.mana < 125) {
        send_to_char("You don't have enough mana to shout a bear call.\n", ch)
        return
    }
    ch.mana -= 125

    check_improve(ch, Skill.bear_call, true, 1)
    val bear = create_mobile(get_mob_index_nn(MOB_VNUM_BEAR))

    bear.perm_stat.fill { s -> Math.min(25, 2 * ch.perm_stat[s]) }

    val pcdata = ch.pcdata
    bear.max_hit = pcdata?.perm_hit ?: ch.max_hit
    bear.hit = bear.max_hit
    bear.max_mana = pcdata?.perm_mana ?: ch.max_mana
    bear.mana = bear.max_mana
    bear.alignment = ch.alignment
    bear.level = Math.min(100, ch.level - 2)

    bear.armor.fill(interpolate(bear.level, 100, -100), 0, 3)
    bear.armor[3] = interpolate(bear.level, 100, 0)
    bear.sex = ch.sex
    bear.gold = 0

    val bear2 = create_mobile(bear.pIndexData)
    clone_mobile(bear, bear2)

    bear.affected_by = SET_BIT(bear.affected_by, AFF_CHARM)
    bear2.affected_by = SET_BIT(bear2.affected_by, AFF_CHARM)
    bear2.master = ch
    bear.master = bear2.master
    bear2.leader = ch
    bear.leader = bear2.leader

    char_to_room(bear, ch.room)
    char_to_room(bear2, ch.room)
    send_to_char("Two bears come to your rescue!\n", ch)
    act("Two bears come to \$n's rescue!", ch, null, null, TO_ROOM)
    val af = Affect()
    af.type = Skill.bear_call
    af.level = ch.level
    af.duration = 24
    affect_to_char(ch, af)

}


fun do_identify(ch: CHAR_DATA, argument: String) {
    val obj = get_obj_carry(ch, argument)
    if (obj == null) {
        send_to_char("You are not carrying that.\n", ch)
        return
    }

    val rch: CHAR_DATA? = ch.room.people.firstOrNull { it.pcdata == null && it.pIndexData.vnum == MOB_VNUM_SAGE }
    if (rch == null) {
        send_to_char("No one here seems to know much about that.\n", ch)
        return
    }

    when {
        IS_IMMORTAL(ch) -> act("\$n looks at you!\n", rch, obj, ch, TO_VICT)
        ch.gold < 1 -> {
            act("\$n resumes to identify by looking at \$p.", rch, obj, null, TO_ROOM)
            send_to_char(" You need at least 1 gold.\n", ch)
            return
        }
        else -> {
            ch.gold -= 1
            send_to_char("Your purse feels lighter.\n", ch)
        }
    }

    act("\$n gives a wise look at \$p.", rch, obj, null, TO_ROOM)
    spell_identify(ch, obj)
}


fun do_affects(ch: CHAR_DATA) {
    if (ch.affected != null) {
        send_to_char("You are affected by the following spells:\n", ch)
        var paf: Affect? = ch.affected
        var paf_last: Affect? = null
        while (paf != null) {
            if (paf_last != null && paf.type == paf_last.type) {
                if (ch.level < 20) {
                    paf = paf.next
                    continue
                }
                send_to_char("                      ", ch)
            } else {
                send_to_char("{rSpell{x: {Y%-15s{x".format(paf.type.skillName), ch)
            }

            if (ch.level >= 20) {
                send_to_char(": modifies {m%s{x by {m%d{x ".format(paf.location.locName, paf.modifier), ch)
                val msg = if (paf.duration == -1 || paf.duration == -2) {
                    "{cpermanently{x"
                } else {
                    "for {m%d{x hours".format(paf.duration)
                }
                send_to_char(msg, ch)
            }
            send_to_char("\n", ch)
            paf_last = paf
            paf = paf.next
        }
    } else {
        send_to_char("You are not affected by any spells.\n", ch)
    }

}


fun do_lion_call(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.lion_call, true, 0, null)) {
        return
    }

    send_to_char("You call for lions help you.\n", ch)
    act("\$n shouts a lion call.", ch, null, null, TO_ROOM)

    if (is_affected(ch, Skill.lion_call)) {
        send_to_char("You cannot summon the strength to handle more lions right now.\n", ch)
        return
    }
    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_LION) {
            send_to_char("What's wrong with the lion you've got?", ch)
            return
        }
    }

    if (IS_SET(ch.room.room_flags, ROOM_NO_MOB)) {
        send_to_char("No lions can listen you.\n", ch)
        return
    }

    if (randomPercent() > get_skill(ch, Skill.lion_call)) {
        send_to_char("No lions listen you.\n", ch)
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_SAFE) ||
            IS_SET(ch.room.room_flags, ROOM_PRIVATE) ||
            IS_SET(ch.room.room_flags, ROOM_SOLITARY) ||
            ch.room.exit[0] == null &&
                    ch.room.exit[1] == null &&
                    ch.room.exit[2] == null &&
                    ch.room.exit[3] == null &&
                    ch.room.exit[4] == null &&
                    ch.room.exit[5] == null ||
            ch.room.sector_type != SECT_FIELD &&
                    ch.room.sector_type != SECT_FOREST &&
                    ch.room.sector_type != SECT_MOUNTAIN &&
                    ch.room.sector_type != SECT_HILLS) {
        send_to_char("No lions come to your rescue.\n", ch)
        return
    }

    if (ch.mana < 125) {
        send_to_char("You don't have enough mana to shout a lion call.\n", ch)
        return
    }
    ch.mana -= 125

    val lion = create_mobile(get_mob_index_nn(MOB_VNUM_LION))

    lion.perm_stat.fill { Math.min(25, 2 * ch.perm_stat[it]) }

    val pcdata = ch.pcdata
    lion.max_hit = pcdata?.perm_hit ?: ch.max_hit
    lion.hit = lion.max_hit
    lion.max_mana = pcdata?.perm_mana ?: ch.max_mana
    lion.mana = lion.max_mana
    lion.alignment = ch.alignment
    lion.level = Math.min(100, ch.level - 2)

    lion.armor.fill(interpolate(lion.level, 100, -100), 0, 3)
    lion.armor[3] = interpolate(lion.level, 100, 0)
    lion.sex = ch.sex
    lion.gold = 0

    val lion2 = create_mobile(lion.pIndexData)
    clone_mobile(lion, lion2)

    lion.affected_by = SET_BIT(lion.affected_by, AFF_CHARM)
    lion2.affected_by = SET_BIT(lion2.affected_by, AFF_CHARM)
    lion2.master = ch
    lion.master = lion2.master
    lion2.leader = ch
    lion.leader = lion2.leader

    char_to_room(lion, ch.room)
    char_to_room(lion2, ch.room)
    send_to_char("Two lions come to your rescue!\n", ch)
    act("Two bears come to \$n's rescue!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.lion_call
    af.level = ch.level
    af.duration = 24
    affect_to_char(ch, af)
}

/* object condition aliases */
fun get_cond_alias(obj: Obj) = when {
    obj.condition > 99 -> "excellent"
    obj.condition >= 80 -> "good"
    obj.condition >= 60 -> "fine"
    obj.condition >= 40 -> "average"
    obj.condition >= 20 -> "poor"
    else -> "fragile"
}

/* room affects */

fun do_raffects(ch: CHAR_DATA) {
    if (ch.room.affected != null) {
        val buf = TextBuffer()
        send_to_char("The room is affected by the following spells:\n", ch)
        var paf: Affect? = ch.room.affected
        var paf_last: Affect? = null
        while (paf != null) {
            if (paf_last != null && paf.type == paf_last.type) {
                if (ch.level >= 20) {
                    buf.sprintf("                      ")
                } else {
                    paf = paf.next
                    continue
                }
            } else {
                buf.sprintf("Spell: %-15s", paf.type.skillName)
            }

            send_to_char(buf, ch)

            if (ch.level >= 20) {
                buf.sprintf(": modifies %s by %d ", paf.location.locName, paf.modifier)
                send_to_char(buf, ch)
                if (paf.duration == -1 || paf.duration == -2) {
                    buf.sprintf("permanently")
                } else {
                    buf.sprintf("for %d hours", paf.duration)
                }
                send_to_char(buf, ch)
            }
            send_to_char("\n", ch)
            paf_last = paf
            paf = paf.next
        }
    } else {
        send_to_char("The room is not affected by any spells.\n", ch)
    }

}

/* new practice */

fun do_pracnew(ch: CHAR_DATA, argument: String) {
    val pcdata = ch.pcdata ?: return

    if (argument.isEmpty()) {
        var col = 0
        val buf2 = StringBuilder()
        val buf = TextBuffer()
        for (sn in Skill.skills) {
            if (skill_failure_nomessage(ch, sn, 0) != 0) {
                continue
            }

            buf.sprintf("%-18s %3d%%  ", sn.skillName, pcdata.learned[sn.ordinal])
            buf2.append(buf.toString())
            if (++col % 3 == 0) {
                buf2.append("\n")
            }
        }

        if (col % 3 != 0) {
            buf2.append("\n")
        }

        buf.sprintf("You have {w%d{x practice sessions left.\n", ch.practice)
        buf2.append(buf.toString())
        page_to_char(buf2, ch)
    } else {
        if (!IS_AWAKE(ch)) {
            send_to_char("In your dreams, or what?\n", ch)
            return
        }

        if (ch.practice <= 0) {
            send_to_char("You have no practice sessions left.\n", ch)
            return
        }
        val sn = Skill.find_spell(ch, argument)
        if (sn == null || skill_failure_nomessage(ch, sn, 0) != 0) {
            send_to_char("You can't practice that.\n", ch)
            return
        }

        if (eq("vampire", sn.skillName)) {
            send_to_char("You can't practice that, only available at questor.\n", ch)
            return
        }


        var mob: CHAR_DATA? = null
        for (m in ch.room.people) {
            if (m.pcdata == null && IS_SET(m.act, ACT_PRACTICE)) {
                if (sn.cabal == Clan.None) {
                    if ((m.pIndexData.practicer == SG.None
                            && (sn.group == SG.None
                            || sn.group == SG.Creation
                            || sn.group == SG.Harmful
                            || sn.group == SG.Protective
                            || sn.group == SG.Detection
                            || sn.group == SG.Weather))
                            || m.pIndexData.practicer === sn.group) {
                        mob = m
                        break
                    }
                } else if (ch.cabal == m.cabal) {
                    mob = m
                    break
                }
            }
        }

        if (mob == null) {
            send_to_char("You can't do that here. USE glist ,slook for more info.\n", ch)
            return
        }

        val adept = ch.clazz.skill_adept

        if (pcdata.learned[sn.ordinal] >= adept) {
            val buf = TextBuffer()
            buf.sprintf("You are already learned at %s.\n", sn.skillName)
            send_to_char(buf, ch)
        } else {
            if (pcdata.learned[sn.ordinal] == 0) {
                pcdata.learned[sn.ordinal] = 1
            }
            ch.practice--
            pcdata.learned[sn.ordinal] += ch.learn / Math.max(sn.rating[ch.clazz.id], 1)
            if (pcdata.learned[sn.ordinal] < adept) {
                act("You practice \$T.", ch, null, sn.skillName, TO_CHAR)
                act("\$n practices \$T.", ch, null, sn.skillName, TO_ROOM)
            } else {
                pcdata.learned[sn.ordinal] = adept
                act("You are now learned at \$T.", ch, null, sn.skillName, TO_CHAR)
                act("\$n is now learned at \$T.", ch, null, sn.skillName, TO_ROOM)
            }
        }
    }
}

/*
* New 'who_col' command by chronos
*/


fun do_camp(ch: CHAR_DATA) {

    if (skill_failure_check(ch, Skill.camp, false, 0, null)) {
        return
    }

    if (is_affected(ch, Skill.camp)) {
        send_to_char("You don't have enough power to handle more camp areas.\n", ch)
        return
    }

    if (randomPercent() > get_skill(ch, Skill.camp)) {
        send_to_char("You failed to make your camp.\n", ch)
        check_improve(ch, Skill.camp, true, 4)
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_SAFE) ||
            IS_SET(ch.room.room_flags, ROOM_PRIVATE) ||
            IS_SET(ch.room.room_flags, ROOM_SOLITARY) ||
            ch.room.sector_type != SECT_FIELD &&
                    ch.room.sector_type != SECT_FOREST &&
                    ch.room.sector_type != SECT_MOUNTAIN &&
                    ch.room.sector_type != SECT_HILLS) {
        send_to_char("There are not enough leaves to camp here.\n", ch)
        return
    }

    if (ch.mana < 150) {
        send_to_char("You don't have enough mana to make a camp.\n", ch)
        return
    }

    check_improve(ch, Skill.camp, true, 4)
    ch.mana -= 150

    WAIT_STATE(ch, Skill.camp.beats)

    send_to_char("You succeeded to make your camp.\n", ch)
    act("\$n succeeded to make \$s camp.", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.camp
    af.level = ch.level
    af.duration = 12
    affect_to_char(ch, af)

    val af2 = Affect()
    af2.where = TO_ROOM_CONST
    af2.type = Skill.camp
    af2.level = ch.level
    af2.duration = ch.level / 20
    af2.modifier = 2 * ch.level
    af2.location = Apply.RoomHeal
    affect_to_room(ch.room, af2)

    af2.modifier = ch.level
    af2.location = Apply.RoomMana
    affect_to_room(ch.room, af2)
}


fun do_demand(ch: CHAR_DATA, argument: String) {
    var rest = argument
    var chance: Int
    val arg1b = StringBuilder()
    val arg2b = StringBuilder()
    rest = one_argument(rest, arg1b)
    one_argument(rest, arg2b)

    if (ch.pcdata == null) {
        return
    }

    if (skill_failure_check(ch, Skill.demand, false, 0, "You can't do that.\n")) {
        return
    }

    if (arg1b.isEmpty() || arg2b.isEmpty()) {
        send_to_char("Demand what from whom?\n", ch)
        return
    }
    val arg2 = arg2b.toString()
    val victim = get_char_room(ch, arg2)
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim.pcdata != null) {
        send_to_char("Why don't you just want that directly from the player?\n", ch)
        return
    }

    WAIT_STATE(ch, PULSE_VIOLENCE)

    chance = if (victim.isEvil()) 10 else if (victim.isGood()) -5 else 0
    chance += (ch.curr_stat(PrimeStat.Charisma) - 15) * 10
    chance += ch.level - victim.level

    if (victim.level >= ch.level + 10 || victim.level >= ch.level * 2) {
        chance = 0
    }

    if (randomPercent() > chance) {
        do_say(victim, "I'm not about to give you anything!")
        do_murder(victim, ch.name)
        return
    }

    val arg1 = arg1b.toString()
    var obj = get_obj_carry(victim, arg1)
    if (obj == null) {
        obj = get_obj_wear(victim, arg1)
        if (obj == null || IS_SET(obj.extra_flags, ITEM_INVENTORY)) {
            do_say(victim, "Sorry, I don't have that.")
            return
        }
    }


    if (obj.wear_loc.isOn()) {
        unequip_char(victim, obj)
    }

    if (!can_drop_obj(ch, obj)) {
        do_say(victim,
                "It's cursed so, I can't let go of it. Forgive me, my master")
        return
    }

    if (ch.carry_number + get_obj_number(obj) > can_carry_items(ch)) {
        send_to_char("Your hands are full.\n", ch)
        return
    }

    if (ch.carry_weight + get_obj_weight(obj) > can_carry_weight(ch)) {
        send_to_char("You can't carry that much weight.\n", ch)
        return
    }

    if (!can_see_obj(ch, obj)) {
        act("You don't see that.", ch, null, victim, TO_CHAR)
        return
    }

    obj_from_char(obj)
    obj_to_char(obj, ch)
    act("\$n demands \$p from \$N.", ch, obj, victim, TO_NOTVICT)
    act("You demand \$p from \$N.", ch, obj, victim, TO_CHAR)
    act("\$n demands \$p from you.", ch, obj, victim, TO_VICT)


    if (IS_SET(obj.progtypes, OPROG_GIVE)) {
        obj.pIndexData.oprogs.give_prog!!(obj, ch, victim)
    }

    if (IS_SET(victim.progtypes, MPROG_GIVE)) {
        victim.pIndexData.mprogs.give_prog!!(victim, ch, obj)
    }

    send_to_char("Your power makes all around the world shivering.\n", ch)

}


fun do_control(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (skill_failure_check(ch, Skill.control_animal, true, 0, null)) {
        return
    }

    if (arg.isEmpty()) {
        send_to_char("Charm what?\n", ch)
        return
    }

    val victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (get_pc(victim) != null) {
        send_to_char("You should try this on monsters?\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (count_charmed(ch) != 0) {
        return
    }

    WAIT_STATE(ch, PULSE_VIOLENCE)

    var chance = get_skill(ch, Skill.control_animal)

    chance += (ch.curr_stat(PrimeStat.Charisma) - 20) * 5
    chance += (ch.level - victim.level) * 3
    chance += (ch.curr_stat(PrimeStat.Intelligence) - victim.curr_stat(PrimeStat.Intelligence)) * 5

    if (IS_AFFECTED(victim, AFF_CHARM)
            || IS_AFFECTED(ch, AFF_CHARM)
            || randomPercent() > chance
            || ch.level < victim.level + 2
            || IS_SET(victim.imm_flags, IMM_CHARM)
            || victim.pcdata == null && victim.pIndexData.pShop != null) {
        check_improve(ch, Skill.control_animal, false, 2)
        do_say(victim, "I'm not about to follow you!")
        do_murder(victim, ch.name)
        return
    }

    check_improve(ch, Skill.control_animal, true, 2)

    if (victim.master != null) {
        stop_follower(victim)
    }
    victim.affected_by = SET_BIT(victim.affected_by, AFF_CHARM)
    victim.leader = ch
    victim.master = victim.leader

    act("Isn't \$n just so nice?", ch, null, victim, TO_VICT)
    if (ch != victim) {
        act("\$N looks at you with adoring eyes.", ch, null, victim, TO_CHAR)
    }

}


fun do_nscore(ch: CHAR_DATA) {
    var ekle = 0
    val buf = TextBuffer()
    buf.sprintf("{G\n\n      /~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~/~~\\\n")

    send_to_char(buf, ch)
    val pcdata = ch.pcdata
    val titlebuf = pcdata?.title ?: "Believer of Chronos."

    buf.sprintf("     {G|   {R%-12s{w%-33s {y%3d{x years old   {G|{g____|{G\n", ch.name, titlebuf, get_age(ch))
    send_to_char(buf, ch)

    buf.sprintf("     |{C+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+{G|\n")
    send_to_char(buf, ch)

    buf.sprintf("     | {RLevel:{x  %3d          {C|  {RStr:{x  %2d(%2d)  {C| {RReligion  :{x  %-10s {G|\n",
            ch.level, ch.perm_stat[PrimeStat.Strength], ch.curr_stat(PrimeStat.Strength), ch.religion.leader)
    send_to_char(buf, ch)

    buf.sprintf("     | {RRace :{x  %-11s  {C|  {RInt:{x  %2d(%2d)  {C| {RPractice  :{x   %3d       {G|\n",
            ch.race.name, ch.perm_stat[PrimeStat.Intelligence], ch.curr_stat(PrimeStat.Intelligence), ch.practice)
    send_to_char(buf, ch)

    buf.sprintf("     | {RSex  :{x  %-11s  {C|  {RWis:{x  %2d(%2d)  {C| {RTrain     :{x   %3d       {G|\n",
            ch.sex.title, ch.perm_stat[PrimeStat.Wisdom], ch.curr_stat(PrimeStat.Wisdom), ch.train)
    send_to_char(buf, ch)

    buf.sprintf("     | {RClass:{x  %-12s {C|  {RDex:{x  %2d(%2d)  {C| {RQuest Pnts:{x  %4d       {G|\n",
            if (pcdata == null) "mobile" else ch.clazz.name, ch.perm_stat[PrimeStat.Dexterity],
            ch.curr_stat(PrimeStat.Dexterity), pcdata?.questpoints ?: 0)
    send_to_char(buf, ch)

    buf.sprintf("     | {RHome :{x  %-12s {C|  {RCon:{x  %2d(%2d)  {C| {RQuest Time:{x   %3d       {G|\n",
            ch.hometown.displayName, ch.perm_stat[PrimeStat.Constitution], ch.curr_stat(PrimeStat.Constitution), pcdata?.nextquest ?: 0)
    send_to_char(buf, ch)
    buf.sprintf("     | {REthos:{x  %-11s  {C|  {RCha:{x  %2d(%2d)  {C| {R%s     :{x   %3d       {G|\n",
            if (pcdata == null) "mobile" else ch.ethos.title,
            ch.perm_stat[PrimeStat.Charisma], ch.curr_stat(PrimeStat.Charisma),
            if (ch.clazz === Clazz.SAMURAI) "Death" else "Wimpy", if (ch.clazz === Clazz.SAMURAI) pcdata?.death else ch.wimpy)
    send_to_char(buf, ch)
    val buf2 = when (ch.position) {
        Pos.Dead -> "You are DEAD!!"
        Pos.Mortal -> "You're fatally wounded."
        Pos.Incap -> "You are incapacitated."
        Pos.Stunned -> "You are stunned."
        Pos.Sleeping -> "You are sleeping."
        Pos.Resting -> "You are resting."
        Pos.Sitting -> "You are sitting."
        Pos.Fighting -> "You are fighting."
        Pos.Standing -> "You are standing."
    }

    buf.sprintf("     | {RAlign:{x  %-11s  {C|                | {y%-23s {G|\n",
            if (ch.isGood()) "good" else if (ch.isEvil()) "evil" else "neutral", buf2)
    send_to_char(buf, ch)

    buf.sprintf("     |{C+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+{G|\n")
    send_to_char(buf, ch)

    val guarding = ch.guarding
    if (guarding != null) {
        ekle = 1
        buf.sprintf("     | {WYou are guarding:{x %-10s                                    {G|\n", guarding.name)
        send_to_char(buf, ch)
    }

    val guarded_by = ch.guarded_by
    if (guarded_by != null) {
        ekle = 1
        buf.sprintf("     | {WYou are guarded by:{x %-10s                                  {G|\n", guarded_by.name)
        send_to_char(buf, ch)
    }

    if (pcdata != null) {
        if (pcdata.condition[COND_DRUNK] > 10) {
            ekle = 1
            buf.sprintf("     | {WYou are drunk.                                                  {G|\n")
            send_to_char(buf, ch)
        }

        if (pcdata.condition[COND_THIRST] <= 0) {
            ekle = 1
            buf.sprintf("     | {WYou are thirsty.                                                {G|\n")
            send_to_char(buf, ch)
        }
        /*    if ( !IS_NPC(ch) && ch.pcdata.condition[COND_FULL]   ==  0 ) */
        if (pcdata.condition[COND_HUNGER] <= 0) {
            ekle = 1
            buf.sprintf("     | {WYou are hungry.                                                 {G|\n")
            send_to_char(buf, ch)
        }

        if (pcdata.condition[COND_BLOODLUST] <= 0) {
            ekle = 1
            buf.sprintf("     | {WYou are hungry for blood.                                       {G|\n")
            send_to_char(buf, ch)
        }

        if (pcdata.condition[COND_DESIRE] <= 0) {
            ekle = 1
            buf.sprintf("     | {WYou are desiring your home.                                     {G|\n")
            send_to_char(buf, ch)
        }

        if (ch.last_fight_time != -1L && !IS_IMMORTAL(ch) && current_time - ch.last_fight_time < FIGHT_DELAY_TIME) {
            ekle = 1
            buf.sprintf("     | {WYour adrenalin is gushing!                                      {G|\n")
            send_to_char(buf, ch)
        }
    }

    if (ekle != 0) {
        buf.sprintf("     |{c+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+{G|\n")
        send_to_char(buf, ch)
    }

    buf.sprintf("     | {RItems Carried :{x     %3d/%-4d        {RArmor vs magic  :{x %4d      {G|\n",
            ch.carry_number, can_carry_items(ch), GET_AC(ch, AC_EXOTIC))
    send_to_char(buf, ch)

    buf.sprintf("     | {RWeight Carried:{x  %6d/%-8d    {RArmor vs bash   :{x %4d      {G|\n",
            get_carry_weight(ch), can_carry_weight(ch), GET_AC(ch, AC_BASH))
    send_to_char(buf, ch)

    buf.sprintf("     | {RGold          :{x   %-10d        {RArmor vs pierce :{x %4d      {G|\n",
            ch.gold, GET_AC(ch, AC_PIERCE))
    send_to_char(buf, ch)

    buf.sprintf("     | {RSilver        :{x   %-10d        {RArmor vs slash  :{x %4d      {G|\n",
            ch.silver, GET_AC(ch, AC_SLASH))
    send_to_char(buf, ch)

    buf.sprintf("     | {RCurrent exp   :{x   %-6d            {RSaves vs Spell  :{x %4d      {G|\n",
            ch.exp, ch.saving_throw)
    send_to_char(buf, ch)

    buf.sprintf("     | {RExp to level  :{x   %-6d                                        {G|\n",
            if (pcdata == null) 0 else exp_to_level(ch))
    send_to_char(buf, ch)

    buf.sprintf("     |                                     {RHitP:{x %5d / %5d         {G|\n",
            ch.hit, ch.max_hit)
    send_to_char(buf, ch)
    buf.sprintf("     | {RHitroll       :{x   %-3d               {RMana:{x %5d / %5d         {G|\n",
            GET_HITROLL(ch), ch.mana, ch.max_mana)
    send_to_char(buf, ch)
    buf.sprintf("     | {RDamroll       :{x   %-3d               {RMove:{x %5d / %5d         {G|\n",
            GET_DAMROLL(ch), ch.move, ch.max_move)
    send_to_char(buf, ch)
    buf.sprintf("  /~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~/   |\n")
    send_to_char(buf, ch)
    buf.sprintf("  \\________________________________________________________________\\__/{x\n")
    send_to_char(buf, ch)
    if (ch.affected != null && IS_SET(ch.comm, COMM_SHOW_AFFECTS)) {
        do_affects(ch)
    }
}


fun do_make_arrow(ch: CHAR_DATA, argument: String) {
    var arrow: Obj
    var str: String

    if (skill_failure_check(ch, Skill.make_arrow, false, 0, "You don't know how to make arrows.\n")) {
        return
    }

    if (ch.room.sector_type != SECT_FIELD &&
            ch.room.sector_type != SECT_FOREST &&
            ch.room.sector_type != SECT_HILLS) {
        send_to_char("You couldn't find enough wood.\n", ch)
        return
    }

    var mana = Skill.make_arrow.min_mana
    var wait = Skill.make_arrow.beats

    val argb = StringBuilder()
    one_argument(argument, argb)
    val arg = argb.toString()
    val color: Skill?
    color = when {
        arg.isEmpty() -> null
        startsWith(arg, "green") -> Skill.green_arrow
        startsWith(arg, "red") -> Skill.red_arrow
        startsWith(arg, "white") -> Skill.white_arrow
        startsWith(arg, "blue") -> Skill.blue_arrow
        else -> {
            send_to_char("You don't know how to make that kind of arrow.\n", ch)
            return
        }
    }

    if (color != null) {
        mana += color.min_mana
        wait += color.beats
    }

    if (ch.mana < mana) {
        send_to_char("You don't have enough energy to make that kind of arrows.\n", ch)
        return
    }
    ch.mana -= mana
    WAIT_STATE(ch, wait)

    send_to_char("You start to make arrows!\n", ch)
    act("\$n starts to make arrows!", ch, null, null, TO_ROOM)
    val buf = TextBuffer()
    for (count in 0 until ch.level / 5) {
        if (randomPercent() > get_skill(ch, Skill.make_arrow)) {
            send_to_char("You failed to make the arrow, and broke it.\n", ch)
            check_improve(ch, Skill.make_arrow, false, 3)
            continue
        }
        send_to_char("You successfully make an arrow.\n", ch)
        check_improve(ch, Skill.make_arrow, true, 3)

        arrow = create_object(get_obj_index_nn(OBJ_VNUM_RANGER_ARROW), ch.level)
        arrow.level = ch.level
        arrow.value[1] = (ch.level / 10).toLong()
        arrow.value[2] = (ch.level / 10).toLong()

        val tohit = Affect()
        tohit.where = TO_OBJECT
        tohit.type = Skill.make_arrow
        tohit.level = ch.level
        tohit.duration = -1
        tohit.location = Apply.Hitroll
        tohit.modifier = ch.level / 10
        affect_to_obj(arrow, tohit)

        val todam = Affect()
        todam.where = TO_OBJECT
        todam.type = Skill.make_arrow
        todam.level = ch.level
        todam.duration = -1
        todam.location = Apply.Damroll
        todam.modifier = ch.level / 10
        affect_to_obj(arrow, todam)

        var saf: Affect? = null
        if (color != null) {
            saf = Affect()
            saf.where = TO_WEAPON
            saf.type = color
            saf.level = ch.level
            saf.duration = -1
            saf.location = Apply.None

            when (color) {
                Skill.green_arrow -> {
                    saf.bitvector = WEAPON_POISON
                    str = "green"
                }
                Skill.red_arrow -> {
                    saf.bitvector = WEAPON_FLAMING
                    str = "red"
                }
                Skill.white_arrow -> {
                    saf.bitvector = WEAPON_FROST
                    str = "white"
                }
                else -> {
                    saf.bitvector = WEAPON_SHOCKING
                    str = "blue"
                }
            }
        } else {
            str = "wooden"
        }

        buf.sprintf(arrow.name, str)
        arrow.name = buf.toString()

        buf.sprintf(arrow.short_desc, str)
        arrow.short_desc = buf.toString()

        buf.sprintf(arrow.description, str)
        arrow.description = buf.toString()

        if (color != null) {
            affect_to_obj(arrow, saf)
        }
        obj_to_char(arrow, ch)
    }
}


fun do_make_bow(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.make_bow, false, 0, "You don't know how to make bows.\n")) {
        return
    }

    if (ch.room.sector_type != SECT_FIELD &&
            ch.room.sector_type != SECT_FOREST &&
            ch.room.sector_type != SECT_HILLS) {
        send_to_char("You couldn't find enough wood.\n", ch)
        return
    }

    val mana = Skill.make_bow.min_mana
    val wait = Skill.make_bow.beats

    if (ch.mana < mana) {
        send_to_char("You don't have enough energy to make a bow.\n", ch)
        return
    }
    ch.mana -= mana
    WAIT_STATE(ch, wait)

    if (randomPercent() > get_skill(ch, Skill.make_bow)) {
        send_to_char("You failed to make the bow, and broke it.\n", ch)
        check_improve(ch, Skill.make_bow, false, 1)
        return
    }
    send_to_char("You successfully make bow.\n", ch)
    check_improve(ch, Skill.make_bow, true, 1)

    val bow = create_object(get_obj_index_nn(OBJ_VNUM_RANGER_BOW), ch.level)
    bow.level = ch.level
    bow.value[1] = (3 + ch.level / 12).toLong()
    bow.value[2] = (4 + ch.level / 12).toLong()

    val tohit = Affect()
    tohit.where = TO_OBJECT
    tohit.type = Skill.make_arrow
    tohit.level = ch.level
    tohit.duration = -1
    tohit.location = Apply.Hitroll
    tohit.modifier = ch.level / 10
    affect_to_obj(bow, tohit)

    val todam = Affect()
    todam.where = TO_OBJECT
    todam.type = Skill.make_arrow
    todam.level = ch.level
    todam.duration = -1
    todam.location = Apply.Damroll
    todam.modifier = ch.level / 10
    affect_to_obj(bow, todam)

    obj_to_char(bow, ch)
}


fun do_make(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val argb = StringBuilder()
    rest = one_argument(rest, argb)
    if (argb.isEmpty()) {
        send_to_char("You can make either bow or arrow.\n", ch)
        return
    }

    val arg = argb.toString()
    when {
        startsWith(arg, "arrow") -> do_make_arrow(ch, rest)
        startsWith(arg, "bow") -> do_make_bow(ch)
        else -> do_make(ch, "")
    }
}


fun do_nocancel(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    if (IS_SET(ch.act, PLR_NOCANCEL)) {
        send_to_char("You now accept others' cancellation spells.\n", ch)
        ch.act = REMOVE_BIT(ch.act, PLR_NOCANCEL)
    } else {
        send_to_char("You no longer accept others' cancellation spells.\n", ch)
        ch.act = SET_BIT(ch.act, PLR_NOCANCEL)
    }
}

fun doppel_name(ch: CHAR_DATA, victim: CHAR_DATA) = if (ch.doppel != null && !IS_IMMORTAL(victim)) ch.doppel!!.name else ch.name
