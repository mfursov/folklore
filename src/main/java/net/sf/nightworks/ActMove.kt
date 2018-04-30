package net.sf.nightworks

import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.act.info.do_look
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.EXIT_DATA
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.ROOM_INDEX_DATA
import net.sf.nightworks.model.Sex
import net.sf.nightworks.model.WeaponType
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.model.max
import net.sf.nightworks.model.todam
import net.sf.nightworks.model.tohit
import net.sf.nightworks.util.GET_HITROLL
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_AWAKE
import net.sf.nightworks.util.IS_BLINK_ON
import net.sf.nightworks.util.IS_DRUNK
import net.sf.nightworks.util.IS_HARA_KIRI
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_RAFFECTED
import net.sf.nightworks.util.IS_ROOM_AFFECTED
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_TRUSTED
import net.sf.nightworks.util.IS_VAMPIRE
import net.sf.nightworks.util.IS_WATER
import net.sf.nightworks.util.IS_WEAPON_STAT
import net.sf.nightworks.util.MOUNTED
import net.sf.nightworks.util.PERS
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.RIDDEN
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.capitalize
import net.sf.nightworks.util.dice
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.flipCoin
import net.sf.nightworks.util.get_carry_weight
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.randomPercent
import java.util.Formatter


/** movement loss by sector type */
val movement_loss = intArrayOf(1, 2, 2, 3, 4, 6, 4, 1, 6, 10, 6)

fun move_char(ch: CHAR_DATA, dir: Int) {
    var door = dir
    var ch_mount = RIDDEN(ch)
    if (ch_mount != null) {
        move_char(ch_mount, door)
        return
    }

    ch_mount = MOUNTED(ch)
    if (IS_AFFECTED(ch, AFF_WEB) || (ch_mount != null && IS_AFFECTED(ch_mount, AFF_WEB))) {
        WAIT_STATE(ch, PULSE_VIOLENCE)
        if (randomPercent() < ch.tohit * 5) {
            affect_strip(ch, Skill.web)
            ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_WEB)
            send_to_char("When you attempt to leave the room, you break the webs holding you tight.\n", ch)
            act("\$n struggles against the webs which hold \$m in place, and break it.", ch, null, null, TO_ROOM)
        } else {
            send_to_char("You attempt to leave the room, but the webs hold you tight.\n", ch)
            act("\$n struggles vainly against the webs which hold \$m in place.", ch, null, null, TO_ROOM)
            return
        }
    }

    if (door < 0 || door > 5) {
        bug("Do_move: bad door %d.", door)
        return
    }

    if (IS_AFFECTED(ch, AFF_HIDE) && !IS_AFFECTED(ch, AFF_SNEAK) || IS_AFFECTED(ch, AFF_FADE) && !IS_AFFECTED(ch, AFF_SNEAK)) {
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_HIDE)
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_FADE)
        send_to_char("You step out of shadows.\n", ch)
        act("\$n steps out of shadows.", ch, null, null, TO_ROOM)
    }
    if (IS_AFFECTED(ch, AFF_CAMOUFLAGE)) {
        if (randomPercent() < get_skill(ch, Skill.camouflage_move)) {
            check_improve(ch, Skill.camouflage_move, true, 5)
        } else {
            ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_CAMOUFLAGE)
            send_to_char("You step out from your cover.\n", ch)
            act("\$n steps out from \$m's cover.", ch, null, null, TO_ROOM)
            check_improve(ch, Skill.camouflage_move, false, 5)
        }
    }
    if (IS_AFFECTED(ch, AFF_EARTHFADE)) {
        send_to_char("You fade to your neutral form.\n", ch)
        act("Earth forms \$n in front of you.", ch, null, null, TO_ROOM)
        affect_strip(ch, Skill.earthfade)
        WAIT_STATE(ch, PULSE_VIOLENCE / 2)
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_EARTHFADE)
    }

    val in_room = ch.room
    var exit: EXIT_DATA? = in_room.exit[door]
    var to_room: ROOM_INDEX_DATA? = if (exit == null) null else exit.to_room
    if (to_room == null || !can_see_room(ch, exit!!.to_room)) {
        send_to_char("Alas, you cannot go that way.\n", ch)
        return
    }

    if (IS_ROOM_AFFECTED(in_room, AFF_ROOM_RANDOMIZER)) {
        for (i in 0..999) {//todo ???
            val d0 = number_range(0, 5)
            val newExit = in_room.exit[d0]
            if (newExit == null || !can_see_room(ch, newExit.to_room)) {
                continue
            }
            exit = newExit
            to_room = exit.to_room
            door = d0
            break
        }
    }

    if (IS_SET(exit!!.exit_info, EX_CLOSED) && (!IS_AFFECTED(ch, AFF_PASS_DOOR) || IS_SET(exit.exit_info, EX_NO_PASS)) && !IS_TRUSTED(ch, ANGEL)) {
        if (IS_AFFECTED(ch, AFF_PASS_DOOR) && IS_SET(exit.exit_info, EX_NO_PASS)) {
            act("You failed to pass through the \$d.", ch, null, exit.keyword, TO_CHAR)
            act("\$n tries to pass through the \$d, but \$e fails", ch, null, exit.keyword, TO_ROOM)
        } else {
            act("The \$d is closed.", ch, null, exit.keyword, TO_CHAR)
        }
        return
    }

    val ch_master = ch.master
    if (IS_AFFECTED(ch, AFF_CHARM) && ch_master != null && in_room == ch_master.room) {
        send_to_char("What?  And leave your beloved master?\n", ch)
        return
    }

    /*    if ( !is_room_owner(ch,to_room) && room_is_private( to_room ) )   */
    if (room_is_private(to_room!!)) {
        send_to_char("That room is private right now.\n", ch)
        return
    }

    if (ch_mount != null) {
        if (ch_mount.position < Pos.Fighting) {
            send_to_char("Your mount must be standing.\n", ch)
            return
        }
        if (!mount_success(ch, ch_mount, false)) {
            send_to_char("Your mount subbornly refuses to go that way.\n", ch)
            return
        }
    }

    if (ch.pcdata != null) {
        for (c in Clazz.classes) {
            for (gvnum in c.guildVnums) {
                if (to_room.vnum == gvnum && !IS_IMMORTAL(ch)) {
                    if (c !== ch.clazz) {
                        send_to_char("You aren't allowed in there.\n", ch)
                        return
                    }
                    if (ch.last_fight_time != -1L && current_time - ch.last_fight_time < FIGHT_DELAY_TIME) {
                        send_to_char("You feel too bloody to go in there now.\n", ch)
                        return
                    }
                }
            }
        }

        if (in_room.sector_type == SECT_AIR || to_room.sector_type == SECT_AIR) {
            if (ch_mount != null) {
                if (!IS_AFFECTED(ch_mount, AFF_FLYING)) {
                    send_to_char("Your mount can't fly.\n", ch)
                    return
                }
            } else if (!IS_AFFECTED(ch, AFF_FLYING) && !IS_IMMORTAL(ch)) {
                send_to_char("You can't fly.\n", ch)
                return
            }
        }

        if ((in_room.sector_type == SECT_WATER_NO_SWIM || to_room.sector_type == SECT_WATER_NO_SWIM) && ch_mount != null && !IS_AFFECTED(ch_mount, AFF_FLYING)) {
            send_to_char("You can't take your mount there.\n", ch)
            return
        }

        if ((in_room.sector_type == SECT_WATER_NO_SWIM || to_room.sector_type == SECT_WATER_NO_SWIM) && ch_mount == null && !IS_AFFECTED(ch, AFF_FLYING)) {
            /*
                * Look for a boat.
                */
            var found = IS_IMMORTAL(ch)
            for (obj in ch.carrying) {
                if (obj.item_type == ItemType.Boat) {
                    found = true
                    break
                }
            }
            if (!found) {
                send_to_char("You need a boat to go there.\n", ch)
                return
            }
        }

        var move = movement_loss[Math.min(SECT_MAX - 1, in_room.sector_type)] + movement_loss[Math.min(SECT_MAX - 1, to_room.sector_type)]
        move /= 2  /* i.e. the average */

        /* conditional effects */
        if (IS_AFFECTED(ch, AFF_FLYING) || IS_AFFECTED(ch, AFF_HASTE)) {
            move /= 2
        }

        if (IS_AFFECTED(ch, AFF_SLOW)) {
            move *= 2
        }

        if (ch_mount == null && ch.move < move) {
            send_to_char("You are too exhausted.\n", ch)
            return
        }

        if (ch_mount == null && (ch.room.sector_type == SECT_DESERT || IS_WATER(ch.room))) {
            WAIT_STATE(ch, 2)
        } else {
            WAIT_STATE(ch, 1)
        }

        if (ch_mount == null) {
            ch.move -= move
        }
    }

    if (!IS_AFFECTED(ch, AFF_SNEAK) && !IS_AFFECTED(ch, AFF_CAMOUFLAGE) && ch.invis_level < LEVEL_HERO) {
        val buf = Formatter()
        if (ch.pcdata != null && ch.room.sector_type != SECT_INSIDE &&
                ch.room.sector_type != SECT_CITY && randomPercent() < get_skill(ch, Skill.quiet_movement)) {
            when {
                ch_mount != null -> buf.format("\$n leaves, riding on %s.", ch_mount.short_desc)
                else -> buf.format("\$n leaves.")
            }
            check_improve(ch, Skill.quiet_movement, true, 1)
        } else {
            when {
                ch_mount != null -> buf.format("\$n leaves \$T, riding on %s.", ch_mount.short_desc)
                else -> buf.format("\$n leaves \$T.")
            }
        }
        act(buf.toString(), ch, null, dir_name[door], TO_ROOM)
    }

    if (IS_AFFECTED(ch, AFF_CAMOUFLAGE)
            && to_room.sector_type != SECT_FIELD
            && to_room.sector_type != SECT_FOREST
            && to_room.sector_type != SECT_MOUNTAIN
            && to_room.sector_type != SECT_HILLS) {
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_CAMOUFLAGE)
        send_to_char("You step out from your cover.\n", ch)
        act("\$n steps out from \$m's cover.", ch, null, null, TO_ROOM)
    }

    if (IS_AFFECTED(ch, AFF_HIDE) && (to_room.sector_type == SECT_FOREST || to_room.sector_type == SECT_FIELD)) {
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_HIDE)
        send_to_char("You step out of shadows.\n", ch)
        act("\$n steps out of shadows.", ch, null, null, TO_ROOM)
    }

    char_from_room(ch)
    char_to_room(ch, to_room)

    if (ch.room != to_room) {
        bug("Is char dead!")
        return
    }

    /* room record for tracking */
    if (ch.pcdata != null) {
        room_record(ch.name, in_room, door)
    }


    if (!IS_AFFECTED(ch, AFF_SNEAK) && ch.invis_level < LEVEL_HERO) {
        when {
            ch_mount != null -> act("\$n has arrived, riding \$N.", ch, null, ch_mount, TO_ROOM)
            else -> act("\$n has arrived.", ch, null, null, TO_ROOM)
        }
    }

    do_look(ch, "auto")

    if (ch_mount != null) {
        char_from_room(ch_mount)
        char_to_room(ch_mount, to_room)
        ch.riding = true
        ch_mount.riding = true
    }

    if (in_room == to_room)
    /* no circular follows */ {
        return
    }


    val room_has_pc = to_room.people.any { it.pcdata != null }

    if (room_has_pc) {
        for (fch in to_room.people) {
            /* greet progs for items carried by people in room */
            fch.carrying
                    .filter { IS_SET(it.progtypes, OPROG_GREET) }
                    .forEach { it.pIndexData.oprogs.greet_prog!!(it, ch) }


            /* greet programs for npcs  */
            if (IS_SET(fch.progtypes, MPROG_GREET)) {
                fch.pIndexData.mprogs.greet_prog!!(fch, ch)
            }
        }

        /* entry programs for items */
        ch.carrying
                .filter { IS_SET(it.progtypes, OPROG_ENTRY) }
                .forEach { it.pIndexData.oprogs.entry_prog!!(it) }
    }

    for (fch in in_room.people) {
        if (fch.master == ch && IS_AFFECTED(fch, AFF_CHARM) && fch.position < Pos.Standing) {
            do_stand(fch, "")
        }

        if (fch.master == ch && fch.position == Pos.Standing && can_see_room(fch, to_room)) {
            if (IS_SET(ch.room.room_flags, ROOM_LAW) && fch.pcdata == null && IS_SET(fch.act, ACT_AGGRESSIVE)) {
                act("You can't bring \$N into the city.", ch, null, fch, TO_CHAR)
                act("You aren't allowed in the city.", fch, null, null, TO_CHAR)
                continue
            }
            act("You follow \$N.", fch, null, ch, TO_CHAR)
            move_char(fch, door)
        }
    }

    if (room_has_pc) {
        ch.room.objects
                .filter { IS_SET(it.progtypes, OPROG_GREET) }
                .forEach { it.pIndexData.oprogs.greet_prog!!(it, ch) }
    }
    if (IS_SET(ch.progtypes, MPROG_ENTRY)) {
        ch.pIndexData.mprogs.entry_prog!!(ch)
    }
}


