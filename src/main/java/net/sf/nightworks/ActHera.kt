package net.sf.nightworks

import net.sf.nightworks.act.info.do_look
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Auction
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.ROOM_INDEX_DATA
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_OBJ_STAT
import net.sf.nightworks.util.IS_RAFFECTED
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_TRUSTED
import net.sf.nightworks.util.IS_WEAPON_STAT
import net.sf.nightworks.util.MOUNTED
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.atoi
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.isDigit
import net.sf.nightworks.util.log_string
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.randomDir
import net.sf.nightworks.util.randomPercent

/** random room generation procedure */
fun get_random_room(ch: CHAR_DATA): ROOM_INDEX_DATA {
    while (true) {
        val room = get_room_index(number_range(0, 65535))
        if (room != null) {
            if (can_see_room(ch, room)
                    && !room_is_private(room)
                    && !IS_SET(room.room_flags, ROOM_PRIVATE)
                    && !IS_SET(room.room_flags, ROOM_SOLITARY)
                    && !IS_SET(room.room_flags, ROOM_SAFE)
                    && (ch.pcdata == null || IS_SET(ch.act, ACT_AGGRESSIVE)
                    || !IS_SET(room.room_flags, ROOM_LAW))) {
                return room
            }
        }
    }
}

/* RT Enter portals */
fun do_enter(ch: CHAR_DATA, argument: String) {
    val location: ROOM_INDEX_DATA?

    if (ch.fighting != null) {
        return
    }

    /* nifty portal stuff */
    if (argument.isNotEmpty()) {
        val old_room = ch.room
        val portal = get_obj_list(ch, argument, ch.room.objects)
        val mount = MOUNTED(ch)

        if (portal == null) {
            send_to_char("You don't see that here.\n", ch)
            return
        }

        if (portal.item_type != ItemType.Portal || IS_SET(portal.value[1], EX_CLOSED) && !IS_TRUSTED(ch, ANGEL)) {
            send_to_char("You can't seem to find a way in.\n", ch)
            return
        }

        if (!IS_TRUSTED(ch, ANGEL) && !IS_SET(portal.value[2], GATE_NOCURSE)
                && (IS_AFFECTED(ch, AFF_CURSE)
                || IS_SET(old_room.room_flags, ROOM_NO_RECALL)
                || IS_RAFFECTED(old_room, AFF_ROOM_CURSE))) {
            send_to_char("Something prevents you from leaving...\n", ch)
            return
        }

        if (IS_SET(portal.value[2], GATE_RANDOM) || portal.value[3] == -1L) {
            location = get_random_room(ch)
            portal.value[3] = location.vnum.toLong() /* keeps record */
        } else if (IS_SET(portal.value[2], GATE_BUGGY) && randomPercent() < 5) {
            location = get_random_room(ch)
        } else {
            location = get_room_index(portal.value[3].toInt())
        }

        if (location == null
                || location == old_room
                || !can_see_room(ch, location)
                || room_is_private(location) && !IS_TRUSTED(ch, IMPLEMENTOR)) {
            act("\$p doesn't seem to go anywhere.", ch, portal, null, TO_CHAR)
            return
        }

        if (ch.pcdata == null && IS_SET(ch.act, ACT_AGGRESSIVE) && IS_SET(location.room_flags, ROOM_LAW)) {
            send_to_char("Something prevents you from leaving...\n", ch)
            return
        }

        val msg = when {
            mount != null -> "\$n steps into \$p, riding on ${mount.short_desc}."
            else -> "\$n steps into \$p."
        }
        act(msg, ch, portal, null, TO_ROOM)

        if (IS_SET(portal.value[2], GATE_NORMAL_EXIT)) {
            act("You enter \$p.", ch, portal, null, TO_CHAR)
        } else {
            act("You walk through \$p and find yourself somewhere else...", ch, portal, null, TO_CHAR)
        }

        char_from_room(ch)
        char_to_room(ch, location)

        if (IS_SET(portal.value[2], GATE_GOWITH)) { /* take the gate along */
            obj_from_room(portal)
            obj_to_room(portal, location)
        }

        if (IS_SET(portal.value[2], GATE_NORMAL_EXIT)) {
            if (mount != null) {
                act("\$n has arrived, riding \$N", ch, portal, mount, TO_ROOM)
            } else {
                act("\$n has arrived.", ch, portal, null, TO_ROOM)
            }
        } else {
            if (mount != null) {
                act("\$n has arrived through \$p, riding \$N.", ch, portal, mount, TO_ROOM)
            } else {
                act("\$n has arrived through \$p.", ch, portal, null, TO_ROOM)
            }
        }

        do_look(ch, "auto")

        if (mount != null) {
            char_from_room(mount)
            char_to_room(mount, location)
            ch.riding = true
            mount.riding = true
        }

        /* charges */
        if (portal.value[0] > 0) {
            portal.value[0]--
            if (portal.value[0] == 0L) {
                portal.value[0] = -1L
            }
        }

        /* protect against circular follows */
        for (fch in old_room.people) {
            if (portal.value[0] == -1L) {/* no following through dead portals */
                continue
            }

            if (fch.master == ch && IS_AFFECTED(fch, AFF_CHARM) && fch.position < Pos.Standing) {
                do_stand(fch, "")
            }

            if (fch.master == ch && fch.position == Pos.Standing) {
                if (IS_SET(ch.room.room_flags, ROOM_LAW) && fch.pcdata == null && IS_SET(fch.act, ACT_AGGRESSIVE)) {
                    act("You can't bring \$N into the city.", ch, null, fch, TO_CHAR)
                    act("You aren't allowed in the city.", fch, null, null, TO_CHAR)
                    continue
                }

                act("You follow \$N.", fch, null, ch, TO_CHAR)
                do_enter(fch, argument)
            }
        }

        if (portal.value[0] == -1L) {
            act("\$p fades out of existence.", ch, portal, null, TO_CHAR)
            if (ch.room == old_room) {
                act("\$p fades out of existence.", ch, portal, null, TO_ROOM)
            } else {
                act("\$p fades out of existence.", old_room, portal, null, TO_ROOM)
            }
            extract_obj(portal)
        }
        return
    }

    send_to_char("Nope, can't do it.\n", ch)
}

