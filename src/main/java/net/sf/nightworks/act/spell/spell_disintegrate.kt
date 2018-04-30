package net.sf.nightworks.act.spell

import net.sf.nightworks.COND_BLOODLUST
import net.sf.nightworks.COND_DESIRE
import net.sf.nightworks.COND_FULL
import net.sf.nightworks.COND_HUNGER
import net.sf.nightworks.COND_THIRST
import net.sf.nightworks.DT
import net.sf.nightworks.Index
import net.sf.nightworks.MAX_LEVEL
import net.sf.nightworks.PLR_BOUGHT_PET
import net.sf.nightworks.PLR_WANTED
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_NOTVICT
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.affect_remove
import net.sf.nightworks.current_time
import net.sf.nightworks.damage
import net.sf.nightworks.equip_char
import net.sf.nightworks.extract_char
import net.sf.nightworks.extract_obj
import net.sf.nightworks.get_eq_char
import net.sf.nightworks.kill_table
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.Wear
import net.sf.nightworks.obj_from_char
import net.sf.nightworks.obj_to_char
import net.sf.nightworks.saves_spell
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.URANGE
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.flipCoin

fun spell_disintegrate(level: Int, ch: CHAR_DATA, victim: CHAR_DATA) {
    if (saves_spell(level, victim, DT.Mental) || flipCoin()) {
        val dam = dice(level, 24)
        damage(ch, victim, dam, Skill.disintegrate, DT.Mental, true)
        return
    }

    act("\$N's thin light ray {r###DISINTEGRATES###{x you!", victim, null, ch, TO_CHAR, Pos.Resting)
    act("\$n's thin light ray {r###DISINTEGRATES###{x \$N!", ch, null, victim, TO_NOTVICT, Pos.Resting)
    act("Your thin light ray {r###DISINTEGRATES###{x \$N!", ch, null, victim, TO_CHAR, Pos.Resting)
    send_to_char("You have been KILLED!\n", victim)

    act("\$N does not exist anymore!\n", ch, null, victim, TO_CHAR)
    act("\$N does not exist anymore!\n", ch, null, victim, TO_ROOM)

    send_to_char("You turn into an invincible ghost for a few minutes.\n", victim)
    send_to_char("As long as you don't attack anything.\n", victim)

    /*  disintegrate the objects... */
    val tattoo = get_eq_char(victim, Wear.Tattoo) /* keep tattoos for later */
    if (tattoo != null) obj_from_char(tattoo)

    victim.gold = 0
    victim.silver = 0

    victim.carrying.forEach { extract_obj(it) }

    val pcdata = victim.pcdata
    if (pcdata == null) {
        victim.pIndexData.killed++
        kill_table[URANGE(0, victim.level, MAX_LEVEL - 1)].killed++
        extract_char(victim, true)
        return
    }

    extract_char(victim, false)

    while (victim.affected != null) {
        affect_remove(victim, victim.affected!!)
    }
    victim.affected_by = 0
    victim.armor.fill(100)
    victim.position = Pos.Resting
    victim.hit = 1
    victim.mana = 1

    victim.act = REMOVE_BIT(victim.act, PLR_WANTED)
    victim.act = REMOVE_BIT(victim.act, PLR_BOUGHT_PET)

    pcdata.condition[COND_THIRST] = 40
    pcdata.condition[COND_HUNGER] = 40
    pcdata.condition[COND_FULL] = 40
    pcdata.condition[COND_BLOODLUST] = 40
    pcdata.condition[COND_DESIRE] = 40

    victim.last_death_time = current_time

    if (tattoo != null) {
        obj_to_char(tattoo, victim)
        equip_char(victim, tattoo, Wear.Tattoo)
    }

    Index.CHARS
            .filter { it.last_fought == victim }
            .forEach { it.last_fought = null }

}