@file:Suppress("unused")

package net.sf.nightworks

import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.ObjProto
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.ROOM_HISTORY_DATA
import net.sf.nightworks.model.ROOM_INDEX_DATA
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.carry
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.util.ALIGN_OK
import net.sf.nightworks.util.CABAL_OK
import net.sf.nightworks.util.CAN_WEAR
import net.sf.nightworks.util.CLEVEL_OK
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_OBJ_STAT
import net.sf.nightworks.util.IS_ROOM_AFFECTED
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_VAMPIRE
import net.sf.nightworks.util.IS_WATER
import net.sf.nightworks.util.LEFT_HANDER
import net.sf.nightworks.util.MAX_CHARM
import net.sf.nightworks.util.MOUNTED
import net.sf.nightworks.util.RACE_OK
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.RIDDEN
import net.sf.nightworks.util.RIGHT_HANDER
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.URANGE
import net.sf.nightworks.util.WEIGHT_MULT
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.currentTimeSeconds
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.is_number
import net.sf.nightworks.util.number_fuzzy
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.oneChanceOf
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.util.startsWith
import java.util.Formatter

/* friend stuff -- for NPC's mostly */

fun is_friend(ch: CHAR_DATA, victim: CHAR_DATA): Boolean {
    if (is_same_group(ch, victim)) {
        return true
    }

    if (ch.pcdata != null) {
        return false
    }

    if (victim.pcdata != null) {
        return IS_SET(ch.off_flags, ASSIST_PLAYERS)
    }

    if (IS_AFFECTED(ch, AFF_CHARM)) {
        return false
    }

    if (IS_SET(ch.off_flags, ASSIST_ALL)) {
        return true
    }

    if (ch.group != 0 && ch.group == victim.group) {
        return true
    }

    if (IS_SET(ch.off_flags, ASSIST_VNUM) && ch.pIndexData == victim.pIndexData) {
        return true
    }


    return if (IS_SET(ch.off_flags, ASSIST_RACE) && ch.race === victim.race) {
        true
    } else IS_SET(ch.off_flags, ASSIST_ALIGN)
            && !IS_SET(ch.act, ACT_NOALIGN) && !IS_SET(victim.act, ACT_NOALIGN)
            && (ch.align === victim.align)
}

/**
 * Room record:
 * For less than 5 people in room create a new record.
 * Else use the oldest one.
 */
fun room_record(name: String, room: ROOM_INDEX_DATA, door: Int) {
    var rh: ROOM_HISTORY_DATA?
    var i = 0

    rh = room.history
    while (i < 5 && rh != null) {
        i++
        rh = rh.next
    }

    //todo: use List or Stack for history
    if (i < 5) {
        rh = ROOM_HISTORY_DATA()
    } else {
        rh = room.history!!.next!!.next!!.next!!.next
        room.history!!.next!!.next!!.next!!.next = null
    }

    rh!!.next = room.history
    room.history = rh
    rh.name = name
    rh.went = door
}

/* returns number of people on an object */

fun count_users(obj: Obj): Int {
    val in_room = obj.in_room ?: return 0
    return in_room.people.count { it.on == obj }
}


fun cabal_ok(ch: CHAR_DATA, sn: Skill): Boolean {
    val obj_ptr = ch.cabal.obj_ptr
    if (ch.pcdata == null || sn.cabal == Clan.None ||
            obj_ptr == null ||
            obj_ptr.in_room == null ||
            obj_ptr.in_room!!.vnum == ch.cabal.room_vnum) {
        return true
    }

    for (c in Clan.values()) {
        if (obj_ptr.in_room!!.vnum == c.room_vnum) {
            send_to_char("You cannot find the Cabal Power within you.\n", ch)
            return false
        }
    }

    return true
}

/** Check the material */
fun check_material(obj: Obj, material: String) = obj.material.contains(material)

fun is_metal(obj: Obj) = check_material(obj, "silver") ||
        check_material(obj, "gold") ||
        check_material(obj, "iron") ||
        check_material(obj, "mithril") ||
        check_material(obj, "adamantite") ||
        check_material(obj, "steel") ||
        check_material(obj, "lead") ||
        check_material(obj, "bronze") ||
        check_material(obj, "copper") ||
        check_material(obj, "brass") ||
        check_material(obj, "platinium") ||
        check_material(obj, "titanium") ||
        check_material(obj, "aliminum")

fun may_float(obj: Obj) = check_material(obj, "wood") ||
        check_material(obj, "ebony") ||
        check_material(obj, "ice") ||
        check_material(obj, "energy") ||
        check_material(obj, "hardwood") ||
        check_material(obj, "softwood") ||
        check_material(obj, "flesh") ||
        check_material(obj, "silk") ||
        check_material(obj, "wool") ||
        check_material(obj, "cloth") ||
        check_material(obj, "fur") ||
        check_material(obj, "water") ||
        check_material(obj, "ice") ||
        check_material(obj, "oak") || obj.item_type === ItemType.Boat


fun cant_float(obj: Obj) = check_material(obj, "steel") ||
        check_material(obj, "iron") ||
        check_material(obj, "brass") ||
        check_material(obj, "silver") ||
        check_material(obj, "gold") ||
        check_material(obj, "ivory") ||
        check_material(obj, "copper") ||
        check_material(obj, "diamond") ||
        check_material(obj, "pearl") ||
        check_material(obj, "gem") ||
        check_material(obj, "platinium") ||
        check_material(obj, "ruby") ||
        check_material(obj, "bronze") ||
        check_material(obj, "titanium") ||
        check_material(obj, "mithril") ||
        check_material(obj, "obsidian") ||
        check_material(obj, "lead")

fun floating_time(obj: Obj): Int {
    var ftime = when (obj.item_type) {
        ItemType.Key -> 1
        ItemType.Armor -> 2
        ItemType.Treasure -> 2
        ItemType.Pill -> 2
        ItemType.Potion -> 3
        ItemType.Trash -> 3
        ItemType.Food -> 4
        ItemType.Container -> 5
        ItemType.CorpseNpc -> 10
        ItemType.CorpsePc -> 10
        else -> 0
    }
    ftime = number_fuzzy(ftime)

    return if (ftime < 0) 0 else ftime
}

/**
 * for immunity, vulnerabiltiy, and resistant
 * the 'globals' (magic and weapons) may be overriden
 * three other cases -- wood, silver, and iron -- are checked in fight.c
 */
fun check_immune(ch: CHAR_DATA, dam_type: DT): Int {

    var immune = -1
    var def = IS_NORMAL

    if (dam_type == DT.None) {
        return immune
    }

    if (dam_type.isWeapon()) {
        when {
            IS_SET(ch.imm_flags, IMM_WEAPON) -> def = IS_IMMUNE
            IS_SET(ch.res_flags, RES_WEAPON) -> def = IS_RESISTANT
            IS_SET(ch.vuln_flags, VULN_WEAPON) -> def = IS_VULNERABLE
        }
    } else { /* magical attack */
        when {
            IS_SET(ch.imm_flags, IMM_MAGIC) -> def = IS_IMMUNE
            IS_SET(ch.res_flags, RES_MAGIC) -> def = IS_RESISTANT
            IS_SET(ch.vuln_flags, VULN_MAGIC) -> def = IS_VULNERABLE
        }
    }

    /* set bits to check -- VULN etc. must ALL be the same or this will fail */
    val bit = dam_type.immuneFlag
    if (bit == 0L) return def

    when {
        IS_SET(ch.imm_flags, bit) -> immune = IS_IMMUNE
        IS_SET(ch.res_flags, bit) -> immune = IS_RESISTANT
    }
    if (IS_SET(ch.vuln_flags, bit)) {
        immune = when (immune) {
            IS_IMMUNE -> IS_RESISTANT
            IS_RESISTANT -> IS_NORMAL
            else -> IS_VULNERABLE
        }
    }

    if (ch.pcdata != null && ch.curr_stat(PrimeStat.Charisma) < 18 && dam_type == DT.Charm) {
        immune = IS_VULNERABLE
    }

    return if (immune == -1) def else immune
}

/** for returning skill information */
fun get_skill(ch: CHAR_DATA?, sn: Skill?): Int {
    if (ch == null) {
        return 0
    }
    var skill: Int

    val pcdata = ch.pcdata
    when {
        sn == null -> skill = ch.level * 5 / 2 /* shorthand for level based skills */
        pcdata != null -> skill = if (ch.level < sn.skill_level[ch.clazz.id]) 0 else pcdata.learned[sn.ordinal]
        else -> /* mobiles */
            when {
                sn.isSpell -> skill = 40 + 2 * ch.level
                sn == Skill.sneak || sn == Skill.hide -> skill = ch.level + 20
                sn == Skill.dodge && IS_SET(ch.off_flags, OFF_DODGE) || sn == Skill.parry && IS_SET(ch.off_flags, OFF_PARRY) -> skill = ch.level * 2
                sn == Skill.shield_block -> skill = 10 + 2 * ch.level
                sn == Skill.second_attack -> skill = 30 + ch.level
                sn == Skill.third_attack && (IS_SET(ch.act, ACT_WARRIOR) || IS_SET(ch.act, ACT_THIEF)) -> skill = 30 + ch.level / 2
                sn == Skill.fourth_attack && IS_SET(ch.act, ACT_WARRIOR) -> skill = 20 + ch.level / 2
                sn == Skill.second_weapon && IS_SET(ch.act, ACT_WARRIOR) -> skill = 30 + ch.level / 2
                sn == Skill.hand_to_hand -> skill = 40 + 2 * ch.level
                sn == Skill.trip && IS_SET(ch.off_flags, OFF_TRIP) -> skill = 10 + 3 * ch.level
                sn == Skill.bash && IS_SET(ch.off_flags, OFF_BASH) -> skill = 10 + 3 * ch.level
                sn == Skill.disarm && (IS_SET(ch.off_flags, OFF_DISARM)
                        || IS_SET(ch.act, ACT_WARRIOR)
                        || IS_SET(ch.act, ACT_THIEF)) -> skill = 20 + 3 * ch.level
                sn == Skill.grip && (IS_SET(ch.act, ACT_WARRIOR) || IS_SET(ch.act, ACT_THIEF)) -> skill = ch.level
                sn == Skill.berserk && IS_SET(ch.off_flags, OFF_BERSERK) -> skill = 3 * ch.level
                sn == Skill.kick -> skill = 10 + 3 * ch.level
                sn == Skill.backstab && IS_SET(ch.act, ACT_THIEF) -> skill = 20 + 2 * ch.level
                sn == Skill.rescue -> skill = 40 + ch.level
                sn == Skill.recall -> skill = 40 + ch.level
                sn == Skill.sword || sn == Skill.dagger || sn == Skill.spear || sn == Skill.mace ||
                        sn == Skill.axe || sn == Skill.flail || sn == Skill.whip || sn == Skill.polearm || sn == Skill.bow || sn == Skill.arrow || sn == Skill.lance -> skill = 40 + 5 * ch.level / 2
                else -> skill = 0
            }
    }

    if (ch.daze > 0) {
        if (sn != null && sn.isSpell) {
            skill /= 2
        } else {
            skill = 2 * skill / 3
        }
    }

    if (pcdata != null && pcdata.condition[COND_DRUNK] > 10) {
        skill = 9 * skill / 10
    }

    if (ch.hit < ch.max_hit * 0.6) {
        skill = 9 * skill / 10
    }

    skill = URANGE(0, skill, 100)

    if (skill != 0 && pcdata != null) {
        if (sn != null && !sn.isSpell) {
            skill += sn.mod[ch.clazz.id]
        }
    }
    return skill

}

/* for returning weapon information */

fun get_weapon_sn(ch: CHAR_DATA, second: Boolean): Skill? {
    val wield = get_wield_char(ch, second)
    return if (wield == null || wield.item_type != ItemType.Weapon) Skill.hand_to_hand else wield.weaponType.skill
}

fun get_weapon_skill(ch: CHAR_DATA, sn: Skill?): Int {
    /* -1 is exotic */
    val pcdata = ch.pcdata
    var skill = if (pcdata == null) when (sn) {
        null -> 3 * ch.level
        Skill.hand_to_hand -> 40 + 2 * ch.level
        else -> 40 + 5 * ch.level / 2
    } else when (sn) {
        null -> Math.min(3 * ch.level, 100)
        else -> pcdata.learned[sn.ordinal]
    }

    if (ch.hit < ch.max_hit * 0.6) {
        skill = 9 * skill / 10
    }

    return URANGE(0, skill, 100)
}

/* used to de-screw characters */

fun reset_char(ch: CHAR_DATA) {
    var af: Affect?

    val pcdata = ch.pcdata ?: return

    ch.sex = pcdata.true_sex
    ch.max_hit = pcdata.perm_hit
    ch.max_mana = pcdata.perm_mana
    ch.max_move = pcdata.perm_move
    /*
    ch.hit     = ch.max_hit;
    ch.mana        = ch.max_mana;
    ch.move        = ch.max_move;
*/

    /* a little hack */

    ch.extracted = true
    /* now add back spell effects */
    af = ch.affected
    while (af != null) {
        affect_modify(ch, af, true)
        af = af.next
    }

    /* now start adding back the effects */
    for (obj in ch.carrying) {
        if (obj.wear_loc.isOn()) {
            obj.wear_loc = Wear.None
            equip_char(ch, obj, obj.wear_loc)
        }
    }
    ch.extracted = false
}

/** Retrieve a character's trusted level for permission checking. */
fun get_trust(chArg: CHAR_DATA): Int {
    var ch = chArg
    val pc = get_pc(ch)
    if (pc != null) {
        ch = pc.o_ch
    }

    if (ch.trust != 0 && IS_SET(ch.comm, COMM_true_TRUST)) {
        return ch.trust
    }

    return if (ch.pcdata == null && ch.level >= LEVEL_HERO) {
        LEVEL_HERO - 1
    } else {
        ch.level
    }
}

/*
* Retrieve a character's age.
*/

fun get_age(ch: CHAR_DATA) = 17 + (ch.played + (current_time - ch.logon).toInt()) / 72000

fun age_to_num(age: Int) = age * 72000