fun do_settraps(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.settraps, false, 0, "You don't know how to set traps.\n")) return

    if (IS_SET(ch.room.room_flags, ROOM_LAW)) {
        send_to_char("A mystical power protects the room.\n", ch)
        return
    }

    WAIT_STATE(ch, Skill.settraps.beats)

    if (ch.pcdata != null && randomPercent() >= get_skill(ch, Skill.settraps) * 0.7) {
        check_improve(ch, Skill.settraps, false, 1)
        return
    }

    check_improve(ch, Skill.settraps, true, 1)

    if (is_affected_room(ch.room, Skill.settraps)) {
        send_to_char("This room has already trapped.\n", ch)
        return
    }

    if (is_affected(ch, Skill.settraps)) {
        send_to_char("This skill is used too recently.\n", ch)
        return
    }
    val af = Affect()
    af.where = TO_ROOM_AFFECTS
    af.type = Skill.settraps
    af.level = ch.level
    af.duration = ch.level / 40
    af.bitvector = AFF_ROOM_THIEF_TRAP
    affect_to_room(ch.room, af)

    val af2 = Affect()
    af2.type = Skill.settraps
    af2.level = ch.level

    if (ch.last_fight_time != -1L && !IS_IMMORTAL(ch) && current_time - ch.last_fight_time < FIGHT_DELAY_TIME) {
        af2.duration = 1
    } else {
        af2.duration = ch.level / 10
    }

    affect_to_char(ch, af2)
    send_to_char("You set the room with your trap.\n", ch)
    act("\$n set the room with \$s trap.", ch, null, null, TO_ROOM)
}

fun get_char_area(ch: CHAR_DATA, argument: String): CHAR_DATA? {
    if (argument.isEmpty()) return null

    val arg1 = StringBuilder()
    val number = number_argument(argument, arg1)
    if (arg1.isEmpty()) {
        return null
    }

    val rch = get_char_room(ch, argument)
    if (rch != null) {
        return rch
    }
    return Index.CHARS
            .filter { it.room.area == ch.room.area && can_see(ch, it) && is_name(arg1.toString(), it.name) }
            .filterIndexed { count, _ -> count + 1 == number }
            .firstOrNull()
}


@Suppress("UNUSED_PARAMETER")
fun find_path(in_room_vnum: Int, out_room_vnum: Int, ch: CHAR_DATA, depth: Int, in_zone: Boolean): Int {
    //TODO:
    return -1
}


