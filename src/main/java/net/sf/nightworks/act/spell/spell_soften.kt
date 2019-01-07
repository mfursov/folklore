package net.sf.nightworks.act.spell


import net.sf.nightworks.AFF_FAERIE_FIRE
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.is_affected
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char

fun spell_soften(level: Int, victim: CHAR_DATA) {
    if (is_affected(victim, Skill.soften)) {
        return
    }

    val af = Affect()
    af.type = Skill.soften
    af.level = level
    af.duration = 5 + level / 10
    af.location = Apply.Ac
    af.modifier = 4 * level
    af.bitvector = AFF_FAERIE_FIRE
    affect_to_char(victim, af)

    af.type = Skill.soften
    af.level = level
    af.duration = 10 + level / 5
    af.location = Apply.SavingSpell
    af.modifier = -1
    affect_to_char(victim, af)

    send_to_char("Your skin starts to wrinkle.\n", victim)
    act("\$n skin starts to wrinkle.", victim, null, null, TO_ROOM)
}
