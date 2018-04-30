package net.sf.nightworks.act.info

import net.sf.nightworks.AFF_FADE
import net.sf.nightworks.AFF_HIDE
import net.sf.nightworks.AFF_SNEAK
import net.sf.nightworks.AREA_PROTECTED
import net.sf.nightworks.Index
import net.sf.nightworks.PLR_HOLYLIGHT
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.act
import net.sf.nightworks.can_see
import net.sf.nightworks.check_blind
import net.sf.nightworks.is_name
import net.sf.nightworks.is_safe_nomessage
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.room_is_dark
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.PERS
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.one_argument

fun do_where(ch: CHAR_DATA, argument: String) {
    val argb = StringBuilder()
    one_argument(argument, argb)

    if (!check_blind(ch)) {
        return
    }

    if (room_is_dark(ch) && !IS_SET(ch.act, PLR_HOLYLIGHT)) {
        send_to_char("It's too dark to see.\n", ch)
        return
    }
    val arg = argb.toString()
    if (eq(arg, "protector")) {
        if (IS_SET(ch.room.area.area_flag, AREA_PROTECTED)) {
            send_to_char("This area is protected by Rulers!\n", ch)
        } else {
            send_to_char("This area is not protected.\n", ch)
        }
        return
    }

    var fPKonly = false
    if (eq(arg, "pk")) {
        fPKonly = true
    }

    val pkbuf = "{r(PK){x "

    val buf = TextBuffer()
    if (arg.isEmpty() || fPKonly) {
        send_to_char("Players near you:\n", ch)
        var found = false
        for (d in Index.PLAYERS) {
            val victim = d.ch
            val v_pcdata = victim.pcdata
            if (v_pcdata != null && !(fPKonly && is_safe_nomessage(ch, victim)) && victim.room.area == ch.room.area && can_see(ch, victim)) {
                found = true
                val doppel = victim.doppel
                buf.sprintf("%s%-28s %s\n",
                        if (is_safe_nomessage(ch, doppel ?: victim)) "  " else pkbuf,
                        if (doppel != null && !IS_SET(ch.act, PLR_HOLYLIGHT)) doppel.name else victim.name,
                        victim.room.name)
                send_to_char(buf, ch)
            }
        }
        if (!found) {
            send_to_char("None\n", ch)
        }
    } else {
        var found = false
        for (victim in Index.CHARS) {
            if (victim.room.area == ch.room.area
                    && !IS_AFFECTED(victim, AFF_HIDE)
                    && !IS_AFFECTED(victim, AFF_FADE)
                    && !IS_AFFECTED(victim, AFF_SNEAK)
                    && can_see(ch, victim)
                    && is_name(arg, victim.name)) {
                found = true
                buf.sprintf("%-28s %s\n", PERS(victim, ch), victim.room.name)
                send_to_char(buf, ch)
                break
            }
        }
        if (!found) {
            act("You didn't find any \$T.", ch, null, arg, TO_CHAR)
        }
    }
}