/* Retrieves a character's carry capacity in items count.*/
fun can_carry_items(ch: CHAR_DATA) = when {
    ch.pcdata != null && ch.level >= LEVEL_IMMORTAL -> 1000
    ch.pcdata != null || !IS_SET(ch.act, ACT_PET) -> 7 + ch.curr_stat(PrimeStat.Dexterity) + ch.size.ordinal
    else -> 0
}

/* Retrieves a character's carry capacity in items weight. */
fun can_carry_weight(ch: CHAR_DATA) = when {
    ch.pcdata != null && ch.level >= LEVEL_IMMORTAL -> 10000000
    ch.pcdata != null || !IS_SET(ch.act, ACT_PET) -> ch.carry * 10 + ch.level * 25
    else -> 0
}

/**
 * See if a string is one of the names of an object.
 */
fun is_name(strArg: String, nameList: String): Boolean {
    var str = strArg
    val wholeString = str
    val part = StringBuilder()
    /* we need ALL parts of wholeString to match part of namelist */
    while (true)
    /* start parsing wholeString */ {
        part.setLength(0)
        str = one_argument(str, part)

        if (part.isEmpty()) {
            return false
        }

        /* check to see if this is part of namelist */
        val subStr = part.toString()

        var list = nameList
        while (true) {
            part.setLength(0)
            list = one_argument(list, part)
            if (part.isEmpty()) {
                return false /* this name was not found */
            }

            val nameStr = part.toString()
            if (startsWith(wholeString, nameStr)) {
                return true /* full pattern match */
            }

            if (startsWith(subStr, nameStr)) {
                break
            }
        }
    }
}

/* enchanted stuff for eq */

fun affect_enchant(obj: Obj) {
    /* okay, move all the old flags into new vectors if we have to */
    if (obj.enchanted) return
    obj.enchanted = true
    var paf = obj.pIndexData.affected
    while (paf != null) {
        val af_new = Affect()

        af_new.next = obj.affected
        obj.affected = af_new

        af_new.where = paf.where
        af_new.type = paf.type
        af_new.level = paf.level
        af_new.duration = paf.duration
        af_new.location = paf.location
        af_new.modifier = paf.modifier
        af_new.bitvector = paf.bitvector
        paf = paf.next
    }
}

private var depth: Int = 0
/*
 * Apply or remove an affect to a character.
 */

fun affect_modify(ch: CHAR_DATA?, paf: Affect, fAdd: Boolean) {
    if (ch == null) {
        return
    }
    var mod = paf.modifier
    if (fAdd) {
        when (paf.where) {
            TO_AFFECTS -> {
                ch.affected_by = SET_BIT(ch.affected_by, paf.bitvector)
                if (IS_SET(paf.bitvector, AFF_FLYING) && ch.pcdata != null) {
                    ch.act = REMOVE_BIT(ch.act, PLR_CHANGED_AFF)
                }
            }
            TO_IMMUNE -> ch.imm_flags = SET_BIT(ch.imm_flags, paf.bitvector)
            TO_RESIST -> ch.res_flags = SET_BIT(ch.res_flags, paf.bitvector)
            TO_ACT_FLAG -> ch.act = SET_BIT(ch.act, paf.bitvector)
            TO_VULN -> ch.vuln_flags = SET_BIT(ch.vuln_flags, paf.bitvector)
            TO_RACE -> {
                ch.race = paf.raceModifier ?: ch.race
                ch.affected_by = REMOVE_BIT(ch.affected_by, ch.race.aff)
                ch.affected_by = SET_BIT(ch.affected_by, ch.race.aff)
                ch.imm_flags = REMOVE_BIT(ch.imm_flags, ch.race.imm)
                ch.imm_flags = SET_BIT(ch.imm_flags, ch.race.imm)
                ch.res_flags = REMOVE_BIT(ch.res_flags, ch.race.res)
                ch.res_flags = SET_BIT(ch.res_flags, ch.race.res)
                ch.vuln_flags = REMOVE_BIT(ch.vuln_flags, ch.race.vuln)
                ch.vuln_flags = SET_BIT(ch.vuln_flags, ch.race.vuln)
                ch.form = ch.race.form
                ch.parts = ch.race.parts
            }
        }
    } else {
        when (paf.where) {
            TO_AFFECTS -> ch.affected_by = REMOVE_BIT(ch.affected_by, paf.bitvector)
            TO_IMMUNE -> ch.imm_flags = REMOVE_BIT(ch.imm_flags, paf.bitvector)
            TO_RESIST -> ch.res_flags = REMOVE_BIT(ch.res_flags, paf.bitvector)
            TO_ACT_FLAG -> ch.act = REMOVE_BIT(ch.act, paf.bitvector)
            TO_VULN -> ch.vuln_flags = REMOVE_BIT(ch.vuln_flags, paf.bitvector)
            TO_RACE -> {
                ch.affected_by = REMOVE_BIT(ch.affected_by, ch.race.aff)
                ch.affected_by = SET_BIT(ch.affected_by, ch.race.aff)
                ch.imm_flags = REMOVE_BIT(ch.imm_flags, ch.race.imm)
                ch.imm_flags = SET_BIT(ch.imm_flags, ch.race.imm)
                ch.res_flags = REMOVE_BIT(ch.res_flags, ch.race.res)
                ch.res_flags = SET_BIT(ch.res_flags, ch.race.res)
                ch.vuln_flags = REMOVE_BIT(ch.vuln_flags, ch.race.vuln)
                ch.vuln_flags = SET_BIT(ch.vuln_flags, ch.race.vuln)
                ch.form = ch.race.form
                ch.parts = ch.race.parts
                ch.race = ch.race
            }
        }
        mod = 0 - mod
    }

    when (paf.location) {

        Apply.None -> {
        }
        Apply.Str -> ch.mod_stat[PrimeStat.Strength] += mod
        Apply.Dex -> ch.mod_stat[PrimeStat.Dexterity] += mod
        Apply.Intelligence -> ch.mod_stat[PrimeStat.Intelligence] += mod
        Apply.Wis -> ch.mod_stat[PrimeStat.Wisdom] += mod
        Apply.Con -> ch.mod_stat[PrimeStat.Constitution] += mod
        Apply.Cha -> ch.mod_stat[PrimeStat.Charisma] += mod
        Apply.Class -> {
        }
        Apply.Level -> {
        }
        Apply.Age -> ch.played += age_to_num(mod)
        Apply.Height -> {
        }
        Apply.Weight -> {
        }
        Apply.Mana -> ch.max_mana += mod
        Apply.Hit -> ch.max_hit += mod
        Apply.Move -> ch.max_move += mod
        Apply.Gold -> {
        }
        Apply.Exp -> {
        }
        Apply.Ac -> {
            for (i in 0 until ch.armor.size) ch.armor[i] += mod
        }
        Apply.Hitroll -> ch.hitroll += mod
        Apply.Damroll -> ch.damroll += mod
        Apply.Size -> ch.size = ch.size + mod
        Apply.Saves -> ch.saving_throw += mod
        Apply.SavingRod -> ch.saving_throw += mod
        Apply.SavingPetri -> ch.saving_throw += mod
        Apply.SavingBreath -> ch.saving_throw += mod
        Apply.SavingSpell -> ch.saving_throw += mod
        Apply.SpellAffect -> {
        }
        else -> {
            bug("Affect_modify: unknown location %d.", paf.location)
            return
        }
    }

    /* Check for weapon wielding. Guard against recursion (for weapons with affects). */
    if (ch.pcdata != null && !ch.extracted) {
        var hold = get_eq_char(ch, Wear.Both)
        if (hold != null && get_obj_weight(hold) > ch.carry) {
            if (depth == 0) {
                depth++
                act("You drop \$p.", ch, hold, null, TO_CHAR)
                act("\$n drops \$p.", ch, hold, null, TO_ROOM)
                obj_from_char(hold)
                obj_to_room(hold, ch.room)
                depth--
            }
        }

        hold = get_eq_char(ch, Wear.Right)
        if (hold != null && get_obj_weight(hold) > ch.carry) {
            if (depth == 0) {
                depth++
                act("You drop \$p.", ch, hold, null, TO_CHAR)
                act("\$n drops \$p.", ch, hold, null, TO_ROOM)
                obj_from_char(hold)
                obj_to_room(hold, ch.room)
                depth--
            }
        }

        hold = get_eq_char(ch, Wear.Left)
        if (hold != null && get_obj_weight(hold) > ch.carry) {
            if (depth == 0) {
                depth++
                act("You drop \$p.", ch, hold, null, TO_CHAR)
                act("\$n drops \$p.", ch, hold, null, TO_ROOM)
                obj_from_char(hold)
                obj_to_room(hold, ch.room)
                depth--
            }
        }
    }

}

/* find an effect in an affect list */
fun affect_find(paf: Affect?, sn: Skill): Affect? {
    var paf_find = paf
    while (paf_find != null) {
        if (paf_find.type == sn) {
            return paf_find
        }
        paf_find = paf_find.next
    }

    return null
}

/* fix object affects when removing one */

fun affect_check(ch: CHAR_DATA, where: Int, vector: Long) {
    if (where == TO_OBJECT || where == TO_WEAPON || vector == 0L) {
        return
    }

    var paf = ch.affected
    while (paf != null) {
        if (paf.where == where && paf.bitvector == vector) {
            when (where) {
                TO_AFFECTS -> ch.affected_by = SET_BIT(ch.affected_by, vector)
                TO_IMMUNE -> ch.imm_flags = SET_BIT(ch.imm_flags, vector)
                TO_RESIST -> ch.res_flags = SET_BIT(ch.res_flags, vector)
                TO_ACT_FLAG -> ch.act = SET_BIT(ch.act, paf.bitvector)
                TO_VULN -> ch.vuln_flags = SET_BIT(ch.vuln_flags, vector)
                TO_RACE -> if (ch.race === ch.race) {
                    ch.race = paf.raceModifier ?: ch.race
                    ch.affected_by = REMOVE_BIT(ch.affected_by, ch.race.aff)
                    ch.affected_by = SET_BIT(ch.affected_by, ch.race.aff)
                    ch.imm_flags = REMOVE_BIT(ch.imm_flags, ch.race.imm)
                    ch.imm_flags = SET_BIT(ch.imm_flags, ch.race.imm)
                    ch.res_flags = REMOVE_BIT(ch.res_flags, ch.race.res)
                    ch.res_flags = SET_BIT(ch.res_flags, ch.race.res)
                    ch.vuln_flags = REMOVE_BIT(ch.vuln_flags, ch.race.vuln)
                    ch.vuln_flags = SET_BIT(ch.vuln_flags, ch.race.vuln)
                    ch.form = ch.race.form
                    ch.parts = ch.race.parts
                }
            }
            return
        }
        paf = paf.next
    }

    for (obj in ch.carrying) {
        if (obj.wear_loc.isNone() || obj.wear_loc == Wear.StuckIn) {
            continue
        }

        paf = obj.affected
        while (paf != null) {
            if (paf.where == where && paf.bitvector == vector) {
                when (where) {
                    TO_AFFECTS -> ch.affected_by = SET_BIT(ch.affected_by, vector)
                    TO_IMMUNE -> ch.imm_flags = SET_BIT(ch.imm_flags, vector)
                    TO_ACT_FLAG -> ch.act = SET_BIT(ch.act, paf.bitvector)
                    TO_RESIST -> ch.res_flags = SET_BIT(ch.res_flags, vector)
                    TO_VULN -> ch.vuln_flags = SET_BIT(ch.vuln_flags, vector)
                    TO_RACE -> if (ch.race === ch.race) {
                        ch.race = paf.raceModifier ?: ch.race
                        ch.affected_by = REMOVE_BIT(ch.affected_by, ch.race.aff)
                        ch.affected_by = SET_BIT(ch.affected_by, ch.race.aff)
                        ch.imm_flags = REMOVE_BIT(ch.imm_flags, ch.race.imm)
                        ch.imm_flags = SET_BIT(ch.imm_flags, ch.race.imm)
                        ch.res_flags = REMOVE_BIT(ch.res_flags, ch.race.res)
                        ch.res_flags = SET_BIT(ch.res_flags, ch.race.res)
                        ch.vuln_flags = REMOVE_BIT(ch.vuln_flags, ch.race.vuln)
                        ch.vuln_flags = SET_BIT(ch.vuln_flags, ch.race.vuln)
                        ch.form = ch.race.form
                        ch.parts = ch.race.parts
                    }
                }
                return
            }
            paf = paf.next
        }

        if (obj.enchanted) {
            continue
        }

        paf = obj.pIndexData.affected
        while (paf != null) {
            if (paf.where == where && paf.bitvector == vector) {
                when (where) {
                    TO_AFFECTS -> ch.affected_by = SET_BIT(ch.affected_by, vector)
                    TO_IMMUNE -> ch.imm_flags = SET_BIT(ch.imm_flags, vector)
                    TO_ACT_FLAG -> ch.act = SET_BIT(ch.act, paf.bitvector)
                    TO_RESIST -> ch.res_flags = SET_BIT(ch.res_flags, vector)
                    TO_VULN -> ch.vuln_flags = SET_BIT(ch.vuln_flags, vector)
                    TO_RACE -> if (ch.race === ch.race) {
                        ch.race = paf.raceModifier ?: ch.race
                        ch.affected_by = REMOVE_BIT(ch.affected_by, ch.race.aff)
                        ch.affected_by = SET_BIT(ch.affected_by, ch.race.aff)
                        ch.imm_flags = REMOVE_BIT(ch.imm_flags, ch.race.imm)
                        ch.imm_flags = SET_BIT(ch.imm_flags, ch.race.imm)
                        ch.res_flags = REMOVE_BIT(ch.res_flags, ch.race.res)
                        ch.res_flags = SET_BIT(ch.res_flags, ch.race.res)
                        ch.vuln_flags = REMOVE_BIT(ch.vuln_flags, ch.race.vuln)
                        ch.vuln_flags = SET_BIT(ch.vuln_flags, ch.race.vuln)
                        ch.form = ch.race.form
                        ch.parts = ch.race.parts
                    }
                }
                return
            }
            paf = paf.next
        }
    }
}

/*
 * Give an affect to a char.
 */

//TODO: avoid copying?