fun do_north(ch: CHAR_DATA) = move_char(ch, DIR_NORTH)

fun do_east(ch: CHAR_DATA) = move_char(ch, DIR_EAST)

fun do_south(ch: CHAR_DATA) = move_char(ch, DIR_SOUTH)

fun do_west(ch: CHAR_DATA) = move_char(ch, DIR_WEST)

fun do_up(ch: CHAR_DATA) = move_char(ch, DIR_UP)

fun do_down(ch: CHAR_DATA) = move_char(ch, DIR_DOWN)

fun find_exit(ch: CHAR_DATA, arg: String) = when {
    eq(arg, "n") || eq(arg, "north") -> 0
    eq(arg, "e") || eq(arg, "east") -> 1
    eq(arg, "s") || eq(arg, "south") -> 2
    eq(arg, "w") || eq(arg, "west") -> 3
    eq(arg, "u") || eq(arg, "up") -> 4
    eq(arg, "d") || eq(arg, "down") -> 5
    else -> {
        act("I see no exit \$T here.", ch, null, arg, TO_CHAR)
        -1
    }
}


fun find_door(ch: CHAR_DATA, arg: String): Int {
    var door: Int

    when {
        eq(arg, "n") || eq(arg, "north") -> door = 0
        eq(arg, "e") || eq(arg, "east") -> door = 1
        eq(arg, "s") || eq(arg, "south") -> door = 2
        eq(arg, "w") || eq(arg, "west") -> door = 3
        eq(arg, "u") || eq(arg, "up") -> door = 4
        eq(arg, "d") || eq(arg, "down") -> door = 5
        else -> {
            door = 0
            while (door <= 5) {
                val pexit = ch.room.exit[door]
                if (pexit != null
                        && IS_SET(pexit.exit_info, EX_IS_DOOR)
                        && pexit.keyword.isNotEmpty()
                        && is_name(arg, pexit.keyword)) {
                    return door
                }
                door++
            }
            act("I see no \$T here.", ch, null, arg, TO_CHAR)
            return -1
        }
    }

    val pexit = ch.room.exit[door]
    if (pexit == null) {
        act("I see no door \$T here.", ch, null, arg, TO_CHAR)
        return -1
    }

    if (!IS_SET(pexit.exit_info, EX_IS_DOOR)) {
        send_to_char("You can't do that.\n", ch)
        return -1
    }

    return door
}

/* scan.c */
val distance = arrayOf("right here.", "nearby to the %s.", "not far %s.", "off in the distance %s.")

fun do_scan2(ch: CHAR_DATA) {
    act("\$n looks all around.", ch, null, null, TO_ROOM)
    send_to_char("Looking around you see:\n", ch)
    scan_list(ch.room, ch, 0, -1)
    for (door in 0..5) {
        val pExit = ch.room.exit[door]
        if (pExit == null || IS_SET(pExit.exit_info, EX_CLOSED)) {
            continue
        }
        scan_list(pExit.to_room, ch, 1, door)
    }
}

fun scan_list(scan_room: ROOM_INDEX_DATA?, ch: CHAR_DATA, depth: Int, door: Int) {
    if (scan_room == null) {
        return
    }
    scan_room.people
            .filter { it != ch && !(it.pcdata != null && it.invis_level > get_trust(ch)) && can_see(ch, it) }
            .forEach { scan_char(it, ch, depth, door) }
}

fun scan_char(victim: CHAR_DATA, ch: CHAR_DATA, depth: Int, door: Int) {
    val buf = TextBuffer()
    buf.append(if (is_affected(victim, Skill.doppelganger) && !IS_SET(ch.act, PLR_HOLYLIGHT)) PERS(victim.doppel, ch) else PERS(victim, ch))
    buf.append(", ")

    buf.sprintf(distance[depth], dir_name[door])
    buf.append("\n")

    send_to_char(buf, ch)
}

fun do_open(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Open what?\n", ch)
        return
    }

    val obj = get_obj_here(ch, arg.toString())
    if (obj != null) {
        /* open portal */
        if (obj.item_type == ItemType.Portal) {
            if (!IS_SET(obj.value[1], EX_IS_DOOR)) {
                send_to_char("You can't do that.\n", ch)
                return
            }

            if (!IS_SET(obj.value[1], EX_CLOSED)) {
                send_to_char("It's already open.\n", ch)
                return
            }

            if (IS_SET(obj.value[1], EX_LOCKED)) {
                send_to_char("It's locked.\n", ch)
                return
            }

            obj.value[1] = REMOVE_BIT(obj.value[1], EX_CLOSED)
            act("You open \$p.", ch, obj, null, TO_CHAR)
            act("\$n opens \$p.", ch, obj, null, TO_ROOM)
            return
        }

        /* 'open object' */
        if (obj.item_type != ItemType.Container) {
            send_to_char("That's not a container.\n", ch)
            return
        }
        if (!IS_SET(obj.value[1], CONT_CLOSED)) {
            send_to_char("It's already open.\n", ch)
            return
        }
        if (!IS_SET(obj.value[1], CONT_CLOSEABLE)) {
            send_to_char("You can't do that.\n", ch)
            return
        }
        if (IS_SET(obj.value[1], CONT_LOCKED)) {
            send_to_char("It's locked.\n", ch)
            return
        }

        obj.value[1] = REMOVE_BIT(obj.value[1], CONT_CLOSED)
        act("You open \$p.", ch, obj, null, TO_CHAR)
        act("\$n opens \$p.", ch, obj, null, TO_ROOM)
        return
    }
    val door = find_door(ch, arg.toString())
    val pexit = if (door >= 0) ch.room.exit[door] else null
    if (pexit != null) {
        /* 'open door' */
        if (!IS_SET(pexit.exit_info, EX_CLOSED)) {
            send_to_char("It's already open.\n", ch)
            return
        }
        if (IS_SET(pexit.exit_info, EX_LOCKED)) {
            send_to_char("It's locked.\n", ch)
            return
        }

        pexit.exit_info = REMOVE_BIT(pexit.exit_info, EX_CLOSED)
        act("\$n opens the \$d.", ch, null, pexit.keyword, TO_ROOM)
        send_to_char("Ok.\n", ch)

        /* open the other side */
        val to_room = pexit.to_room
        val pexit_rev = to_room.exit[rev_dir[door]]
        if (pexit_rev != null && pexit_rev.to_room == ch.room) {
            pexit_rev.exit_info = REMOVE_BIT(pexit_rev.exit_info, EX_CLOSED)
            to_room.people.forEach { rch -> act("The \$d opens.", rch, null, pexit_rev.keyword, TO_CHAR) }
        }
    }
}


fun do_close(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Close what?\n", ch)
        return
    }

    val obj = get_obj_here(ch, arg.toString())
    if (obj != null) {
        /* portal stuff */
        if (obj.item_type == ItemType.Portal) {

            if (!IS_SET(obj.value[1], EX_IS_DOOR) || IS_SET(obj.value[1], EX_NO_CLOSE)) {
                send_to_char("You can't do that.\n", ch)
                return
            }

            if (IS_SET(obj.value[1], EX_CLOSED)) {
                send_to_char("It's already closed.\n", ch)
                return
            }

            obj.value[1] = SET_BIT(obj.value[1], EX_CLOSED)
            act("You close \$p.", ch, obj, null, TO_CHAR)
            act("\$n closes \$p.", ch, obj, null, TO_ROOM)
            return
        }

        /* 'close object' */
        if (obj.item_type != ItemType.Container) {
            send_to_char("That's not a container.\n", ch)
            return
        }
        if (IS_SET(obj.value[1], CONT_CLOSED)) {
            send_to_char("It's already closed.\n", ch)
            return
        }
        if (!IS_SET(obj.value[1], CONT_CLOSEABLE)) {
            send_to_char("You can't do that.\n", ch)
            return
        }

        obj.value[1] = SET_BIT(obj.value[1], CONT_CLOSED)
        act("You close \$p.", ch, obj, null, TO_CHAR)
        act("\$n closes \$p.", ch, obj, null, TO_ROOM)
        return
    }

    val door = find_door(ch, arg.toString())
    val pexit = if (door >= 0) ch.room.exit[door] else null
    if (pexit != null) {
        /* 'close door' */
        if (IS_SET(pexit.exit_info, EX_CLOSED)) {
            send_to_char("It's already closed.\n", ch)
            return
        }

        pexit.exit_info = SET_BIT(pexit.exit_info, EX_CLOSED)
        act("\$n closes the \$d.", ch, null, pexit.keyword, TO_ROOM)
        send_to_char("Ok.\n", ch)

        /* close the other side */
        val to_room = pexit.to_room
        val pexit_rev = to_room.exit[rev_dir[door]]
        if (pexit_rev != null && pexit_rev.to_room == ch.room) {
            pexit_rev.exit_info = SET_BIT(pexit_rev.exit_info, EX_CLOSED)
            to_room.people.forEach { rch -> act("The \$d closes.", rch, null, pexit_rev.keyword, TO_CHAR) }
        }
    }
}

fun has_key(ch: CHAR_DATA, key: Int) = ch.carrying.any { it.pIndexData.vnum == key && can_see_obj(ch, it) }

fun has_key_ground(ch: CHAR_DATA, key: Int) = ch.room.objects.any { it.pIndexData.vnum == key && can_see_obj(ch, it) }

fun do_lock(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Lock what?\n", ch)
        return
    }


    val obj = get_obj_here(ch, arg.toString())
    if (obj != null) {
        /* portal stuff */
        if (obj.item_type == ItemType.Portal) {
            if (!IS_SET(obj.value[1], EX_IS_DOOR) || IS_SET(obj.value[1], EX_NO_CLOSE)) {
                send_to_char("You can't do that.\n", ch)
                return
            }
            if (!IS_SET(obj.value[1], EX_CLOSED)) {
                send_to_char("It's not closed.\n", ch)
                return
            }

            if (obj.value[4] < 0 || IS_SET(obj.value[1], EX_NO_LOCK)) {
                send_to_char("It can't be locked.\n", ch)
                return
            }

            if (!has_key(ch, obj.value[4].toInt())) {
                send_to_char("You lack the key.\n", ch)
                return
            }

            if (IS_SET(obj.value[1], EX_LOCKED)) {
                send_to_char("It's already locked.\n", ch)
                return
            }

            obj.value[1] = SET_BIT(obj.value[1], EX_LOCKED)
            act("You lock \$p.", ch, obj, null, TO_CHAR)
            act("\$n locks \$p.", ch, obj, null, TO_ROOM)
            return
        }

        /* 'lock object' */
        if (obj.item_type != ItemType.Container) {
            send_to_char("That's not a container.\n", ch)
            return
        }
        if (!IS_SET(obj.value[1], CONT_CLOSED)) {
            send_to_char("It's not closed.\n", ch)
            return
        }
        if (obj.value[2] < 0) {
            send_to_char("It can't be locked.\n", ch)
            return
        }
        if (!has_key(ch, obj.value[2].toInt())) {
            send_to_char("You lack the key.\n", ch)
            return
        }
        if (IS_SET(obj.value[1], CONT_LOCKED)) {
            send_to_char("It's already locked.\n", ch)
            return
        }

        obj.value[1] = SET_BIT(obj.value[1], CONT_LOCKED)
        act("You lock \$p.", ch, obj, null, TO_CHAR)
        act("\$n locks \$p.", ch, obj, null, TO_ROOM)
        return
    }

    val door = find_door(ch, arg.toString())
    val pexit = if (door >= 0) ch.room.exit[door] else null
    if (pexit != null) {
        /* 'lock door' */
        if (!IS_SET(pexit.exit_info, EX_CLOSED)) {
            send_to_char("It's not closed.\n", ch)
            return
        }
        if (pexit.key < 0) {
            send_to_char("It can't be locked.\n", ch)
            return
        }
        if (!has_key(ch, pexit.key) && !has_key_ground(ch, pexit.key)) {
            send_to_char("You lack the key.\n", ch)
            return
        }
        if (IS_SET(pexit.exit_info, EX_LOCKED)) {
            send_to_char("It's already locked.\n", ch)
            return
        }

        pexit.exit_info = SET_BIT(pexit.exit_info, EX_LOCKED)
        send_to_char("*Click*\n", ch)
        act("\$n locks the \$d.", ch, null, pexit.keyword, TO_ROOM)

        /* lock the other side */
        val to_room = pexit.to_room
        val pexit_rev = to_room.exit[rev_dir[door]]
        if (pexit_rev != null && pexit_rev.to_room == ch.room) {
            pexit_rev.exit_info = SET_BIT(pexit_rev.exit_info, EX_LOCKED)
            to_room.people.forEach { act("The \$d clicks.", it, null, pexit_rev.keyword, TO_CHAR) }
        }
    }
}


