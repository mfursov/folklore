package net.sf.nightworks.util

import net.sf.nightworks.COND_DRUNK
import net.sf.nightworks.LEVEL_HERO
import net.sf.nightworks.LEVEL_IMMORTAL
import net.sf.nightworks.MOB_VNUM_ADAMANTITE_GOLEM
import net.sf.nightworks.MOB_VNUM_IRON_GOLEM
import net.sf.nightworks.MOB_VNUM_LESSER_GOLEM
import net.sf.nightworks.MOB_VNUM_STONE_GOLEM
import net.sf.nightworks.PLR_BLINK_ON
import net.sf.nightworks.PLR_HARA_KIRI
import net.sf.nightworks.PLR_LEFTHAND
import net.sf.nightworks.PLR_NO_TITLE
import net.sf.nightworks.PLR_QUESTOR
import net.sf.nightworks.PLR_VAMPIRE
import net.sf.nightworks.ROOM_INDOORS
import net.sf.nightworks.SECT_WATER_NO_SWIM
import net.sf.nightworks.SECT_WATER_SWIM
import net.sf.nightworks.Skill
import net.sf.nightworks.can_see
import net.sf.nightworks.get_trust
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.MOB_INDEX_DATA
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.ObjProto
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.ROOM_INDEX_DATA
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.model.defensive
import net.sf.nightworks.model.todam
import net.sf.nightworks.model.tohit
import java.util.Date
import java.util.Formatter


fun IS_SET(flag: Long, bit: Long) = flag and bit != 0L

fun IS_SET(flag: Int, bit: Int) = flag and bit != 0

fun SET_BIT(flag: Long, bit: Long): Long = flag or bit

fun SET_BIT(flat: Int, bit: Int) = flat or bit

fun REMOVE_BIT(flag: Long, bit: Long): Long = flag and bit.inv()

fun REMOVE_BIT(flag: Int, bit: Int) = flag and bit.inv()


/*
* room macros
*/

fun IS_ROOM_AFFECTED(room: ROOM_INDEX_DATA, sn: Long) = IS_SET(room.affected_by, sn)

fun IS_RAFFECTED(room: ROOM_INDEX_DATA, sn: Long) = IS_SET(room.affected_by, sn)

/** returns ch.mount if PC rides other chart */
fun MOUNTED(ch: CHAR_DATA): CHAR_DATA? = if (ch.pcdata != null && ch.mount != null && ch.riding) ch.mount else null

/** returns ch.mount if NPC is beeing ridden by other ch */
fun RIDDEN(ch: CHAR_DATA): CHAR_DATA? = if (ch.pcdata == null && ch.mount != null && ch.riding) ch.mount else null

fun IS_DRUNK(ch: CHAR_DATA): Boolean {
    val pcdata = ch.pcdata
    return pcdata != null && pcdata.condition[COND_DRUNK] > 10
}

fun IS_GOLEM(ch: CHAR_DATA) =
        ch.pcdata == null && (ch.pIndexData.vnum == MOB_VNUM_LESSER_GOLEM
                || ch.pIndexData.vnum == MOB_VNUM_STONE_GOLEM
                || ch.pIndexData.vnum == MOB_VNUM_IRON_GOLEM
                || ch.pIndexData.vnum == MOB_VNUM_ADAMANTITE_GOLEM)
/*
* Object macros.
*/

fun CAN_WEAR(obj: ObjProto, part: Long) = IS_SET(obj.wear_flags, part)

fun CAN_WEAR(obj: Obj, part: Long) = IS_SET(obj.wear_flags, part)

fun IS_OBJ_STAT(obj: ObjProto, stat: Long) = IS_SET(obj.extra_flags, stat)

fun IS_OBJ_STAT(obj: Obj, stat: Long) = IS_SET(obj.extra_flags, stat)

fun IS_WEAPON_STAT(obj: Obj, stat: Long) = IS_SET(obj.value[4], stat)

fun WEIGHT_MULT(obj: Obj) = if (obj.item_type === ItemType.Container) obj.value[4].toInt() else 100

/* skill defines */

fun CLEVEL_OK(ch: CHAR_DATA, skill: Skill) = ch.level >= skill.skill_level[ch.clazz.id]

fun RACE_OK(ch: CHAR_DATA, skill: Skill) = skill.race == null || skill.race == ch.race

fun CABAL_OK(ch: CHAR_DATA, skill: Skill) = skill.cabal === Clan.None || skill.cabal === ch.cabal

