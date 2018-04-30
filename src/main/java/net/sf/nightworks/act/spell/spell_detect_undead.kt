package net.sf.nightworks.act.spell

import net.sf.nightworks.AFF_DETECT_UNDEAD
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_AFFECTS
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.act
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_AFFECTED

fun spell_detect_undead(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (IS_AFFECTED(victim, AFF_DETECT_UNDEAD)) {
        if (victim == ch) {
            send_to_char("You can already sense undead.\n", ch)
        } else {
            act("\$N can already detect undead.", ch, null, victim, TO_CHAR)
        }
        return
    }
    val af = Affect()
    af.type = Skill.detect_undead
    af.level = level
    af.duration = 5 + level / 3
    af.bitvector = AFF_DETECT_UNDEAD
    affect_to_char(victim, af)
    send_to_char("Your eyes tingle.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
}