fun affect_to_char(ch: CHAR_DATA, paf: Affect) {
    val paf_new = Affect()

    paf_new.assignValuesFrom(paf)
    paf_new.next = ch.affected
    ch.affected = paf_new

    affect_modify(ch, paf_new, true)
}

/* give an affect to an object */

fun affect_to_obj(obj: Obj, paf: Affect?) {
    if (paf == null) {
        return
    }
    val paf_new = Affect()

    paf_new.assignValuesFrom(paf)
    paf_new.next = obj.affected
    obj.affected = paf_new

    /* apply any affect vectors to the object's extra_flags */
    if (paf.bitvector != 0L) {
        when (paf.where) {
            TO_OBJECT -> obj.extra_flags = SET_BIT(obj.extra_flags, paf.bitvector)
            TO_WEAPON -> if (obj.item_type == ItemType.Weapon) {
                obj.value[4] = SET_BIT(obj.value[4], paf.bitvector)
            }
        }
    }
}

/*
* Remove an affect from a char.
*/

fun affect_remove(ch: CHAR_DATA, paf: Affect) {
    if (ch.affected == null) {
        bug("Affect_remove: no affect.")
        return
    }

    affect_modify(ch, paf, false)
    val where = paf.where
    val vector = paf.bitvector

    if (paf == ch.affected) {
        ch.affected = paf.next
    } else {
        var prev: Affect?

        prev = ch.affected
        while (prev != null) {
            if (prev.next == paf) {
                prev.next = paf.next
                break
            }
            prev = prev.next
        }

        if (prev == null) {
            bug("Affect_remove: cannot find paf.")
            return
        }
    }

    affect_check(ch, where, vector)
}

fun affect_remove_obj(obj: Obj, paf: Affect) {
    if (obj.affected == null) {
        bug("Affect_remove_object: no affect.")
        return
    }

    val carried_by = obj.carried_by
    if (carried_by != null && obj.wear_loc.isOn()) {
        affect_modify(carried_by, paf, false)
    }

    val where = paf.where
    val vector = paf.bitvector

    /* remove flags from the object if needed */
    if (paf.bitvector != 0L) {
        when (paf.where) {
            TO_OBJECT -> obj.extra_flags = REMOVE_BIT(obj.extra_flags, paf.bitvector)
            TO_WEAPON -> if (obj.item_type == ItemType.Weapon) {
                obj.value[4] = REMOVE_BIT(obj.value[4], paf.bitvector)
            }
        }
    }

    if (paf == obj.affected) {
        obj.affected = paf.next
    } else {
        var prev: Affect?

        prev = obj.affected
        while (prev != null) {
            if (prev.next == paf) {
                prev.next = paf.next
                break
            }
            prev = prev.next
        }

        if (prev == null) {
            bug("Affect_remove_object: cannot find paf.")
            return
        }
    }

    if (carried_by != null && obj.wear_loc.isOn()) {
        affect_check(carried_by, where, vector)
    }
}

/*
* Strip all affects of a given sn.
*/

fun affect_strip(ch: CHAR_DATA, sn: Skill) {
    var paf = ch.affected
    while (paf != null) {
        val paf_next = paf.next
        if (paf.type == sn) {
            affect_remove(ch, paf)
        }
        paf = paf_next
    }
}

/**
 * Return true if a char is affected by a spell.
 */

fun is_affected(ch: CHAR_DATA?, sn: Skill): Boolean {
    if (ch == null) {
        return false
    }
    var paf: Affect?

    paf = ch.affected
    while (paf != null) {
        if (paf.type == sn) {
            return true
        }
        paf = paf.next
    }

    return false
}

/**
 * Add or enhance an affect.
 */
fun affect_join(ch: CHAR_DATA, paf: Affect) {
    var paf_old: Affect?
    paf_old = ch.affected
    while (paf_old != null) {
        if (paf_old.type == paf.type) {
            paf.level = (paf.level + paf_old.level) / 2
            paf.duration += paf_old.duration
            paf.modifier += paf_old.modifier
            affect_remove(ch, paf_old)
            break
        }
        paf_old = paf_old.next
    }

    affect_to_char(ch, paf)
}

/*
* Move a char out of a room.
*/
fun char_from_room(ch: CHAR_DATA) {
    val prev_room = ch.room

    if (ch.pcdata != null) {
        --ch.room.area.nPlayers
    }

    val obj = get_light_char(ch)
    if (obj != null && ch.room.light > 0) {
        --ch.room.light
    }

    ch.room.people.remove(ch)
    //todo: ch.room = null
    ch.on = null  /* sanity check! */

    if (MOUNTED(ch) != null) {
        ch.mount!!.riding = false
        ch.riding = false
    }

    if (RIDDEN(ch) != null) {
        ch.mount!!.riding = false
        ch.riding = false
    }

    if (prev_room.affected_by != 0L) {
        raffect_back_char(prev_room, ch)
    }

}

/* Move a char into a room. */

fun char_to_room(ch: CHAR_DATA, pRoomIndex: ROOM_INDEX_DATA?) {
    if (pRoomIndex == null) {
        bug("Char_to_room: null.")
        val room = get_room_index(ROOM_VNUM_TEMPLE)
        if (room != null) {
            char_to_room(ch, room)
        }
        return
    }

    ch.room = pRoomIndex
    pRoomIndex.people.add(ch)

    if (ch.pcdata != null) {
        if (ch.room.area.empty) {
            ch.room.area.empty = false
            ch.room.area.age = 0
        }
        ++ch.room.area.nPlayers
    }
    val obj = get_light_char(ch)
    if (obj != null)
    // &&   obj.item_type == ItemType.Light &&   obj.value[2] != 0 )
    {
        ++ch.room.light
    }

    if (IS_AFFECTED(ch, AFF_PLAGUE)) {
        var af: Affect?
        af = ch.affected
        while (af != null) {
            if (af.type == Skill.plague) {
                break
            }
            af = af.next
        }

        if (af == null) {
            ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_PLAGUE)
        } else {
            if (af.level != 1) {
                val plague = Affect()
                plague.type = Skill.plague
                plague.level = af.level - 1
                plague.duration = number_range(1, 2 * plague.level)
                plague.location = Apply.Str
                plague.modifier = -5
                plague.bitvector = AFF_PLAGUE

                for (vch in ch.room.people) {
                    if (!saves_spell(plague.level - 2, vch, DT.Disease)
                            && !IS_IMMORTAL(vch) &&
                            !IS_AFFECTED(vch, AFF_PLAGUE) && oneChanceOf(64)) {
                        send_to_char("You feel hot and feverish.\n", vch)
                        act("\$n shivers and looks very ill.", vch, null, null, TO_ROOM)
                        affect_join(vch, plague)
                    }
                }
            }
        }
    }

    if (ch.room.affected_by != 0L) {
        if (IS_IMMORTAL(ch)) {
            do_raffects(ch)
        } else {
            raffect_to_char(ch.room, ch)
        }
    }

}

/** Give an obj to a char.*/
fun obj_to_char(obj: Obj, ch: CHAR_DATA) {
    ch.carrying.add(obj)
    obj.carried_by = ch
    obj.in_room = null
    obj.in_obj = null
    ch.carry_number += get_obj_number(obj)
    ch.carry_weight += get_obj_weight(obj)
}

/*
* Take an obj from its character.
*/

fun obj_from_char(obj: Obj) {
    val ch = obj.carried_by
    if (ch == null) {
        bug("Obj_from_char: null ch.")
        return
    }

    if (obj.wear_loc.isOn()) {
        unequip_char(ch, obj)
    }

    ch.carrying.remove(obj)
    obj.carried_by = null
    ch.carry_number -= get_obj_number(obj)
    ch.carry_weight -= get_obj_weight(obj)
}

/*
* Find the ac value of an obj, including position effect.
*/
fun apply_ac(obj: Obj, iWear: Wear, type: Int): Int {
    if (obj.item_type != ItemType.Armor) {
        return 0
    }
    val t = obj.value[type].toInt()
    return t * iWear.acMult
}

/** Find a piece of eq on a character. */
fun get_eq_char(ch: CHAR_DATA?, iWear: Wear) = ch?.carrying?.firstOrNull { it.wear_loc === iWear }

/* Equip a char with an obj. */
fun equip_char(ch: CHAR_DATA, obj: Obj, iWear: Wear) {
    if (iWear === Wear.StuckIn) {
        obj.wear_loc = iWear
        return
    }

    if (count_worn(ch, iWear) >= max_can_wear(ch, iWear)) {
        bug("Equip_char: already equipped (%d).", iWear)
        return
    }

    if (IS_OBJ_STAT(obj, ITEM_ANTI_EVIL) && ch.isEvil()
            || IS_OBJ_STAT(obj, ITEM_ANTI_GOOD) && ch.isGood()
            || IS_OBJ_STAT(obj, ITEM_ANTI_NEUTRAL) && ch.isNeutralA()) {
        /*
            * Thanks to Morgenes for the bug fix here!
            */
        act("You are zapped by \$p and drop it.", ch, obj, null, TO_CHAR)
        act("\$n is zapped by \$p and drops it.", ch, obj, null, TO_ROOM)
        obj_from_char(obj)
        obj_to_room(obj, ch.room)
        return
    }


    var i = 0
    while (i < 4) {
        ch.armor[i] -= apply_ac(obj, iWear, i)
        i++
    }

    if (get_light_char(ch) == null &&
            (obj.item_type === ItemType.Light && obj.value[2] != 0L || iWear === Wear.Head && IS_OBJ_STAT(obj, ITEM_GLOW))) {
        ++ch.room.light
    }

    obj.wear_loc = iWear

    if (!obj.enchanted) {
        var paf = obj.pIndexData.affected
        while (paf != null) {
            if (paf.location != Apply.SpellAffect) {
                affect_modify(ch, paf, true)
            }
            paf = paf.next
        }
    }
    var paf = obj.affected
    while (paf != null) {
        if (paf.location == Apply.SpellAffect) {
            affect_to_char(ch, paf)
        } else {
            affect_modify(ch, paf, true)
        }
        paf = paf.next
    }

    if (IS_SET(obj.progtypes, OPROG_WEAR)) {
        obj.pIndexData.oprogs.wear_prog!!(obj, ch)
    }

}

/*
* Unequip a char with an obj.
*/

fun unequip_char(ch: CHAR_DATA, obj: Obj) {
    var paf: Affect?
    var lpaf: Affect?
    var lpaf_next: Affect?
    if (obj.wear_loc.isNone()) {
        bug("Unequip_char: already unequipped.")
        return
    }

    if (obj.wear_loc === Wear.StuckIn) {
        obj.wear_loc = Wear.None
        return
    }

    var i = 0
    while (i < 4) {
        ch.armor[i] += apply_ac(obj, obj.wear_loc, i)
        i++
    }
    val old_wear = obj.wear_loc
    obj.wear_loc = Wear.None

    if (!obj.enchanted) {
        paf = obj.pIndexData.affected
        while (paf != null) {
            if (paf.location == Apply.SpellAffect) {
                lpaf = ch.affected
                while (lpaf != null) {
                    lpaf_next = lpaf.next
                    if (lpaf.type == paf.type &&
                            lpaf.level == paf.level &&
                            lpaf.location == Apply.SpellAffect) {
                        affect_remove(ch, lpaf)
                        lpaf_next = null
                    }
                    lpaf = lpaf_next
                }
            } else {
                affect_modify(ch, paf, false)
                affect_check(ch, paf.where, paf.bitvector)
            }
            paf = paf.next
        }
    }

    paf = obj.affected
    while (paf != null) {
        if (paf.location == Apply.SpellAffect) {
            bug("Norm-Apply: %d", 0)
            lpaf = ch.affected
            while (lpaf != null) {
                lpaf_next = lpaf.next
                if (lpaf.type == paf.type &&
                        lpaf.level == paf.level &&
                        lpaf.location == Apply.SpellAffect) {
                    bug("location = %d", lpaf.location)
                    bug("type = " + lpaf.type)
                    affect_remove(ch, lpaf)
                    lpaf_next = null
                }
                lpaf = lpaf_next
            }
        } else {
            affect_modify(ch, paf, false)
            affect_check(ch, paf.where, paf.bitvector)
        }
        paf = paf.next
    }

    if (get_light_char(ch) == null && (obj.item_type == ItemType.Light && obj.value[2] != 0L || old_wear == Wear.Head && IS_OBJ_STAT(obj, ITEM_GLOW)) && ch.room.light > 0) {
        --ch.room.light
    }

    if (IS_SET(obj.progtypes, OPROG_REMOVE)) {
        obj.pIndexData.oprogs.remove_prog!!(obj, ch)
    }


}

/**
 * Count occurrences of an obj in a list.
 */
fun count_obj_list(pObjIndex: ObjProto, list: List<Obj>) = list.count { it.pIndexData == pObjIndex }

/** Move an obj out of a room. */
fun obj_from_room(obj: Obj) {
    val in_room = obj.in_room ?: return
    in_room.people
            .filter { it.on == obj }
            .forEach { it.on = null }

    in_room.objects.remove(obj)
    obj.in_room = null
}

/*
* Move an obj into a room.
*/

fun obj_to_room(obj: Obj, room: ROOM_INDEX_DATA) {
    room.objects.add(obj)
    obj.in_room = room
    obj.carried_by = null
    obj.in_obj = null

    if (IS_WATER(room)) {
        if (may_float(obj)) {
            obj.water_float = -1
        } else {
            obj.water_float = floating_time(obj)
        }
    }
}

/*
* Move an object into an object.
*/

fun obj_to_obj(obj: Obj, obj_to_arg: Obj) {

    obj_to_arg.contains.add(obj)
    obj.in_obj = obj_to_arg
    obj.in_room = null
    obj.carried_by = null
    if (obj_to_arg.pIndexData.vnum == OBJ_VNUM_PIT) {
        obj.cost = 0
    }

    var obj_to: Obj? = obj_to_arg
    while (obj_to != null) {
        if (obj_to.carried_by != null) {
            /*  obj_to.carried_by.carry_number += get_obj_number( obj ); */
            obj_to.carried_by!!.carry_weight += get_obj_weight(obj) * WEIGHT_MULT(obj_to) / 100
        }
        obj_to = obj_to.in_obj
    }

}

