package net.sf.nightworks.act.spell


import net.sf.nightworks.AFF_FLYING
import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.act
import net.sf.nightworks.damage
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.dice

fun spell_earthmaw(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    val buf = TextBuffer()
    buf.sprintf("You tremble the earth underneath the %s.\n", victim.name)
    send_to_char(buf, ch)

    act("\$n trembles the earth underneath you!.", ch, null, victim, TO_VICT)
    val dam = if (IS_AFFECTED(victim, AFF_FLYING)) 0 else dice(level, 16)
    damage(ch, victim, dam, Skill.earthmaw, DT.Bash, true)
}