fun do_hunt(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.hunt, false, 0, null)) {
        return
    }
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Whom are you trying to hunt?\n", ch)
        return
    }

    /*  fArea = ( get_trust(ch) < MAX_LEVEL ); */
    var fArea = !IS_IMMORTAL(ch)

    if (randomPercent() < get_skill(ch, Skill.world_find)) {
        fArea = false
        check_improve(ch, Skill.world_find, true, 1)
    } else {
        check_improve(ch, Skill.world_find, false, 1)
    }

    val victim = if (fArea) get_char_area(ch, arg.toString()) else get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("No-one around by that name.\n", ch)
        return
    }

    if (ch.room == victim.room) {
        act("\$N is here!", ch, null, victim, TO_CHAR)
        return
    }

    if (ch.pcdata == null) {
        ch.hunting = victim
        hunt_victim(ch)
        return
    }

    /*
    * Deduct some movement.
    */
    if (!IS_IMMORTAL(ch)) {
        if (ch.endur > 2) {
            ch.endur -= 3
        } else {
            send_to_char("You're too exhausted to hunt anyone!\n", ch)
            return
        }
    }

    act("\$n stares intently at the ground.", ch, null, null, TO_ROOM)

    WAIT_STATE(ch, Skill.hunt.beats)
    var direction = find_path(ch.room.vnum, victim.room.vnum, ch, -40000, fArea)

    if (direction == -1) {
        act("You couldn't find a path to \$N from here.",
                ch, null, victim, TO_CHAR)
        return
    }

    if (direction < 0 || direction > 5) {
        send_to_char("Hmm... Something seems to be wrong.\n", ch)
        return
    }

    /*
    * Give a random direction if the player misses the die roll.
    */
    if (ch.pcdata == null && randomPercent() > 75)
    /* NPC @ 25% */ {
        log_string("Do PC hunt")
        var ok = false
        for (i in 0..5) {
            if (ch.room.exit[direction] != null) {
                ok = true
                break
            }
        }
        if (ok) {
            do {
                direction = randomDir()
            } while (ch.room.exit[direction] == null)
        } else {
            log_string("Do hunt, player hunt, no exits from room!")
            ch.hunting = null
            send_to_char("Your room has not exits!!!!\n", ch)
            return
        }
    }
    act("\$N is ${dir_name[direction]} from here.", ch, null, victim, TO_CHAR)
}

fun hunt_victim(ch: CHAR_DATA) {
    val victim = ch.hunting ?: return
    val dir = find_path(ch.room.vnum, victim.room.vnum, ch, -40000, true)

    /* Make sure the victim still exists.*/
    val found = Index.CHARS.contains(victim)
    if (!found || !can_see(ch, victim)) {
        if (get_char_area(ch, victim.name) != null) {
            log_string("mob portal")
            do_cast(ch, "portal ${victim.name}")
            log_string("do_enter1")
            do_enter(ch, "portal")
            hunt_victim_check_room(ch, victim)
        } else {
            do_say(ch, "Ahhhh!  My prey is gone!!")
            ch.hunting = null
        }
        return
    }

    if (dir < 0 || dir > 5) {
        /* 1 */
        if (get_char_area(ch, victim.name) != null && ch.level > 35) {
            do_cast(ch, "portal ${victim.name}")
            do_enter(ch, "portal")
            hunt_victim_check_room(ch, victim)
        } else {
            act("\$n says 'I have lost \$M!'", ch, null, victim, TO_ROOM)
            ch.hunting = null
        }
        return
    } /* if dir < 0 or > 5 */


    val exit_dir = ch.room.exit[dir]
    if (exit_dir != null && IS_SET(exit_dir.exit_info, EX_CLOSED)) {
        do_open(ch, dir_name[dir])
        return
    }
    if (exit_dir == null) {
        log_string("BUG:  hunt through null door")
        ch.hunting = null
        return
    }
    move_char(ch, dir)
    hunt_victim_check_room(ch, victim)
}

private fun hunt_victim_check_room(ch: CHAR_DATA, victim: CHAR_DATA) {
    if (ch.room != victim.room) return
    act("\$n glares at \$N and says, 'Ye shall DIE!'", ch, null, victim, TO_NOTVICT)
    act("\$n glares at you and says, 'Ye shall DIE!'", ch, null, victim, TO_VICT)
    act("You glare at \$N and say, 'Ye shall DIE!", ch, null, victim, TO_CHAR)
    multi_hit(ch, victim, null)
    ch.hunting = null
}


/**
 * ************************************************************************
 * ***********************      repair.c       ******************************
 * *************************************************************************
 */
fun damage_to_obj(ch: CHAR_DATA, wield: Obj, worn: Obj, damage: Int) {

    if (damage == 0) {
        return
    }
    worn.condition -= damage

    act("{gThe \$p inflicts damage on \$P.{x", ch, wield, worn, TO_ROOM, Pos.Resting)

    if (worn.condition < 1) {
        act("{WThe \$P breaks into pieces.{x", ch, wield, worn, TO_ROOM, Pos.Resting)
        extract_obj(worn)
        return
    }


    if (IS_SET(wield.extra_flags, ITEM_ANTI_EVIL) && IS_SET(wield.extra_flags, ITEM_ANTI_NEUTRAL) && IS_SET(worn.extra_flags, ITEM_ANTI_EVIL) && IS_SET(worn.extra_flags, ITEM_ANTI_NEUTRAL)) {
        act("{g\$p doesn't want to fight against \$P.{x", ch, wield, worn, TO_ROOM, Pos.Resting)
        act("{g\$p removes itself from you!{x.", ch, wield, worn, TO_CHAR, Pos.Resting)
        act("{g\$p removes itself from \$n{x.", ch, wield, worn, TO_ROOM, Pos.Resting)
        unequip_char(ch, wield)
        return
    }

    if (IS_SET(wield.extra_flags, ITEM_ANTI_EVIL) && IS_SET(worn.extra_flags, ITEM_ANTI_EVIL)) {
        act("{gThe \$p worries for the damage to \$P.{x", ch, wield, worn, TO_ROOM, Pos.Resting)
    }
}