/*
* Move an object out of an object.
*/

fun obj_from_obj(obj: Obj) {
    var obj_from: Obj? = obj.in_obj ?: return
    obj.in_obj = null
    while (obj_from != null) {
        if (obj_from.carried_by != null) {
            /*      obj_from.carried_by.carry_number -= get_obj_number( obj ); */
            obj_from.carried_by!!.carry_weight -= get_obj_weight(obj) * WEIGHT_MULT(obj_from) / 100
        }
        obj_from = obj_from.in_obj
    }

}

/*
 * Extract an object consider limit
 */
fun extract_obj(obj: Obj) {
    if (obj.extracted)
    /* if the object has already been extracted once */ {
        bug("Warning! Extraction of " + obj.name + ", vnum " + obj.pIndexData.vnum + ".")
        return  /* if it's already been extracted, something bad is going on */
    } else {
        obj.extracted = true
    }  /* if it hasn't been extracted yet, now
                                   * it's being extracted. */

    when {
        obj.in_room != null -> obj_from_room(obj)
        obj.carried_by != null -> obj_from_char(obj)
        obj.in_obj != null -> obj_from_obj(obj)
    }

    for (c in Clan.values()) {
        if (obj.pIndexData.vnum == c.obj_vnum && c.obj_ptr === obj) {
            obj.pIndexData.count--
            c.obj_ptr = null
        }
    }

    for (obj_content in obj.contains) extract_obj(obj_content)

    if (obj.pIndexData.vnum == OBJ_VNUM_MAGIC_JAR) {
        for (wch in Index.CHARS) {
            if (wch.pcdata == null) {
                continue
            }
            if (is_name(obj.name, wch.name)) {
                wch.act = REMOVE_BIT(wch.act, PLR_NO_EXP)
                send_to_char("Now you catch your spirit.\n", wch)
                break
            }
        }
    }
    if (Index.OBJECTS.remove(obj)) {
        obj.pIndexData.count--
    }
}

/** Extracts character from the world */
fun extract_char(ch: CHAR_DATA, fPull: Boolean) {
    if (fPull)
    /* only for total extractions should it check */ {
        if (ch.extracted)
        /* if the char has already been extracted once */ {
            bug("Warning! Extraction of " + ch.name + ".")
            return  /* if it's already been extracted, something bad is going on */
        } else {
            ch.extracted = true
        }  /* if it hasn't been extracted yet, now
                                   * it's being extracted. */
    }

    nuke_pets(ch)
    ch.pet = null /* just in case */

    if (fPull) {
        die_follower(ch)
    }

    stop_fighting(ch, true)

    char_from_room(ch)

    char_to_room(ch, get_room_index(ROOM_VNUM_LIMBO))

    for (obj in ch.carrying) extract_obj(obj)

    char_from_room(ch)

    if (!fPull) {
        val align = when {
            ch.isGood() -> 0
            ch.isEvil() -> 2
            else -> 1
        }
        char_to_room(ch, get_room_index(ch.hometown.altar[align]))
        return
    }

    val pcdata = ch.pcdata
    if (pcdata == null) {
        --ch.pIndexData.count
    } else {
        do_return(ch)
        ch.pcdata = null
    }

    Index.CHARS
            .filter { it.reply == ch }
            .forEach { it.reply = null }

    Index.CHARS.remove(ch)
    free_char(ch)
}

/** Find a char in the room. */
fun get_char_room(ch: CHAR_DATA, argument: String): CHAR_DATA? {
    val argB = StringBuilder()
    val number = number_argument(argument, argB)
    val arg = argB.toString()
    var count = 0
    var ugly = 0
    if (eq(arg, "self")) {
        return ch
    }
    if (eq(arg, "ugly")) {
        ugly = 1
    }

    for (rch in ch.room.people) {
        if (!can_see(ch, rch)) {
            continue
        }

        if (ugly != 0 && count + 1 == number && IS_VAMPIRE(rch)) {
            return rch
        }

        val doppel = rch.doppel
        val name = if (doppel != null && !IS_SET(ch.act, PLR_HOLYLIGHT)) doppel.name else rch.name
        if (!is_name(arg, name)) {
            continue
        }

        if (++count == number) {
            return rch
        }
    }

    return null
}

/*
* Find a char in the room.
* Chronos uses in act_move.c
*/

fun get_char_room2(ch: CHAR_DATA, room: ROOM_INDEX_DATA?, argument: String, number: IntArray): CHAR_DATA? {
    if (room == null) {
        return null
    }
    var count = 0
    var ugly = 0

    if (eq(argument, "ugly")) {
        ugly = 1
    }

    for (rch in room.people) {
        if (!can_see(ch, rch)) {
            continue
        }

        if (ugly != 0 && count + 1 == number[0] && IS_VAMPIRE(rch)) {
            return rch
        }

        val doppel = rch.doppel
        val name = if (doppel != null && !IS_SET(rch.act, PLR_HOLYLIGHT)) doppel.name else rch.name
        if (!is_name(argument, name)) {
            continue
        }

        if (++count == number[0]) {
            return rch
        }
    }

    number[0] -= count
    return null
}

/**
 * Find a char in the world.
 */
fun get_char_world(ch: CHAR_DATA, argument: String): CHAR_DATA? {
    val wch = get_char_room(ch, argument)
    if (wch != null) {
        return wch
    }

    val arg = StringBuilder()
    val number = number_argument(argument, arg)
    val argstr = arg.toString()

    return Index.CHARS
            .filter { can_see(ch, it) && is_name(argstr, it.name) }
            .filterIndexed { count, _ -> count + 1 == number }
            .firstOrNull()
}

/**
 * Find some object with a given index data.
 * Used by area-reset 'P' command.
 */

fun get_obj_type(pObjIndex: ObjProto): Obj? = Index.OBJECTS.firstOrNull { it.pIndexData == pObjIndex }

/**
 * Find an obj in a list.
 */
fun get_obj_list(ch: CHAR_DATA, argument: String, list: List<Obj>): Obj? {
    val arg = StringBuilder()
    val number = number_argument(argument, arg)
    val argstr = arg.toString()

    return list
            .filter { can_see_obj(ch, it) && is_name(argstr, it.name) }
            .filterIndexed { count, _ -> count + 1 == number }
            .firstOrNull()
}

/*
* Find an obj in player's inventory.
*/

fun get_obj_carry(ch: CHAR_DATA, argument: String): Obj? {
    val number: Int

    val arg = StringBuilder()
    number = number_argument(argument, arg)
    val argstr = arg.toString()
    var count = 0
    for (obj in ch.carrying) {
        if (obj.wear_loc.isNone() && can_see_obj(ch, obj) && is_name(argstr, obj.name)) {
            if (++count == number) {
                return obj
            }
        }
    }

    return null
}

/*
* Find an obj in player's equipment.
*/

fun get_obj_wear(ch: CHAR_DATA, argument: String): Obj? {
    val arg = StringBuilder()
    val number = number_argument(argument, arg)
    val argstr = arg.toString()
    var count = 0
    for (obj in ch.carrying) {
        if (obj.wear_loc.isOn() && can_see_obj(ch, obj) && is_name(argstr, obj.name)) {
            if (++count == number) {
                return obj
            }
        }
    }

    return null
}

/*
* Find an obj in the room or in inventory.
*/

fun get_obj_here(ch: CHAR_DATA, argument: String): Obj? {
    var obj = get_obj_list(ch, argument, ch.room.objects)
    if (obj != null) {
        return obj
    }

    obj = get_obj_carry(ch, argument)
    if (obj != null) {
        return obj
    }

    obj = get_obj_wear(ch, argument)
    return if (obj != null) obj else null

}

/*
* Find an obj in the world.
*/

fun get_obj_world(ch: CHAR_DATA, argument: String): Obj? {
    val obj = get_obj_here(ch, argument)
    if (obj != null) {
        return obj
    }

    val arg = StringBuilder()
    val number = number_argument(argument, arg)
    val argstr = arg.toString()
    return Index.OBJECTS
            .filter { can_see_obj(ch, it) && is_name(argstr, it.name) }
            .filterIndexed { count, _ -> count + 1 == number }
            .firstOrNull()
}

/* deduct cost from a character */

fun deduct_cost(ch: CHAR_DATA, cost: Int) {
    var silver: Int
    var gold = 0

    silver = Math.min(ch.silver, cost)

    if (silver < cost) {
        gold = (cost - silver + 99) / 100
        silver = cost - 100 * gold
    }

    ch.gold -= gold
    ch.silver -= silver

    if (ch.gold < 0) {
        bug("deduct costs: gold %d < 0", ch.gold)
        ch.gold = 0
    }
    if (ch.silver < 0) {
        bug("deduct costs: silver %d < 0", ch.silver)
        ch.silver = 0
    }
}
/*
 * Create a 'money' obj.
 */

fun create_money(goldArg: Int, silverArg: Int): Obj {
    var gold = goldArg
    var silver = silverArg
    val obj: Obj

    if (gold < 0 || silver < 0 || gold == 0 && silver == 0) {
        bug("Create_money: zero or negative money.", Math.min(gold, silver))
        gold = Math.max(1, gold)
        silver = Math.max(1, silver)
    }

    if (gold == 0 && silver == 1) {
        obj = create_object(get_obj_index_nn(OBJ_VNUM_SILVER_ONE), 0)
    } else if (gold == 1 && silver == 0) {
        obj = create_object(get_obj_index_nn(OBJ_VNUM_GOLD_ONE), 0)
    } else if (silver == 0) {
        obj = create_object(get_obj_index_nn(OBJ_VNUM_GOLD_SOME), 0)
        val f = Formatter()
        f.format(obj.short_desc, gold)
        obj.short_desc = f.toString()
        obj.value[1] = gold.toLong()
        obj.cost = gold
        obj.weight = gold / 5
    } else if (gold == 0) {
        obj = create_object(get_obj_index_nn(OBJ_VNUM_SILVER_SOME), 0)
        val f = Formatter()
        f.format(obj.short_desc, silver)
        obj.short_desc = f.toString()
        obj.value[0] = silver.toLong()
        obj.cost = silver
        obj.weight = silver / 20
    } else {
        obj = create_object(get_obj_index_nn(OBJ_VNUM_COINS), 0)
        val f = Formatter()
        f.format(obj.short_desc, silver, gold)
        obj.short_desc = f.toString()
        obj.value[0] = silver.toLong()
        obj.value[1] = gold.toLong()
        obj.cost = 100 * gold + silver
        obj.weight = gold / 5 + silver / 20
    }

    return obj
}

/*
* Return # of objects which an object counts as.
* Thanks to Tony Chamberlain for the correct recursive code here.
*/

fun get_obj_number(obj: Obj?): Int {
    if (obj == null) {
        return 0
    }
    /*
    if (obj.item_type == ItemType.Container || obj.item_type == ItemType.Money
    ||  obj.item_type == ItemType.Gem || obj.item_type == ItemType.Jewelry)
        number = 0;
*/

    /*
    for ( obj = obj.contains; obj != null; obj = obj.next_content )
        number += get_obj_number( obj );
*/
    return if (obj.item_type == ItemType.Money) 0 else 1
}

fun get_obj_realnumber(objArg: Obj) = 1 + objArg.contains.sumBy { get_obj_number(it) }

/*
 * Return weight of an object, including weight of contents.
 */

fun get_obj_weight(obj: Obj?): Int {
    if (obj == null) {
        return 0
    }
    var weight = obj.weight
    for (tobj in obj.contains) {
        weight += get_obj_weight(tobj) * WEIGHT_MULT(obj) / 100
    }

    return weight
}

fun get_true_weight(objArg: Obj) = objArg.weight + objArg.contains.sumBy { get_obj_weight(it) }

/*
 * true if room is dark.
 */

fun room_is_dark(ch: CHAR_DATA): Boolean {
    val pRoomIndex = ch.room

    if (IS_VAMPIRE(ch)) {
        return false
    }

    if (pRoomIndex.light > 0) {
        return false
    }

    if (IS_SET(pRoomIndex.room_flags, ROOM_DARK)) {
        return true
    }


    return if (pRoomIndex.sector_type == SECT_INSIDE || pRoomIndex.sector_type == SECT_CITY) {
        false
    } else weather.sunlight == SUN_SET || weather.sunlight == SUN_DARK

}

fun room_dark(pRoomIndex: ROOM_INDEX_DATA): Boolean {
    if (pRoomIndex.light > 0) {
        return false
    }

    if (IS_SET(pRoomIndex.room_flags, ROOM_DARK)) {
        return true
    }


    return if (pRoomIndex.sector_type == SECT_INSIDE || pRoomIndex.sector_type == SECT_CITY) {
        false
    } else weather.sunlight == SUN_SET || weather.sunlight == SUN_DARK

}


fun is_room_owner(ch: CHAR_DATA, room: ROOM_INDEX_DATA): Boolean {
    val owner = room.owner
    return owner != null && !owner.isEmpty() && is_name(ch.name, owner)

}

/*
 * true if room is private.
 */

fun room_is_private(pRoomIndex: ROOM_INDEX_DATA): Boolean {
    /*
    if (pRoomIndex.owner != null && pRoomIndex.owner.length()!=0)
    return true;
*/
    val count = pRoomIndex.people.count()
    if (IS_SET(pRoomIndex.room_flags, ROOM_PRIVATE) && count >= 2) {
        return true
    }

    return if (IS_SET(pRoomIndex.room_flags, ROOM_SOLITARY) && count >= 1) true else IS_SET(pRoomIndex.room_flags, ROOM_IMP_ONLY)
}

/* visibility on a room -- for entering and exits */

fun can_see_room(ch: CHAR_DATA, pRoomIndex: ROOM_INDEX_DATA): Boolean {
    if (IS_SET(pRoomIndex.room_flags, ROOM_IMP_ONLY) && get_trust(ch) < MAX_LEVEL) {
        return false
    }

    if (IS_SET(pRoomIndex.room_flags, ROOM_GODS_ONLY) && !IS_IMMORTAL(ch)) {
        return false
    }


    return if (IS_SET(pRoomIndex.room_flags, ROOM_HEROES_ONLY) && !IS_IMMORTAL(ch)) {
        false
    } else !(IS_SET(pRoomIndex.room_flags, ROOM_NEWBIES_ONLY)
            && ch.level > 5 && !IS_IMMORTAL(ch))

}

