package net.sf.nightworks.act.spell


import net.sf.nightworks.DT
import net.sf.nightworks.IMM_SUMMON
import net.sf.nightworks.ROOM_NO_RECALL
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.act.info.do_look
import net.sf.nightworks.char_from_room
import net.sf.nightworks.char_to_room
import net.sf.nightworks.get_random_room
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.ROOM_INDEX_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_SET

fun spell_teleport(level: Int, ch: CHAR_DATA, vo: CHAR_DATA) {
    var victim = vo
    val pRoomIndex: ROOM_INDEX_DATA

    if (ch.pcdata != null) {
        victim = ch
    }

    if (IS_SET(victim.room.room_flags, ROOM_NO_RECALL)
            || victim != ch && IS_SET(victim.imm_flags, IMM_SUMMON)
            || ch.pcdata != null && victim.fighting != null
            || victim != ch && saves_spell(level - 5, victim, DT.Other)) {
        send_to_char("You failed.\n", ch)
        return
    }

    pRoomIndex = get_random_room(victim)

    if (victim != ch) {
        send_to_char("You have been teleported!\n", victim)
    }

    act("\$n vanishes!", victim, null, null, TO_ROOM)
    char_from_room(victim)
    char_to_room(victim, pRoomIndex)
    act("\$n slowly fades into existence.", victim, null, null, TO_ROOM)
    do_look(victim, "auto")
}