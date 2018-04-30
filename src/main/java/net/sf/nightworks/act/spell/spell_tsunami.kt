package net.sf.nightworks.act.spell


import net.sf.nightworks.DT
import net.sf.nightworks.SECT_WATER_NO_SWIM
import net.sf.nightworks.SECT_WATER_SWIM
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.damage
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.dice

fun spell_tsunami(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch.room.sector_type != SECT_WATER_SWIM && ch.room.sector_type != SECT_WATER_NO_SWIM) {
        send_to_char("You can't reach a water source to create a tsunami.\n", ch)
        ch.wait = 0
        return
    }

    act("An existing parcel of water rises up and forms a fist and pummels \$n.", victim, null, null, TO_ROOM)
    act("An existing parcel of water rises up and forms a fist and pummels you.", victim, null, null, TO_CHAR)
    val dam = dice(level, 16)
    damage(ch, victim, dam, Skill.tsunami, DT.Drowing, true)
}