/*
* true if char can see victim.
*/

fun can_see(ch: CHAR_DATA, victim: CHAR_DATA): Boolean {
    if (ch == victim) {
        return true
    }

    if (get_trust(ch) < victim.invis_level) {
        return false
    }


    if (get_trust(ch) < victim.incog_level && ch.room != victim.room) {
        return false
    }

    if (ch.pcdata != null && IS_SET(ch.act, PLR_HOLYLIGHT) || ch.pcdata == null && IS_IMMORTAL(ch)) {
        return true
    }

    if (IS_AFFECTED(ch, AFF_BLIND)) {
        return false
    }

    if (room_is_dark(ch) && !IS_AFFECTED(ch, AFF_INFRARED)) {
        return false
    }

    if (IS_AFFECTED(victim, AFF_INVISIBLE) && !IS_AFFECTED(ch, AFF_DETECT_INVIS)) {
        return false
    }

    if (IS_AFFECTED(victim, AFF_IMP_INVIS) && !IS_AFFECTED(ch, AFF_DETECT_IMP_INVIS)) {
        return false
    }

    /* sneaking */

    if (IS_AFFECTED(victim, AFF_SNEAK) && !IS_AFFECTED(ch, AFF_DETECT_HIDDEN) && victim.fighting == null) {
        var chance = get_skill(victim, Skill.sneak)
        chance += ch.curr_stat(PrimeStat.Dexterity) * 3 / 2
        chance -= ch.curr_stat(PrimeStat.Intelligence) * 2
        chance += ch.level - victim.level * 3 / 2
        if (randomPercent() < chance) return false
    }


    if (IS_AFFECTED(victim, AFF_CAMOUFLAGE) && !IS_AFFECTED(ch, AFF_ACUTE_VISION)) {
        return false
    }

    if (IS_AFFECTED(victim, AFF_HIDE)
            && !IS_AFFECTED(ch, AFF_DETECT_HIDDEN)
            && victim.fighting == null) {
        return false
    }


    return when {
        IS_AFFECTED(victim, AFF_FADE) && !IS_AFFECTED(ch, AFF_DETECT_FADE) && victim.fighting == null -> false
        else -> !IS_AFFECTED(victim, AFF_EARTHFADE)
    }

}

/*
* true if char can see obj.
*/

fun can_see_obj(ch: CHAR_DATA, obj: Obj?): Boolean {
    if (obj == null) {
        return false
    }
    if (ch.pcdata != null && IS_SET(ch.act, PLR_HOLYLIGHT)) {
        return true
    }

    if (IS_SET(obj.extra_flags, ITEM_VIS_DEATH)) {
        return false
    }

    if (IS_AFFECTED(ch, AFF_BLIND) && obj.item_type != ItemType.Potion) {
        return false
    }

    if (obj.item_type == ItemType.Light && obj.value[2] != 0L) {
        return true
    }

    if (IS_SET(obj.extra_flags, ITEM_INVIS) && !IS_AFFECTED(ch, AFF_DETECT_INVIS)) {
        return false
    }

    if (IS_SET(obj.extra_flags, ITEM_BURIED) && !IS_IMMORTAL(ch)) {
        return false
    }

    if (IS_OBJ_STAT(obj, ITEM_GLOW)) {
        return true
    }

    if (room_is_dark(ch) && !IS_AFFECTED(ch, AFF_INFRARED)) {
        return false
    }

    return if (obj.item_type == ItemType.Tattoo) {
        true
    } else true

}

/*
* true if char can drop obj.
*/

fun can_drop_obj(ch: CHAR_DATA, obj: Obj?): Boolean {
    if (obj == null) {
        return false
    }
    return if (!IS_SET(obj.extra_flags, ITEM_NODROP)) {
        true
    } else ch.pcdata != null && ch.level >= LEVEL_IMMORTAL

}

/*
* Return ascii name of an affect location.
*/


/*
    * Return ascii name of an affect bit vector.
    */
private val stat_buf = StringBuilder()

fun affect_bit_name(vector: Long): String {
    stat_buf.setLength(0)
    if (vector and AFF_BLIND != 0L) {
        stat_buf.append(" blind")
    }
    if (vector and AFF_INVISIBLE != 0L) {
        stat_buf.append(" invisible")
    }
    if (vector and AFF_IMP_INVIS != 0L) {
        stat_buf.append(" imp_invis")
    }
    if (vector and AFF_FADE != 0L) {
        stat_buf.append(" fade")
    }
    if (vector and AFF_SCREAM != 0L) {
        stat_buf.append(" scream")
    }
    if (vector and AFF_BLOODTHIRST != 0L) {
        stat_buf.append(" bloodthirst")
    }
    if (vector and AFF_STUN != 0L) {
        stat_buf.append(" stun")
    }
    if (vector and AFF_SANCTUARY != 0L) {
        stat_buf.append(" sanctuary")
    }
    if (vector and AFF_FAERIE_FIRE != 0L) {
        stat_buf.append(" faerie_fire")
    }
    if (vector and AFF_INFRARED != 0L) {
        stat_buf.append(" infrared")
    }
    if (vector and AFF_CURSE != 0L) {
        stat_buf.append(" curse")
    }
    if (vector and AFF_POISON != 0L) {
        stat_buf.append(" poison")
    }
    if (vector and AFF_PROTECT_EVIL != 0L) {
        stat_buf.append(" prot_evil")
    }
    if (vector and AFF_PROTECT_GOOD != 0L) {
        stat_buf.append(" prot_good")
    }
    if (vector and AFF_SLEEP != 0L) {
        stat_buf.append(" sleep")
    }
    if (vector and AFF_SNEAK != 0L) {
        stat_buf.append(" sneak")
    }
    if (vector and AFF_HIDE != 0L) {
        stat_buf.append(" hide")
    }
    if (vector and AFF_CHARM != 0L) {
        stat_buf.append(" charm")
    }
    if (vector and AFF_FLYING != 0L) {
        stat_buf.append(" flying")
    }
    if (vector and AFF_PASS_DOOR != 0L) {
        stat_buf.append(" pass_door")
    }
    if (vector and AFF_BERSERK != 0L) {
        stat_buf.append(" berserk")
    }
    if (vector and AFF_CALM != 0L) {
        stat_buf.append(" calm")
    }
    if (vector and AFF_HASTE != 0L) {
        stat_buf.append(" haste")
    }
    if (vector and AFF_SLOW != 0L) {
        stat_buf.append(" slow")
    }
    if (vector and AFF_WEAKEN != 0L) {
        stat_buf.append(" weaken")
    }
    if (vector and AFF_PLAGUE != 0L) {
        stat_buf.append(" plague")
    }
    if (vector and AFF_REGENERATION != 0L) {
        stat_buf.append(" regeneration")
    }
    if (vector and AFF_CAMOUFLAGE != 0L) {
        stat_buf.append(" camouflage")
    }
    if (vector and AFF_SWIM != 0L) {
        stat_buf.append(" swim")
    }
    return if (stat_buf.isEmpty()) stat_buf.toString() else "none"
}

/*
* Return ascii name of an affect bit vector.
*/

fun detect_bit_name(vector: Long): String {

    stat_buf.setLength(0)
    if (vector and AFF_DETECT_IMP_INVIS != 0L) {
        stat_buf.append(" detect_imp_inv")
    }
    if (vector and AFF_DETECT_EVIL != 0L) {
        stat_buf.append(" detect_evil")
    }
    if (vector and AFF_DETECT_GOOD != 0L) {
        stat_buf.append(" detect_good")
    }
    if (vector and AFF_DETECT_INVIS != 0L) {
        stat_buf.append(" detect_invis")
    }
    if (vector and AFF_DETECT_MAGIC != 0L) {
        stat_buf.append(" detect_magic")
    }
    if (vector and AFF_DETECT_HIDDEN != 0L) {
        stat_buf.append(" detect_hidden")
    }
    if (vector and AFF_DARK_VISION != 0L) {
        stat_buf.append(" dark_vision")
    }
    if (vector and AFF_ACUTE_VISION != 0L) {
        stat_buf.append(" acute_vision")
    }
    if (vector and AFF_FEAR != 0L) {
        stat_buf.append(" fear")
    }
    if (vector and AFF_FORM_TREE != 0L) {
        stat_buf.append(" form_tree")
    }
    if (vector and AFF_FORM_GRASS != 0L) {
        stat_buf.append(" form_grass")
    }
    if (vector and AFF_WEB != 0L) {
        stat_buf.append(" web")
    }
    if (vector and AFF_DETECT_LIFE != 0L) {
        stat_buf.append(" life")
    }
    if (vector and AFF_DETECT_SNEAK != 0L) {
        stat_buf.append(" detect_sneak")
    }
    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}

/*
* Return ascii name of extra flags vector.
*/

fun extra_bit_name(extra_flags: Long): String {

    stat_buf.setLength(0)
    if (extra_flags and ITEM_GLOW != 0L) {
        stat_buf.append(" glow")
    }
    if (extra_flags and ITEM_HUM != 0L) {
        stat_buf.append(" hum")
    }
    if (extra_flags and ITEM_DARK != 0L) {
        stat_buf.append(" dark")
    }
    if (extra_flags and ITEM_LOCK != 0L) {
        stat_buf.append(" lock")
    }
    if (extra_flags and ITEM_EVIL != 0L) {
        stat_buf.append(" evil")
    }
    if (extra_flags and ITEM_INVIS != 0L) {
        stat_buf.append(" invis")
    }
    if (extra_flags and ITEM_MAGIC != 0L) {
        stat_buf.append(" magic")
    }
    if (extra_flags and ITEM_NODROP != 0L) {
        stat_buf.append(" nodrop")
    }
    if (extra_flags and ITEM_BLESS != 0L) {
        stat_buf.append(" bless")
    }
    if (extra_flags and ITEM_ANTI_GOOD != 0L) {
        stat_buf.append(" anti-good")
    }
    if (extra_flags and ITEM_ANTI_EVIL != 0L) {
        stat_buf.append(" anti-evil")
    }
    if (extra_flags and ITEM_ANTI_NEUTRAL != 0L) {
        stat_buf.append(" anti-neutral")
    }
    if (extra_flags and ITEM_NOREMOVE != 0L) {
        stat_buf.append(" noremove")
    }
    if (extra_flags and ITEM_INVENTORY != 0L) {
        stat_buf.append(" inventory")
    }
    if (extra_flags and ITEM_NOPURGE != 0L) {
        stat_buf.append(" nopurge")
    }
    if (extra_flags and ITEM_VIS_DEATH != 0L) {
        stat_buf.append(" vis_death")
    }
    if (extra_flags and ITEM_ROT_DEATH != 0L) {
        stat_buf.append(" rot_death")
    }
    if (extra_flags and ITEM_NOLOCATE != 0L) {
        stat_buf.append(" no_locate")
    }
    if (extra_flags and ITEM_SELL_EXTRACT != 0L) {
        stat_buf.append(" sell_extract")
    }
    if (extra_flags and ITEM_BURN_PROOF != 0L) {
        stat_buf.append(" burn_proof")
    }
    if (extra_flags and ITEM_NOUNCURSE != 0L) {
        stat_buf.append(" no_uncurse")
    }
    if (extra_flags and ITEM_BURIED != 0L) {
        stat_buf.append(" buried")
    }
    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}

/* return ascii name of an act vector */

fun act_bit_name(act_flags: Long): String {
    stat_buf.setLength(0)


    if (IS_SET(act_flags, ACT_IS_NPC)) {
        stat_buf.append(" npc")
        if (act_flags and ACT_SENTINEL != 0L) {
            stat_buf.append(" sentinel")
        }
        if (act_flags and ACT_SCAVENGER != 0L) {
            stat_buf.append(" scavenger")
        }
        if (act_flags and ACT_AGGRESSIVE != 0L) {
            stat_buf.append(" aggressive")
        }
        if (act_flags and ACT_STAY_AREA != 0L) {
            stat_buf.append(" stay_area")
        }
        if (act_flags and ACT_WIMPY != 0L) {
            stat_buf.append(" wimpy")
        }
        if (act_flags and ACT_PET != 0L) {
            stat_buf.append(" pet")
        }
        if (act_flags and ACT_TRAIN != 0L) {
            stat_buf.append(" train")
        }
        if (act_flags and ACT_PRACTICE != 0L) {
            stat_buf.append(" practice")
        }
        if (act_flags and ACT_UNDEAD != 0L) {
            stat_buf.append(" undead")
        }
        if (act_flags and ACT_HUNTER != 0L) {
            stat_buf.append(" hunter")
        }
        if (act_flags and ACT_CLERIC != 0L) {
            stat_buf.append(" cleric")
        }
        if (act_flags and ACT_MAGE != 0L) {
            stat_buf.append(" mage")
        }
        if (act_flags and ACT_THIEF != 0L) {
            stat_buf.append(" thief")
        }
        if (act_flags and ACT_WARRIOR != 0L) {
            stat_buf.append(" warrior")
        }
        if (act_flags and ACT_NOALIGN != 0L) {
            stat_buf.append(" no_align")
        }
        if (act_flags and ACT_NOPURGE != 0L) {
            stat_buf.append(" no_purge")
        }
        if (act_flags and ACT_IS_HEALER != 0L) {
            stat_buf.append(" healer")
        }
        if (act_flags and ACT_IS_CHANGER != 0L) {
            stat_buf.append(" changer")
        }
        if (act_flags and ACT_GAIN != 0L) {
            stat_buf.append(" skill_train")
        }
        if (act_flags and ACT_UPDATE_ALWAYS != 0L) {
            stat_buf.append(" update_always")
        }
    } else {
        stat_buf.append(" player")
        if (act_flags and PLR_AUTO_ASSIST != 0L) {
            stat_buf.append(" autoassist")
        }
        if (act_flags and PLR_AUTO_EXIT != 0L) {
            stat_buf.append(" autoexit")
        }
        if (act_flags and PLR_AUTO_LOOT != 0L) {
            stat_buf.append(" autoloot")
        }
        if (act_flags and PLR_AUTO_SAC != 0L) {
            stat_buf.append(" autosac")
        }
        if (act_flags and PLR_AUTO_GOLD != 0L) {
            stat_buf.append(" autogold")
        }
        if (act_flags and PLR_AUTO_SPLIT != 0L) {
            stat_buf.append(" autosplit")
        }
        if (act_flags and PLR_COLOR != 0L) {
            stat_buf.append(" color_on")
        }
        if (act_flags and PLR_WANTED != 0L) {
            stat_buf.append(" wanted")
        }
        if (act_flags and PLR_NO_TITLE != 0L) {
            stat_buf.append(" no_title")
        }
        if (act_flags and PLR_NO_EXP != 0L) {
            stat_buf.append(" no_exp")
        }
        if (act_flags and PLR_HOLYLIGHT != 0L) {
            stat_buf.append(" holy_light")
        }
        if (act_flags and PLR_NOCANCEL != 0L) {
            stat_buf.append(" no_cancel")
        }
        if (act_flags and PLR_CANLOOT != 0L) {
            stat_buf.append(" loot_corpse")
        }
        if (act_flags and PLR_NOSUMMON != 0L) {
            stat_buf.append(" no_summon")
        }
        if (act_flags and PLR_NOFOLLOW != 0L) {
            stat_buf.append(" no_follow")
        }
        if (act_flags and PLR_CANINDUCT != 0L) {
            stat_buf.append(" Cabal_LEADER")
        }
        if (act_flags and PLR_GHOST != 0L) {
            stat_buf.append(" ghost")
        }
        if (act_flags and PLR_PERMIT != 0L) {
            stat_buf.append(" permit")
        }
        if (act_flags and PLR_REMORTED != 0L) {
            stat_buf.append(" remorted")
        }
        if (act_flags and PLR_LOG != 0L) {
            stat_buf.append(" log")
        }
        if (act_flags and PLR_FREEZE != 0L) {
            stat_buf.append(" frozen")
        }
        if (act_flags and PLR_LEFTHAND != 0L) {
            stat_buf.append(" lefthand")
        }
        if (act_flags and PLR_CANREMORT != 0L) {
            stat_buf.append(" canremort")
        }
        if (act_flags and PLR_QUESTOR != 0L) {
            stat_buf.append(" questor")
        }
        if (act_flags and PLR_VAMPIRE != 0L) {
            stat_buf.append(" VAMPIRE")
        }
        if (act_flags and PLR_HARA_KIRI != 0L) {
            stat_buf.append(" harakiri")
        }
        if (act_flags and PLR_BLINK_ON != 0L) {
            stat_buf.append(" blink_on")
        }
    }
    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}