fun check_weapon_destroy(ch: CHAR_DATA, victim: CHAR_DATA, second: Boolean) {
    var chance = 0

    if (victim.pcdata == null || randomPercent() < 94) {
        return
    }

    val wield = get_wield_char(ch, second) ?: return
    val sn = get_weapon_sn(ch, second)
    val skill = get_skill(ch, sn)

    if (is_metal(wield)) {
        for (i in Wear.values()) {
            val destroy = get_eq_char(victim, i)
            //todo: recheck algo
            if (destroy == null
                    || randomPercent() > 95
                    || randomPercent() > 94
                    || randomPercent() > skill
                    || ch.level < victim.level - 10
                    || check_material(destroy, "platinum")
                    || destroy.pIndexData.limit != -1
                    || i.isHold() || i == Wear.Tattoo || i === Wear.StuckIn) {
                continue
            }

            chance += 20
            if (check_material(wield, "platinum") || check_material(wield, "titanium")) {
                chance += 5
            }

            if (is_metal(destroy)) {
                chance -= 20
            } else {
                chance += 20
            }

            chance += (ch.level - victim.level) / 5

            chance += (wield.level - destroy.level) / 2

            /* sharpness    */
            if (IS_WEAPON_STAT(wield, WEAPON_SHARP)) {
                chance += 10
            }

            if (sn == Skill.axe) {
                chance += 10
            }
            /* spell affects */
            if (IS_OBJ_STAT(destroy, ITEM_BLESS)) {
                chance -= 10
            }
            if (IS_OBJ_STAT(destroy, ITEM_MAGIC)) {
                chance -= 20
            }

            chance += skill - 85
            chance += ch.curr_stat(PrimeStat.Strength)

            /*   chance /= 2;   */
            if (randomPercent() < chance && chance > 50) {
                damage_to_obj(ch, wield, destroy, chance / 5)
                break
            }
        }
    } else {
        for (w in Wear.values()) {
            val destroy = get_eq_char(victim, w)
            //todo: recheck algo
            if (destroy == null
                    || randomPercent() > 95
                    || randomPercent() > 94
                    || randomPercent() < skill
                    || ch.level < victim.level - 10
                    || check_material(destroy, "platinum")
                    || destroy.pIndexData.limit != -1
                    || w.isHold() || w == Wear.Tattoo || w == Wear.StuckIn) {
                continue
            }

            chance += 10
            if (is_metal(destroy)) {
                chance -= 20
            }
            chance += ch.level - victim.level
            chance += wield.level - destroy.level

            /* sharpness    */
            if (IS_WEAPON_STAT(wield, WEAPON_SHARP)) {
                chance += 10
            }

            if (sn == Skill.axe) {
                chance += 10
            }

            /* spell affects */
            if (IS_OBJ_STAT(destroy, ITEM_BLESS)) {
                chance -= 10
            }
            if (IS_OBJ_STAT(destroy, ITEM_MAGIC)) {
                chance -= 20
            }

            chance += skill - 85
            chance += ch.curr_stat(PrimeStat.Strength)

            /*   chance /= 2;   */
            if (randomPercent() < chance && chance > 50) {
                damage_to_obj(ch, wield, destroy, chance / 5)
                break
            }
        }
    }
}


fun do_repair(ch: CHAR_DATA, argument: String) {
    val mob: CHAR_DATA? = ch.room.people.firstOrNull { it.pcdata == null && it.spec_fun === spec_lookup("spec_repairman") }
    if (mob == null) {
        send_to_char("You can't do that here.\n", ch)
        return
    }

    val argb = StringBuilder()
    one_argument(argument, argb)
    val arg = argb.toString()

    if (arg.isEmpty()) {
        do_say(mob, "I will repair a weapon for you, for a price.")
        send_to_char("Type estimate <weapon> to be assessed for damage.\n", ch)
        return
    }
    val obj = get_obj_carry(ch, arg)
    if (obj == null) {
        do_say(mob, "You don't have that item")
        return
    }

    if (obj.pIndexData.vnum == OBJ_VNUM_HAMMER) {
        do_say(mob, "That hammer is beyond my power.")
        return
    }

    if (obj.condition >= 100) {
        do_say(mob, "But that item is not broken.")
        return
    }

    if (obj.cost == 0) {
        do_say(mob, "${obj.short_desc} is beyond repair.\n")
        return
    }

    var cost = obj.level * 10 + obj.cost * (100 - obj.condition) / 100
    cost /= 100

    if (cost > ch.gold) {
        do_say(mob, "You do not have enough gold for my services.")
        return
    }

    WAIT_STATE(ch, PULSE_VIOLENCE)

    ch.gold -= cost
    mob.gold += cost
    act("\$N takes ${obj.short_desc} from \$n, repairs it, and returns it to \$n", ch, null, mob, TO_ROOM)
    send_to_char("${mob.short_desc} takes ${obj.short_desc}, repairs it, and returns it\n", ch)
    obj.condition = 100
}

