package net.sf.nightworks.act.spell


import net.sf.nightworks.AFF_GROUNDING
import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.TARGET_CHAR
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_NOTVICT
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.act
import net.sf.nightworks.damage
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.send_to_char
import net.sf.nightworks.shock_effect
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.number_range

fun spell_lightning_breath(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    act("\$n breathes a bolt of lightning at \$N.", ch, null, victim, TO_NOTVICT)
    act("\$n breathes a bolt of lightning at you!", ch, null, victim, TO_VICT)
    act("You breathe a bolt of lightning at \$N.", ch, null, victim, TO_CHAR)

    val hpch = Math.max(10, ch.hit)
    val hp_dam = number_range(hpch / 9 + 1, hpch / 5)
    val dice_dam = dice(level, 20)

    val dam = Math.max(hp_dam + dice_dam / 10, dice_dam + hp_dam / 10)

    if (IS_AFFECTED(victim, AFF_GROUNDING)) {
        send_to_char("The electricity fizzles at your foes.\n", victim)
        act("A lightning bolt fizzles at \$N's foes.\n", ch, null, victim, TO_ROOM)
        return
    }

    if (saves_spell(level, victim, DT.Lightning)) {
        shock_effect(victim, level / 2, dam / 4, TARGET_CHAR)
        damage(ch, victim, dam / 2, Skill.lightning_breath, DT.Lightning, true)
    } else {
        shock_effect(victim, level, dam, TARGET_CHAR)
        damage(ch, victim, dam, Skill.lightning_breath, DT.Lightning, true)
    }
}