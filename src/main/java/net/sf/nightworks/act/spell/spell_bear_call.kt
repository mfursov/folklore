package net.sf.nightworks.act.spell

import net.sf.nightworks.AFF_CHARM
import net.sf.nightworks.Index
import net.sf.nightworks.MOB_VNUM_BEAR
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_AFFECTS
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.char_to_room
import net.sf.nightworks.clone_mobile
import net.sf.nightworks.count_charmed
import net.sf.nightworks.create_mobile
import net.sf.nightworks.get_mob_index_nn
import net.sf.nightworks.util.interpolate
import net.sf.nightworks.is_affected
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.SET_BIT

//todo: spell or skill?

@Suppress("unused")
fun spell_bear_call(sn: Skill, level: Int, ch: CHAR_DATA) {
    send_to_char("You call for bears help you.\n", ch)
    act("\$n shouts a bear call.", ch, null, null, TO_ROOM)

    if (is_affected(ch, sn)) {
        send_to_char("You cannot summon the strength to handle more bears right now.\n", ch)
        return
    }
    for (gch in Index.CHARS) {
        if (gch.pcdata == null && IS_AFFECTED(gch, AFF_CHARM) && gch.master == ch && gch.pIndexData.vnum == MOB_VNUM_BEAR) {
            send_to_char("What's wrong with the bear you've got?", ch)
            return
        }
    }

    if (count_charmed(ch) != 0) {
        return
    }

    val bear = create_mobile(get_mob_index_nn(MOB_VNUM_BEAR))
    bear.perm_stat.fill { Math.min(25, 2 * ch.perm_stat[it]) }

    val pcdata = ch.pcdata
    bear.max_hit = pcdata?.perm_hit ?: ch.max_hit
    bear.hit = bear.max_hit
    bear.max_mana = pcdata?.perm_mana ?: ch.max_mana
    bear.mana = bear.max_mana
    bear.alignment = ch.alignment
    bear.level = Math.min(70, ch.level)

    bear.armor.fill(interpolate(bear.level, 100, -100), 0, 3)
    bear.armor[3] = interpolate(bear.level, 100, 0)
    bear.sex = ch.sex
    bear.gold = 0

    val bear2 = create_mobile(bear.pIndexData)
    clone_mobile(bear, bear2)

    bear.affected_by = SET_BIT(bear.affected_by, AFF_CHARM)
    bear2.affected_by = SET_BIT(bear2.affected_by, AFF_CHARM)
    bear2.master = ch
    bear.master = bear2.master
    bear2.leader = ch
    bear.leader = bear2.leader

    char_to_room(bear, ch.room)
    char_to_room(bear2, ch.room)
    send_to_char("Two bears come to your rescue!\n", ch)
    act("Two bears come to \$n's rescue!", ch, null, null, TO_ROOM)

    val af = Affect()
    af.type = sn
    af.level = level
    af.duration = 24
    affect_to_char(ch, af)
}