fun do_estimate(ch: CHAR_DATA, argument: String) {
    val mob: CHAR_DATA? = ch.room.people.firstOrNull { it.pcdata == null && it.spec_fun === spec_lookup("spec_repairman") }
    if (mob == null) {
        send_to_char("You can't do that here.\n", ch)
        return
    }
    val argb = StringBuilder()
    one_argument(argument, argb)
    val arg = argb.toString()

    if (arg.isEmpty()) {
        do_say(mob, "Try estimate <item>")
        return
    }
    val obj = get_obj_carry(ch, arg)
    if (obj == null) {
        do_say(mob, "You don't have that item")
        return
    }
    if (obj.pIndexData.vnum == OBJ_VNUM_HAMMER) {
        do_say(mob, "That hammer is beyond my power.")
        return
    }
    if (obj.condition >= 100) {
        do_say(mob, "But that item's not broken")
        return
    }
    if (obj.cost == 0) {
        do_say(mob, "That item is beyond repair")
        return
    }

    var cost = obj.level * 10 + obj.cost * (100 - obj.condition) / 100
    cost /= 100
    do_say(mob, "It will cost $cost to fix that item")
}

fun check_shield_destroyed(ch: CHAR_DATA, victim: CHAR_DATA, second: Boolean) {
    val destroy = get_shield_char(victim)
    var chance = 0

    if (victim.pcdata == null || randomPercent() < 94) {
        return
    }

    val wield = get_wield_char(ch, second) ?: return
    val sn = get_weapon_sn(ch, second)
    val skill = get_skill(ch, sn)

    if (destroy == null) {
        return
    }

    if (is_metal(wield)) {
        if (randomPercent() > 94
                || randomPercent() > skill
                || ch.level < victim.level - 10
                || check_material(destroy, "platinum")
                || destroy.pIndexData.limit != -1) {
            return
        }

        chance += 20
        if (check_material(wield, "platinum") || check_material(wield, "titanium")) {
            chance += 5
        }

        if (is_metal(destroy)) {
            chance -= 20
        } else {
            chance += 20
        }

        chance += (ch.level - victim.level) / 5

        chance += (wield.level - destroy.level) / 2

        /* sharpness    */
        if (IS_WEAPON_STAT(wield, WEAPON_SHARP)) {
            chance += 10
        }

        if (sn == Skill.axe) {
            chance += 10
        }
        /* spell affects */
        if (IS_OBJ_STAT(destroy, ITEM_BLESS)) {
            chance -= 10
        }
        if (IS_OBJ_STAT(destroy, ITEM_MAGIC)) {
            chance -= 20
        }

        chance += skill - 85
        chance += ch.curr_stat(PrimeStat.Strength)

        /*   chance /= 2;   */
        if (randomPercent() < chance && chance > 20) {
            damage_to_obj(ch, wield, destroy, chance / 4)
        }
    } else {
        if (randomPercent() > 94
                || randomPercent() < skill
                || ch.level < victim.level - 10
                || check_material(destroy, "platinum")
                || destroy.pIndexData.limit != -1) {
            return
        }

        chance += 10

        if (is_metal(destroy)) {
            chance -= 20
        }

        chance += ch.level - victim.level

        chance += wield.level - destroy.level

        /* sharpness    */
        if (IS_WEAPON_STAT(wield, WEAPON_SHARP)) {
            chance += 10
        }

        if (sn == Skill.axe) {
            chance += 10
        }

        /* spell affects */
        if (IS_OBJ_STAT(destroy, ITEM_BLESS)) {
            chance -= 10
        }
        if (IS_OBJ_STAT(destroy, ITEM_MAGIC)) {
            chance -= 20
        }

        chance += skill - 85
        chance += ch.curr_stat(PrimeStat.Strength)

        /*   chance /= 2;   */
        if (randomPercent() < chance && chance > 20) {
            damage_to_obj(ch, wield, destroy, chance / 4)
        }
    }
}

