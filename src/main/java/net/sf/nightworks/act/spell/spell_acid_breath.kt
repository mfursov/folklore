package net.sf.nightworks.act.spell

import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.TARGET_CHAR
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_NOTVICT
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.acid_effect
import net.sf.nightworks.act
import net.sf.nightworks.damage
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.number_range

fun spell_acid_breath(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    act("\$n spits acid at \$N.", ch, null, victim, TO_NOTVICT)
    act("\$n spits a stream of corrosive acid at you.", ch, null, victim, TO_VICT)
    act("You spit acid at \$N.", ch, null, victim, TO_CHAR)

    val hpch = Math.max(12, ch.hit)
    val hp_dam = number_range(hpch / 11 + 1, hpch / 6)
    val dice_dam = dice(level, 16)

    val dam = Math.max(hp_dam + dice_dam / 10, dice_dam + hp_dam / 10)

    if (saves_spell(level, victim, DT.Acid)) {
        acid_effect(victim, level / 2, dam / 4, TARGET_CHAR)
        damage(ch, victim, dam / 2, Skill.acid_breath, DT.Acid, true)
    } else {
        acid_effect(victim, level, dam, TARGET_CHAR)
        damage(ch, victim, dam, Skill.acid_breath, DT.Acid, true)
    }
}