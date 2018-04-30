package net.sf.nightworks.act.spell

import net.sf.nightworks.AFF_EARTHFADE

import net.sf.nightworks.SECT_AIR
import net.sf.nightworks.SECT_WATER_NO_SWIM
import net.sf.nightworks.SECT_WATER_SWIM
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_AFFECTS
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.MOUNTED

fun spell_earthfade(level: Int, ch: CHAR_DATA) {
    if (IS_AFFECTED(ch, AFF_EARTHFADE)) {
        return
    }

    if (ch.room.sector_type == SECT_AIR
            || ch.room.sector_type == SECT_WATER_SWIM
            || ch.room.sector_type == SECT_WATER_NO_SWIM) {
        send_to_char("You cannot reach the earth to fade.\n", ch)
        return
    }

    if (MOUNTED(ch) != null) {
        send_to_char("You can't fade to earth while mounted.\n", ch)
        return
    }

    act("\$n fades into earth.", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.earthfade
    af.level = level
    af.duration = level / 8 + 10
    af.bitvector = AFF_EARTHFADE
    affect_to_char(ch, af)
    send_to_char("You fade into earth.\n", ch)
}