fun check_weapon_destroyed(ch: CHAR_DATA, victim: CHAR_DATA, second: Boolean) {
    val destroy = get_wield_char(victim, false)
    var chance = 0

    if (victim.pcdata == null || randomPercent() < 94) {
        return
    }

    val wield = get_wield_char(ch, second) ?: return
    val sn = get_weapon_sn(ch, second)
    val skill = get_skill(ch, sn)

    if (destroy == null) {
        return
    }

    if (is_metal(wield)) {
        if (randomPercent() > 94
                || randomPercent() > skill
                || ch.level < victim.level - 10
                || check_material(destroy, "platinum")
                || destroy.pIndexData.limit != -1) {
            return
        }

        chance += 20
        if (check_material(wield, "platinum") || check_material(wield, "titanium")) {
            chance += 5
        }

        if (is_metal(destroy)) {
            chance -= 20
        } else {
            chance += 20
        }

        chance += (ch.level - victim.level) / 5

        chance += (wield.level - destroy.level) / 2

        /* sharpness    */
        if (IS_WEAPON_STAT(wield, WEAPON_SHARP)) {
            chance += 10
        }

        if (sn == Skill.axe) {
            chance += 10
        }
        /* spell affects */
        if (IS_OBJ_STAT(destroy, ITEM_BLESS)) {
            chance -= 10
        }
        if (IS_OBJ_STAT(destroy, ITEM_MAGIC)) {
            chance -= 20
        }

        chance += skill - 85
        chance += ch.curr_stat(PrimeStat.Strength)

        /*   chance /= 2;   */
        if (randomPercent() < chance / 2 && chance > 20) {
            damage_to_obj(ch, wield, destroy, chance / 4)
        }
    } else {
        if (randomPercent() > 94
                || randomPercent() < skill
                || ch.level < victim.level - 10
                || check_material(destroy, "platinum")
                || destroy.pIndexData.limit != -1) {
            return
        }

        chance += 10

        if (is_metal(destroy)) {
            chance -= 20
        }

        chance += ch.level - victim.level

        chance += wield.level - destroy.level

        /* sharpness    */
        if (IS_WEAPON_STAT(wield, WEAPON_SHARP)) {
            chance += 10
        }

        if (sn == Skill.axe) {
            chance += 10
        }

        /* spell affects */
        if (IS_OBJ_STAT(destroy, ITEM_BLESS)) {
            chance -= 10
        }
        if (IS_OBJ_STAT(destroy, ITEM_MAGIC)) {
            chance -= 20
        }

        chance += skill - 85
        chance += ch.curr_stat(PrimeStat.Strength)

        /*   chance /= 2;   */
        if (randomPercent() < chance / 2 && chance > 20) {
            damage_to_obj(ch, wield, destroy, chance / 4)
        }
    }
}


fun do_smithing(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.smithing, false, 0, null)) {
        return
    }

    if (ch.fighting != null) {
        send_to_char("Wait until the fight finishes.\n", ch)
        return
    }

    val argb = StringBuilder()
    one_argument(argument, argb)
    val arg = argb.toString()

    if (arg.isEmpty()) {
        send_to_char("Which object do you want to repair.\n", ch)
        return
    }

    val obj = get_obj_carry(ch, arg)
    if (obj == null) {
        send_to_char("You are not carrying that.\n", ch)
        return
    }

    if (obj.condition >= 100) {
        send_to_char("But that item is not broken.\n", ch)
        return
    }

    val hammer = get_hold_char(ch)
    if (hammer == null) {
        send_to_char("You are not holding a hammer.\n", ch)
        return
    }

    if (hammer.pIndexData.vnum != OBJ_VNUM_HAMMER) {
        send_to_char("That is not the correct hammer.\n", ch)
        return
    }

    WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
    if (randomPercent() > get_skill(ch, Skill.smithing)) {
        check_improve(ch, Skill.smithing, false, 8)
        act("\$n try to repair ${obj.short_desc} with the hammer.But \$n fails.", ch, null, obj, TO_ROOM)
        send_to_char("You failed to repair ${obj.short_desc}\n", ch)
        hammer.condition -= 25
    } else {
        check_improve(ch, Skill.smithing, true, 4)
        act("\$n repairs ${obj.short_desc} with the hammer.", ch, null, null, TO_ROOM)
        send_to_char("You repair ${obj.short_desc}\n", ch)
        obj.condition = Math.max(100, obj.condition + get_skill(ch, Skill.smithing) / 2)
        hammer.condition -= 25
    }
    if (hammer.condition < 1) {
        extract_obj(hammer)
    }
}

/**
 * ************************************************************************
 * This snippet was originally written by Erwin S. Andreasen.              *
 * erwin@pip.dknet.dk, http://pip.dknet.dk/~pip1773/                  *
 * Adopted to Nightworks MUD by chronos.                                    *
 * *************************************************************************
 */


fun talk_auction(argument: String) = Index.PLAYERS
        .filter { !IS_SET(it.o_ch.comm, COMM_NOAUCTION) }
        .forEach { act("AUCTION: $argument", it.o_ch, null, null, TO_CHAR) }

