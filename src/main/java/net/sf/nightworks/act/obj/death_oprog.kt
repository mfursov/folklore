package net.sf.nightworks.act.obj


import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.affect_remove
import net.sf.nightworks.cabal_area_check
import net.sf.nightworks.char_from_room
import net.sf.nightworks.char_to_room
import net.sf.nightworks.current_time
import net.sf.nightworks.extract_obj
import net.sf.nightworks.get_room_index
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.OPROG_FUN_DEATH
import net.sf.nightworks.model.Pos
import net.sf.nightworks.send_to_char

fun create_death_prog(name: String): OPROG_FUN_DEATH = when (name) {
    "death_prog_excalibur" -> ::death_prog_excalibur
    "death_prog_ranger_staff" -> ::death_prog_ranger_staff
    "death_prog_chaos_blade" -> ::death_prog_chaos_blade
    "death_prog_golden_weapon" -> ::death_prog_golden_weapon
    else -> throw IllegalArgumentException("Prog not found: $name")
}

private fun death_prog_excalibur(obj: Obj, ch: CHAR_DATA): Boolean {
    act("\$p starts to glow with a blue aura.", ch, obj, null, TO_CHAR, Pos.Dead)
    act("\$p starts to glow with a blue aura,", ch, obj, null, TO_ROOM)
    ch.hit = ch.max_hit
    send_to_char("You feel much better.", ch)
    act("\$n looks much better.", ch, null, null, TO_ROOM)
    return true
}

private fun death_prog_ranger_staff(obj: Obj, ch: CHAR_DATA): Boolean {
    send_to_char("Your ranger's staff disappears.\n", ch)
    act("\$n's ranger's staff disappears.", ch, null, null, TO_ROOM)
    extract_obj(obj)
    return false
}

private fun death_prog_chaos_blade(obj: Obj, ch: CHAR_DATA): Boolean {
    send_to_char("Your chaotic blade disappears.\n", ch)
    act("\$n's chaotic blade disappears.", ch, null, null, TO_ROOM)
    extract_obj(obj)
    return false
}

private fun death_prog_golden_weapon(obj: Obj, ch: CHAR_DATA): Boolean {
    send_to_char("Your golden weapon disappears.\n", ch)
    act("\$n's golden weapon disappears.", ch, null, null, TO_ROOM)
    extract_obj(obj)
    ch.hit = 1
    while (ch.affected != null) {
        affect_remove(ch, ch.affected!!)
    }
    ch.last_fight_time = -1
    ch.last_death_time = current_time
    if (cabal_area_check(ch)) {
        act("\$n disappears.", ch, null, null, TO_ROOM)
        char_from_room(ch)
        char_to_room(ch, get_room_index(Clan.Knight.room_vnum))
        act("\$n appears in the room.", ch, null, null, TO_ROOM)
    }
    return true
}