fun do_unlock(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Unlock what?\n", ch)
        return
    }

    val obj = get_obj_here(ch, arg.toString())
    if (obj != null) {
        /* portal stuff */
        if (obj.item_type == ItemType.Portal) {
            if (IS_SET(obj.value[1], EX_IS_DOOR)) {
                send_to_char("You can't do that.\n", ch)
                return
            }

            if (!IS_SET(obj.value[1], EX_CLOSED)) {
                send_to_char("It's not closed.\n", ch)
                return
            }

            if (obj.value[4] < 0) {
                send_to_char("It can't be unlocked.\n", ch)
                return
            }

            if (!has_key(ch, obj.value[4].toInt())) {
                send_to_char("You lack the key.\n", ch)
                return
            }

            if (!IS_SET(obj.value[1], EX_LOCKED)) {
                send_to_char("It's already unlocked.\n", ch)
                return
            }

            obj.value[1] = REMOVE_BIT(obj.value[1], EX_LOCKED)
            act("You unlock \$p.", ch, obj, null, TO_CHAR)
            act("\$n unlocks \$p.", ch, obj, null, TO_ROOM)
            return
        }

        /* 'unlock object' */
        if (obj.item_type != ItemType.Container) {
            send_to_char("That's not a container.\n", ch)
            return
        }
        if (!IS_SET(obj.value[1], CONT_CLOSED)) {
            send_to_char("It's not closed.\n", ch)
            return
        }
        if (obj.value[2] < 0) {
            send_to_char("It can't be unlocked.\n", ch)
            return
        }
        if (!has_key(ch, obj.value[2].toInt())) {
            send_to_char("You lack the key.\n", ch)
            return
        }
        if (!IS_SET(obj.value[1], CONT_LOCKED)) {
            send_to_char("It's already unlocked.\n", ch)
            return
        }

        obj.value[1] = REMOVE_BIT(obj.value[1], CONT_LOCKED)
        act("You unlock \$p.", ch, obj, null, TO_CHAR)
        act("\$n unlocks \$p.", ch, obj, null, TO_ROOM)
        return
    }

    val door = find_door(ch, arg.toString())
    val pexit = if (door >= 0) ch.room.exit[door] else null
    if (pexit != null) {
        /* 'unlock door' */
        if (!IS_SET(pexit.exit_info, EX_CLOSED)) {
            send_to_char("It's not closed.\n", ch)
            return
        }
        if (pexit.key < 0) {
            send_to_char("It can't be unlocked.\n", ch)
            return
        }
        if (!has_key(ch, pexit.key) && !has_key_ground(ch, pexit.key)) {
            send_to_char("You lack the key.\n", ch)
            return
        }
        if (!IS_SET(pexit.exit_info, EX_LOCKED)) {
            send_to_char("It's already unlocked.\n", ch)
            return
        }

        pexit.exit_info = REMOVE_BIT(pexit.exit_info, EX_LOCKED)
        send_to_char("*Click*\n", ch)
        act("\$n unlocks the \$d.", ch, null, pexit.keyword, TO_ROOM)

        /* unlock the other side */
        val to_room = pexit.to_room
        val pexit_rev = to_room.exit[rev_dir[door]]
        if (pexit_rev != null && pexit_rev.to_room == ch.room) {
            pexit_rev.exit_info = REMOVE_BIT(pexit_rev.exit_info, EX_LOCKED)
            to_room.people.forEach { act("The \$d clicks.", it, null, pexit_rev.keyword, TO_CHAR) }
        }
    }
}


fun do_pick(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Pick what?\n", ch)
        return
    }

    if (MOUNTED(ch) != null) {
        send_to_char("You can't pick while mounted.\n", ch)
        return
    }

    WAIT_STATE(ch, Skill.pick_lock.beats)

    /* look for guards */
    for (gch in ch.room.people) {
        if (gch.pcdata == null && IS_AWAKE(gch) && ch.level + 5 < gch.level) {
            act("\$N is standing too close to the lock.", ch, null, gch, TO_CHAR)
            return
        }
    }

    if (ch.pcdata != null && randomPercent() > get_skill(ch, Skill.pick_lock)) {
        send_to_char("You failed.\n", ch)
        check_improve(ch, Skill.pick_lock, false, 2)
        return
    }

    val obj = get_obj_here(ch, arg.toString())
    if (obj != null) {
        /* portal stuff */
        if (obj.item_type == ItemType.Portal) {
            if (!IS_SET(obj.value[1], EX_IS_DOOR)) {
                send_to_char("You can't do that.\n", ch)
                return
            }

            if (!IS_SET(obj.value[1], EX_CLOSED)) {
                send_to_char("It's not closed.\n", ch)
                return
            }

            if (obj.value[4] < 0) {
                send_to_char("It can't be unlocked.\n", ch)
                return
            }

            if (IS_SET(obj.value[1], EX_PICK_PROOF)) {
                send_to_char("You failed.\n", ch)
                return
            }

            obj.value[1] = REMOVE_BIT(obj.value[1], EX_LOCKED)
            act("You pick the lock on \$p.", ch, obj, null, TO_CHAR)
            act("\$n picks the lock on \$p.", ch, obj, null, TO_ROOM)
            check_improve(ch, Skill.pick_lock, true, 2)
            return
        }

        /* 'pick object' */
        if (obj.item_type != ItemType.Container) {
            send_to_char("That's not a container.\n", ch)
            return
        }
        if (!IS_SET(obj.value[1], CONT_CLOSED)) {
            send_to_char("It's not closed.\n", ch)
            return
        }
        if (obj.value[2] < 0) {
            send_to_char("It can't be unlocked.\n", ch)
            return
        }
        if (!IS_SET(obj.value[1], CONT_LOCKED)) {
            send_to_char("It's already unlocked.\n", ch)
            return
        }
        if (IS_SET(obj.value[1], CONT_PICKPROOF)) {
            send_to_char("You failed.\n", ch)
            return
        }

        obj.value[1] = REMOVE_BIT(obj.value[1], CONT_LOCKED)
        act("You pick the lock on \$p.", ch, obj, null, TO_CHAR)
        act("\$n picks the lock on \$p.", ch, obj, null, TO_ROOM)
        check_improve(ch, Skill.pick_lock, true, 2)
        return
    }

    val door = find_door(ch, arg.toString())
    val pexit = if (door >= 0) ch.room.exit[door] else null
    if (pexit != null) {
        /* 'pick door' */
        if (!IS_SET(pexit.exit_info, EX_CLOSED) && !IS_IMMORTAL(ch)) {
            send_to_char("It's not closed.\n", ch)
            return
        }
        if (pexit.key < 0 && !IS_IMMORTAL(ch)) {
            send_to_char("It can't be picked.\n", ch)
            return
        }
        if (!IS_SET(pexit.exit_info, EX_LOCKED)) {
            send_to_char("It's already unlocked.\n", ch)
            return
        }
        if (IS_SET(pexit.exit_info, EX_PICK_PROOF) && !IS_IMMORTAL(ch)) {
            send_to_char("You failed.\n", ch)
            return
        }

        pexit.exit_info = REMOVE_BIT(pexit.exit_info, EX_LOCKED)
        send_to_char("*Click*\n", ch)
        act("\$n picks the \$d.", ch, null, pexit.keyword, TO_ROOM)
        check_improve(ch, Skill.pick_lock, true, 2)

        /* pick the other side */
        val to_room = pexit.to_room
        val pexit_rev = to_room.exit[rev_dir[door]]
        if (pexit_rev != null && pexit_rev.to_room == ch.room) {
            pexit_rev.exit_info = REMOVE_BIT(pexit_rev.exit_info, EX_LOCKED)
        }
    }
}


fun do_stand(ch: CHAR_DATA?, argument: String) {
    var obj: Obj? = null
    if (argument.isNotEmpty()) {
        if (ch!!.position == Pos.Fighting) {
            send_to_char("Maybe you should finish fighting first?\n", ch)
            return
        }
        obj = get_obj_list(ch, argument, ch.room.objects)
        if (obj == null) {
            send_to_char("You don't see that here.\n", ch)
            return
        }
        if (obj.item_type != ItemType.Furniture || !IS_SET(obj.value[2], STAND_AT)
                && !IS_SET(obj.value[2], STAND_ON)
                && !IS_SET(obj.value[2], STAND_IN)) {
            send_to_char("You can't seem to find a place to stand.\n", ch)
            return
        }
        if (ch.on != obj && count_users(obj) >= obj.value[0]) {
            act("There's no room to stand on \$p.", ch, obj, null, TO_ROOM, Pos.Dead)
            return
        }
    }
    when (ch!!.position) {
        Pos.Sleeping -> {
            if (IS_AFFECTED(ch, AFF_SLEEP)) {
                send_to_char("You can't wake up!\n", ch)
                return
            }
            when {
                obj == null -> {
                    send_to_char("You wake and stand up.\n", ch)
                    act("\$n wakes and stands up.", ch, null, null, TO_ROOM)
                    ch.on = null
                }
                IS_SET(obj.value[2], STAND_AT) -> {
                    act("You wake and stand at \$p.", ch, obj, null, TO_CHAR, Pos.Dead)
                    act("\$n wakes and stands at \$p.", ch, obj, null, TO_ROOM)
                }
                IS_SET(obj.value[2], STAND_ON) -> {
                    act("You wake and stand on \$p.", ch, obj, null, TO_CHAR, Pos.Dead)
                    act("\$n wakes and stands on \$p.", ch, obj, null, TO_ROOM)
                }
                else -> {
                    act("You wake and stand in \$p.", ch, obj, null, TO_CHAR, Pos.Dead)
                    act("\$n wakes and stands in \$p.", ch, obj, null, TO_ROOM)
                }
            }

            if (IS_HARA_KIRI(ch)) {
                send_to_char("You feel your blood heats your body.\n", ch)
                ch.act = REMOVE_BIT(ch.act, PLR_HARA_KIRI)
            }

            ch.position = Pos.Standing
            do_look(ch, "auto")
        }

        Pos.Resting, Pos.Sitting -> {
            when {
                obj == null -> {
                    send_to_char("You stand up.\n", ch)
                    act("\$n stands up.", ch, null, null, TO_ROOM)
                    ch.on = null
                }
                IS_SET(obj.value[2], STAND_AT) -> {
                    act("You stand at \$p.", ch, obj, null, TO_CHAR)
                    act("\$n stands at \$p.", ch, obj, null, TO_ROOM)
                }
                IS_SET(obj.value[2], STAND_ON) -> {
                    act("You stand on \$p.", ch, obj, null, TO_CHAR)
                    act("\$n stands on \$p.", ch, obj, null, TO_ROOM)
                }
                else -> {
                    act("You stand in \$p.", ch, obj, null, TO_CHAR)
                    act("\$n stands on \$p.", ch, obj, null, TO_ROOM)
                }
            }
            ch.position = Pos.Standing
        }
        Pos.Standing -> send_to_char("You are already standing.\n", ch)
        Pos.Fighting -> send_to_char("You are already fighting!\n", ch)
        Pos.Dead, Pos.Mortal, Pos.Incap, Pos.Stunned -> {
        }
    }

}


fun do_rest(ch: CHAR_DATA, argument: String) {
    val obj: Obj?

    if (ch.position == Pos.Fighting) {
        send_to_char("You are already fighting!\n", ch)
        return
    }

    if (MOUNTED(ch) != null) {
        send_to_char("You can't rest while mounted.\n", ch)
        return
    }
    if (RIDDEN(ch) != null) {
        send_to_char("You can't rest while being ridden.\n", ch)
        return
    }


    if (IS_AFFECTED(ch, AFF_SLEEP)) {
        send_to_char("You are already sleeping.\n", ch)
        return
    }

    /* okay, now that we know we can rest, find an object to rest on */
    if (argument.isNotEmpty()) {
        obj = get_obj_list(ch, argument, ch.room.objects)
        if (obj == null) {
            send_to_char("You don't see that here.\n", ch)
            return
        }
    } else {
        obj = ch.on
    }

    if (obj != null) {
        if (obj.item_type !== ItemType.Furniture || !IS_SET(obj.value[2], REST_ON)
                && !IS_SET(obj.value[2], REST_IN)
                && !IS_SET(obj.value[2], REST_AT)) {
            send_to_char("You can't rest on that.\n", ch)
            return
        }

        if (ch.on != obj && count_users(obj) >= obj.value[0]) {
            act("There's no more room on \$p.", ch, obj, null, TO_CHAR, Pos.Dead)
            return
        }

        ch.on = obj
    }

    when (ch.position) {
        Pos.Sleeping -> {
            when {
                obj == null -> {
                    send_to_char("You wake up and start resting.\n", ch)
                    act("\$n wakes up and starts resting.", ch, null, null, TO_ROOM)
                }
                IS_SET(obj.value[2], REST_AT) -> {
                    act("You wake up and rest at \$p.", ch, obj, null, TO_CHAR, Pos.Sleeping)
                    act("\$n wakes up and rests at \$p.", ch, obj, null, TO_ROOM)
                }
                IS_SET(obj.value[2], REST_ON) -> {
                    act("You wake up and rest on \$p.", ch, obj, null, TO_CHAR, Pos.Sleeping)
                    act("\$n wakes up and rests on \$p.", ch, obj, null, TO_ROOM)
                }
                else -> {
                    act("You wake up and rest in \$p.", ch, obj, null, TO_CHAR, Pos.Sleeping)
                    act("\$n wakes up and rests in \$p.", ch, obj, null, TO_ROOM)
                }
            }
            ch.position = Pos.Resting
        }

        Pos.Resting -> send_to_char("You are already resting.\n", ch)

        Pos.Standing -> {
            when {
                obj == null -> {
                    send_to_char("You rest.\n", ch)
                    act("\$n sits down and rests.", ch, null, null, TO_ROOM)
                }
                IS_SET(obj.value[2], REST_AT) -> {
                    act("You sit down at \$p and rest.", ch, obj, null, TO_CHAR)
                    act("\$n sits down at \$p and rests.", ch, obj, null, TO_ROOM)
                }
                IS_SET(obj.value[2], REST_ON) -> {
                    act("You sit on \$p and rest.", ch, obj, null, TO_CHAR)
                    act("\$n sits on \$p and rests.", ch, obj, null, TO_ROOM)
                }
                else -> {
                    act("You rest in \$p.", ch, obj, null, TO_CHAR)
                    act("\$n rests in \$p.", ch, obj, null, TO_ROOM)
                }
            }
            ch.position = Pos.Resting
        }

        Pos.Sitting -> {
            when {
                obj == null -> {
                    send_to_char("You rest.\n", ch)
                    act("\$n rests.", ch, null, null, TO_ROOM)
                }
                IS_SET(obj.value[2], REST_AT) -> {
                    act("You rest at \$p.", ch, obj, null, TO_CHAR)
                    act("\$n rests at \$p.", ch, obj, null, TO_ROOM)
                }
                IS_SET(obj.value[2], REST_ON) -> {
                    act("You rest on \$p.", ch, obj, null, TO_CHAR)
                    act("\$n rests on \$p.", ch, obj, null, TO_ROOM)
                }
                else -> {
                    act("You rest in \$p.", ch, obj, null, TO_CHAR)
                    act("\$n rests in \$p.", ch, obj, null, TO_ROOM)
                }
            }
            ch.position = Pos.Resting

            if (IS_HARA_KIRI(ch)) {
                send_to_char("You feel your blood heats your body.\n", ch)
                ch.act = REMOVE_BIT(ch.act, PLR_HARA_KIRI)
            }
        }
        Pos.Dead, Pos.Mortal, Pos.Incap, Pos.Stunned, Pos.Fighting -> {
        }
    }
}


