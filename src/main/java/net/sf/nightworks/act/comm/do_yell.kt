package net.sf.nightworks.act.comm


import net.sf.nightworks.Index
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.act
import net.sf.nightworks.garble
import net.sf.nightworks.is_affected
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Pos
import net.sf.nightworks.send_to_char
import net.sf.nightworks.translate

fun do_yell(ch: CHAR_DATA, arg: CharSequence) {
    if (arg.isEmpty()) {
        send_to_char("Yell what?\n", ch)
        return
    }

    val argument = arg.toString()
    val buf = if (is_affected(ch, Skill.garble)) garble(argument) else argument

    if (!is_affected(ch, Skill.deafen)) {
        act("You yell '{y\$t{x'", ch, buf, null, TO_CHAR, Pos.Dead)
    }

    for (pc in Index.PLAYERS) {
        if (pc.ch != ch
                && pc.ch.room.area == ch.room.area
                && !is_affected(pc.ch, Skill.deafen)) {
            val trans = translate(ch, pc.ch, buf)
            act("\$n yells '{y\$t{x'", ch, trans, pc.ch, TO_VICT, Pos.Dead)
        }
    }

}
