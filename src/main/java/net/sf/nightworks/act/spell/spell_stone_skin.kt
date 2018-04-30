package net.sf.nightworks.act.spell


import net.sf.nightworks.Skill
import net.sf.nightworks.TO_AFFECTS
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.is_affected
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char

fun spell_stone_skin(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (is_affected(ch, Skill.stone_skin)) {
        when (victim) {
            ch -> send_to_char("Your skin is already as hard as a rock.\n", ch)
            else -> act("\$N is already as hard as can be.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.stone_skin
    af.level = level
    af.duration = 10 + level / 5
    af.location = Apply.Ac
    af.modifier = -1 * Math.max(40, 20 + level / 2)  /*af.modifier=-40;*/
    affect_to_char(victim, af)
    act("\$n's skin turns to stone.", victim, null, null, TO_ROOM)
    send_to_char("Your skin turns to stone.\n", victim)
}