fun do_sit(ch: CHAR_DATA, argument: String) {
    val obj: Obj?

    if (ch.position == Pos.Fighting) {
        send_to_char("Maybe you should finish this fight first?\n", ch)
        return
    }
    if (MOUNTED(ch) != null) {
        send_to_char("You can't sit while mounted.\n", ch)
        return
    }
    if (RIDDEN(ch) != null) {
        send_to_char("You can't sit while being ridden.\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_SLEEP)) {
        send_to_char("You are already sleeping.\n", ch)
        return
    }

    /* okay, now that we know we can sit, find an object to sit on */
    if (argument.isNotEmpty()) {
        obj = get_obj_list(ch, argument, ch.room.objects)
        if (obj == null) {
            if (IS_AFFECTED(ch, AFF_SLEEP)) {
                send_to_char("You are already sleeping.\n", ch)
                return
            }
            send_to_char("You don't see that here.\n", ch)
            return
        }
    } else {
        obj = ch.on
    }

    if (obj != null) {
        if (obj.item_type !== ItemType.Furniture || !IS_SET(obj.value[2], SIT_ON)
                && !IS_SET(obj.value[2], SIT_IN)
                && !IS_SET(obj.value[2], SIT_AT)) {
            send_to_char("You can't sit on that.\n", ch)
            return
        }

        if (ch.on != obj && count_users(obj) >= obj.value[0]) {
            act("There's no more room on \$p.", ch, obj, null, TO_CHAR, Pos.Dead)
            return
        }

        ch.on = obj
    }
    when (ch.position) {
        Pos.Sleeping -> {
            when {
                obj == null -> {
                    send_to_char("You wake and sit up.\n", ch)
                    act("\$n wakes and sits up.", ch, null, null, TO_ROOM)
                }
                IS_SET(obj.value[2], SIT_AT) -> {
                    act("You wake and sit at \$p.", ch, obj, null, TO_CHAR, Pos.Dead)
                    act("\$n wakes and sits at \$p.", ch, obj, null, TO_ROOM)
                }
                IS_SET(obj.value[2], SIT_ON) -> {
                    act("You wake and sit on \$p.", ch, obj, null, TO_CHAR, Pos.Dead)
                    act("\$n wakes and sits at \$p.", ch, obj, null, TO_ROOM)
                }
                else -> {
                    act("You wake and sit in \$p.", ch, obj, null, TO_CHAR, Pos.Dead)
                    act("\$n wakes and sits in \$p.", ch, obj, null, TO_ROOM)
                }
            }

            ch.position = Pos.Sitting
        }
        Pos.Resting -> {
            when {
                obj == null -> send_to_char("You stop resting.\n", ch)
                IS_SET(obj.value[2], SIT_AT) -> {
                    act("You sit at \$p.", ch, obj, null, TO_CHAR)
                    act("\$n sits at \$p.", ch, obj, null, TO_ROOM)
                }
                IS_SET(obj.value[2], SIT_ON) -> {
                    act("You sit on \$p.", ch, obj, null, TO_CHAR)
                    act("\$n sits on \$p.", ch, obj, null, TO_ROOM)
                }
            }
            ch.position = Pos.Sitting
        }
        Pos.Sitting -> send_to_char("You are already sitting down.\n", ch)
        Pos.Standing -> {
            when {
                obj == null -> {
                    send_to_char("You sit down.\n", ch)
                    act("\$n sits down on the ground.", ch, null, null, TO_ROOM)
                }
                IS_SET(obj.value[2], SIT_AT) -> {
                    act("You sit down at \$p.", ch, obj, null, TO_CHAR)
                    act("\$n sits down at \$p.", ch, obj, null, TO_ROOM)
                }
                IS_SET(obj.value[2], SIT_ON) -> {
                    act("You sit on \$p.", ch, obj, null, TO_CHAR)
                    act("\$n sits on \$p.", ch, obj, null, TO_ROOM)
                }
                else -> {
                    act("You sit down in \$p.", ch, obj, null, TO_CHAR)
                    act("\$n sits down in \$p.", ch, obj, null, TO_ROOM)
                }
            }
            ch.position = Pos.Sitting
        }
        Pos.Dead, Pos.Mortal, Pos.Incap, Pos.Stunned, Pos.Fighting -> {
        }
    }
    if (IS_HARA_KIRI(ch)) {
        send_to_char("You feel your blood heats your body.\n", ch)
        ch.act = REMOVE_BIT(ch.act, PLR_HARA_KIRI)
    }
}


fun do_sleep(ch: CHAR_DATA, argument: String) {
    val obj: Obj?

    if (MOUNTED(ch) != null) {
        send_to_char("You can't sleep while mounted.\n", ch)
        return
    }
    if (RIDDEN(ch) != null) {
        send_to_char("You can't sleep while being ridden.\n", ch)
        return
    }

    when (ch.position) {
        Pos.Sleeping -> send_to_char("You are already sleeping.\n", ch)

        Pos.Resting, Pos.Sitting, Pos.Standing -> if (argument.isEmpty() && ch.on == null) {
            send_to_char("You go to sleep.\n", ch)
            act("\$n goes to sleep.", ch, null, null, TO_ROOM)
            ch.position = Pos.Sleeping
        } else {
            /* find an object and sleep on it */
            obj = if (argument.isEmpty()) ch.on else get_obj_list(ch, argument, ch.room.objects)

            if (obj == null) {
                send_to_char("You don't see that here.\n", ch)
                return
            }
            if (obj.item_type != ItemType.Furniture || !IS_SET(obj.value[2], SLEEP_ON)
                    && !IS_SET(obj.value[2], SLEEP_IN)
                    && !IS_SET(obj.value[2], SLEEP_AT)) {
                send_to_char("You can't sleep on that!\n", ch)
                return
            }

            if (ch.on != obj && count_users(obj) >= obj.value[0]) {
                act("There is no room on \$p for you.", ch, obj, null, TO_CHAR, Pos.Dead)
                return
            }

            ch.on = obj
            when {
                IS_SET(obj.value[2], SLEEP_AT) -> {
                    act("You go to sleep at \$p.", ch, obj, null, TO_CHAR)
                    act("\$n goes to sleep at \$p.", ch, obj, null, TO_ROOM)
                }
                IS_SET(obj.value[2], SLEEP_ON) -> {
                    act("You go to sleep on \$p.", ch, obj, null, TO_CHAR)
                    act("\$n goes to sleep on \$p.", ch, obj, null, TO_ROOM)
                }
                else -> {
                    act("You go to sleep in \$p.", ch, obj, null, TO_CHAR)
                    act("\$n goes to sleep in \$p.", ch, obj, null, TO_ROOM)
                }
            }
            ch.position = Pos.Sleeping
        }

        Pos.Fighting -> send_to_char("You are already fighting!\n", ch)
        Pos.Dead, Pos.Mortal, Pos.Incap, Pos.Stunned -> {
        }
    }
}


fun do_wake(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?

    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        do_stand(ch, argument)
        return
    }

    if (!IS_AWAKE(ch)) {
        send_to_char("You are asleep yourself!\n", ch)
        return
    }

    victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (IS_AWAKE(victim)) {
        act("\$N is already awake.", ch, null, victim, TO_CHAR)
        return
    }

    if (IS_AFFECTED(victim, AFF_SLEEP)) {
        act("You can't wake \$M!", ch, null, victim, TO_CHAR)
        return
    }

    act("\$n wakes you.", ch, null, victim, TO_VICT, Pos.Sleeping)
    do_stand(victim, "")
}


fun do_sneak(ch: CHAR_DATA) {

    if (MOUNTED(ch) != null) {
        send_to_char("You can't sneak while mounted.\n", ch)
        return
    }

    if (RIDDEN(ch) != null) {
        send_to_char("You can't hide while being ridden.\n", ch)
        return
    }

    send_to_char("You attempt to move silently.\n", ch)
    affect_strip(ch, Skill.sneak)

    if (IS_AFFECTED(ch, AFF_SNEAK)) {
        return
    }

    if (randomPercent() < get_skill(ch, Skill.sneak)) {
        check_improve(ch, Skill.sneak, true, 3)
        val af = Affect()
        af.type = Skill.sneak
        af.level = ch.level
        af.duration = ch.level
        af.bitvector = AFF_SNEAK
        affect_to_char(ch, af)
    } else {
        check_improve(ch, Skill.sneak, false, 3)
    }

}


fun do_hide(ch: CHAR_DATA) {
    if (MOUNTED(ch) != null) {
        send_to_char("You can't hide while mounted.\n", ch)
        return
    }

    if (RIDDEN(ch) != null) {
        send_to_char("You can't hide while being ridden.\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_FAERIE_FIRE)) {
        send_to_char("You can not hide while glowing.\n", ch)
        return
    }
    var forest = 0
    forest += if (ch.room.sector_type == SECT_FOREST) 60 else 0
    forest += if (ch.room.sector_type == SECT_FIELD) 60 else 0

    send_to_char("You attempt to hide.\n", ch)

    if (randomPercent() < get_skill(ch, Skill.hide) - forest) {
        ch.affected_by = SET_BIT(ch.affected_by, AFF_HIDE)
        check_improve(ch, Skill.hide, true, 3)
    } else {
        if (IS_AFFECTED(ch, AFF_HIDE)) {
            ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_HIDE)
        }
        check_improve(ch, Skill.hide, false, 3)
    }

}

fun do_camouflage(ch: CHAR_DATA) {

    if (skill_failure_check(ch, Skill.camouflage, false, 0,
            "You don't know how to camouflage yourself.\n")) {
        return
    }

    if (RIDDEN(ch) != null) {
        send_to_char("You can't camouflage while being ridden.\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_FAERIE_FIRE)) {
        send_to_char("You can't camouflage yourself while glowing.\n", ch)
        return
    }

    if (ch.room.sector_type != SECT_FOREST &&
            ch.room.sector_type != SECT_HILLS &&
            ch.room.sector_type != SECT_MOUNTAIN) {
        send_to_char("There is no cover here.\n", ch)
        act("\$n tries to camouflage \$mself against the lone leaf on the ground.", ch, null, null, TO_ROOM)
        return
    }
    send_to_char("You attempt to camouflage yourself.\n", ch)
    WAIT_STATE(ch, Skill.camouflage.beats)

    if (IS_AFFECTED(ch, AFF_CAMOUFLAGE)) {
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_CAMOUFLAGE)
    }


    if (ch.pcdata == null || randomPercent() < get_skill(ch, Skill.camouflage)) {
        ch.affected_by = SET_BIT(ch.affected_by, AFF_CAMOUFLAGE)
        check_improve(ch, Skill.camouflage, true, 1)
    } else {
        check_improve(ch, Skill.camouflage, false, 1)
    }

}

/*
 * Contributed by Alander
 */

