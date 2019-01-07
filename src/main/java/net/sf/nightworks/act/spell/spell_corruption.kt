package net.sf.nightworks.act.spell

import net.sf.nightworks.ACT_UNDEAD
import net.sf.nightworks.AFF_CORRUPTION
import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.affect_join
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_SET

fun spell_corruption(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_CORRUPTION)) {
        act("\$N is already corrupting.\n", ch, null, victim, TO_CHAR)
        return
    }

    if (saves_spell(level, victim, DT.Negative) || victim.pcdata == null && IS_SET(victim.act, ACT_UNDEAD)) {
        if (ch == victim) {
            send_to_char("You feel momentarily ill, but it passes.\n", ch)
        } else {
            act("\$N seems to be unaffected.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.corruption
    af.level = level * 3 / 4
    af.duration = 10 + level / 5
    af.bitvector = AFF_CORRUPTION
    affect_join(victim, af)

    send_to_char("You scream in agony as you start to decay into dust.\n", victim)
    act("\$n screams in agony as \$n start to decay into dust.", victim, null, null, TO_ROOM)
}
