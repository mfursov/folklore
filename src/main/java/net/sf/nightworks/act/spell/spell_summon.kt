package net.sf.nightworks.act.spell


import net.sf.nightworks.ACT_AGGRESSIVE
import net.sf.nightworks.DT
import net.sf.nightworks.IMM_SUMMON
import net.sf.nightworks.LEVEL_IMMORTAL
import net.sf.nightworks.PLR_NOSUMMON
import net.sf.nightworks.ROOM_NOSUMMON
import net.sf.nightworks.ROOM_PRIVATE
import net.sf.nightworks.ROOM_SAFE
import net.sf.nightworks.ROOM_SOLITARY
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.act
import net.sf.nightworks.act.info.do_look
import net.sf.nightworks.char_from_room
import net.sf.nightworks.char_to_room
import net.sf.nightworks.get_char_world
import net.sf.nightworks.is_safe_nomessage
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.send_to_char
import net.sf.nightworks.target_name
import net.sf.nightworks.util.IS_SET

fun spell_summon(level: Int, ch: CHAR_DATA) {
    val victim = get_char_world(ch, target_name)
    if (victim == null
            || victim == ch
            || IS_SET(ch.room.room_flags, ROOM_SAFE)
            || IS_SET(victim.room.room_flags, ROOM_SAFE)
            || IS_SET(victim.room.room_flags, ROOM_PRIVATE)
            || IS_SET(victim.room.room_flags, ROOM_SOLITARY)
            || IS_SET(ch.room.room_flags, ROOM_NOSUMMON)
            || IS_SET(victim.room.room_flags, ROOM_NOSUMMON)
            || victim.pcdata == null && IS_SET(victim.act, ACT_AGGRESSIVE)
            || victim.level >= level + 3
            || victim.pcdata != null && victim.level >= LEVEL_IMMORTAL
            || victim.fighting != null
            || victim.pcdata == null && IS_SET(victim.imm_flags, IMM_SUMMON)
            || victim.pcdata == null && victim.pIndexData.pShop != null
            || victim.pcdata != null && is_safe_nomessage(ch, victim) && IS_SET(victim.act, PLR_NOSUMMON)
            || saves_spell(level, victim, DT.Other)
            || ch.room.area != victim.room.area && victim.pcdata != null
            || victim.room.exit[0] == null &&
            victim.room.exit[1] == null &&
            victim.room.exit[2] == null &&
            victim.room.exit[3] == null &&
            victim.room.exit[4] == null && victim.room.exit[5] == null) {
        send_to_char("You failed.\n", ch)
        return
    }

    if (victim.pcdata == null && victim.in_mind.isEmpty()) {
        victim.in_mind = victim.room.vnum.toString()
    }

    act("\$n disappears suddenly.", victim, null, null, TO_ROOM)
    char_from_room(victim)
    char_to_room(victim, ch.room)
    act("\$n arrives suddenly.", victim, null, null, TO_ROOM)
    act("\$n has summoned you!", ch, null, victim, TO_VICT)
    do_look(victim, "auto")
}