/*
This function allows the following kinds of bets to be made:

Absolute bet
============

bet 14k, bet 50m66, bet 100k

Relative bet
============

These bets are calculated relative to the current bet. The '+' symbol adds
a certain number of percent to the current bet. The default is 25, so
with a current bet of 1000, bet + gives 1250, bet +50 gives 1500 etc.
Please note that the number must follow exactly after the +, without any
spaces!

The '*' or 'x' bet multiplies the current bet by the number specified,
defaulting to 2. If the current bet is 1000, bet x  gives 2000, bet x10
gives 10,000 etc.

*/

fun advatoi(s: String): Int {
    /*
util function, converts an 'advanced' ASCII-number-string into a number.
Used by parsebet() but could also be used by do_give or do_wimpy.

Advanced strings can contain 'k' (or 'K') and 'm' ('M') in them, not just
numbers. The letters multiply whatever is left of them by 1,000 and
1,000,000 respectively. Example:

14k = 14 * 1,000 = 14,000
23m = 23 * 1,000,0000 = 23,000,000

If any digits follow the 'k' or 'm', the are also added, but the number
which they are multiplied is divided by ten, each time we get one left. This
is best illustrated in an example :)

14k42 = 14 * 1000 + 14 * 100 + 2 * 10 = 14420

Of course, it only pays off to use that notation when you can skip many 0's.
There is not much point in writing 66k666 instead of 66666, except maybe
when you want to make sure that you get 66,666.

More than 3 (in case of 'k') or 6 ('m') digits after 'k'/'m' are automatically
disregarded. Example:

14k1234 = 14,123

If the number contains any other characters than digits, 'k' or 'm', the
function returns 0. It also returns 0 if 'k' or 'm' appear more than
once.

*/

    /* the pointer to buffer stuff is not really necessary, but originally I
modified the buffer, so I had to make a copy of it. What the hell, it
works:) (read: it seems to work:)
*/

    var number = 0           /* number to be returned */
    var multiplier: Int       /* multiplier used to get the extra digits right */


    var pos = 0
    while (pos < s.length && isDigit(s[pos])) {/* as long as the current character is a digit */
        number = number * 10 + atoi(s.substring(pos, pos + 1)) /* add to current number */
        pos++                                /* advance */
    }
    if (pos >= s.length - 1) {
        return number
    }

    when (s[pos]) {
        'k', 'K' -> {
            multiplier = 1000
            number *= multiplier
            pos++
        }
        'm', 'M' -> {
            multiplier = 1000000
            number *= multiplier
            pos++
        }
        else -> return 0 /* not k nor m nor NUL - return 0! */
    }

    while (pos < s.length && isDigit(s[pos]) && multiplier > 1)
    /* if any digits follow k/m, add those too */ {
        multiplier /= 10  /* the further we get to right, the less are the digit 'worth' */
        number += atoi(s.substring(pos, pos + 1)) * multiplier
        pos++
    }
    return if (pos != s.length - 1) 0 else number
}


fun parseBet(currentBet: Int, argument: String): Int {
    var newBet = 0                /* a variable to temporarily hold the new bet */
    if (argument.isNotEmpty()) {
        if (Character.isDigit(argument[0])) { /* first char is a digit assume e.g. 433k */
            newBet = advatoi(argument) /* parse and set newbet to that value */
        } else if (argument[0] == '+') { /* add ?? percent */
            newBet = when {
                argument.length == 1 -> currentBet * 125 / 100 /* default: add 25% */
                else -> currentBet * (100 + atoi(argument.substring(1))) / 100 /* cut off the first char */
            }
        } else {
            if (argument[0] == '*' || argument[0] == 'x') {
                newBet = when {
                    argument.length == 1 -> currentBet * 2 /* default: twice */
                    else -> currentBet * atoi(argument.substring(1)) /* cut off the first char */
                }
            }
        }
    }
    return newBet        /* return the calculated bet */
}


fun auction_update() {
    val a = auction
    if (a != null) {
        if (--a.pulse <= 0)
        /* decrease pulse */ {
            a.pulse = PULSE_AUCTION
            when (++a.going) {
                1, 2 -> {
                    val msg = when {
                        a.bet > 0 -> "${a.item.short_desc}: going ${if (a.going == 1) "once" else "twice"} for ${a.bet}."
                        else -> "${a.item.short_desc}: going ${if (a.going == 1) "once" else "twice"} (not bet received yet)."
                    }
                    talk_auction("{c$msg{x")
                }

                3 -> {  /* DONE! */
                    val buyer = a.buyer
                    if (buyer != null) {
                        val msg = "${a.item.short_desc} sold to ${if (buyer.pcdata == null) buyer.short_desc else buyer.name} for ${a.bet}."
                        talk_auction("{c$msg{x")
                        obj_to_char(a.item, buyer)
                        act("The auctioneer appears before you in a puff of smoke and hands you \$p.", buyer, a.item, null, TO_CHAR)
                        act("The auctioneer appears before \$n, and hands \$m \$p", buyer, a.item, null, TO_ROOM)
                        a.seller.gold += a.bet /* give him the money */
                    } else {
                        talk_auction("{cNo bets received for ${a.item.short_desc} - object has been removed.{x")
                        talk_auction("{cThe auctioneer puts the unsold item to his pit.{x")
                        extract_obj(a.item)
                    }
                }
            }
            auction = null
        }
    }
}


