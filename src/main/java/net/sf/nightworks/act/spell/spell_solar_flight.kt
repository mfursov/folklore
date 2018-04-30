package net.sf.nightworks.act.spell


import net.sf.nightworks.DT
import net.sf.nightworks.IMM_SUMMON
import net.sf.nightworks.PLR_NOSUMMON
import net.sf.nightworks.ROOM_NOSUMMON
import net.sf.nightworks.ROOM_PRIVATE
import net.sf.nightworks.ROOM_SAFE
import net.sf.nightworks.ROOM_SOLITARY
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.act.info.do_look
import net.sf.nightworks.can_see_room
import net.sf.nightworks.char_from_room
import net.sf.nightworks.char_to_room
import net.sf.nightworks.get_char_world
import net.sf.nightworks.is_safe_nomessage
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.send_to_char
import net.sf.nightworks.target_name
import net.sf.nightworks.time_info
import net.sf.nightworks.util.IS_SET

/*  Cleric version of astra_walk  */

fun spell_solar_flight(level: Int, ch: CHAR_DATA) {
    if (time_info.hour > 18 || time_info.hour < 8) {
        send_to_char("You need sunlight for solar flight.\n", ch)
        return
    }

    val victim = get_char_world(ch, target_name)
    if (victim == null
            || victim == ch
            || !can_see_room(ch, victim.room)
            || IS_SET(victim.room.room_flags, ROOM_SAFE)
            || IS_SET(victim.room.room_flags, ROOM_PRIVATE)
            || IS_SET(victim.room.room_flags, ROOM_SOLITARY)
            || IS_SET(ch.room.room_flags, ROOM_NOSUMMON)
            || IS_SET(victim.room.room_flags, ROOM_NOSUMMON)
            || victim.level >= level + 1
            /*    ||   (!IS_NPC(victim) && victim.level >= LEVEL_HERO)  * NOT trust */
            || saves_spell(level, victim, DT.Other)
            || victim.pcdata == null && is_safe_nomessage(ch, victim) && IS_SET(victim.imm_flags, IMM_SUMMON)
            || victim.pcdata != null && is_safe_nomessage(ch, victim) && IS_SET(victim.act, PLR_NOSUMMON)
            || victim.pcdata != null && ch.room.area != victim.room.area
            || victim.pcdata == null && saves_spell(level, victim, DT.Other)) {
        send_to_char("You failed.\n", ch)
        return
    }

    act("\$n disappears in a blinding flash of light!", ch, null, null, TO_ROOM)
    send_to_char("You dissolve in a blinding flash of light!.\n", ch)

    char_from_room(ch)
    char_to_room(ch, victim.room)

    act("\$n appears in a blinding flash of light!", ch, null, null, TO_ROOM)
    do_look(ch, "auto")

}