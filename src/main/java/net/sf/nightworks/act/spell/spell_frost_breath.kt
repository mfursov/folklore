package net.sf.nightworks.act.spell


import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.TARGET_CHAR
import net.sf.nightworks.TARGET_ROOM
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_NOTVICT
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.act
import net.sf.nightworks.cold_effect
import net.sf.nightworks.damage
import net.sf.nightworks.is_safe
import net.sf.nightworks.is_safe_spell
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.number_range

fun spell_frost_breath(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {

    act("\$n breathes out a freezing cone of frost!", ch, null, victim, TO_NOTVICT)
    act("\$n breathes a freezing cone of frost over you!", ch, null, victim, TO_VICT)
    act("You breath out a cone of frost.", ch, null, null, TO_CHAR)

    val hpch = Math.max(12, ch.hit)
    val hp_dam = number_range(hpch / 11 + 1, hpch / 6)
    val dice_dam = dice(level, 16)

    val dam = Math.max(hp_dam + dice_dam / 10, dice_dam + hp_dam / 10)
    cold_effect(victim.room, level, dam / 2, TARGET_ROOM)

    victim.room.people
            .filter { !is_safe_spell(ch, it, true) && !(it.pcdata == null && ch.pcdata == null && ch.fighting != it) && !is_safe(ch, it) }
            .forEach {
                if (it == victim) { /* full damage */
                    if (saves_spell(level, it, DT.Cold)) {
                        cold_effect(it, level / 2, dam / 4, TARGET_CHAR)
                        damage(ch, it, dam / 2, Skill.holy_word, DT.Cold, true)
                    } else {
                        cold_effect(it, level, dam, TARGET_CHAR)
                        damage(ch, it, dam, Skill.holy_word, DT.Cold, true)
                    }
                } else {
                    if (saves_spell(level - 2, it, DT.Cold)) {
                        cold_effect(it, level / 4, dam / 8, TARGET_CHAR)
                        damage(ch, it, dam / 4, Skill.holy_word, DT.Cold, true)
                    } else {
                        cold_effect(it, level / 2, dam / 4, TARGET_CHAR)
                        damage(ch, it, dam / 2, Skill.holy_word, DT.Cold, true)
                    }
                }
            }
}