fun do_visible(ch: CHAR_DATA) {
    if (IS_SET(ch.affected_by, AFF_HIDE)) {
        send_to_char("You step out of the shadows.\n", ch)
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_HIDE)
        act("\$n steps out of the shadows.", ch, null, null, TO_ROOM)
    }
    if (IS_SET(ch.affected_by, AFF_FADE)) {
        send_to_char("You step out of the shadows.\n", ch)
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_FADE)
        act("\$n steps out of the shadows.", ch, null, null, TO_ROOM)
    }
    if (IS_SET(ch.affected_by, AFF_CAMOUFLAGE)) {
        send_to_char("You step out from your cover.\n", ch)
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_CAMOUFLAGE)
        act("\$n steps out from \$s cover.", ch, null, null, TO_ROOM)
    }
    if (IS_SET(ch.affected_by, AFF_INVISIBLE)) {
        send_to_char("You fade into existence.\n", ch)
        affect_strip(ch, Skill.invisibility)
        affect_strip(ch, Skill.mass_invis)
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_INVISIBLE)
        act("\$n fades into existence.", ch, null, null, TO_ROOM)
    }
    if (IS_SET(ch.affected_by, AFF_IMP_INVIS)) {
        send_to_char("You fade into existence.\n", ch)
        affect_strip(ch, Skill.improved_invis)
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_IMP_INVIS)
        act("\$n fades into existence.", ch, null, null, TO_ROOM)
    }
    if (IS_SET(ch.affected_by, AFF_SNEAK) && ch.pcdata != null && !IS_SET(ch.race.aff, AFF_SNEAK)) {
        send_to_char("You trample around loudly again.\n", ch)
        affect_strip(ch, Skill.sneak)
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_SNEAK)
    }

    affect_strip(ch, Skill.mass_invis)

    if (IS_AFFECTED(ch, AFF_EARTHFADE)) {
        send_to_char("You fade to your neutral form.\n", ch)
        act("Earth forms \$n in front of you.", ch, null, null, TO_ROOM)
        affect_strip(ch, Skill.earthfade)
        WAIT_STATE(ch, PULSE_VIOLENCE / 2)
        ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_EARTHFADE)
    }
}

fun do_recall(ch: CHAR_DATA) {
    val point = ch.hometown.recall[when {
        ch.isGood() -> 0
        ch.isEvil() -> 2
        else -> 1
    }]

    if (ch.pcdata == null && !IS_SET(ch.act, ACT_PET)) {
        send_to_char("Only players can recall.\n", ch)
        return
    }

    if (ch.level >= 11 && !IS_IMMORTAL(ch)) {
        send_to_char("Recall is for only levels below 10.\n", ch)
        return
    }
    if (ch.pcdata != null && current_time - ch.last_fight_time < FIGHT_DELAY_TIME) {
        send_to_char("You are too pumped to pray now.\n", ch)
        return
    }

    act("\$n prays for transportation!", ch, null, null, TO_ROOM)

    val location = get_room_index(point)
    if (location == null) {
        send_to_char("You are completely lost.\n", ch)
        return
    }

    if (ch.room == location) return

    if (IS_SET(ch.room.room_flags, ROOM_NO_RECALL) || IS_AFFECTED(ch, AFF_CURSE) || IS_RAFFECTED(ch.room, AFF_ROOM_CURSE)) {
        send_to_char("The gods have forsaken you.\n", ch)
        return
    }

    if (ch.fighting != null) {
        send_to_char("You are still fighting!\n", ch)

        val skill = if (ch.pcdata == null) 40 + ch.level else get_skill(ch, Skill.recall)

        if (randomPercent() < 80 * skill / 100) {
            check_improve(ch, Skill.recall, false, 6)
            WAIT_STATE(ch, 4)
            send_to_char("You failed!.\n", ch)
            return
        }

        val lose = 25
        gain_exp(ch, 0 - lose)
        check_improve(ch, Skill.recall, true, 4)
        val buf = TextBuffer()
        buf.sprintf("You recall from combat!  You lose %d exps.\n", lose)
        send_to_char(buf, ch)
        stop_fighting(ch, true)
    }

    ch.move /= 2
    act("\$n disappears.", ch, null, null, TO_ROOM)
    char_from_room(ch)
    char_to_room(ch, location)
    act("\$n appears in the room.", ch, null, null, TO_ROOM)
    do_look(ch, "auto")

    val pet = ch.pet
    if (pet != null) {
        char_from_room(pet)
        char_to_room(pet, location)
        do_look(pet, "auto")
    }
}


fun do_train(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val pc_data = ch.pcdata ?: return

    /* Check for trainer. */
    val mob: CHAR_DATA? = ch.room.people.firstOrNull { it.pcdata == null && (IS_SET(it.act, ACT_PRACTICE) || IS_SET(it.act, ACT_TRAIN) || IS_SET(it.act, ACT_GAIN)) }
    if (mob == null) {
        send_to_char("You can't do that here.\n", ch)
        return
    }

    if (rest.isEmpty()) {
        val buf = TextBuffer()
        buf.sprintf("You have %d training sessions.\n", ch.train)
        send_to_char(buf, ch)
        rest = "foo"
    }

    var cost = 1
    var stat: PrimeStat? = null
    var pOutput = ""
    when {
        eq(rest, "str") -> {
            if (ch.clazz.attr_prime == PrimeStat.Strength) {
                cost = 1
            }
            stat = PrimeStat.Strength
            pOutput = "strength"
        }
        eq(rest, "int") -> {
            if (ch.clazz.attr_prime == PrimeStat.Intelligence) {
                cost = 1
            }
            stat = PrimeStat.Intelligence
            pOutput = "intelligence"
        }
        eq(rest, "wis") -> {
            if (ch.clazz.attr_prime == PrimeStat.Wisdom) {
                cost = 1
            }
            stat = PrimeStat.Wisdom
            pOutput = "wisdom"
        }
        eq(rest, "dex") -> {
            if (ch.clazz.attr_prime == PrimeStat.Dexterity) {
                cost = 1
            }
            stat = PrimeStat.Dexterity
            pOutput = "dexterity"
        }
        eq(rest, "con") -> {
            if (ch.clazz.attr_prime == PrimeStat.Constitution) {
                cost = 1
            }
            stat = PrimeStat.Constitution
            pOutput = "constitution"
        }
        eq(rest, "cha") -> {
            if (ch.clazz.attr_prime == PrimeStat.Charisma) {
                cost = 1
            }
            stat = PrimeStat.Charisma
            pOutput = "charisma"
        }
        eq(rest, "hp") -> cost = 1
        eq(rest, "mana") -> cost = 1
        else -> {
            val buf = StringBuilder("You can train:")
            if (ch.perm_stat[PrimeStat.Strength] < ch.max(PrimeStat.Strength)) {
                buf.append(" str")
            }
            if (ch.perm_stat[PrimeStat.Intelligence] < ch.max(PrimeStat.Intelligence)) {
                buf.append(" int")
            }
            if (ch.perm_stat[PrimeStat.Wisdom] < ch.max(PrimeStat.Wisdom)) {
                buf.append(" wis")
            }
            if (ch.perm_stat[PrimeStat.Dexterity] < ch.max(PrimeStat.Dexterity)) {
                buf.append(" dex")
            }
            if (ch.perm_stat[PrimeStat.Constitution] < ch.max(PrimeStat.Constitution)) {
                buf.append(" con")
            }
            if (ch.perm_stat[PrimeStat.Charisma] < ch.max(PrimeStat.Charisma)) {
                buf.append(" cha")
            }
            buf.append(" hp mana")

            if (buf[buf.length - 1] != ':') {
                buf.append(".\n")
                send_to_char(buf, ch)
            } else {
                act("You have nothing left to train, you \$T!",
                        ch, null,
                        when (ch.sex) {
                            Sex.Male -> "big stud"
                            Sex.Female -> "hot babe"
                            else -> "wild thing"
                        }, TO_CHAR)
            }
            return
        }
    }

    if (eq("hp", rest)) {
        if (cost > ch.train) {
            send_to_char("You don't have enough training sessions.\n", ch)
            return
        }

        ch.train -= cost
        pc_data.perm_hit += 10
        ch.max_hit += 10
        ch.hit += 10
        act("Your durability increases!", ch, null, null, TO_CHAR)
        act("\$n's durability increases!", ch, null, null, TO_ROOM)
        return
    }

    if (eq("mana", rest)) {
        if (cost > ch.train) {
            send_to_char("You don't have enough training sessions.\n", ch)
            return
        }

        ch.train -= cost
        pc_data.perm_mana += 10
        ch.max_mana += 10
        ch.mana += 10
        act("Your power increases!", ch, null, null, TO_CHAR)
        act("\$n's power increases!", ch, null, null, TO_ROOM)
        return
    }
    if (stat == null) {
        bug("Stat is null: $argument")
        return
    }
    if (ch.perm_stat[stat] >= ch.max(stat)) {
        act("Your \$T is already at maximum.", ch, null, pOutput, TO_CHAR)
        return
    }

    if (cost > ch.train) {
        send_to_char("You don't have enough training sessions.\n", ch)
        return
    }

    ch.train -= cost

    ch.perm_stat[stat] += 1
    act("Your \$T increases!", ch, null, pOutput, TO_CHAR)
    act("\$n's \$T increases!", ch, null, pOutput, TO_ROOM)
}


private var door_name = arrayOf("north", "east", "south", "west", "up", "down", "that way")

fun do_track(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.track, false, 0, "There are no train tracks here.\n")) {
        return
    }

    WAIT_STATE(ch, Skill.track.beats)
    act("\$n checks the ground for tracks.", ch, null, null, TO_ROOM)

    if (ch.pcdata == null || randomPercent() < get_skill(ch, Skill.track)) {
        if (ch.pcdata == null) {
            ch.status = 0
            if (ch.last_fought != null && !IS_SET(ch.act, ACT_NOTRACK)) {
                add_mind(ch, ch.last_fought!!.name)
            }
        }
        var rh = ch.room.history
        while (rh != null) {
            if (is_name(argument, rh.name)) {
                check_improve(ch, Skill.track, true, 1)
                val d = rh.went
                if (d == -1) {
                    rh = rh.next
                    continue
                }
                val buf = TextBuffer()
                buf.sprintf("%s's tracks lead %s.\n", capitalize(rh.name), door_name[d])
                send_to_char(buf, ch)
                val pexit = ch.room.exit[d]
                if (pexit != null && IS_SET(pexit.exit_info, EX_IS_DOOR) && pexit.keyword.isNotEmpty()) {
                    do_open(ch, door_name[d])
                }
                move_char(ch, rh.went)
                return
            }
            rh = rh.next
        }
    }
    send_to_char("You don't see any tracks.\n", ch)
    if (ch.pcdata == null) {
        ch.status = 5 /* for stalker */
    }
    check_improve(ch, Skill.track, false, 1)
}


fun do_vampire(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.vampire, false, 0, "You try to show yourself even more uggly.\n")) {
        return
    }

    if (is_affected(ch, Skill.vampire)) {
        send_to_char("You can't be much more vampire!\n", ch)
        return
    }

    if (get_skill(ch, Skill.vampire) < 50) {
        send_to_char("Go and ask the questor to help you.\n", ch)
        return
    }

    if (is_affected(ch, Skill.vampire)) {
        send_to_char("If you wan't to be more vampire go and kill a player.\n", ch)
        return
    }

    if (weather.sunlight == SUN_LIGHT || weather.sunlight == SUN_RISE) {
        send_to_char("You should have waited the evening or night to tranform to a vampire.\n", ch)
    }

    val level = ch.level
    var duration = level / 10
    duration += 5

    /* haste */
    val af1 = Affect()
    af1.type = Skill.vampire
    af1.level = level
    af1.duration = duration
    af1.location = Apply.Dex
    af1.modifier = 1 + level / 20
    af1.bitvector = AFF_HASTE
    affect_to_char(ch, af1)

    /* giant strength + infrared */
    val af2 = Affect()
    af2.type = Skill.vampire
    af2.level = level
    af2.duration = duration
    af2.location = Apply.Str
    af2.modifier = 1 + level / 20
    af2.bitvector = AFF_INFRARED
    affect_to_char(ch, af2)

    /* size */
    val af3 = Affect()
    af3.type = Skill.vampire
    af3.level = level
    af3.duration = duration
    af3.location = Apply.Size
    af3.modifier = 1 + level / 50
    af3.bitvector = AFF_SNEAK
    affect_to_char(ch, af3)

    /* damroll */
    val af4 = Affect()
    af4.type = Skill.vampire
    af4.level = level
    af4.duration = duration
    af4.location = Apply.Damroll
    af4.modifier = ch.damroll
    af4.bitvector = AFF_BERSERK
    affect_to_char(ch, af4)

    /* negative immunity */
    val af5 = Affect()
    af5.where = TO_IMMUNE
    af5.type = Skill.vampire
    af5.duration = duration
    af5.level = level
    af5.bitvector = IMM_NEGATIVE
    affect_to_char(ch, af5)

    /* vampire flag */
    val af6 = Affect()
    af6.where = TO_ACT_FLAG
    af6.type = Skill.vampire
    af6.level = level
    af6.duration = duration
    af6.bitvector = PLR_VAMPIRE
    affect_to_char(ch, af6)

    send_to_char("You feel yourself getting greater and greater.\n", ch)
    act("You cannot recognize \$n anymore.", ch, null, null, TO_ROOM)
}

