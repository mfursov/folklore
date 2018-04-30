package net.sf.nightworks.act.spell


import net.sf.nightworks.AFF_WEAKEN
import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_AFFECTS
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.is_affected
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.send_to_char

fun spell_weaken(level: Int, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.weaken) || saves_spell(level, victim, DT.Other)) {
        return
    }
    val af = Affect()
    af.type = Skill.weaken
    af.level = level
    af.duration = 4 + level / 12
    af.modifier = -1 * (2 + level / 12)
    af.bitvector = AFF_WEAKEN
    affect_to_char(victim, af)
    send_to_char("You feel your strength slip away.\n", victim)
    act("\$n looks tired and weak.", victim, null, null, TO_ROOM)
}