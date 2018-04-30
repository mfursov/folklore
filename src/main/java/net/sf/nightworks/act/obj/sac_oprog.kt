package net.sf.nightworks.act.obj


import net.sf.nightworks.DT
import net.sf.nightworks.DT.Holy
import net.sf.nightworks.OBJ_VNUM_BATTLE_THRONE
import net.sf.nightworks.OBJ_VNUM_CHAOS_ALTAR
import net.sf.nightworks.OBJ_VNUM_HUNTER_ALTAR
import net.sf.nightworks.OBJ_VNUM_INVADER_SKULL
import net.sf.nightworks.OBJ_VNUM_KNIGHT_ALTAR
import net.sf.nightworks.OBJ_VNUM_LIONS_ALTAR
import net.sf.nightworks.OBJ_VNUM_RULER_STAND
import net.sf.nightworks.OBJ_VNUM_SHALAFI_ALTAR
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_ALL
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.create_object
import net.sf.nightworks.damage
import net.sf.nightworks.extract_obj
import net.sf.nightworks.get_obj_index_nn
import net.sf.nightworks.get_room_index
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.OPROG_FUN_SAC
import net.sf.nightworks.obj_from_room
import net.sf.nightworks.obj_to_obj
import net.sf.nightworks.util.bug

fun create_sac_prog(name: String): OPROG_FUN_SAC = when (name) {
    "sac_prog_excalibur" -> { _, ch -> sac_prog_excalibur(ch) }
    "sac_prog_cabal_item" -> ::sac_prog_cabal_item
    else -> throw IllegalArgumentException("Prog not found: $name")
}

private fun sac_prog_excalibur(ch: CHAR_DATA): Boolean {
    act("The gods are infuriated!", ch, null, null, TO_CHAR)
    act("The gods are infuriated!", ch, null, null, TO_ROOM)
    val dam = if (ch.hit - 1 > 1000) 1000 else ch.hit - 1
    damage(ch, ch, dam, Skill.x_hit, DT.Holy, true)
    ch.gold = 0
    return true
}

private fun sac_prog_cabal_item(obj: Obj, ch: CHAR_DATA): Boolean {
    act("The gods are infuriated!", ch, null, null, TO_CHAR)
    act("The gods are infuriated!", ch, null, null, TO_ROOM)
    damage(ch, ch, ch.hit / 10, Skill.x_hit, DT.Holy, true)
    ch.gold = 0

    obj_from_room(obj)
    val c = Clan.values().firstOrNull({it.obj_ptr === obj})
    if (c ===  null) {
        extract_obj(obj)
        bug("oprog: Sac_cabal_item: Was not the cabal's item.")
        return false
    }

    val vnum = obj.pIndexData.vnum
    val containerVnum = when (vnum) {
        Clan.Ruler.obj_vnum -> OBJ_VNUM_RULER_STAND
        Clan.Invader.obj_vnum -> OBJ_VNUM_INVADER_SKULL
        Clan.BattleRager.obj_vnum -> OBJ_VNUM_BATTLE_THRONE
        Clan.Knight.obj_vnum -> OBJ_VNUM_KNIGHT_ALTAR
        Clan.Chaos.obj_vnum -> OBJ_VNUM_CHAOS_ALTAR
        Clan.Lion.obj_vnum -> OBJ_VNUM_LIONS_ALTAR
        Clan.Hunter.obj_vnum -> OBJ_VNUM_HUNTER_ALTAR
        else -> OBJ_VNUM_SHALAFI_ALTAR // TODO: unsafe
    }
    val container = create_object(get_obj_index_nn(containerVnum), 100)

    obj_to_obj(obj, container)
    val cabal_room = get_room_index(c.room_vnum)!!
    act("You see ${container.short_desc} forming again slowly.\n", cabal_room, null, null, TO_ALL)
    return true
}
