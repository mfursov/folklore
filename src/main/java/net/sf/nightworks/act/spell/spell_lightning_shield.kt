package net.sf.nightworks.act.spell


import net.sf.nightworks.AFF_ROOM_L_SHIELD
import net.sf.nightworks.Skill
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

fun spell_lightning_shield(level: Int, ch: CHAR_DATA) {

    if (is_affected_room(ch.room, Skill.lightning_shield)) {
        send_to_char("This room has already shielded.\n", ch)
        return
    }

    if (is_affected(ch, Skill.lightning_shield)) {
        send_to_char("This spell is used too recently.\n", ch)
        return
    }

    val af = Affect()
    af.where = TO_ROOM_AFFECTS
    af.type = Skill.lightning_shield
    af.level = ch.level
    af.duration = level / 40
    af.bitvector = AFF_ROOM_L_SHIELD
    affect_to_room(ch.room, af)

    val af2 = Affect()
    af2.type = Skill.lightning_shield
    af2.level = ch.level
    af2.duration = level / 10
    affect_to_char(ch, af2)

    ch.room.owner = ch.name
    send_to_char("The room starts to be filled with lightnings.\n", ch)
    act("The room starts to be filled with \$n's lightnings.", ch, null, null, TO_ROOM)
}