fun ALIGN_OK(ch: CHAR_DATA, skill: Skill) = skill.align == null || skill.align == ch.align

/*
* Utility macros.
*/

fun URANGE(a: Int, b: Int, c: Int) = if (b < a) a else if (b > c) c else b

fun URANGE(a: Long, b: Long, c: Long): Long = if (b < a) a else if (b > c) c else b

fun IS_WATER(room: ROOM_INDEX_DATA) = room.sector_type == SECT_WATER_SWIM || room.sector_type == SECT_WATER_NO_SWIM

fun IS_IMMORTAL(ch: CHAR_DATA) = get_trust(ch) >= LEVEL_IMMORTAL

fun IS_HERO(ch: CHAR_DATA) = get_trust(ch) >= LEVEL_HERO

fun IS_TRUSTED(ch: CHAR_DATA, level: Int) = get_trust(ch) >= level

fun IS_AFFECTED(ch: CHAR_DATA, sn: Long) = IS_SET(ch.affected_by, sn)

fun IS_AFFECTED(m: MOB_INDEX_DATA, sn: Long) = IS_SET(m.affected_by, sn)

fun IS_AWAKE(ch: CHAR_DATA) = ch.position > Pos.Sleeping

fun GET_AC(ch: CHAR_DATA, type: Int) = ch.armor[type] + if (IS_AWAKE(ch)) ch.defensive else 0

fun GET_HITROLL(ch: CHAR_DATA) = ch.hitroll + ch.tohit

fun GET_DAMROLL(ch: CHAR_DATA) = ch.damroll + ch.todam

fun IS_OUTSIDE(ch: CHAR_DATA) = !IS_SET(ch.room.room_flags, ROOM_INDOORS)

fun WAIT_STATE(ch: CHAR_DATA, npulse: Int) {
    ch.wait = if (IS_IMMORTAL(ch)) 1 else Math.max(ch.wait, npulse)
}

fun DAZE_STATE(ch: CHAR_DATA, npulse: Int) {
    ch.daze = Math.max(ch.daze, npulse)
}

fun get_carry_weight(ch: CHAR_DATA) = ch.carry_weight + ch.silver / 10 + ch.gold * 2 / 5

/*
* Description macros.
*/
fun PERS(ch: CHAR_DATA?, looker: CHAR_DATA): String {
    if (ch == null) {
        return "nobody"
    }
    return when {
        can_see(looker, ch) ->
            when {
                ch.pcdata == null -> ch.short_desc
                IS_VAMPIRE(ch) && !IS_IMMORTAL(looker) -> "An ugly creature"
                else -> ch.name
            }
        ch.pcdata != null && ch.level > LEVEL_HERO -> "an immortal"
        else -> "someone"
    }
}

/* new defines */
fun MAX_CHARM(ch: CHAR_DATA) = ch.curr_stat(PrimeStat.Intelligence) / 6 + ch.level / 45

fun IS_QUESTOR(ch: CHAR_DATA) = IS_SET(ch.act, PLR_QUESTOR)

fun IS_VAMPIRE(ch: CHAR_DATA) = ch.pcdata != null && IS_SET(ch.act, PLR_VAMPIRE)

fun IS_HARA_KIRI(ch: CHAR_DATA) = IS_SET(ch.act, PLR_HARA_KIRI)

fun CANT_CHANGE_TITLE(ch: CHAR_DATA) = IS_SET(ch.act, PLR_NO_TITLE)

fun IS_BLINK_ON(ch: CHAR_DATA) = IS_SET(ch.act, PLR_BLINK_ON)

fun RIGHT_HANDER(ch: CHAR_DATA) = ch.pcdata == null || !IS_SET(ch.act, PLR_LEFTHAND)

fun LEFT_HANDER(ch: CHAR_DATA) = ch.pcdata != null && IS_SET(ch.act, PLR_LEFTHAND)

/** Converts first letter to uppercase, does not affect other letters (capitalize() method does) */
fun upfirst(str: String?) = if (str == null || str.isEmpty()) "" else Character.toUpperCase(str[0]) + str.substring(1)

/** Simple linear interpolation.*/
fun interpolate(level: Int, value_00: Int, value_32: Int) = value_00 + level * (value_32 - value_00) / 32

/**  Writes a string to the log. */
fun log_string(str: CharSequence) = System.err.println(Date().toString() + "::" + str)

/** Reports a bug. */
fun bug(str: CharSequence, vararg params: Any?) {
    log_string(Formatter().format(str.toString(), *params).toString())
}