package net.sf.nightworks.act.spell


import net.sf.nightworks.ACT_UNDEAD
import net.sf.nightworks.DT
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_NOTVICT
import net.sf.nightworks.act
import net.sf.nightworks.damage
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_VAMPIRE
import net.sf.nightworks.util.dice

fun spell_hand_of_undead(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (saves_spell(level, victim, DT.Negative)) {
        send_to_char("You feel a momentary chill.\n", victim)
        return
    }

    if (victim.pcdata == null && IS_SET(victim.act, ACT_UNDEAD) || IS_VAMPIRE(victim)) {
        send_to_char("Your victim is unaffected by hand of undead.\n", ch)
        return
    }

    val dam: Int
    if (victim.level <= 2) {
        dam = ch.hit + 1
    } else {
        dam = dice(level, 10)
        victim.mana /= 2
        victim.move /= 2
        ch.hit += dam / 2
    }

    send_to_char("You feel your life slipping away!\n", victim)
    act("\$N is grasped by an incomprehensible hand of undead!", ch, null, victim, TO_NOTVICT)
    damage(ch, victim, dam, Skill.hand_of_undead, DT.Negative, true)
}