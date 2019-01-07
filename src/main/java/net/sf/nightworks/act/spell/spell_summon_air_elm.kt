package net.sf.nightworks.act.spell

import net.sf.nightworks.AFF_CHARM
import net.sf.nightworks.DICE_BONUS
import net.sf.nightworks.DICE_NUMBER
import net.sf.nightworks.DICE_TYPE
import net.sf.nightworks.Index.CHARS
import net.sf.nightworks.MOB_VNUM_ELM_AIR
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.char_to_room
import net.sf.nightworks.count_charmed
import net.sf.nightworks.create_mobile
import net.sf.nightworks.get_mob_index_nn
import net.sf.nightworks.is_affected
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.URANGE
import net.sf.nightworks.util.interpolate

fun spell_summon_air_elm(level: Int, ch: CHAR_DATA) {
    if (is_affected(ch, Skill.summon_air_elemental)) {
        send_to_char("You lack the power to create another elemental right now.\n", ch)
        return
    }

    send_to_char("You attempt to create an air elemental.\n", ch)
    act("\$n attempts to create an air elemental.", ch, null, null, TO_ROOM)

    var i = 0
    for (gch in CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_ELM_AIR) {
            i++
            if (i > 2) {
                send_to_char("More air elementals are more than you can control!\n", ch)
                return
            }
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val elm = create_mobile(get_mob_index_nn(MOB_VNUM_ELM_AIR))
    elm.perm_stat.fill { _ -> Math.min(25, 15 + ch.level / 10) }

    elm.perm_stat[PrimeStat.Strength] += 3
    elm.perm_stat[PrimeStat.Intelligence] -= 1
    elm.perm_stat[PrimeStat.Constitution] += 2

    val pcdata = ch.pcdata
    elm.max_hit = if (pcdata == null)
        URANGE(ch.max_hit, ch.max_hit, 30000)
    else
        Math.min(4 * pcdata.perm_hit + 1000, 30000)
    elm.hit = elm.max_hit
    elm.max_mana = pcdata?.perm_mana ?: ch.max_mana
    elm.mana = elm.max_mana
    elm.level = ch.level

    elm.armor.fill(interpolate(elm.level, 100, -100), 0, 3)
    elm.armor[3] = interpolate(elm.level, 100, 0)
    elm.gold = 0
    elm.timer = 0
    elm.damage[DICE_NUMBER] = 7
    elm.damage[DICE_TYPE] = 4
    elm.damage[DICE_BONUS] = ch.level / 2

    char_to_room(elm, ch.room)
    send_to_char("You created an air elemental!\n", ch)
    act("\$n creates an air elemental!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = Skill.summon_air_elemental
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)

    elm.affected_by = SET_BIT(elm.affected_by, AFF_CHARM)
    elm.leader = ch
    elm.master = elm.leader
}