fun do_vbite(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (skill_failure_check(ch, Skill.vampiric_bite, false, 0, "You don't know how to bite creatures.\n")) {
        return
    }

    if (!IS_VAMPIRE(ch)) {
        send_to_char("You must transform vampire before biting.\n", ch)
        return
    }

    if (arg.isEmpty()) {
        send_to_char("Bite whom?\n", ch)
        return
    }

    val victim = get_char_room(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim.position != Pos.Sleeping) {
        send_to_char("They must be sleeping.\n", ch)
        return
    }

    if (ch.pcdata == null && victim.pcdata != null) {
        return
    }


    if (victim == ch) {
        send_to_char("How can you sneak up on yourself?\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (victim.fighting != null) {
        send_to_char("You can't bite a fighting person.\n", ch)
        return
    }


    WAIT_STATE(ch, Skill.vampiric_bite.beats)

    if (victim.hit < 0.8 * victim.max_hit && IS_AWAKE(victim)) {
        act("\$N is hurt and suspicious ... doesn't worth up.", ch, null, victim, TO_CHAR)
        return
    }

    if (current_time - victim.last_fight_time < 300 && IS_AWAKE(victim)) {
        act("\$N is suspicious ... it doesn't worth to do.", ch, null, victim, TO_CHAR)
        return
    }

    if (!IS_AWAKE(victim) && (ch.pcdata == null || randomPercent() < get_skill(ch, Skill.vampiric_bite) * 0.7 + 2 * (ch.level - victim.level))) {
        check_improve(ch, Skill.vampiric_bite, true, 1)
        one_hit(ch, victim, Skill.vampiric_bite, false)
    } else {
        check_improve(ch, Skill.vampiric_bite, false, 1)
        damage(ch, victim, 0, Skill.vampiric_bite, DT.None, true)
    }
    /* Player shouts if he doesn't die */
    if (victim.pcdata != null && ch.pcdata != null && victim.position == Pos.Fighting) {
        do_yell(victim, "Help, an ugly creature tried to bite me!")
    }
}

fun do_bash_door(ch: CHAR_DATA, argument: String) {
    var chance = 0
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (skill_failure_check(ch, Skill.bash_door, false, OFF_BASH, "Bashing? What's that?\n")) {
        return
    }

    if (RIDDEN(ch) != null) {
        send_to_char("You can't bash doors while being ridden.\n", ch)
        return
    }

    if (arg.isEmpty()) {
        send_to_char("Bash which door or direction.\n", ch)
        return
    }

    if (ch.fighting != null) {
        send_to_char("Wait until the fight finishes.\n", ch)
        return
    }

    /* look for guards */
    for (gch in ch.room.people) {
        if (gch.pcdata == null && IS_AWAKE(gch) && ch.level + 5 < gch.level) {
            act("\$N is standing too close to the door.", ch, null, gch, TO_CHAR)
            return
        }
    }

    val door = find_door(ch, arg.toString())
    val pexit = if (door >= 0) ch.room.exit[door] else null
    if (pexit != null) {
        /* 'bash door' */
        if (!IS_SET(pexit.exit_info, EX_CLOSED)) {
            send_to_char("It's already open.\n", ch)
            return
        }
        if (!IS_SET(pexit.exit_info, EX_LOCKED)) {
            send_to_char("Just try to open it.\n", ch)
            return
        }
        if (IS_SET(pexit.exit_info, EX_NO_PASS)) {
            send_to_char("A mystical shield protects the exit.\n", ch)
            return
        }

        /* modifiers */

        /* size  and weight */
        chance += get_carry_weight(ch) / 100
        chance += (ch.size - 2) * 20

        /* stats */
        chance += ch.curr_stat(PrimeStat.Strength)

        if (IS_AFFECTED(ch, AFF_FLYING)) {
            chance -= 10
        }

        /* level
            chance += ch.level / 10;
            */

        chance += get_skill(ch, Skill.bash_door) - 90

        act("You slam into \$d, and try to break \$d!", ch, null, pexit.keyword, TO_CHAR)
        act("You slam into \$d, and try to break \$d!", ch, null, pexit.keyword, TO_ROOM)

        if (room_dark(ch.room)) {
            chance /= 2
        }

        /* now the attack */
        if (randomPercent() < chance) {
            check_improve(ch, Skill.bash_door, true, 1)

            pexit.exit_info = REMOVE_BIT(pexit.exit_info, EX_LOCKED)
            pexit.exit_info = REMOVE_BIT(pexit.exit_info, EX_CLOSED)
            act("\$n bashes the the \$d and breaks the lock.", ch, null, pexit.keyword, TO_ROOM)
            send_to_char("You successed to open the door.\n", ch)

            /* open the other side */
            val to_room = pexit.to_room
            val pexit_rev = to_room.exit[rev_dir[door]]
            if (pexit_rev != null && pexit_rev.to_room == ch.room) {
                pexit_rev.exit_info = REMOVE_BIT(pexit_rev.exit_info, EX_CLOSED)
                pexit_rev.exit_info = REMOVE_BIT(pexit_rev.exit_info, EX_LOCKED)
                to_room.people.forEach { act("The \$d opens.", it, null, pexit_rev.keyword, TO_CHAR) }
            }


            if (randomPercent() < chance) {
                move_char(ch, door)
                ch.position = Pos.Resting
            }
            WAIT_STATE(ch, Skill.bash_door.beats)

        } else {
            act("You fall flat on your face!", ch, null, null, TO_CHAR)
            act("\$n falls flat on \$s face.", ch, null, null, TO_ROOM)
            check_improve(ch, Skill.bash_door, false, 1)
            ch.position = Pos.Resting
            WAIT_STATE(ch, Skill.bash.beats * 3 / 2)
            val damage_bash = ch.damroll + number_range(4, 4 + ch.size * 4 + chance / 5)
            damage(ch, ch, damage_bash, Skill.bash, DT.Bash, true)
        }
    }
}

fun do_blink(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (skill_failure_check(ch, Skill.blink, false, 0, null)) {
        return
    }
    if (arg.isEmpty()) {
        val buf = TextBuffer()
        buf.sprintf("Your current blink status : %s.\n", if (IS_BLINK_ON(ch)) "ON" else "OFF")
        send_to_char(buf, ch)
        return
    }

    if (eq(arg.toString(), "ON")) {
        ch.act = SET_BIT(ch.act, PLR_BLINK_ON)
        send_to_char("Now ,your current blink status is ON.\n", ch)
        return
    }

    if (eq(arg.toString(), "OFF")) {
        ch.act = REMOVE_BIT(ch.act, PLR_BLINK_ON)
        send_to_char("Now ,your current blink status is OFF.\n", ch)
        return
    }

    val buf = TextBuffer()
    buf.sprintf("What is that?.Is %s a status?\n", arg)
    send_to_char(buf, ch)
}

fun do_vanish(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.vanish, false, 0, null)) {
        return
    }

    if (ch.mana < 25) {
        send_to_char("You don't have enough power.\n", ch)
        return
    }

    ch.mana -= 25

    WAIT_STATE(ch, Skill.vanish.beats)

    if (randomPercent() > get_skill(ch, Skill.vanish)) {
        send_to_char("You failed.\n", ch)
        check_improve(ch, Skill.vanish, false, 1)
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_NO_RECALL) || cabal_area_check(ch)) {
        send_to_char("You failed.\n", ch)
        return
    }

    var pRoomIndex: ROOM_INDEX_DATA? = null
    var i = 0
    while (i < 65535) {
        pRoomIndex = get_room_index(number_range(0, 65535))
        if (pRoomIndex == null) {
            i++
            continue
        }
        if (can_see_room(ch, pRoomIndex) && !room_is_private(pRoomIndex) && ch.room.area == pRoomIndex.area) {
            break
        }
        i++
    }

    if (pRoomIndex == null) {
        send_to_char("You failed.\n", ch)
        return
    }

    act("\$n throws down a small globe.", ch, null, null, TO_ROOM)

    check_improve(ch, Skill.vanish, true, 1)

    if (ch.pcdata != null && ch.fighting != null && flipCoin()) {
        send_to_char("You failed.\n", ch)
        return
    }

    act("\$n is gone!", ch, null, null, TO_ROOM)

    char_from_room(ch)
    char_to_room(ch, pRoomIndex)
    act("\$n appears from nowhere.", ch, null, null, TO_ROOM)
    do_look(ch, "auto")
    stop_fighting(ch, true)
}

@Suppress("unused")
fun do_detect_sneak(ch: CHAR_DATA) {

    if (skill_failure_check(ch, Skill.detect_sneak, false, 0, null)) {
        return
    }

    if (RIDDEN(ch) != null) {
        if (is_affected(ch, Skill.detect_sneak)) {
            send_to_char("You can already detect sneaking.\n", ch)
        }
    }
    val af = Affect()
    af.type = Skill.detect_sneak
    af.level = ch.level
    af.duration = ch.level / 10
    af.location = Apply.None
    af.bitvector = AFF_DETECT_SNEAK
    affect_to_char(ch, af)
    send_to_char("You can detect the sneaking.\n", ch)
}


fun do_fade(ch: CHAR_DATA) {
    if (skill_failure_check(ch, Skill.fade, false, 0, null)) {
        return
    }

    if (RIDDEN(ch) != null) {
        send_to_char("You can't fade while being ridden.\n", ch)
        return
    }

    send_to_char("You attempt to fade.\n", ch)

    ch.affected_by = SET_BIT(ch.affected_by, AFF_FADE)
    check_improve(ch, Skill.fade, true, 3)

}

fun do_vtouch(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.vampiric_touch, false, 0, "You lack the skill to draining touch.\n")) {
        return
    }

    if (!IS_VAMPIRE(ch)) {
        send_to_char("Let it be.\n", ch)
        return
    }

    val victim = get_char_room(ch, argument)
    if (victim == null) {
        send_to_char("You do not see that person here.\n", ch)
        return
    }

    if (ch == victim) {
        send_to_char("Even you are not that stupid.\n", ch)
        return
    }

    if (IS_AFFECTED(ch, AFF_CHARM) && victim == ch.leader) {
        send_to_char("You don't want to drain your master.\n", ch)
        return
    }

    if (IS_AFFECTED(victim, AFF_CHARM)) {
        send_to_char("Your victim is already sleeping.\n", ch)
        return
    }

    if (is_affected(victim, Skill.blackguard)) {
        act("\$N's doesn't let you to go that much close.", ch, null, victim, TO_CHAR)
        return
    }

    if (is_affected(victim, Skill.vampiric_touch)) {
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    victim.last_fight_time = current_time
    ch.last_fight_time = current_time

    WAIT_STATE(ch, Skill.vampiric_touch.beats)

    if (ch.pcdata == null || randomPercent() < 0.85 * get_skill(ch, Skill.vampiric_touch)) {
        act("You deadly touch  \$N's neck and put \$M to nightmares.", ch, null, victim, TO_CHAR)
        act("\$n's deadly touch your neck and puts you to nightmares.", ch, null, victim, TO_VICT)
        act("\$n's deadly touch \$N's neck and puts \$M to nightmares.", ch, null, victim, TO_NOTVICT)
        check_improve(ch, Skill.vampiric_touch, true, 1)

        val af = Affect()
        af.type = Skill.vampiric_touch
        af.level = ch.level
        af.duration = ch.level / 50 + 1
        af.bitvector = AFF_SLEEP
        affect_join(victim, af)

        if (IS_AWAKE(victim)) {
            victim.position = Pos.Sleeping
        }
    } else {

        damage(ch, victim, 0, Skill.vampiric_touch, DT.None, true)
        check_improve(ch, Skill.vampiric_touch, false, 1)
        if (!can_see(victim, ch)) {
            do_yell(victim, "Help! I'm being strangled by someone!")
        } else {
            val buf = TextBuffer()
            buf.sprintf("Help! I'm being attacked by %s!",
                    if (is_affected(ch, Skill.doppelganger) && !IS_IMMORTAL(victim)) ch.doppel!!.name else ch.name)
            if (victim.pcdata != null) {
                do_yell(victim, buf.toString())
            }
        }
        val af = Affect()
        af.type = Skill.blackguard
        af.level = victim.level
        af.duration = 2 + victim.level / 50
        af.location = Apply.None
        affect_join(victim, af)
    }
}

fun do_fly(ch: CHAR_DATA, argument: String) {

    if (ch.pcdata == null) {
        return
    }

    val arg = StringBuilder()
    one_argument(argument, arg)
    if (eq(arg.toString(), "up")) {
        if (IS_AFFECTED(ch, AFF_FLYING)) {
            send_to_char("You are already flying.\n", ch)
            return
        }
        if (is_affected(ch, Skill.fly) || ch.race.aff and AFF_FLYING != 0L || affect_check_obj(ch, AFF_FLYING)) {
            ch.affected_by = SET_BIT(ch.affected_by, AFF_FLYING)
            ch.act = REMOVE_BIT(ch.act, PLR_CHANGED_AFF)
            send_to_char("You start to fly.\n", ch)
        } else {
            send_to_char("To fly , find a potion or wings.\n", ch)
        }
    } else if (eq(arg.toString(), "down")) {
        if (IS_AFFECTED(ch, AFF_FLYING)) {
            ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_FLYING)
            ch.act = SET_BIT(ch.act, PLR_CHANGED_AFF)
            send_to_char("You slowly touch the ground.\n", ch)
        } else {
            send_to_char("You are already on the ground.\n", ch)
            return
        }
    } else {
        send_to_char("Type fly with 'up' or 'down'.\n", ch)
        return
    }

    WAIT_STATE(ch, Skill.fly.beats)
}


