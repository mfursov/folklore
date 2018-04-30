package net.sf.nightworks.act.spell


import net.sf.nightworks.AFF_CHARM
import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.can_see
import net.sf.nightworks.current_time
import net.sf.nightworks.damage
import net.sf.nightworks.doppel_name
import net.sf.nightworks.is_safe_spell
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Pos
import net.sf.nightworks.saves_spell
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.number_range

fun spell_fire_and_ice(level: Int, ch: CHAR_DATA) {
    var dam = dice(level, 8)
    val movedam = number_range(ch.level, 2 * ch.level)

    for (tmp_vict in ch.room.people) {
        if (!is_safe_spell(ch, tmp_vict, true)) {
            if (ch.pcdata != null && tmp_vict != ch &&
                    ch.fighting != tmp_vict &&
                    tmp_vict.fighting != ch &&
                    (IS_SET(tmp_vict.affected_by, AFF_CHARM) || tmp_vict.pcdata != null)) {
                if (!can_see(tmp_vict, ch)) {
                    do_yell(tmp_vict, "Help someone is attacking me!")
                } else {
                    val buf = TextBuffer()
                    buf.sprintf("Die, %s, you sorcerous dog!", doppel_name(ch, tmp_vict))
                    do_yell(tmp_vict, buf.toString())
                }
            }

            if (saves_spell(level, tmp_vict, DT.Fire)) {
                dam /= 2
            }
            var dam_sn = Skill.fireball
            damage(ch, tmp_vict, dam, dam_sn, DT.Fire, true)
            tmp_vict.move -= Math.min(tmp_vict.move, movedam)

            if (tmp_vict.pcdata == null && tmp_vict.position == Pos.Dead || tmp_vict.pcdata != null && current_time - tmp_vict.last_death_time < 10) {
                if (saves_spell(level, tmp_vict, DT.Cold)) {
                    dam /= 2
                }
                dam_sn = Skill.iceball
                damage(ch, tmp_vict, dam, dam_sn, DT.Cold, true)
            }

        }
    }
}