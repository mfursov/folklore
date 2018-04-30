package net.sf.nightworks.act.spell


import net.sf.nightworks.AFF_ROOM_SHOCKING
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_AFFECTS
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.TO_ROOM_AFFECTS
import net.sf.nightworks.act
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.affect_to_room
import net.sf.nightworks.is_affected
import net.sf.nightworks.is_affected_room
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char

fun spell_shocking_trap(level: Int, ch: CHAR_DATA) {
    if (is_affected_room(ch.room, Skill.shocking_trap)) {
        send_to_char("This room has already trapped with shocks waves.\n", ch)
        return
    }

    if (is_affected(ch, Skill.shocking_trap)) {
        send_to_char("This spell is used too recently.\n", ch)
        return
    }
    val af = Affect()
    af.where = TO_ROOM_AFFECTS
    af.type = Skill.shocking_trap
    af.level = ch.level
    af.duration = level / 40
    af.bitvector = AFF_ROOM_SHOCKING
    affect_to_room(ch.room, af)

    val af2 = Affect()
    af2.type = Skill.shocking_trap
    af2.level = level
    af2.duration = ch.level / 10
    affect_to_char(ch, af2)
    send_to_char("The room starts to be filled with shock waves.\n", ch)
    act("The room starts to be filled with \$n's shock waves.", ch, null, null, TO_ROOM)
}