package net.sf.nightworks.act.spell

import net.sf.nightworks.AFF_CURSE
import net.sf.nightworks.AFF_ROOM_CURSE
import net.sf.nightworks.Clazz
import net.sf.nightworks.FIGHT_DELAY_TIME
import net.sf.nightworks.ROOM_NO_RECALL
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.act.info.do_look
import net.sf.nightworks.char_from_room
import net.sf.nightworks.char_to_room
import net.sf.nightworks.current_time
import net.sf.nightworks.do_visible
import net.sf.nightworks.gain_exp
import net.sf.nightworks.get_pc
import net.sf.nightworks.get_room_index
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char
import net.sf.nightworks.stop_fighting
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_RAFFECTED
import net.sf.nightworks.util.IS_SET

fun spell_word_of_recall(ch: CHAR_DATA, victim: CHAR_DATA?) {
    if (ch.clazz === Clazz.SAMURAI && ch.fighting != null && victim == null) {
        send_to_char("Your honour doesn't let you recall!.\n", ch)
        return
    }

    if (victim != null) {
        if (victim.fighting != null && victim.clazz === Clazz.SAMURAI) {
            send_to_char("You can't cast this spell to a honourable fighting Samurai!.\n", ch)
            return
        }
    }

    if (victim?.pcdata == null) return
    val recallIdx = if (victim.isGood()) 0 else if (victim.isNeutralA()) 1 else if (victim.isEvil()) 2 else 1
    val to_room_vnum = victim.hometown.recall[recallIdx]

    val location = get_room_index(to_room_vnum)
    if (location == null) {
        send_to_char("You are completely lost.\n", victim)
        return
    }
    if (get_pc(victim) != null && (current_time - victim.last_fight_time) < FIGHT_DELAY_TIME) {
        send_to_char("You are too pumped to pray now.\n", victim)
        return
    }
    if (IS_SET(victim.room.room_flags, ROOM_NO_RECALL) ||
            IS_AFFECTED(victim, AFF_CURSE) ||
            IS_RAFFECTED(victim.room, AFF_ROOM_CURSE)) {
        send_to_char("Spell failed.\n", victim)
        return
    }

    if (victim.fighting != null) {
        if (victim == ch) {
            gain_exp(victim, 0 - (victim.level + 25))
        }
        stop_fighting(victim, true)
    }

    ch.move /= 2
    act("\$n disappears.", victim, null, null, TO_ROOM)
    char_from_room(victim)
    char_to_room(victim, location)
    do_visible(ch)
    act("\$n appears in the room.", victim, null, null, TO_ROOM)
    do_look(victim, "auto")

    val v_pet = victim.pet
    if (v_pet != null) {
        char_from_room(v_pet)
        char_to_room(v_pet, location)
        do_look(v_pet, "auto")
    }
}
