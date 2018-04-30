package net.sf.nightworks.act.spell


import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.TARGET_CHAR
import net.sf.nightworks.TARGET_ROOM
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_NOTVICT
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.act
import net.sf.nightworks.damage
import net.sf.nightworks.fire_effect
import net.sf.nightworks.is_safe
import net.sf.nightworks.is_safe_spell
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.number_range

fun spell_fire_breath(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    act("\$n breathes forth a cone of fire.", ch, null, victim, TO_NOTVICT)
    act("\$n breathes a cone of hot fire over you!", ch, null, victim, TO_VICT)
    act("You breath forth a cone of fire.", ch, null, null, TO_CHAR)

    val hpch = Math.max(10, ch.hit)
    val hp_dam = number_range(hpch / 9 + 1, hpch / 5)
    val dice_dam = dice(level, 20)

    val dam = Math.max(hp_dam + dice_dam / 10, dice_dam + hp_dam / 10)
    fire_effect(victim.room, level, dam / 2, TARGET_ROOM)

    victim.room.people
            .filter { !is_safe_spell(ch, it, true) && !(it.pcdata == null && ch.pcdata == null && ch.fighting != it) && !is_safe(ch, it) }
            .forEach {
                if (it == victim) { /* full damage */
                    if (saves_spell(level, it, DT.Fire)) {
                        fire_effect(it, level / 2, dam / 4, TARGET_CHAR)
                        damage(ch, it, dam / 2, Skill.fire_breath, DT.Fire, true)
                    } else {
                        fire_effect(it, level, dam, TARGET_CHAR)
                        damage(ch, it, dam, Skill.fire_breath, DT.Fire, true)
                    }
                } else { /* partial damage */
                    if (saves_spell(level - 2, it, DT.Fire)) {
                        fire_effect(it, level / 4, dam / 8, TARGET_CHAR)
                        damage(ch, it, dam / 4, Skill.fire_breath, DT.Fire, true)
                    } else {
                        fire_effect(it, level / 2, dam / 4, TARGET_CHAR)
                        damage(ch, it, dam / 2, Skill.fire_breath, DT.Fire, true)
                    }
                }
            }
}