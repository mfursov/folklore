package net.sf.nightworks.act.spell


import net.sf.nightworks.AFF_CHARM
import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.TARGET_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.can_see
import net.sf.nightworks.damage
import net.sf.nightworks.doppel_name
import net.sf.nightworks.gain_exp
import net.sf.nightworks.is_safe_spell
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char
import net.sf.nightworks.spell_bless
import net.sf.nightworks.spell_curse
import net.sf.nightworks.spell_frenzy
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.number_range

/** RT really nasty high-level attack spell */

fun spell_holy_word(level: Int, ch: CHAR_DATA) {

    act("\$n utters a word of divine power!", ch, null, null, TO_ROOM)
    send_to_char("You utter a word of divine power.\n", ch)

    for (vch in ch.room.people) {
        if (ch.align === vch.align) {
            send_to_char("You feel full more powerful.\n", vch)
            spell_frenzy(level, ch, vch)
            spell_bless(level, ch, vch, TARGET_CHAR)
        } else if (ch.isGood() && vch.isEvil() || ch.isEvil() && vch.isGood()) {
            if (!is_safe_spell(ch, vch, true)) {
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

                spell_curse(level, ch, vch, TARGET_CHAR)
                send_to_char("You are struck down!\n", vch)
                val dam = dice(level, 6)
                damage(ch, vch, dam, Skill.holy_word, DT.Energy, true)
            }
        } else if (ch.isNeutralA()) {
            if (!is_safe_spell(ch, vch, true)) {
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

                spell_curse(level / 2, ch, vch, TARGET_CHAR)
                send_to_char("You are struck down!\n", vch)
                val dam = dice(level, 4)
                damage(ch, vch, dam, Skill.holy_word, DT.Energy, true)
            }
        }
    }

    send_to_char("You feel drained.\n", ch)
    gain_exp(ch, -1 * number_range(1, 10) * 5)
    ch.move /= 4 / 3
    ch.hit /= 4 / 3
}