fun do_push(ch: CHAR_DATA, argument: String) {
    var rest = argument

    if (skill_failure_check(ch, Skill.push, false, 0, null)) {
        return
    }

    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg1)
    one_argument(rest, arg2)

    if (arg1.isEmpty() || arg2.isEmpty()) {
        send_to_char("Push whom to what diretion?\n", ch)
        return
    }

    if (ch.pcdata == null && IS_SET(ch.affected_by, AFF_CHARM)
            && ch.master != null) {
        send_to_char("You are to dazed to push anyone.\n", ch)
        return
    }

    val victim = get_char_room(ch, arg1.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim.pcdata != null && get_pc(victim) == null) {
        send_to_char("You can't do that.\n", ch)
        return
    }

    if (victim == ch) {
        send_to_char("That's pointless.\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        return
    }

    if (victim.position == Pos.Fighting) {
        send_to_char("Wait till the end of fight.\n", ch)
        return
    }

    val door = find_exit(ch, arg2.toString())
    if (door > 0) {
        /* 'push' */
        val pexit = if (door >= 0) ch.room.exit[door] else null
        if (pexit != null) {
            if (IS_SET(pexit.exit_info, EX_IS_DOOR)) {
                if (IS_SET(pexit.exit_info, EX_CLOSED)) {
                    send_to_char("Direction is closed.\n", ch)
                } else if (IS_SET(pexit.exit_info, EX_LOCKED)) {
                    send_to_char("Direction is locked.\n", ch)
                }
                return
            }
        }

        if (IS_AFFECTED(ch, AFF_WEB)) {
            send_to_char("You're webbed, and want to do WHAT?!?\n", ch)
            act("\$n stupidly tries to push \$N while webbed.", ch, null, victim, TO_ROOM)
            return
        }

        if (IS_AFFECTED(victim, AFF_WEB)) {
            act("You attempt to push \$N, but the webs hold \$m in place.", ch, null, victim, TO_CHAR)
            act("\$n attempts to push \$n, but fails as the webs hold \$n in place.", ch, null, victim, TO_ROOM)
            return
        }

        ch.last_death_time = -1

        WAIT_STATE(ch, Skill.push.beats)
        var percent = randomPercent() + if (IS_AWAKE(victim)) 10 else -50
        percent += if (can_see(victim, ch)) -10 else 0

        val buf = TextBuffer()

        if (/* ch.level + 5 < victim.level || */
        victim.position == Pos.Fighting || ch.pcdata != null && percent > get_skill(ch, Skill.push)) {
            /* Failure. */

            send_to_char("Oops.\n", ch)
            if (!IS_AFFECTED(victim, AFF_SLEEP)) {
                victim.position = if (victim.position == Pos.Sleeping) Pos.Standing else victim.position
                act("\$n tried to push you.\n", ch, null, victim, TO_VICT)
            }
            act("\$n tried to push \$N.\n", ch, null, victim, TO_NOTVICT)
            buf.sprintf("Keep your hands out of me, %s!", ch.name)

            if (IS_AWAKE(victim)) do_yell(victim, buf.toString())
            if (ch.pcdata != null && victim.pcdata == null) {
                check_improve(ch, Skill.push, false, 2)
                multi_hit(victim, ch, null)
            }
            return
        }


        buf.sprintf("{YYou push \$N to %s.{x", dir_name[door])
        act(buf.toString(), ch, null, victim, TO_CHAR, Pos.Sleeping)
        buf.clear()
        buf.sprintf("{Y\$n pushes you to %s.{x", dir_name[door])
        act(buf.toString(), ch, null, victim, TO_VICT, Pos.Sleeping)
        buf.clear()
        buf.sprintf("{Y\$n pushes \$N to %s.{x", dir_name[door])
        act(buf.toString(), ch, null, victim, TO_NOTVICT, Pos.Sleeping)
        move_char(victim, door)

        check_improve(ch, Skill.push, true, 1)
    }
}


fun do_crecall(ch: CHAR_DATA) {
    var point = ROOM_VNUM_BATTLE

    if (skill_failure_check(ch, Skill.cabal_recall, false, 0, null)) {
        return
    }

    if (is_affected(ch, Skill.cabal_recall)) {
        send_to_char("You can't pray now.\n", ch)
    }
    val pc = get_pc(ch)
    if (pc != null && current_time - ch.last_fight_time < FIGHT_DELAY_TIME) {
        send_to_char("You are too pumped to pray now.\n", ch)
        return
    }
    if (pc == null && ch.pcdata != null) {
        point = ROOM_VNUM_BATTLE
    }

    act("\$n prays upper lord of Battleragers for transportation!", ch, null, null, TO_ROOM)

    val location = get_room_index(point)
    if (location == null) {
        send_to_char("You are completely lost.\n", ch)
        return
    }

    if (ch.room == location) {
        return
    }

    if (IS_SET(ch.room.room_flags, ROOM_NO_RECALL) || IS_AFFECTED(ch, AFF_CURSE) || IS_RAFFECTED(ch.room, AFF_ROOM_CURSE)) {
        send_to_char("The gods have forsaken you.\n", ch)
        return
    }

    if (ch.fighting != null) {
        send_to_char("You are still fighting!.\n", ch)
        return
    }

    if (ch.mana < ch.max_mana * 0.3) {
        send_to_char("You don't have enough power to pray now.\n", ch)
        return
    }

    ch.move /= 2
    ch.mana /= 10
    act("\$n disappears.", ch, null, null, TO_ROOM)
    char_from_room(ch)
    char_to_room(ch, location)
    act("\$n appears in the room.", ch, null, null, TO_ROOM)
    do_look(ch, "auto")

    val pet = ch.pet
    if (pet != null) {
        char_from_room(pet)
        char_to_room(pet, location)
        do_look(pet, "auto")
    }

    val af = Affect()
    af.type = Skill.cabal_recall
    af.level = ch.level
    af.duration = ch.level / 6 + 15
    af.location = Apply.None
    affect_to_char(ch, af)

}

fun do_escape(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.escape, false, 0, "Try flee. It may suit you better.\n")) {
        return
    }

    if (ch.fighting == null) {
        if (ch.position == Pos.Fighting) {
            ch.position = Pos.Standing
        }
        send_to_char("You aren't fighting anyone.\n", ch)
        return
    }

    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Escape to what diretion?\n", ch)
        return
    }

    if (RIDDEN(ch) != null) {
        send_to_char("You can't escape while being ridden.\n", ch)
        return
    }

    val was_in = ch.room
    val door = find_exit(ch, arg.toString())
    if (door >= 0) {
        val pexit = was_in.exit[door]
        if (pexit == null || IS_SET(pexit.exit_info, EX_CLOSED) && (!IS_AFFECTED(ch, AFF_PASS_DOOR) || IS_SET(pexit.exit_info, EX_NO_PASS)) && !IS_TRUSTED(ch, ANGEL) || IS_SET(pexit.exit_info, EX_NO_FLEE) || ch.pcdata == null && IS_SET(pexit.to_room.room_flags, ROOM_NO_MOB)) {
            send_to_char("Something prevents you to escape that direction.\n", ch)
            return
        }

        if (randomPercent() > get_skill(ch, Skill.escape)) {
            send_to_char("PANIC! You couldn't escape!\n", ch)
            check_improve(ch, Skill.escape, false, 1)
            return
        }

        check_improve(ch, Skill.escape, true, 1)
        move_char(ch, door)
        val now_in = ch.room
        if (now_in == was_in) {
            send_to_char("You couldn't reach that direction, try another.\n", ch)
            return
        }

        ch.room = was_in
        act("\$n has escaped!", ch, null, null, TO_ROOM)
        ch.room = now_in

        if (ch.pcdata != null) {
            send_to_char("You escaped from combat!  You lose 10 exps.\n", ch)
            gain_exp(ch, -10)
        } else {
            ch.last_fought = null  /* Once fled, the mob will not go after */
        }

        stop_fighting(ch, true)
    } else {
        send_to_char("You chose the wrong direction.\n", ch)
    }
}

fun do_layhands(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.lay_hands, false, 0, "You lack the skill to heal others with touching.\n")) {
        return
    }

    val victim = get_char_room(ch, argument)
    if (victim == null) {
        send_to_char("You do not see that person here.\n", ch)
        return
    }

    if (is_affected(ch, Skill.lay_hands)) {
        send_to_char("You can't concentrate enough.\n", ch)
        return
    }
    WAIT_STATE(ch, Skill.lay_hands.beats)
    val af = Affect()
    af.type = Skill.lay_hands
    af.level = ch.level
    af.duration = 2
    af.location = Apply.None
    affect_to_char(ch, af)

    victim.hit = Math.min(victim.hit + ch.level * 2, victim.max_hit)
    update_pos(victim)
    send_to_char("A warm feeling fills your body.\n", victim)
    if (ch != victim) {
        send_to_char("Ok.\n", ch)
    }
    check_improve(ch, Skill.lay_hands, true, 1)

}

fun mount_success(ch: CHAR_DATA, mount: CHAR_DATA, canattack: Boolean): Boolean {
    var percent = randomPercent() + if (ch.level < mount.level)
        (mount.level - ch.level) * 3
    else
        (mount.level - ch.level) * 2

    if (ch.fighting == null) {
        percent -= 25
    }

    if (ch.pcdata != null && IS_DRUNK(ch)) {
        percent += get_skill(ch, Skill.riding) / 2
        send_to_char("Due to your being under the influence, riding seems a bit harder...\n", ch)
    }

    val success = percent - get_skill(ch, Skill.riding)
    if (success <= 0) { /* Success */
        check_improve(ch, Skill.riding, true, 1)
        return true
    } else {
        check_improve(ch, Skill.riding, false, 1)
        if (success >= 10 && MOUNTED(ch) == mount) {
            act("You lose control and fall off of \$N.", ch, null, mount, TO_CHAR)
            act("\$n loses control and falls off of \$N.", ch, null, mount, TO_ROOM)
            act("\$n loses control and falls off of you.", ch, null, mount, TO_VICT)

            ch.riding = false
            mount.riding = false
            if (ch.position > Pos.Stunned) {
                ch.position = Pos.Sitting
            }

            /*  if (ch.hit > 2) { */
            ch.hit -= 5
            update_pos(ch)

        }
        if (success >= 40 && canattack) {
            act("\$N doesn't like the way you've been treating \$M.", ch, null, mount, TO_CHAR)
            act("\$N doesn't like the way \$n has been treating \$M.", ch, null, mount, TO_ROOM)
            act("You don't like the way \$n has been treating you.", ch, null, mount, TO_VICT)

            act("\$N snarls and attacks you!", ch, null, mount, TO_CHAR)
            act("\$N snarls and attacks \$n!", ch, null, mount, TO_ROOM)
            act("You snarl and attack \$n!", ch, null, mount, TO_VICT)

            damage(mount, ch, number_range(1, mount.level), Skill.kick, DT.Bash, true)

            /*      multi_hit( mount, ch, TYPE_UNDEFINED ); */
        }
    }
    return false
}

/*
 * It is not finished yet to implement all.
 */

fun do_mount(ch: CHAR_DATA, argument: String) {

    val arg = StringBuilder()
    one_argument(argument, arg)

    if (skill_failure_check(ch, Skill.riding, true, 0, "You don't know how to ride!\n")) {
        return
    }

    if (arg.isEmpty()) {
        send_to_char("Mount what?\n", ch)
        return
    }

    val mount = get_char_room(ch, arg.toString())
    if (mount == null) {
        send_to_char("You don't see that here.\n", ch)
        return
    }

    if (mount.pcdata != null
            || !IS_SET(mount.act, ACT_RIDEABLE)
            || IS_SET(mount.act, ACT_NOTRACK)) {
        send_to_char("You can't ride that.\n", ch)
        return
    }

    if (mount.level - 5 > ch.level) {
        send_to_char("That beast is too powerful for you to ride.", ch)
        return
    }

    val mount_mount = mount.mount
    if (mount_mount != null && !mount.riding && mount_mount != ch) {
        val buf = TextBuffer()
        buf.sprintf("%s belongs to %s, not you.\n", mount.short_desc, mount_mount.name)
        send_to_char(buf, ch)
        return
    }

    if (mount.position < Pos.Standing) {
        send_to_char("Your mount must be standing.\n", ch)
        return
    }

    if (RIDDEN(mount) != null) {
        send_to_char("This beast is already ridden.\n", ch)
        return
    } else if (MOUNTED(ch) != null) {
        send_to_char("You are already riding.\n", ch)
        return
    }

    if (!mount_success(ch, mount, true)) {
        send_to_char("You fail to mount the beast.\n", ch)
        return
    }

    act("You hop on \$N's back.", ch, null, mount, TO_CHAR)
    act("\$n hops on \$N's back.", ch, null, mount, TO_NOTVICT)
    act("\$n hops on your back!", ch, null, mount, TO_VICT)

    ch.mount = mount
    ch.riding = true
    mount.mount = ch
    mount.riding = true

    /* No sneaky people on mounts */
    affect_strip(ch, Skill.sneak)
    ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_SNEAK)
    affect_strip(ch, Skill.hide)
    ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_HIDE)
    affect_strip(ch, Skill.fade)
    ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_FADE)
    affect_strip(ch, Skill.improved_invis)
    ch.affected_by = REMOVE_BIT(ch.affected_by, AFF_IMP_INVIS)
}