fun comm_bit_name(comm_flags: Long): String {
    stat_buf.setLength(0)
    if (comm_flags and COMM_QUIET != 0L) {
        stat_buf.append(" quiet")
    }
    if (comm_flags and COMM_DEAF != 0L) {
        stat_buf.append(" deaf")
    }
    if (comm_flags and COMM_NOWIZ != 0L) {
        stat_buf.append(" no_wiz")
    }
    if (comm_flags and COMM_NOAUCTION != 0L) {
        stat_buf.append(" no_auction")
    }
    if (comm_flags and COMM_NOGOSSIP != 0L) {
        stat_buf.append(" no_gossip")
    }
    if (comm_flags and COMM_NOQUESTION != 0L) {
        stat_buf.append(" no_question")
    }
    if (comm_flags and COMM_NOMUSIC != 0L) {
        stat_buf.append(" no_music")
    }
    if (comm_flags and COMM_NOQUOTE != 0L) {
        stat_buf.append(" no_quote")
    }
    if (comm_flags and COMM_COMPACT != 0L) {
        stat_buf.append(" compact")
    }
    if (comm_flags and COMM_BRIEF != 0L) {
        stat_buf.append(" brief")
    }
    if (comm_flags and COMM_PROMPT != 0L) {
        stat_buf.append(" prompt")
    }
    if (comm_flags and COMM_COMBINE != 0L) {
        stat_buf.append(" combine")
    }
    if (comm_flags and COMM_NOEMOTE != 0L) {
        stat_buf.append(" no_emote")
    }
    if (comm_flags and COMM_NOSHOUT != 0L) {
        stat_buf.append(" no_shout")
    }
    if (comm_flags and COMM_NOTELL != 0L) {
        stat_buf.append(" no_tell")
    }
    if (comm_flags and COMM_NOCHANNELS != 0L) {
        stat_buf.append(" no_channels")
    }


    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}

fun imm_bit_name(imm_flags: Long): String {
    stat_buf.setLength(0)
    if (imm_flags and IMM_SUMMON != 0L) {
        stat_buf.append(" summon")
    }
    if (imm_flags and IMM_CHARM != 0L) {
        stat_buf.append(" charm")
    }
    if (imm_flags and IMM_MAGIC != 0L) {
        stat_buf.append(" magic")
    }
    if (imm_flags and IMM_WEAPON != 0L) {
        stat_buf.append(" weapon")
    }
    if (imm_flags and IMM_BASH != 0L) {
        stat_buf.append(" blunt")
    }
    if (imm_flags and IMM_PIERCE != 0L) {
        stat_buf.append(" piercing")
    }
    if (imm_flags and IMM_SLASH != 0L) {
        stat_buf.append(" slashing")
    }
    if (imm_flags and IMM_FIRE != 0L) {
        stat_buf.append(" fire")
    }
    if (imm_flags and IMM_COLD != 0L) {
        stat_buf.append(" cold")
    }
    if (imm_flags and IMM_LIGHTNING != 0L) {
        stat_buf.append(" lightning")
    }
    if (imm_flags and IMM_ACID != 0L) {
        stat_buf.append(" acid")
    }
    if (imm_flags and IMM_POISON != 0L) {
        stat_buf.append(" poison")
    }
    if (imm_flags and IMM_NEGATIVE != 0L) {
        stat_buf.append(" negative")
    }
    if (imm_flags and IMM_HOLY != 0L) {
        stat_buf.append(" holy")
    }
    if (imm_flags and IMM_ENERGY != 0L) {
        stat_buf.append(" energy")
    }
    if (imm_flags and IMM_MENTAL != 0L) {
        stat_buf.append(" mental")
    }
    if (imm_flags and IMM_DISEASE != 0L) {
        stat_buf.append(" disease")
    }
    if (imm_flags and IMM_DROWNING != 0L) {
        stat_buf.append(" drowning")
    }
    if (imm_flags and IMM_LIGHT != 0L) {
        stat_buf.append(" light")
    }
    if (imm_flags and VULN_IRON != 0L) {
        stat_buf.append(" iron")
    }
    if (imm_flags and VULN_WOOD != 0L) {
        stat_buf.append(" wood")
    }
    if (imm_flags and VULN_SILVER != 0L) {
        stat_buf.append(" silver")
    }

    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}

fun wear_bit_name(wear_flags: Long): String {
    stat_buf.setLength(0)
    if (wear_flags and ITEM_TAKE != 0L) {
        stat_buf.append(" take")
    }
    if (wear_flags and ITEM_WEAR_FINGER != 0L) {
        stat_buf.append(" finger")
    }
    if (wear_flags and ITEM_WEAR_NECK != 0L) {
        stat_buf.append(" neck")
    }
    if (wear_flags and ITEM_WEAR_BODY != 0L) {
        stat_buf.append(" torso")
    }
    if (wear_flags and ITEM_WEAR_HEAD != 0L) {
        stat_buf.append(" head")
    }
    if (wear_flags and ITEM_WEAR_LEGS != 0L) {
        stat_buf.append(" legs")
    }
    if (wear_flags and ITEM_WEAR_FEET != 0L) {
        stat_buf.append(" feet")
    }
    if (wear_flags and ITEM_WEAR_HANDS != 0L) {
        stat_buf.append(" hands")
    }
    if (wear_flags and ITEM_WEAR_ARMS != 0L) {
        stat_buf.append(" arms")
    }
    if (wear_flags and ITEM_WEAR_SHIELD != 0L) {
        stat_buf.append(" shield")
    }
    if (wear_flags and ITEM_WEAR_ABOUT != 0L) {
        stat_buf.append(" body")
    }
    if (wear_flags and ITEM_WEAR_WAIST != 0L) {
        stat_buf.append(" waist")
    }
    if (wear_flags and ITEM_WEAR_WRIST != 0L) {
        stat_buf.append(" wrist")
    }
    if (wear_flags and ITEM_WIELD != 0L) {
        stat_buf.append(" wield")
    }
    if (wear_flags and ITEM_HOLD != 0L) {
        stat_buf.append(" hold")
    }
    if (wear_flags and ITEM_WEAR_FLOAT != 0L) {
        stat_buf.append(" float")
    }
    if (wear_flags and ITEM_WEAR_TATTOO != 0L) {
        stat_buf.append(" tattoo")
    }

    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}

fun form_bit_name(form_flags: Long): String {
    stat_buf.setLength(0)
    if (form_flags and FORM_POISON != 0L) {
        stat_buf.append(" poison")
    } else if (form_flags and FORM_EDIBLE != 0L) {
        stat_buf.append(" edible")
    }
    if (form_flags and FORM_MAGICAL != 0L) {
        stat_buf.append(" magical")
    }
    if (form_flags and FORM_INSTANT_DECAY != 0L) {
        stat_buf.append(" instant_rot")
    }
    if (form_flags and FORM_OTHER != 0L) {
        stat_buf.append(" other")
    }
    if (form_flags and FORM_ANIMAL != 0L) {
        stat_buf.append(" animal")
    }
    if (form_flags and FORM_SENTIENT != 0L) {
        stat_buf.append(" sentient")
    }
    if (form_flags and FORM_UNDEAD != 0L) {
        stat_buf.append(" undead")
    }
    if (form_flags and FORM_CONSTRUCT != 0L) {
        stat_buf.append(" construct")
    }
    if (form_flags and FORM_MIST != 0L) {
        stat_buf.append(" mist")
    }
    if (form_flags and FORM_INTANGIBLE != 0L) {
        stat_buf.append(" intangible")
    }
    if (form_flags and FORM_BIPED != 0L) {
        stat_buf.append(" biped")
    }
    if (form_flags and FORM_CENTAUR != 0L) {
        stat_buf.append(" centaur")
    }
    if (form_flags and FORM_INSECT != 0L) {
        stat_buf.append(" insect")
    }
    if (form_flags and FORM_SPIDER != 0L) {
        stat_buf.append(" spider")
    }
    if (form_flags and FORM_CRUSTACEAN != 0L) {
        stat_buf.append(" crustacean")
    }
    if (form_flags and FORM_WORM != 0L) {
        stat_buf.append(" worm")
    }
    if (form_flags and FORM_BLOB != 0L) {
        stat_buf.append(" blob")
    }
    if (form_flags and FORM_MAMMAL != 0L) {
        stat_buf.append(" mammal")
    }
    if (form_flags and FORM_BIRD != 0L) {
        stat_buf.append(" bird")
    }
    if (form_flags and FORM_REPTILE != 0L) {
        stat_buf.append(" reptile")
    }
    if (form_flags and FORM_SNAKE != 0L) {
        stat_buf.append(" snake")
    }
    if (form_flags and FORM_DRAGON != 0L) {
        stat_buf.append(" dragon")
    }
    if (form_flags and FORM_AMPHIBIAN != 0L) {
        stat_buf.append(" amphibian")
    }
    if (form_flags and FORM_FISH != 0L) {
        stat_buf.append(" fish")
    }
    if (form_flags and FORM_COLD_BLOOD != 0L) {
        stat_buf.append(" cold_blooded")
    }

    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}

fun part_bit_name(part_flags: Long): String {
    stat_buf.setLength(0)
    if (part_flags and PART_HEAD != 0L) {
        stat_buf.append(" head")
    }
    if (part_flags and PART_ARMS != 0L) {
        stat_buf.append(" arms")
    }
    if (part_flags and PART_LEGS != 0L) {
        stat_buf.append(" legs")
    }
    if (part_flags and PART_HEART != 0L) {
        stat_buf.append(" heart")
    }
    if (part_flags and PART_BRAINS != 0L) {
        stat_buf.append(" brains")
    }
    if (part_flags and PART_GUTS != 0L) {
        stat_buf.append(" guts")
    }
    if (part_flags and PART_HANDS != 0L) {
        stat_buf.append(" hands")
    }
    if (part_flags and PART_FEET != 0L) {
        stat_buf.append(" feet")
    }
    if (part_flags and PART_FINGERS != 0L) {
        stat_buf.append(" fingers")
    }
    if (part_flags and PART_EAR != 0L) {
        stat_buf.append(" ears")
    }
    if (part_flags and PART_EYE != 0L) {
        stat_buf.append(" eyes")
    }
    if (part_flags and PART_LONG_TONGUE != 0L) {
        stat_buf.append(" long_tongue")
    }
    if (part_flags and PART_EYESTALKS != 0L) {
        stat_buf.append(" eyestalks")
    }
    if (part_flags and PART_TENTACLES != 0L) {
        stat_buf.append(" tentacles")
    }
    if (part_flags and PART_FINS != 0L) {
        stat_buf.append(" fins")
    }
    if (part_flags and PART_WINGS != 0L) {
        stat_buf.append(" wings")
    }
    if (part_flags and PART_TAIL != 0L) {
        stat_buf.append(" tail")
    }
    if (part_flags and PART_CLAWS != 0L) {
        stat_buf.append(" claws")
    }
    if (part_flags and PART_FANGS != 0L) {
        stat_buf.append(" fangs")
    }
    if (part_flags and PART_HORNS != 0L) {
        stat_buf.append(" horns")
    }
    if (part_flags and PART_SCALES != 0L) {
        stat_buf.append(" scales")
    }

    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}

