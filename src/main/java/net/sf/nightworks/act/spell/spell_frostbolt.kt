package net.sf.nightworks.act.spell


import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.damage
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.util.dice

fun spell_frostbolt(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    var dam = dice(level, 10)
    if (saves_spell(level, victim, DT.Cold)) {
        dam /= 2
    }
    damage(ch, victim, dam, Skill.frostbolt,DT.Cold, true)
}