fun do_dismount(ch: CHAR_DATA) {
    val mount = MOUNTED(ch)
    if (mount != null) {
        act("You dismount from \$N.", ch, null, mount, TO_CHAR)
        act("\$n dismounts from \$N.", ch, null, mount, TO_NOTVICT)
        act("\$n dismounts from you.", ch, null, mount, TO_VICT)

        ch.riding = false
        mount.riding = false
    } else {
        send_to_char("You aren't mounted.\n", ch)
    }
}

fun send_arrow(ch: CHAR_DATA, victim: CHAR_DATA, arrow: Obj, door: Int, chanceOrig: Int, bonus: Int): Int {
    var chance = chanceOrig
    var damroll = 0
    var hitroll = 0
    val sn = if (arrow.weaponType == WeaponType.Staff) Skill.spear else Skill.arrow

    var paf = arrow.affected
    while (paf != null) {
        if (paf.location == Apply.Damroll) {
            damroll += paf.modifier
        }
        if (paf.location == Apply.Hitroll) {
            hitroll += paf.modifier
        }
        paf = paf.next
    }

    var dest_room = ch.room
    chance += (hitroll + ch.tohit + (ch.curr_stat(PrimeStat.Dexterity) - 18)) * 2
    damroll *= 10
    var i = 0
    while (i < 1000) {
        chance -= 10
        if (victim.room == dest_room) {
            if (randomPercent() < chance) {
                if (check_obj_dodge(ch, victim, arrow, chance)) {
                    return 0
                }
                act("\$p strikes you!", victim, arrow, null, TO_CHAR)
                act("Your \$p strikes \$N!", ch, arrow, victim, TO_CHAR)
                if (ch.room == victim.room) {
                    act("\$n's \$p strikes \$N!", ch, arrow, victim, TO_NOTVICT)
                } else {
                    act("\$n's \$p strikes \$N!", ch, arrow, victim, TO_ROOM)
                    act("\$p strikes \$n!", victim, arrow, null, TO_ROOM)
                }
                if (is_safe(ch, victim) || victim.pcdata == null && IS_SET(victim.act, ACT_NOTRACK)) {
                    act("\$p falls from \$n doing no visible damage...", victim, arrow, null, TO_ALL)
                    act("\$p falls from \$n doing no visible damage...", ch, arrow, null, TO_CHAR)
                    obj_to_room(arrow, victim.room)
                } else {
                    var dam = dice(arrow.value[1].toInt(), arrow.value[2].toInt())
                    dam = number_range(dam, 3 * dam)
                    dam += damroll + bonus + 10 * ch.todam

                    if (IS_WEAPON_STAT(arrow, WEAPON_POISON)) {
                        val poison = affect_find(arrow.affected, Skill.poison)
                        val level = when (poison) {
                            null -> arrow.level
                            else -> poison.level
                        }
                        if (!saves_spell(level, victim, DT.Poison)) {
                            send_to_char("You feel poison coursing through your veins.", victim)
                            act("\$n is poisoned by the venom on \$p.", victim, arrow, null, TO_ROOM)

                            val af = Affect()
                            af.type = Skill.poison
                            af.level = level * 3 / 4
                            af.duration = level / 2
                            af.location = Apply.Str
                            af.modifier = -1
                            af.bitvector = AFF_POISON
                            affect_join(victim, af)
                        }

                    }
                    if (IS_WEAPON_STAT(arrow, WEAPON_FLAMING)) {
                        act("\$n is burned by \$p.", victim, arrow, null, TO_ROOM)
                        act("\$p sears your flesh.", victim, arrow, null, TO_CHAR)
                        fire_effect(victim, arrow.level, dam, TARGET_CHAR)
                    }
                    if (IS_WEAPON_STAT(arrow, WEAPON_FROST)) {
                        act("\$p freezes \$n.", victim, arrow, null, TO_ROOM)
                        act("The cold touch of \$p surrounds you with ice.",
                                victim, arrow, null, TO_CHAR)
                        cold_effect(victim, arrow.level, dam, TARGET_CHAR)
                    }
                    if (IS_WEAPON_STAT(arrow, WEAPON_SHOCKING)) {
                        act("\$n is struck by lightning from \$p.", victim, arrow, null, TO_ROOM)
                        act("You are shocked by \$p.", victim, arrow, null, TO_CHAR)
                        shock_effect(victim, arrow.level, dam, TARGET_CHAR)
                    }

                    if (dam > victim.max_hit / 10 && randomPercent() < 50) {
                        val af = Affect()
                        af.type = sn
                        af.level = ch.level
                        af.duration = -1
                        af.location = Apply.Hitroll
                        af.modifier = -(dam / 20)
                        if (victim.pcdata != null) {
                            af.bitvector = AFF_CORRUPTION
                        }

                        affect_join(victim, af)

                        obj_to_char(arrow, victim)
                        equip_char(victim, arrow, Wear.StuckIn)
                    } else {
                        obj_to_room(arrow, victim.room)
                    }

                    damage(ch, victim, dam, sn, DT.Pierce, true)
                    path_to_track(ch, victim, door)

                }
                return 1
            } else {
                obj_to_room(arrow, victim.room)
                act("\$p sticks in the ground at your feet!", victim, arrow, null, TO_ALL)
                return 0
            }
        }
        val pExit = dest_room.exit[door]
        if (pExit == null) {
            break
        } else {
            dest_room = pExit.to_room
            val dirName = dir_name[rev_dir[door]]
            act("\$p sails into the room from the $dirName!", dest_room, arrow, null, TO_ALL)
        }
        i++
    }
    return 0
}


fun do_shoot(ch: CHAR_DATA, argument: String) {
    var rest = argument
    if (skill_failure_check(ch, Skill.bow, false, 0, "You don't know how to shoot.\n")) {
        return
    }

    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg1)
    one_argument(rest, arg2)

    if (arg1.isEmpty() || arg2.isEmpty()) {
        send_to_char("Shoot what diretion and whom?\n", ch)
        return
    }

    if (ch.fighting != null) {
        send_to_char("You cannot concentrate on shooting arrows.\n", ch)
        return
    }

    val direction = find_exit(ch, arg1.toString())

    if (direction < 0 || direction > 5) {
        return
    }

    val range = ch.level / 10 + 1
    val victim = find_char(ch, arg2.toString(), direction, range)
    if (victim == null) {
        send_to_char("You can't see that one.\n", ch)
        return
    }

    if (victim.pcdata != null && get_pc(victim) == null) {
        send_to_char("You can't do that.\n", ch)
        return
    }

    if (victim == ch) {
        send_to_char("That's pointless.\n", ch)
        return
    }

    if (is_at_cabal_area(ch) || is_at_cabal_area(victim)) {
        send_to_char("It is not allowed near cabal areas.\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        val buf = TextBuffer()
        buf.sprintf("Gods protect %s.\n", victim.name)
        send_to_char(buf, ch)
        return
    }

    val wield = get_weapon_char(ch, WeaponType.Bow)
    if (wield == null) {
        send_to_char("You need a bow to shoot!\n", ch)
        return
    }

    val arrow = get_weapon_char(ch, WeaponType.Arrow)
    if (arrow == null) {
        send_to_char("You need an arrow holding for your ammunition!\n", ch)
        return
    }

    WAIT_STATE(ch, Skill.bow.beats)

    var chance = (get_skill(ch, Skill.bow) - 50) * 2
    if (ch.position == Pos.Sleeping) {
        chance += 40
    }
    if (ch.position == Pos.Resting) {
        chance += 10
    }
    if (victim.position == Pos.Fighting) {
        chance -= 40
    }
    chance += GET_HITROLL(ch)

    val buf = TextBuffer()
    buf.sprintf("You shoot \$p to %s.", dir_name[direction])
    act(buf.toString(), ch, arrow, null, TO_CHAR)
    buf.sprintf("\$n shoots \$p to %s.", dir_name[direction])
    act(buf.toString(), ch, arrow, null, TO_ROOM)

    obj_from_char(arrow)
    send_arrow(ch, victim, arrow, direction, chance, dice(wield.value[1].toInt(), wield.value[2].toInt()))
    check_improve(ch, Skill.bow, true, 1)
}


fun find_way(ch: CHAR_DATA, startRoom: ROOM_INDEX_DATA, rend: ROOM_INDEX_DATA): String {
    var rstart = startRoom
    val buf = StringBuilder("Find: ")
    var i = 0
    while (i < 65535) {
        if (rend == rstart) {
            return buf.toString()
        }
        val direction = find_path(rstart.vnum, rend.vnum, ch, -40000, false)
        if (direction == -1) {
            buf.append(" BUGGY")
            return buf.toString()
        }
        if (direction < 0 || direction > 5) {
            buf.append(" VERY BUGGY")
            return buf.toString()
        }
        buf.append(dir_name[direction][0])
        /* find target room */
        val pExit = rstart.exit[direction]
        if (pExit == null) {
            buf.append(" VERY VERY BUGGY")
            return buf.toString()
        } else {
            rstart = pExit.to_room
        }
        i++
    }
    return buf.toString()
}

fun do_human(ch: CHAR_DATA) {
    if (ch.clazz !== Clazz.VAMPIRE) {
        send_to_char("Huh?\n", ch)
        return
    }

    if (!IS_VAMPIRE(ch)) {
        send_to_char("You are already a human.\n", ch)
        return
    }

    affect_strip(ch, Skill.vampire)
    ch.act = REMOVE_BIT(ch.act, PLR_VAMPIRE)
    send_to_char("You return to your original size.\n", ch)
}


fun do_throw_spear(ch: CHAR_DATA, argument: String) {
    var rest = argument

    if (skill_failure_check(ch, Skill.spear, true, 0, "You don't know how to throw a spear.\n")) {
        return
    }

    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg1)
    one_argument(rest, arg2)

    if (arg1.isEmpty() || arg2.isEmpty()) {
        send_to_char("Throw spear what diretion and whom?\n", ch)
        return
    }

    if (ch.fighting != null) {
        send_to_char("You cannot concentrate on throwing spear.\n", ch)
        return
    }

    val direction = find_exit(ch, arg1.toString())

    if (direction < 0 || direction > 5) {
        send_to_char("Throw which direction and whom?\n", ch)
        return
    }
    val range = ch.level / 10 + 1
    val victim = find_char(ch, arg2.toString(), direction, range)
    if (victim == null) {
        send_to_char("You can't see that one.\n", ch)
        return
    }

    if (victim.pcdata != null && get_pc(victim) == null) {
        send_to_char("You can't do that.\n", ch)
        return
    }

    if (victim == ch) {
        send_to_char("That's pointless.\n", ch)
        return
    }

    if (is_at_cabal_area(ch) || is_at_cabal_area(victim)) {
        send_to_char("It is not allowed near cabal areas.\n", ch)
        return
    }

    if (is_safe(ch, victim)) {
        val buf = TextBuffer()
        buf.sprintf("Gods protect %s.\n", victim.name)
        send_to_char(buf, ch)
        return
    }

    val spear = get_weapon_char(ch, WeaponType.Staff)
    if (spear == null) {
        send_to_char("You need a spear to throw!\n", ch)
        return
    }


    WAIT_STATE(ch, Skill.spear.beats)

    var chance = (get_skill(ch, Skill.spear) - 50) * 2
    if (ch.position == Pos.Sleeping) {
        chance += 40
    }
    if (ch.position == Pos.Resting) {
        chance += 10
    }
    if (victim.position == Pos.Fighting) {
        chance -= 40
    }
    chance += GET_HITROLL(ch)
    val buf = TextBuffer()
    buf.sprintf("You throw \$p to %s.", dir_name[direction])
    act(buf.toString(), ch, spear, null, TO_CHAR)
    buf.sprintf("\$n throws \$p to %s.", dir_name[direction])
    act(buf.toString(), ch, spear, null, TO_ROOM)

    obj_from_char(spear)
    send_arrow(ch, victim, spear, direction, chance, dice(spear.value[1].toInt(), spear.value[2].toInt()))
    check_improve(ch, Skill.spear, true, 1)
}

fun get_weapon_char(ch: CHAR_DATA?, wType: WeaponType)
        = ch?.carrying?.firstOrNull { it.item_type === ItemType.Weapon && (it.wear_loc.isHold()) && it.weaponType === wType }