fun do_auction(ch: CHAR_DATA, argument: String) {

    /* NPC extracted can't auction! */
    if (ch.pcdata == null) {
        return
    }

    val arg1 = StringBuilder()
    val rest = one_argument(argument, arg1)

    if (IS_SET(ch.comm, COMM_NOAUCTION)) {
        if (eq(arg1.toString(), "on")) {
            send_to_char("Auction channel is now ON.\n", ch)
            ch.comm = REMOVE_BIT(ch.comm, COMM_NOAUCTION)
        } else {
            send_to_char("Your auction channel is OFF.\n", ch)
            send_to_char("You must first change auction channel ON.\n", ch)
        }
        return
    }
    val a = auction
    val buyer = a?.buyer
    when (arg1.toString().toLowerCase()) {
        "" -> {
            if (a != null) {
                val msg = when {
                    a.bet > 0 -> "Current bet on this item is ${a.bet} gold.\n"
                    else -> "No bets on this item have been received.\n"
                }
                send_to_char("{g$msg{x", ch)
                spell_identify(ch, a.item)
            } else {
                send_to_char("{rAuction WHAT?{x\n", ch)
            }
        }
        "off" -> {
            send_to_char("Auction channel is now OFF.\n", ch)
            ch.comm = SET_BIT(ch.comm, COMM_NOAUCTION)
        }
        "stop" -> {
            if (a == null) {
                send_to_char("There is no auction going on you can stop.\n", ch)
            } else if (IS_IMMORTAL(ch)) {
                talk_auction("{WSale of ${a.item.short_desc} has been stopped by God. Item confiscated.{x")
                obj_to_char(a.item, a.seller)
                if (buyer != null) {
                    buyer.gold += a.bet
                    send_to_char("Your money has been returned.\n", buyer)
                }
                auction = null
            }
        }
        "bet" -> {
            if (a != null) {
                val newBet = parseBet(a.bet, rest)
                if (ch == a.seller) {
                    send_to_char("You cannot bet on your own selling equipment.\n", ch)
                    return
                }

                /* make - perhaps - a bet now */
                if (rest.isEmpty()) {
                    send_to_char("Bet how much?\n", ch)
                    return
                }

                if (newBet < a.bet + 1) {
                    send_to_char("You must at least bid 1 gold over the current bet.\n", ch)
                    return
                }

                if (newBet > ch.gold) {
                    send_to_char("You don't have that much money!\n", ch)
                    return
                }

                /* the actual bet is OK! */

                /* return the gold to the last buyer, if one exists */
                if (buyer != null) {
                    buyer.gold += a.bet
                }

                ch.gold -= newBet /* substract the gold - important :) */
                a.buyer = ch
                a.bet = newBet
                a.going = 0
                a.pulse = PULSE_AUCTION /* start the auction over again */
                talk_auction("{mA bet of $newBet gold has been received on ${a.item.short_desc}.\n{x")
            } else {
                send_to_char("There isn't anything being auctioned right now.\n", ch)
            }
        }
    /* finally... */
    /* does char have the item ? */
        else -> {
            val obj = get_obj_carry(ch, arg1.toString()) /* does char have the item ? */
            when {
                obj == null -> send_to_char("You aren't carrying that.\n", ch)
                obj.pIndexData.vnum < 100 -> send_to_char("You cannot auction that item.\n", ch)
                else -> {
                    for (c in Clan.values()) {
                        if (obj.pIndexData.vnum == c.obj_vnum) {
                            send_to_char("Gods are furied upon your request.\n", ch)
                            return
                        }
                    }

                    if (a == null) {
                        when (obj.item_type) {
                            ItemType.Weapon, ItemType.Armor, ItemType.Staff, ItemType.Wand, ItemType.Scroll -> {
                                obj_from_char(obj)
                                auction = Auction(obj, ch)
                                talk_auction("{rA new item has been received: ${obj.short_desc}.{x")
                            }
                            else -> act("{rYou cannot auction \$Ts.{x", ch, null, obj.item_type.displayName, TO_CHAR, Pos.Sleeping)
                        }
                    } else {
                        act("Try again later - \$p is being auctioned right now!", ch, a.item, null, TO_CHAR)
                    }
                }
            }

        }
    }
}
