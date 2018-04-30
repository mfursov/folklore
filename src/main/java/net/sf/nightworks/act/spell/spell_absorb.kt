package net.sf.nightworks.act.spell

import net.sf.nightworks.AFF_ABSORB
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_AFFECTS
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.is_affected
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char

fun spell_absorb(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.absorb)) {
        send_to_char("You are already absorbing magic surrounding you.\n", ch)
        return
    }
    val af = Affect()
    af.type = Skill.absorb
    af.level = level
    af.duration = 3 + level / 10
    af.bitvector = AFF_ABSORB
    affect_to_char(ch, af)
    send_to_char("Your body is surrounded by an energy field.\n", ch)
}