fun weapon_bit_name(weapon_flags: Long): String {
    stat_buf.setLength(0)

    if (weapon_flags and WEAPON_FLAMING != 0L) {
        stat_buf.append(" flaming")
    }
    if (weapon_flags and WEAPON_FROST != 0L) {
        stat_buf.append(" frost")
    }
    if (weapon_flags and WEAPON_VAMPIRIC != 0L) {
        stat_buf.append(" vampiric")
    }
    if (weapon_flags and WEAPON_SHARP != 0L) {
        stat_buf.append(" sharp")
    }
    if (weapon_flags and WEAPON_VORPAL != 0L) {
        stat_buf.append(" vorpal")
    }
    if (weapon_flags and WEAPON_TWO_HANDS != 0L) {
        stat_buf.append(" two-handed")
    }
    if (weapon_flags and WEAPON_SHOCKING != 0L) {
        stat_buf.append(" shocking")
    }
    if (weapon_flags and WEAPON_POISON != 0L) {
        stat_buf.append(" poison")
    }
    if (weapon_flags and WEAPON_HOLY != 0L) {
        stat_buf.append(" holy")
    }

    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}

fun cont_bit_name(cont_flags: Long): String {
    stat_buf.setLength(0)

    if (cont_flags and CONT_CLOSEABLE != 0L) {
        stat_buf.append(" closable")
    }
    if (cont_flags and CONT_PICKPROOF != 0L) {
        stat_buf.append(" pickproof")
    }
    if (cont_flags and CONT_CLOSED != 0L) {
        stat_buf.append(" closed")
    }
    if (cont_flags and CONT_LOCKED != 0L) {
        stat_buf.append(" locked")
    }

    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}


fun off_bit_name(off_flags: Long): String {
    stat_buf.setLength(0)

    if (off_flags and OFF_AREA_ATTACK != 0L) {
        stat_buf.append(" area attack")
    }
    if (off_flags and OFF_BACKSTAB != 0L) {
        stat_buf.append(" backstab")
    }
    if (off_flags and OFF_BASH != 0L) {
        stat_buf.append(" bash")
    }
    if (off_flags and OFF_BERSERK != 0L) {
        stat_buf.append(" berserk")
    }
    if (off_flags and OFF_DISARM != 0L) {
        stat_buf.append(" disarm")
    }
    if (off_flags and OFF_DODGE != 0L) {
        stat_buf.append(" dodge")
    }
    if (off_flags and OFF_FADE != 0L) {
        stat_buf.append(" fade")
    }
    if (off_flags and OFF_FAST != 0L) {
        stat_buf.append(" fast")
    }
    if (off_flags and OFF_KICK != 0L) {
        stat_buf.append(" kick")
    }
    if (off_flags and OFF_KICK_DIRT != 0L) {
        stat_buf.append(" kick_dirt")
    }
    if (off_flags and OFF_PARRY != 0L) {
        stat_buf.append(" parry")
    }
    if (off_flags and OFF_RESCUE != 0L) {
        stat_buf.append(" rescue")
    }
    if (off_flags and OFF_TAIL != 0L) {
        stat_buf.append(" tail")
    }
    if (off_flags and OFF_TRIP != 0L) {
        stat_buf.append(" trip")
    }
    if (off_flags and OFF_CRUSH != 0L) {
        stat_buf.append(" crush")
    }
    if (off_flags and ASSIST_ALL != 0L) {
        stat_buf.append(" assist_all")
    }
    if (off_flags and ASSIST_ALIGN != 0L) {
        stat_buf.append(" assist_align")
    }
    if (off_flags and ASSIST_RACE != 0L) {
        stat_buf.append(" assist_race")
    }
    if (off_flags and ASSIST_PLAYERS != 0L) {
        stat_buf.append(" assist_players")
    }
    if (off_flags and ASSIST_GUARD != 0L) {
        stat_buf.append(" assist_guard")
    }
    if (off_flags and ASSIST_VNUM != 0L) {
        stat_buf.append(" assist_vnum")
    }

    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}

fun isn_dark_safe(ch: CHAR_DATA) = when {
    !IS_VAMPIRE(ch) -> 0
    IS_SET(ch.room.room_flags, ROOM_DARK) -> 0
    weather.sunlight == SUN_LIGHT || weather.sunlight == SUN_RISE -> 2
    else -> {
        if (ch.room.people
                        .map { get_light_char(it) }
                        .any { it?.item_type == ItemType.Light && IS_OBJ_STAT(it, ITEM_MAGIC) }) 1 else 0
    }
}

/*
* Return types:
* 0: success
* 1: general failure
* 2: no cabal powers
*/

fun skill_failure_nomessage(ch: CHAR_DATA, skill: Skill, npcOffFlag: Long): Int {
    if (ch.pcdata == null && npcOffFlag != 0L && CABAL_OK(ch, skill) && RACE_OK(ch, skill) || IS_SET(ch.off_flags, npcOffFlag)) {
        return 0
    }

    if (ch.pcdata != null && CLEVEL_OK(ch, skill) && RACE_OK(ch, skill) && CABAL_OK(ch, skill) && ALIGN_OK(ch, skill)) {
        val cabal_obj_ptr = ch.cabal.obj_ptr
        if (skill.cabal == Clan.None
                || cabal_obj_ptr == null
                || cabal_obj_ptr.in_room == null
                || cabal_obj_ptr.in_room!!.vnum == ch.cabal.room_vnum) {
            return 0
        }

        for (c in Clan.values()) {
            if (cabal_obj_ptr.in_room!!.vnum == c.room_vnum) {
                return 2
            }
        }
    }

    return 1
}

fun skill_failure_check(ch: CHAR_DATA, skill: Skill, mount_ok: Boolean, npcOffFlag: Long, msg: String?): Boolean {
    if (!mount_ok && MOUNTED(ch) != null) {
        send_to_char("You can't do that while riding!\n", ch)
        return true
    }

    val r = skill_failure_nomessage(ch, skill, npcOffFlag)
    if (r != 0) {
        if (r == 2) {
            send_to_char("You cannot find the Cabal Power within you.\n", ch)
        } else if (msg != null && msg.isNotEmpty()) {
            send_to_char(msg, ch)
        } else {
            send_to_char("Huh?\n", ch)
        }
        return true
    }

    return false
}

/*
 * Apply or remove an affect to a room.
 */

fun affect_modify_room(room: ROOM_INDEX_DATA, paf: Affect, fAdd: Boolean) {
    var mod = paf.modifier

    if (fAdd) {
        when (paf.where) {
            TO_ROOM_AFFECTS -> room.affected_by = SET_BIT(room.affected_by, paf.bitvector)
            TO_ROOM_FLAGS -> room.room_flags = SET_BIT(room.room_flags, paf.bitvector)
            TO_ROOM_CONST -> {
            }
        }
    } else {
        when (paf.where) {
            TO_ROOM_AFFECTS -> room.affected_by = REMOVE_BIT(room.affected_by, paf.bitvector)
            TO_ROOM_FLAGS -> room.room_flags = REMOVE_BIT(room.room_flags, paf.bitvector)
            TO_ROOM_CONST -> {
            }
        }
        mod = 0 - mod
    }

    when (paf.location) {
        Apply.None -> {
        }
        Apply.RoomHeal -> room.heal_rate += mod
        Apply.RoomMana -> room.mana_rate += mod
        else -> {
            bug("Affect_modify_room: unknown location %d.", paf.location)
            return
        }
    }

}

/*
 * Give an affect to a room.
 */

fun affect_to_room(room: ROOM_INDEX_DATA, paf: Affect) {
    if (room.affected == null) {
        val ta = top_affected_room
        if (ta != null) {
            var pRoomIndex: ROOM_INDEX_DATA = ta
            while (pRoomIndex.aff_next != null) {
                pRoomIndex = pRoomIndex.aff_next!!
            }
            pRoomIndex.aff_next = room
        } else {
            top_affected_room = room
        }
        room.aff_next = null
    }

    val paf_new = Affect()

    paf_new.assignValuesFrom(paf)
    paf_new.next = room.affected
    room.affected = paf_new

    affect_modify_room(room, paf_new, true)
}

fun affect_check_room(room: ROOM_INDEX_DATA, where: Int, vector: Long) {
    var paf: Affect?

    if (vector == 0L) {
        return
    }

    paf = room.affected
    while (paf != null) {
        if (paf.where == where && paf.bitvector == vector) {
            when (where) {
                TO_ROOM_AFFECTS -> room.affected_by = SET_BIT(room.affected_by, vector)
                TO_ROOM_FLAGS -> room.room_flags = SET_BIT(room.room_flags, vector)
                TO_ROOM_CONST -> {
                }
            }
            return
        }
        paf = paf.next
    }
}

/*
 * Remove an affect from a room.
 */

fun affect_remove_room(room: ROOM_INDEX_DATA, paf: Affect) {
    if (room.affected == null) {
        bug("Affect_remove_room: no affect.")
        return
    }

    affect_modify_room(room, paf, false)
    val where = paf.where
    val vector = paf.bitvector

    if (paf == room.affected) {
        room.affected = paf.next
    } else {
        var prev: Affect?

        prev = room.affected
        while (prev != null) {
            if (prev.next == paf) {
                prev.next = paf.next
                break
            }
            prev = prev.next
        }

        if (prev == null) {
            bug("Affect_remove_room: cannot find paf.")
            return
        }
    }

    if (room.affected == null) {
        var prev: ROOM_INDEX_DATA

        if (top_affected_room == room) {
            top_affected_room = room.aff_next
        } else {
            prev = top_affected_room!!
            while (prev.aff_next != null) {
                if (prev.aff_next == room) {
                    prev.aff_next = room.aff_next
                    break
                }
                prev = prev.aff_next!!
            }
        }
        room.aff_next = null

    }

    affect_check_room(room, where, vector)
}

/*
 * Strip all affects of a given sn.
 */

fun affect_strip_room(room: ROOM_INDEX_DATA, sn: Skill) {
    var paf = room.affected
    while (paf != null) {
        val paf_next = paf.next
        if (paf.type == sn) {
            affect_remove_room(room, paf)
        }
        paf = paf_next
    }

}

/*
* Return true if a room is affected by a spell.
*/

fun is_affected_room(room: ROOM_INDEX_DATA, sn: Skill): Boolean {
    var paf: Affect?

    paf = room.affected
    while (paf != null) {
        if (paf.type == sn) {
            return true
        }
        paf = paf.next
    }

    return false
}

/*
* Add or enhance an affect.
*/

fun affect_join_room(room: ROOM_INDEX_DATA, paf: Affect) {
    var paf_old: Affect?

    paf_old = room.affected
    while (paf_old != null) {
        if (paf_old.type == paf.type) {
            paf.level = (paf.level + paf_old.level) / 2
            paf.duration += paf_old.duration
            paf.modifier += paf_old.modifier
            affect_remove_room(room, paf_old)
            break
        }
        paf_old = paf_old.next
    }

    affect_to_room(room, paf)
}

/*
 * Return ascii name of an raffect location.
 */

fun raffect_loc_name(location: Int): String {
    when (location) {
        APPLY_ROOM_NONE -> return "none"
        APPLY_ROOM_HEAL -> return "heal rate"
        APPLY_ROOM_MANA -> return "mana rate"
    }
    bug("Affect_location_name: unknown location %d.", location)
    return "(unknown)"
}

/*
* Return ascii name of an affect bit vector.
*/

fun raffect_bit_name(vector: Long): String {
    stat_buf.setLength(0)
    if (vector and AFF_ROOM_SHOCKING != 0L) {
        stat_buf.append(" shocking")
    }
    if (vector and AFF_ROOM_L_SHIELD != 0L) {
        stat_buf.append(" lightning_shield")
    }
    if (vector and AFF_ROOM_THIEF_TRAP != 0L) {
        stat_buf.append(" thief_trap")
    }
    if (vector and AFF_ROOM_CURSE != 0L) {
        stat_buf.append(" curse")
    }
    if (vector and AFF_ROOM_POISON != 0L) {
        stat_buf.append(" poison")
    }
    if (vector and AFF_ROOM_PLAGUE != 0L) {
        stat_buf.append(" plague")
    }
    if (vector and AFF_ROOM_SLEEP != 0L) {
        stat_buf.append(" sleep")
    }
    if (vector and AFF_ROOM_SLOW != 0L) {
        stat_buf.append(" slow")
    }
    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}


fun is_safe_rspell_nom(level: Int, victim: CHAR_DATA): Boolean {
    /* ghosts are safe */
    val v_pcdata = victim.pcdata
    if (v_pcdata != null && IS_SET(victim.act, PLR_GHOST)) {
        return true
    }

    /* link dead players who do not have rushing adrenalin are safe */
    if (v_pcdata != null && (victim.last_fight_time == -1L || current_time - victim.last_fight_time > FIGHT_DELAY_TIME) &&
            get_pc(victim) == null) {
        return true
    }

    if (victim.level < 5 && v_pcdata != null) {
        return true
    }

    return if (v_pcdata != null && victim.last_death_time != -1L && current_time - victim.last_death_time < 600) {
        true
    } else v_pcdata != null && (level >= victim.level + 5 || victim.level >= level + 5)


}


fun is_safe_rspell(level: Int, victim: CHAR_DATA): Boolean {
    if (!is_safe_rspell_nom(level, victim)) return false
    act("The gods protect \$n.", victim, null, null, TO_CHAR)
    act("The gods protect \$n from the spell of room.", victim, null, null, TO_ROOM)
    return true

}


