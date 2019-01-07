package net.sf.nightworks.act.info

import net.sf.nightworks.EX_CLOSED
import net.sf.nightworks.Skill
import net.sf.nightworks.can_see_room
import net.sf.nightworks.check_blind
import net.sf.nightworks.check_improve
import net.sf.nightworks.dir_name
import net.sf.nightworks.get_skill
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.room_dark
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.capitalize
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.randomPercent

fun do_exits(ch: CHAR_DATA, argument: String) {
    val fAuto = eq(argument, "auto")

    if (!check_blind(ch)) {
        return
    }

    val buf = TextBuffer()
    when {
        fAuto -> buf.append("{B[Exits:")
        IS_IMMORTAL(ch) -> buf.sprintf("Obvious exits from room %d:\n", ch.room.vnum)
        else -> buf.sprintf("Obvious exits:\n")
    }

    var found = false
    var door = 0
    while (door <= 5) {
        var pexit = ch.room.exit[door]
        if (pexit != null && can_see_room(ch, pexit.to_room) && !IS_SET(pexit.exit_info, EX_CLOSED)) {
            found = true
            if (fAuto) {
                buf.append(" ")
                buf.append(dir_name[door])
            } else {
                buf.sprintf(false, "%-5s - %s", capitalize(dir_name[door]), if (room_dark(pexit.to_room)) "Too dark to tell" else pexit.to_room.name)
                if (IS_IMMORTAL(ch)) {
                    buf.sprintf(false, " (room %d)\n", pexit.to_room.vnum)
                } else {
                    buf.append("\n")
                }
            }
        }

        pexit = ch.room.exit[door]
        if (randomPercent() < get_skill(ch, Skill.perception) && pexit != null && can_see_room(ch, pexit.to_room) && IS_SET(pexit.exit_info, EX_CLOSED)) {
            check_improve(ch, Skill.perception, true, 5)
            found = true
            if (fAuto) {
                buf.append(" ")
                buf.append(dir_name[door])
                buf.append("*")
            } else {
                buf.sprintf(false, "%-5s * (%s)", capitalize(dir_name[door]), pexit.keyword)
                if (IS_IMMORTAL(ch)) {
                    buf.sprintf(false, " (room %d)\n", pexit.to_room.vnum)
                } else {
                    buf.append("\n")
                }
            }
        }
        door++

    }
    if (!found) {
        buf.append(if (fAuto) " none" else "None.\n")
    }

    if (fAuto) {
        buf.append("]{x\n")
    }

    send_to_char(buf, ch)
}
