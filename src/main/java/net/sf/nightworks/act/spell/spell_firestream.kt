package net.sf.nightworks.act.spell


import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.TARGET_CHAR
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_NOTVICT
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.act
import net.sf.nightworks.damage
import net.sf.nightworks.util.dice
import net.sf.nightworks.fire_effect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell

fun spell_firestream(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    act("\$n throws a stream of searing flames.", ch, null, victim, TO_NOTVICT)
    act("\$n throws a stream of hot flames over you!", ch, null, victim, TO_VICT)
    act("You throw a stream of searing flames to \$N.", ch, null, victim, TO_CHAR)

    val dam = dice(level, 8)

    if (saves_spell(level, victim, DT.Fire)) {
        fire_effect(victim, level / 2, dam / 4, TARGET_CHAR)
        damage(ch, victim, dam / 2, Skill.firestream, DT.Fire, true)
    } else {
        fire_effect(victim, level, dam, TARGET_CHAR)
        damage(ch, victim, dam, Skill.firestream, DT.Fire, true)
    }
}