fun raffect_to_char(room: ROOM_INDEX_DATA, ch: CHAR_DATA) {
    var paf: Affect?

    if (IS_ROOM_AFFECTED(room, AFF_ROOM_L_SHIELD)) {
        val sn = Skill.lightning_shield
        val vch = room.people.firstOrNull { is_room_owner(it, room) }
        if (vch == null) {
            bug("Owner of lightning shield left the room.")
            room.owner = ""
            affect_strip_room(room, sn)
        } else {
            send_to_char("The protective shield of room blocks you.\n", ch)
            act("\$N has entered the room.", vch, null, ch, TO_CHAR)
            do_wake(vch, "")

            paf = affect_find(room.affected, sn)
            if (paf == null) {
                bug("Bad paf for lightning shield")
                return
            }

            if (!is_safe_rspell(paf.level, ch)) {

                if (IS_AFFECTED(ch, AFF_GROUNDING)) {
                    send_to_char("The electricity fizzles at your foes.\n", ch)
                    act("A lightning bolt fizzles at \$N's foes.\n", ch, null, ch, TO_ROOM)
                } else {
                    damage(vch, ch, dice(paf.level, 4) + 12, sn, DT.Lightning, true)
                    room.owner = ""
                    affect_remove_room(room, paf)
                }
            }
        }
    }

    if (IS_ROOM_AFFECTED(room, AFF_ROOM_SHOCKING)) {
        val sn = Skill.shocking_trap
        send_to_char("The shocking waves of room shocks you.\n", ch)

        paf = affect_find(room.affected, sn)
        if (paf == null) {
            bug("Bad paf for shocking shield")
            return
        }

        if (!is_safe_rspell(paf.level, ch)) {
            if (check_immune(ch, DT.Lightning) != IS_IMMUNE) {
                damage(ch, ch, dice(paf.level, 4) + 12, Skill.x_hunger, DT.Trap, true)
            }
            run { affect_remove_room(room, paf!!) }
        }
    }

    if (IS_ROOM_AFFECTED(room, AFF_ROOM_THIEF_TRAP)) {
        send_to_char("The trap ,set by someone, blocks you.\n", ch)

        paf = affect_find(room.affected, Skill.settraps)
        if (paf == null) {
            bug("Bad paf for settraps")
            return
        }

        if (!is_safe_rspell(paf.level, ch)) {
            if (check_immune(ch, DT.Pierce) != IS_IMMUNE) {
                damage(ch, ch, dice(paf.level, 5) + 12, Skill.x_hunger, DT.Trap, true)
            }
            run { affect_remove_room(room, paf) }
        }
    }

    if (IS_ROOM_AFFECTED(room, AFF_ROOM_SLOW) || IS_ROOM_AFFECTED(room, AFF_ROOM_SLEEP)) {
        send_to_char("{YThere is some mist flowing in the air.{x\n", ch)
    }
}

fun raffect_back_char(room: ROOM_INDEX_DATA, ch: CHAR_DATA) {
    if (IS_ROOM_AFFECTED(room, AFF_ROOM_L_SHIELD)) {
        if (is_room_owner(ch, room)) {
            room.owner = ""
            affect_strip_room(room, Skill.lightning_shield)
        }
    }
}

/*
* Return ascii name of an affect bit vector.
*/

fun flag_room_name(vector: Long): String {
    stat_buf.setLength(0)
    if (vector and ROOM_DARK != 0L) {
        stat_buf.append(" dark")
    }
    if (vector and ROOM_NO_MOB != 0L) {
        stat_buf.append(" nomob")
    }
    if (vector and ROOM_INDOORS != 0L) {
        stat_buf.append(" indoors")
    }
    if (vector and ROOM_PRIVATE != 0L) {
        stat_buf.append(" private")
    }
    if (vector and ROOM_SAFE != 0L) {
        stat_buf.append(" safe")
    }
    if (vector and ROOM_SOLITARY != 0L) {
        stat_buf.append(" solitary")
    }
    if (vector and ROOM_PET_SHOP != 0L) {
        stat_buf.append(" petshop")
    }
    if (vector and ROOM_NO_RECALL != 0L) {
        stat_buf.append(" norecall")
    }
    if (vector and ROOM_IMP_ONLY != 0L) {
        stat_buf.append(" imp_only")
    }
    if (vector and ROOM_GODS_ONLY != 0L) {
        stat_buf.append(" god_only")
    }
    if (vector and ROOM_HEROES_ONLY != 0L) {
        stat_buf.append(" heroes")
    }
    if (vector and ROOM_NEWBIES_ONLY != 0L) {
        stat_buf.append(" newbies")
    }
    if (vector and ROOM_LAW != 0L) {
        stat_buf.append(" law")
    }
    if (vector and ROOM_NOWHERE != 0L) {
        stat_buf.append(" nowhere")
    }
    if (vector and ROOM_BANK != 0L) {
        stat_buf.append(" bank")
    }
    if (vector and ROOM_NO_MAGIC != 0L) {
        stat_buf.append(" nomagic")
    }
    if (vector and ROOM_NOSUMMON != 0L) {
        stat_buf.append(" nosummon")
    }
    if (vector and ROOM_REGISTRY != 0L) {
        stat_buf.append(" registry")
    }
    return if (stat_buf.isNotEmpty()) stat_buf.toString() else "none"
}


fun affect_check_obj(ch: CHAR_DATA, vector: Long): Boolean {
    if (vector == 0L) {
        return false
    }

    for (obj in ch.carrying) {
        if (obj.wear_loc.isNone() || obj.wear_loc === Wear.StuckIn) {
            continue
        }

        var paf = obj.affected
        while (paf != null) {
            if (paf.bitvector == vector) {
                return true
            }
            paf = paf.next
        }

        paf = obj.pIndexData.affected
        while (paf != null) {
            if (paf.bitvector == vector) {
                return true
            }
            paf = paf.next
        }
    }
    return false
}

fun count_charmed(ch: CHAR_DATA): Int {
    val count = Index.CHARS.count { IS_AFFECTED(it, AFF_CHARM) && it.master == ch && ch.pet != it }
    if (count >= MAX_CHARM(ch)) {
        send_to_char("You are already controlling as many charmed mobs as you can!\n", ch)
        return count
    }
    return 0
}

fun add_mind(ch: CHAR_DATA, str: String) {

    if (ch.pcdata != null) {
        return
    }

    var in_mind = ch.in_mind
    if (in_mind.isEmpty()) {
        in_mind = ch.room.vnum.toString()
        ch.in_mind = in_mind
    }
    if (!is_name(str, in_mind)) {
        ch.in_mind = "$in_mind $str"
    }

}

fun remove_mind(ch: CHAR_DATA, str: String) {
    var mind: String? = ch.in_mind

    if (ch.pcdata != null || mind == null || !is_name(str, mind)) {
        return
    }

    val arg = StringBuilder()
    val buf = StringBuilder()
    do {
        arg.setLength(0)
        mind = one_argument(mind!!, arg)
        if (!is_name(str, arg.toString())) {
            if (buf.isNotEmpty()) {
                buf.append(" ")
            }
            buf.append(arg)
        }
    } while (mind!!.isNotEmpty())

    do_say(ch, "At last, I took my revenge!")
    ch.in_mind = buf.toString()
    if (is_number(ch.in_mind)) {
        back_home(ch)
    }
}

fun opposite_door(door: Int) = when (door) {
    0 -> 2
    1 -> 3
    2 -> 0
    3 -> 1
    4 -> 5
    5 -> 4
    else -> -1
}

fun back_home(ch: CHAR_DATA) {
    val location: ROOM_INDEX_DATA?

    val pcdata = ch.pcdata
    val in_mind = ch.in_mind
    if (pcdata != null || in_mind.isEmpty()) {
        return
    }

    val arg = StringBuilder()
    one_argument(in_mind, arg)
    location = find_location(ch, arg.toString())
    if (location == null) {
        bug("Mob cannot return to reset place", 0)
        return
    }

    if (ch.fighting == null && location != ch.room) {
        act("\$n prays for transportation.", ch, null, null, TO_ROOM)
        char_from_room(ch)
        char_to_room(ch, location)
        act("\$n appears in the room.", ch, null, null, TO_ROOM)
        if (is_number(in_mind)) {
            ch.in_mind = ""
        }
    }
}

private val _n = IntArray(1)

fun find_char(ch: CHAR_DATA, argument: String, door: Int, range: Int): CHAR_DATA? {
    val arg = StringBuilder()
    _n[0] = number_argument(argument, arg)
    var target = get_char_room2(ch, ch.room, arg.toString(), _n)
    if (target != null) {
        return target
    }
    val opdoor = opposite_door(door)
    if (opdoor == -1) {
        bug("In find_char wrong door: %d", door)
        send_to_char("You don't see that there.\n", ch)
        return null
    }
    if (range > 0) {
        //todo: range--;
        /* find target room */
        val back_room = ch.room
        val pExit = ch.room.exit[door]
        val dest_room = pExit?.to_room
        if (pExit != null && dest_room != null && !IS_SET(pExit.exit_info, EX_CLOSED)) {
            val bExit = dest_room.exit[opdoor]
            if (bExit == null || bExit.to_room != back_room) {
                send_to_char("The path you choose prevents your power to pass.\n", ch)
                return null
            }
            target = get_char_room2(ch, dest_room, arg.toString(), _n)
            if (target != null) {
                return target
            }
        }
    }
    send_to_char("You don't see that there.\n", ch)
    return null
}

fun check_exit(arg: String) = when {
    eq(arg, "n") || eq(arg, "north") -> 0
    eq(arg, "e") || eq(arg, "east") -> 1
    eq(arg, "s") || eq(arg, "south") -> 2
    eq(arg, "w") || eq(arg, "west") -> 3
    eq(arg, "u") || eq(arg, "up") -> 4
    eq(arg, "d") || eq(arg, "down") -> 5
    else -> -1
}

/*
 * Find a char for spell usage.
 */

fun get_char_spell(ch: CHAR_DATA, argument: String, door: IntArray, range: Int): CHAR_DATA? {
    val i = argument.indexOf('.')
    val buf = if (i >= 0) argument.substring(0, i) else argument
    if (i == 0) {
        door[0] = check_exit(buf)
        if (door[0] == -1) {
            return get_char_room(ch, argument)
        }
    }
    return find_char(ch, argument.substring(i + 1), door[0], range)

}

fun path_to_track(ch: CHAR_DATA, victim: CHAR_DATA, door: Int) {
    var range = 0

    ch.last_fight_time = current_time
    if (victim.pcdata != null) {
        victim.last_fight_time = current_time
    }

    if (victim.pcdata == null && victim.position != Pos.Dead) {
        victim.last_fought = ch

        val opdoor = opposite_door(door)
        if (opdoor == -1) {
            bug("In path_to_track wrong door: %d", door)
            return
        }
        var temp: ROOM_INDEX_DATA? = ch.room
        for (i in 0 until 1000) {
            range++
            if (victim.room == temp) {
                break
            }
            val pExit = temp!!.exit[door]
            temp = pExit?.to_room
            if (pExit == null || temp == null) {
                bug("In path_to_track: couldn't calculate range %d", range)
                return
            }
            if (range > 100) {
                bug("In path_to_track: range exceeded 100")
                return
            }
        }

        temp = victim.room
        while (--range > 0) {
            room_record(ch.name, temp!!, opdoor)
            val pExit = temp.exit[opdoor]
            temp = pExit?.to_room
            if (pExit == null || temp == null) {
                bug("Path to track: Range: " + range + " Room: " + (if (temp == null) null else temp.vnum) + " opdoor:" + opdoor)
                return
            }
        }
        do_track(victim, "")
    }
}

/* new staff */

fun get_wield_char(ch: CHAR_DATA?, second: Boolean): Obj? {
    if (ch == null) {
        return null
    }
    ch.carrying
            .filter { it.item_type == ItemType.Weapon }
            .forEach {
                if (second) {
                    if (it.wear_loc === Wear.Right && LEFT_HANDER(ch) || it.wear_loc === Wear.Left && RIGHT_HANDER(ch)) {
                        return it
                    }
                } else {
                    if (it.wear_loc === Wear.Right && RIGHT_HANDER(ch)
                            || it.wear_loc === Wear.Left && LEFT_HANDER(ch)
                            || it.wear_loc === Wear.Both) {
                        return it
                    }
                }
            }

    return null
}


fun get_shield_char(ch: CHAR_DATA?) = ch?.carrying?.firstOrNull { it.wear_loc.isHold() && CAN_WEAR(it, ITEM_WEAR_SHIELD) }

fun get_hold_char(ch: CHAR_DATA?) = ch?.carrying?.firstOrNull { it.wear_loc.isHold() && CAN_WEAR(it, ITEM_HOLD) }

fun get_light_char(ch: CHAR_DATA?) = ch?.carrying?.firstOrNull {
    it.item_type == ItemType.Light
            && it.value[2] != 0L
            && (it.wear_loc == Wear.Left || it.wear_loc == Wear.Right || it.wear_loc == Wear.Both)
            || it.wear_loc == Wear.Head && IS_OBJ_STAT(it, ITEM_GLOW)
}


fun is_wielded_char(ch: CHAR_DATA?, obj: Obj) =
        ch?.carrying?.any { it.wear_loc.isHold() && CAN_WEAR(it, ITEM_WIELD) && it == obj } ?: false

fun is_equiped_n_char(ch: CHAR_DATA?, vnum: VNum, iWear: Wear) =
        ch?.carrying?.any { it.wear_loc === iWear && it.pIndexData.vnum == vnum } ?: false

fun is_equiped_char(ch: CHAR_DATA?, obj: Obj, iWear: Wear) =
        ch?.carrying?.any { it.wear_loc === iWear && it === obj } ?: false

fun count_worn(ch: CHAR_DATA?, iWear: Wear) =
        ch?.carrying?.count { it.wear_loc === iWear } ?: 0


fun max_can_wear(ch: CHAR_DATA, i: Wear) =
        if (i !== Wear.Finger || ch.pcdata == null || !IS_SET(ch.act, PLR_REMORTED)) i.max else i.max + 2

fun get_played_day(t: Int) = (currentTimeSeconds().toInt() - t) / (60 * 24)

fun get_played_time(ch: CHAR_DATA, l: Int): Int {
    val pcdata = ch.pcdata ?: return 0
    if (l != 0) {
        return pcdata.log_time[l]
    }

    /* fix if it is passed midnight */
    val ref_time = if (ch.logon > limit_time) ch.logon else limit_time
    return pcdata.log_time[0] + ((current_time - ref_time) / 60).toInt()
}

fun get_total_played(ch: CHAR_DATA): Int {
    if (ch.pcdata == null) {
        return 0
    }

    /* now calculate the rest */
    var l = 0
    var sum = 0
    while (l < nw_config.max_time_log) {
        sum += get_played_time(ch, l)
        l++
    }

    return sum
}

fun check_time_sync(): Boolean {
    /*TODO: struct tm *am_time;
        int now_day, lim_day;

        am_time = localtime( &current_time );
        now_day = am_time.tm_mday;
        am_time = localtime( &limit_time );
        lim_day = am_time.tm_mday;

        if ( now_day == lim_day )
          return false;
        else
          return true;*/
    return false
}
