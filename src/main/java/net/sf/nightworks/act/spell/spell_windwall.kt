package net.sf.nightworks.act.spell

import net.sf.nightworks.AFF_CHARM
import net.sf.nightworks.AFF_FLYING
import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.can_see
import net.sf.nightworks.damage
import net.sf.nightworks.doppel_name
import net.sf.nightworks.is_safe
import net.sf.nightworks.is_safe_spell
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.number_range

fun spell_windwall(level: Int, ch: CHAR_DATA) {
    act("\$n raise a wall of wind striking everyone.", ch, null, null, TO_ROOM)
    act("You raise a wall of wind.", ch, null, null, TO_CHAR)

    val hpch = Math.max(16, ch.hit)
    val hp_dam = number_range(hpch / 15 + 1, 8)
    val dice_dam = dice(level, 12)

    var dam = Math.max(hp_dam + dice_dam / 10, dice_dam + hp_dam / 10)

    for (vch in ch.room.people) {
        if (is_safe_spell(ch, vch, true) || ch.pcdata == null && vch.pcdata == null && (ch.fighting == vch || vch.fighting == ch)) {
            continue
        }
        if (is_safe(ch, vch)) {
            continue
        }
        if (ch.pcdata != null && vch != ch &&
                ch.fighting != vch && vch.fighting != ch &&
                (IS_SET(vch.affected_by, AFF_CHARM) || vch.pcdata != null)) {
            if (!can_see(vch, ch)) {
                do_yell(vch, "Help someone is attacking me!")
            } else {
                val buf = TextBuffer()
                buf.sprintf("Die, %s, you sorcerous dog!", doppel_name(ch, vch))
                do_yell(vch, buf.toString())
            }
        }

        if (!IS_AFFECTED(vch, AFF_FLYING)) {
            dam /= 2
        }

        dam = (dam * vch.size.victimDamageModifier()).toInt()

        if (saves_spell(level, vch, DT.Other)) {
            damage(ch, vch, dam / 2, Skill.windwall, DT.Other, true)
        } else {
            damage(ch, vch, dam, Skill.windwall, DT.Other, true)
        }
    }
}

