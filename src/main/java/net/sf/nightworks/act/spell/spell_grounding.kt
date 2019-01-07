package net.sf.nightworks.act.spell


import net.sf.nightworks.AFF_GROUNDING
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.act
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.is_affected
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char

fun spell_grounding(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.grounding)) {
        if (victim == ch) {
            send_to_char("You are already at ground potential.\n", ch)
        } else {
            act("\$N is already at ground potential.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.grounding
    af.level = level
    af.duration = 5 + level / 8
    af.bitvector = AFF_GROUNDING
    affect_to_char(victim, af)
    send_to_char("Your body is electrically grounded.\n", victim)
    if (ch != victim) {
        act("\$N is grounded by your magic.", ch, null, victim, TO_CHAR)
    }
}
