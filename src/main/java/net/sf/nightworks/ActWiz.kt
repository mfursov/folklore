package net.sf.nightworks

import net.sf.nightworks.act.info.do_look
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.AttackType
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.EXTRA_DESC_DATA
import net.sf.nightworks.model.Ethos
import net.sf.nightworks.model.HomeTown
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Liquid
import net.sf.nightworks.model.MOB_INDEX_DATA
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.ObjProto
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.ROOM_HISTORY_DATA
import net.sf.nightworks.model.ROOM_INDEX_DATA
import net.sf.nightworks.model.Religion
import net.sf.nightworks.model.Sex
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.Wiznet
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.model.max
import net.sf.nightworks.util.CANT_CHANGE_TITLE
import net.sf.nightworks.util.CAN_WEAR
import net.sf.nightworks.util.GET_AC
import net.sf.nightworks.util.GET_DAMROLL
import net.sf.nightworks.util.GET_HITROLL
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_OBJ_STAT
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_TRUSTED
import net.sf.nightworks.util.PERS
import net.sf.nightworks.util.RACE_OK
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.UPPER
import net.sf.nightworks.util.atoi
import net.sf.nightworks.util.capitalize
import net.sf.nightworks.util.capitalize_nn
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.get_carry_weight
import net.sf.nightworks.util.is_number
import net.sf.nightworks.util.log_string
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.smash_tilde
import net.sf.nightworks.util.sprintf
import net.sf.nightworks.util.startsWith
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Date
import java.util.Formatter

fun do_cabal_scan(ch: CHAR_DATA) {
    if (!IS_IMMORTAL(ch) && (ch.cabal == Clan.None || ch.pcdata == null)) {
        send_to_char("You are not a cabal member yet.\n", ch)
        return
    }
    val buf1 = TextBuffer()
    val buf2 = TextBuffer()
    for (c in Clan.values()) {
        var show = IS_IMMORTAL(ch) || ch.cabal == c
        val cabal_obj = c.obj_ptr
        sprintf(buf1, " Cabal: %-11s, room %4d, item %4d, ptr: %-20s ",
                c.short_name,
                c.room_vnum,
                c.obj_vnum,
                cabal_obj?.short_desc ?: "")

        if (cabal_obj != null) {
            var in_obj: Obj = cabal_obj
            while (in_obj.in_obj != null) {
                in_obj = in_obj.in_obj!!
            }
            if (in_obj.carried_by != null) {
                sprintf(buf2, "\n\t\tcarried_by: %s\n", PERS(in_obj.carried_by, ch))
            } else {
                val in_room = in_obj.in_room
                sprintf(buf2, "\n\t\t\t\t\tin room: %s\n", in_room?.name ?: "unknown")
                if (in_room != null && in_room.vnum == ch.cabal.room_vnum) {
                    show = true
                }
            }
        }

        if (show) {
            send_to_char(buf1, ch)
            send_to_char(buf2, ch)
        }
    }
}

fun do_objlist(ch: CHAR_DATA) {
    try {
        FileWriter("objlist.txt").use { fp ->
            val ff = Formatter(fp)
            for (obj in Index.OBJECTS) {
                if (CAN_WEAR(obj, ITEM_WIELD) && obj.level < 25 && obj.level > 15) {
                    ff.format("\n#Obj: %s (Vnum : %d) \n", obj.short_desc, obj.pIndexData.vnum)
                    ff.format("Object '%s' is type %s, extra flags %s.\nWeight is %d, value is %d, level is %d.\n",
                            obj.name,
                            obj.item_type.displayName,
                            extra_bit_name(obj.extra_flags),
                            obj.weight / 10,
                            obj.cost,
                            obj.level
                    )

                    when (obj.item_type) {
                        ItemType.Scroll, ItemType.Potion, ItemType.Pill -> {
                            ff.format("Level %d spells of:", obj.value[0])

                            if (obj.value[1] in 0..(MAX_SKILL - 1)) {
                                ff.format(" '%s'", Skill.skills[obj.value[1].toInt()].skillName)
                            }

                            if (obj.value[2] in 0..(MAX_SKILL - 1)) {
                                ff.format(" '%s'", Skill.skills[obj.value[2].toInt()].skillName)
                            }

                            if (obj.value[3] in 0..(MAX_SKILL - 1)) {
                                ff.format(" '%s'", Skill.skills[obj.value[3].toInt()].skillName)
                            }

                            if (obj.value[4] in 0..(MAX_SKILL - 1)) {
                                ff.format(" '%s'", Skill.skills[obj.value[4].toInt()].skillName)
                            }

                            ff.format(".\n")
                        }

                        ItemType.Wand, ItemType.Staff -> {
                            ff.format("Has %d charges of level %d", obj.value[2], obj.value[0])

                            if (obj.value[3] in 0..(MAX_SKILL - 1)) {
                                ff.format(" '%s'", Skill.skills[obj.value[3].toInt()].skillName)
                            }

                            ff.format(".\n")
                        }

                        ItemType.DrinkContainer -> ff.format("It holds %s-colored %s.\n",
                                Liquid.of(obj.value[2]).liq_color, Liquid.of(obj.value[2]).liq_name)

                        ItemType.Container -> {
                            ff.format("Capacity: %d#  Maximum weight: %d#  flags: %s\n",
                                    obj.value[0], obj.value[3], cont_bit_name(obj.value[1]))
                            if (obj.value[4] != 100L) {
                                ff.format("Weight multiplier: %d%%\n", obj.value[4])
                            }
                        }

                        ItemType.Weapon -> {
                            ff.format("Weapon type is ${obj.weaponType.typeName}.\n")
                            if (obj.pIndexData.new_format) {
                                ff.format("Damage is %dd%d (average %d).\n",
                                        obj.value[1], obj.value[2], (1 + obj.value[2]) * obj.value[1] / 2)
                            } else {
                                ff.format("Damage is %d to %d (average %d).\n",
                                        obj.value[1], obj.value[2], (obj.value[1] + obj.value[2]) / 2)
                            }
                            if (obj.value[4] != 0L) { /* weapon flags */
                                ff.format("Weapons flags: %s\n", weapon_bit_name(obj.value[4]))
                            }
                        }

                        ItemType.Armor -> ff.format("Armor class is %d pierce, %d bash, %d slash, and %d vs. magic.\n",
                                obj.value[0], obj.value[1], obj.value[2], obj.value[3])
                        else -> {
                        }
                    }
                    var paf: Affect? = obj.pIndexData.affected
                    while (paf != null) {
                        ff.format("  Affects %s by %d.\n", paf.location.locName, paf.modifier)
                        if (paf.bitvector != 0L) {
                            when (paf.where) {
                                TO_AFFECTS -> ff.format("   Adds %s affect.\n", affect_bit_name(paf.bitvector))
                                TO_OBJECT -> ff.format("   Adds %s object flag.\n", extra_bit_name(paf.bitvector))
                                TO_IMMUNE -> ff.format("   Adds immunity to %s.\n", imm_bit_name(paf.bitvector))
                                TO_RESIST -> ff.format("   Adds resistance to %s.\n", imm_bit_name(paf.bitvector))
                                TO_VULN -> ff.format("   Adds vulnerability to %s.\n", imm_bit_name(paf.bitvector))
                                else -> ff.format("   Unknown bit %d: %d\n", paf.where, paf.bitvector)
                            }
                        }
                        paf = paf.next
                    }
                }
            }
        }
    } catch (e: IOException) {
        send_to_char("File error.\n", ch)
        e.printStackTrace()
    }

}

fun do_limited(ch: CHAR_DATA, argument: String) {
    var lCount = 0
    var nMatch = 0

    val buf = TextBuffer()
    if (argument.isNotEmpty()) {
        val obj_index = get_obj_index(atoi(argument))
        if (obj_index == null) {
            send_to_char("Not found.\n", ch)
            return
        }
        if (obj_index.limit == -1) {
            send_to_char("Thats not a limited item.\n", ch)
            return
        }

        buf.sprintf("%-35s [%5d]  Limit: %3d  Current: %3d\n", obj_index.short_descr, obj_index.vnum, obj_index.limit, obj_index.count)
        buf.buffer.setCharAt(0, buf.buffer[0])
        send_to_char(buf, ch)
        var inGameCount = 0
        for (obj in Index.OBJECTS) {
            if (obj.pIndexData.vnum == obj_index.vnum) {
                inGameCount++
                if (obj.carried_by != null) {
                    buf.sprintf("Carried by %-30s\n", obj.carried_by!!.name)
                }
                if (obj.in_room != null) {
                    buf.sprintf("At %-20s [%d]\n", obj.in_room!!.name, obj.in_room!!.vnum)
                }
                if (obj.in_obj != null) {
                    buf.sprintf("In %-20s [%d] \n", obj.in_obj!!.short_desc, obj.in_obj!!.pIndexData.vnum)
                }
                send_to_char(buf, ch)
            }
        }
        buf.sprintf("  %d found in game. %d should be in pFiles.\n", inGameCount, obj_index.count - inGameCount)
        send_to_char(buf, ch)
        return
    }

    val output = TextBuffer()
    for (obj_index in Index.OBJ_INDEX.values) {
        nMatch++
        if (obj_index.limit != -1) {
            lCount++
            buf.sprintf("%-37s [%5d]  Limit: %3d  Current: %3d\n",
                    obj_index.short_descr,
                    obj_index.vnum,
                    obj_index.limit,
                    obj_index.count)
            buf.buffer.setCharAt(0, buf.buffer[0])
            output.append(buf.buffer)
        }
    }

    buf.sprintf("\n%d of %d objects are limited.\n", lCount, nMatch)
    output.append(buf.buffer)
    page_to_char(output, ch)
}

fun do_wiznet(ch: CHAR_DATA, argument: String) {
    if (argument.isEmpty()) {
        if (ch.wiznet.contains(Wiznet.On)) {
            send_to_char("Signing off of Wiznet.\n", ch)
            ch.wiznet.remove(Wiznet.On)
        } else {
            send_to_char("Welcome to Wiznet!\n", ch)
            ch.wiznet.add(Wiznet.On)
        }
        return
    } else if (startsWith(argument, "on")) {
        send_to_char("Welcome to Wiznet!\n", ch)
        ch.wiznet.add(Wiznet.On)
        return
    } else if (startsWith(argument, "off")) {
        send_to_char("Signing off of Wiznet.\n", ch)
        ch.wiznet.remove(Wiznet.On)
        return
    }

    val buf = TextBuffer()

    /* show wiznet status */
    if (startsWith(argument, "status")) {
        if (!ch.wiznet.contains(Wiznet.On)) {
            buf.append("off ")
        }

        for (w in ch.wiznet.sorted()) {
            buf.append(w.name)
            buf.append(" ")
        }

        buf.append("\n")

        send_to_char("Wiznet status:\n", ch)
        send_to_char(buf, ch)
        return
    }

    if (startsWith(argument, "show"))
    /* list of all wiznet options */ {
        buf.clear()

        for (w in Wiznet.values()) {
            if (w.level <= get_trust(ch)) {
                buf.append(w.name)
                buf.append(" ")
            }
        }

        buf.append("\n")

        send_to_char("Wiznet options available to you are:\n", ch)
        send_to_char(buf, ch)
        return
    }

    val flag = Wiznet.of(argument)
    if (flag == null || get_trust(ch) < flag.level) {
        send_to_char("No such option.\n", ch)
    } else if (ch.wiznet.contains(flag)) {
        send_to_char("You will no longer see ${flag.shortName} on wiznet.\n", ch)
        ch.wiznet.remove(flag)
    } else {
        send_to_char("You will now see ${flag.shortName} on wiznet.\n", ch)
        ch.wiznet.add(flag)
    }

}

fun wiznet(string: String, ch: CHAR_DATA?, obj: Obj?, flag: Wiznet?, flag_skip: Wiznet?, min_level: Int) {
    for (pc in Index.PLAYERS) {
        if (IS_IMMORTAL(pc.o_ch)
                && pc.o_ch.wiznet.contains(Wiznet.On)
                && (flag == null || pc.o_ch.wiznet.contains(flag))
                && (flag_skip == null || !pc.o_ch.wiznet.contains(flag_skip))
                && get_trust(pc.o_ch) >= min_level
                && pc.ch != ch) {
            if (pc.o_ch.wiznet.contains(Wiznet.Prefix)) {
                send_to_char("-. ", pc.o_ch)
            }
            if (obj == null) {
                act(string, pc.ch, null, ch, TO_CHAR, Pos.Dead)
            } else {
                act(string, pc.ch, obj, ch, TO_CHAR, Pos.Dead)
            }
        }
    }
}

fun do_tick(ch: CHAR_DATA, argument: String) {
    val argb = StringBuilder()
    one_argument(argument, argb)
    val arg = argb.toString()
    if (arg.isEmpty()) {
        send_to_char("tick area : area update\n", ch)
        send_to_char("tick char : char update\n", ch)
        send_to_char("tick obj  : obj  update\n", ch)
        send_to_char("tick room : room update\n", ch)
        send_to_char("tick track: track update\n", ch)
        return
    }
    when (arg.toLowerCase()) {
        "area" -> {
            area_update()
            send_to_char("Area updated.\n", ch)
        }
        "char player" -> {
            char_update()
            send_to_char("Players updated.\n", ch)
        }
        "obj" -> {
            obj_update()
            send_to_char("Obj updated.\n", ch)
        }
        "room" -> {
            room_update()
            send_to_char("Room updated.\n", ch)
        }
        "track" -> {
            track_update()
            send_to_char("Track updated.\n", ch)
        }
        else -> do_tick(ch, "")
    }
}

/* equips a character */
fun do_outfit(ch: CHAR_DATA) {
    if ((ch.level > 5 || ch.pcdata == null) && !IS_IMMORTAL(ch)) {
        send_to_char("Find it yourself!\n", ch)
        return
    }

    if (ch.carry_number + 1 > can_carry_items(ch)) {
        send_to_char("You can't carry that many items.\n", ch)
        return
    }

    if (get_light_char(ch) == null) {
        val obj = create_object(get_obj_index_nn(OBJ_VNUM_SCHOOL_BANNER), 0)
        obj.cost = 0
        obj.condition = 100
        obj_to_char(obj, ch)
    }

    if (ch.carry_number + 1 > can_carry_items(ch)) {
        send_to_char("You can't carry that many items.\n", ch)
        return
    }

    if (get_eq_char(ch, Wear.Body) == null) {
        val obj = create_object(get_obj_index_nn(OBJ_VNUM_SCHOOL_VEST), 0)
        obj.cost = 0
        obj.condition = 100
        obj_to_char(obj, ch)
    }


    if (ch.carry_number + 1 > can_carry_items(ch)) {
        send_to_char("You can't carry that many items.\n", ch)
        return
    }

    /* do the weapon thing */
    if (get_wield_char(ch, false) == null) {
        val vnum = ch.clazz.weapon
        val obj = create_object(get_obj_index_nn(vnum), 0)
        obj.condition = 100
        obj_to_char(obj, ch)
    }

    val obj = create_object(get_obj_index_nn(OBJ_VNUM_SCHOOL_SHIELD), 0)
    obj.cost = 0
    obj.condition = 100
    obj_to_char(obj, ch)

    send_to_char("You have been given some equipments by gods.\n", ch)
    send_to_char("Type 'inventory' to see the list of the objects that you are carrying.\n", ch)
    send_to_char("Try 'wear <object name>' to wear the object.\r\n\n", ch)
}

/* RT nochannels command, for those spammers */

fun do_nochannels(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Nochannel whom?", ch)
        return
    }

    val victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (get_trust(victim) >= get_trust(ch)) {
        send_to_char("You failed.\n", ch)
        return
    }

    if (IS_SET(victim.comm, COMM_NOCHANNELS)) {
        victim.comm = REMOVE_BIT(victim.comm, COMM_NOCHANNELS)
        send_to_char("The gods have restored your channel priviliges.\n", victim)
        send_to_char("NOCHANNELS removed.\n", ch)
        wiznet("\$N restores channels to ${victim.name}", ch, null, Wiznet.Penalties, Wiznet.Secure, 0)
    } else {
        victim.comm = SET_BIT(victim.comm, COMM_NOCHANNELS)
        send_to_char("The gods have revoked your channel priviliges.\n", victim)
        send_to_char("NOCHANNELS set.\n", ch)
        wiznet("\$N revokes ${victim.name}'s channels.", ch, null, Wiznet.Penalties, Wiznet.Secure, 0)
    }
}


fun do_smote(ch: CHAR_DATA, argument: String) {
    if (ch.pcdata != null && IS_SET(ch.comm, COMM_NOEMOTE)) {
        send_to_char("You can't show your emotions.\n", ch)
        return
    }

    if (argument.isEmpty()) {
        send_to_char("Emote what?\n", ch)
        return
    }

    if (argument.contains(ch.name)) {
        send_to_char("You must include your name in an smote.\n", ch)
        return
    }

    send_to_char(argument, ch)
    send_to_char("\n", ch)

    var matches = 0
    for (vch in ch.room.people) {
        if (vch.pcdata == null || vch == ch) {
            continue
        }
        var letter = argument.indexOf(vch.name)
        if (letter == -1) {
            send_to_char(argument, vch)
            send_to_char("\n", vch)
            continue
        }

        val temp = StringBuilder(argument.substring(0, letter))
        var namePos = 0
        val last = StringBuilder()
        while (letter < argument.length) {
            val c = argument[letter]
            if (c == '\'' && matches == vch.name.length) {
                temp.append("r")
                letter++
                continue
            }

            if (c == 's' && matches == vch.name.length) {
                matches = 0
                letter++
                continue
            }

            if (matches == vch.name.length) {
                matches = 0
            }

            if (c == vch.name[namePos]) {
                matches++
                namePos++
                if (matches == vch.name.length) {
                    temp.append("you")
                    last.setLength(0)
                    namePos = 0
                    letter++
                    continue
                }
                last.append(letter)
                letter++
                continue
            }

            matches = 0
            temp.append(last).append(letter)
            last.setLength(0)
            namePos = 0
            letter++
        }

        send_to_char(temp, vch)
        send_to_char("\n", vch)
    }

}

fun do_bamfin(ch: CHAR_DATA, argument: String) {
    var rest = argument

    val pcdata = ch.pcdata
    if (pcdata != null) {
        rest = smash_tilde(rest)
        val buf = TextBuffer()
        if (rest.isEmpty()) {
            buf.sprintf("Your poofin is %s\n", pcdata.bamfin)
            send_to_char(buf, ch)
            return
        }

        if (rest.contains(ch.name)) {
            send_to_char("You must include your name.\n", ch)
            return
        }

        pcdata.bamfin = rest

        buf.sprintf("Your poofin is now %s\n", pcdata.bamfin)
        send_to_char(buf, ch)
    }
}


fun do_bamfout(ch: CHAR_DATA, argument: String) {
    var rest = argument

    val pcdata = ch.pcdata
    if (pcdata != null) {
        val buf = TextBuffer()
        rest = smash_tilde(rest)

        if (rest.isEmpty()) {
            buf.sprintf("Your poofout is %s\n", pcdata.bamfout)
            send_to_char(buf, ch)
            return
        }

        if (rest.contains(ch.name)) {
            send_to_char("You must include your name.\n", ch)
            return
        }

        pcdata.bamfout = rest

        buf.sprintf("Your poofout is now %s\n", pcdata.bamfout)
        send_to_char(buf, ch)
    }
}


fun do_deny(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        send_to_char("Deny whom?\n", ch)
        return
    }

    val victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim.pcdata == null) {
        send_to_char("Not on NPC's.\n", ch)
        return
    }

    if (get_trust(victim) >= get_trust(ch)) {
        send_to_char("You failed.\n", ch)
        return
    }

    victim.act = SET_BIT(victim.act, PLR_DENY)
    send_to_char("You are denied access!\n", victim)
    wiznet("\$N denies access to ${victim.name}", ch, null, Wiznet.Penalties, Wiznet.Secure, 0)
    send_to_char("OK.\n", ch)
    save_char_obj(victim)
    stop_fighting(victim, true)
    do_quit(victim)
}


fun do_disconnect(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        send_to_char("Disconnect whom?\n", ch)
        return
    }

    if (is_number(arg)) {
        val desc = atoi(arg.toString())
        for (d in Index.CONNECTIONS) {
            if (d.hashCode() == desc) {
                close_socket(d)
                send_to_char("Ok.\n", ch)
                return
            }
        }
    }

    val victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }
    val pc = get_pc(victim)
    if (pc == null) {
        act("\$N is not logged in.", ch, null, victim, TO_CHAR)
        return
    }

    close_socket(pc.con)
    send_to_char("Ok.\n", ch)
}


fun do_echo(ch: CHAR_DATA, argument: String) {
    if (argument.isEmpty()) {
        send_to_char("Global echo what?\n", ch)
        return
    }

    for (d in Index.PLAYERS) {
        if (get_trust(d.ch) >= get_trust(ch)) {
            send_to_char("global> ", d.ch)
        }
        send_to_char(argument, d.ch)
        send_to_char("\n", d.ch)
    }
}


fun do_recho(ch: CHAR_DATA, argument: String) {
    if (argument.isEmpty()) {
        send_to_char("Local echo what?\n", ch)
        return
    }

    for (pc in Index.PLAYERS) {
        if (pc.ch.room == ch.room) {
            if (get_trust(pc.ch) >= get_trust(ch)) {
                send_to_char("local> ", pc.ch)
            }
            send_to_char(argument, pc.ch)
            send_to_char("\n", pc.ch)
        }
    }
}

fun do_zecho(ch: CHAR_DATA, argument: String) {
    if (argument.isEmpty()) {
        send_to_char("Zone echo what?\n", ch)
        return
    }
    for (pc in Index.PLAYERS) {
        if (pc.ch.room.area == ch.room.area) {
            if (get_trust(pc.ch) >= get_trust(ch)) {
                send_to_char("zone> ", pc.ch)
            }
            send_to_char(argument, pc.ch)
            send_to_char("\n", pc.ch)
        }
    }
}

fun do_pecho(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg = StringBuilder()
    rest = one_argument(rest, arg)

    if (rest.isEmpty() || arg.isEmpty()) {
        send_to_char("Personal echo what?\n", ch)
        return
    }

    val victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("Target not found.\n", ch)
        return
    }

    if (get_trust(victim) >= get_trust(ch) && get_trust(ch) != MAX_LEVEL) {
        send_to_char("personal> ", victim)
    }

    send_to_char(rest, victim)
    send_to_char("\n", victim)
    send_to_char("personal> ", ch)
    send_to_char(rest, ch)
    send_to_char("\n", ch)
}


fun find_location(ch: CHAR_DATA, arg: String): ROOM_INDEX_DATA? {
    if (is_number(arg)) {
        return get_room_index(atoi(arg))
    }

    val victim = get_char_world(ch, arg)
    if (victim != null) {
        return victim.room
    }

    return get_obj_world(ch, arg)?.in_room

}


fun do_transfer(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg1)
    one_argument(rest, arg2)

    if (arg1.isEmpty()) {
        send_to_char("Transfer whom (and where)?\n", ch)
        return
    }

    if (eq(arg1.toString(), "all")) {
        Index.PLAYERS
                .filter { it.ch != ch && can_see(ch, it.ch) }
                .forEach { do_transfer(ch, it.ch.name + " " + arg2) }
        return
    }

    // Thanks to Grodyn for the optional location parameter.
    val location: ROOM_INDEX_DATA?
    if (arg2.isEmpty()) {
        location = ch.room
    } else {
        location = find_location(ch, arg2.toString())
        if (location == null) {
            send_to_char("No such location.\n", ch)
            return
        }

        /*  if ( !is_room_owner(ch,location) && room_is_private( location ) */
        if (room_is_private(location) && get_trust(ch) < MAX_LEVEL) {
            send_to_char("That room is private right now.\n", ch)
            return
        }
    }

    val victim = get_char_world(ch, arg1.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim.room.vnum == ROOM_VNUM_LIMBO) {
        send_to_char("They are in limbo.\n", ch)
        return
    }

    if (victim.fighting != null) {
        stop_fighting(victim, true)
    }
    act("\$n disappears in a mushroom cloud.", victim, null, null, TO_ROOM)
    char_from_room(victim)
    char_to_room(victim, location)
    act("\$n arrives from a puff of smoke.", victim, null, null, TO_ROOM)
    if (ch != victim) {
        act("\$n has transferred you.", ch, null, victim, TO_VICT)
    }
    do_look(victim, "auto")
    send_to_char("Ok.\n", ch)
}


fun do_at(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val original = ch.room
    val on = ch.on
    val arg = StringBuilder()
    rest = one_argument(rest, arg)

    if (arg.isEmpty() || rest.isEmpty()) {
        send_to_char("At where what?\n", ch)
        return
    }

    val location = find_location(ch, arg.toString())
    if (location == null) {
        send_to_char("No such location.\n", ch)
        return
    }

    /*    if (!is_room_owner(ch,location) && room_is_private( location ) */
    if (room_is_private(location) && get_trust(ch) < MAX_LEVEL) {
        send_to_char("That room is private right now.\n", ch)
        return
    }

    char_from_room(ch)
    char_to_room(ch, location)
    interpret(ch, rest, false)

    /*
         * See if 'ch' still exists before continuing!
         * Handles 'at XXXX quit' case.
         */
    for (wch in Index.CHARS) {
        if (wch == ch) {
            char_from_room(ch)
            char_to_room(ch, original)
            ch.on = on
            break
        }
    }

}


fun do_goto(ch: CHAR_DATA, argument: String) {
    if (argument.isEmpty()) {
        send_to_char("Goto where?\n", ch)
        return
    }
    val location = find_location(ch, argument)
    if (location == null) {
        send_to_char("No such location.\n", ch)
        return
    }

    /*        int count = 0;
        for ( rch = location.people; rch != null; rch = rch.next_in_room )
            count++;

    if (!is_room_owner(ch,location) && room_is_private(location)
    &&  (count > 1 || get_trust(ch) < MAX_LEVEL))
    {
    send_to_char( "That room is private right now.\n", ch );
    return;
    } */

    if (ch.fighting != null) {
        stop_fighting(ch, true)
    }

    val pcdata = ch.pcdata
    ch.room.people
            .filter { get_trust(it) >= ch.invis_level }
            .forEach {
                if (pcdata != null && pcdata.bamfout.isNotEmpty()) {
                    act("\$t", ch, pcdata.bamfout, it, TO_VICT)
                } else {
                    act("\$n leaves in a swirling mist.", ch, null, it, TO_VICT)
                }
            }

    char_from_room(ch)
    char_to_room(ch, location)

    ch.room.people
            .filter { get_trust(it) >= ch.invis_level }
            .forEach {
                if (pcdata != null && pcdata.bamfin.isNotEmpty()) {
                    act("\$t", ch, pcdata.bamfin, it, TO_VICT)
                } else {
                    act("\$n appears in a swirling mist.", ch, null, it, TO_VICT)
                }
            }

    do_look(ch, "auto")
}

fun do_violate(ch: CHAR_DATA, argument: String) {
    val location = find_location(ch, argument)

    if (argument.isEmpty()) {
        send_to_char("Goto where?\n", ch)
        return
    }

    if (location == null) {
        send_to_char("No such location.\n", ch)
        return
    }

    if (!room_is_private(location)) {
        send_to_char("That room isn't private, use goto.\n", ch)
        return
    }

    if (ch.fighting != null) {
        stop_fighting(ch, true)
    }

    val pcdata = ch.pcdata
    ch.room.people
            .filter { get_trust(it) >= ch.invis_level }
            .forEach {
                if (pcdata != null && pcdata.bamfout.isNotEmpty()) {
                    act("\$t", ch, pcdata.bamfout, it, TO_VICT)
                } else {
                    act("\$n leaves in a swirling mist.", ch, null, it, TO_VICT)
                }
            }

    char_from_room(ch)
    char_to_room(ch, location)


    ch.room.people
            .filter { get_trust(it) >= ch.invis_level }
            .forEach {
                if (pcdata != null && pcdata.bamfin.isNotEmpty()) {
                    act("\$t", ch, pcdata.bamfin, it, TO_VICT)
                } else {
                    act("\$n appears in a swirling mist.", ch, null, it, TO_VICT)
                }
            }

    do_look(ch, "auto")
}

/* RT to replace the 3 stat commands */

fun do_stat(ch: CHAR_DATA, argument: String) {
    val string: String
    val obj = get_obj_world(ch, argument)
    val location = find_location(ch, argument)
    val victim = get_char_world(ch, argument)
    val argb = StringBuilder()
    string = one_argument(argument, argb)
    if (argb.isEmpty()) {
        send_to_char("Syntax:\n", ch)
        send_to_char("  stat <name>\n", ch)
        send_to_char("  stat obj <name>\n", ch)
        send_to_char("  stat mob <name>\n", ch)
        send_to_char("  stat room <number>\n", ch)
        return
    }
    val arg = argb.toString()

    if (eq(arg, "room")) {
        do_rstat(ch, string)
        return
    }

    if (eq(arg, "obj")) {
        do_ostat(ch, string)
        return
    }

    if (eq(arg, "char") || eq(arg, "mob")) {
        do_mstat(ch, string)
        return
    }

    /* do it the old way */

    if (obj != null) {
        do_ostat(ch, argument)
        return
    }

    if (victim != null) {
        do_mstat(ch, argument)
        return
    }

    if (location != null) {
        do_rstat(ch, argument)
        return
    }

    send_to_char("Nothing by that name found anywhere.\n", ch)
}


fun do_rstat(ch: CHAR_DATA, argument: String) {
    val location: ROOM_INDEX_DATA?
    var rh: ROOM_HISTORY_DATA?
    var door = 0
    val arg = StringBuilder()
    one_argument(argument, arg)
    location = if (arg.isEmpty()) ch.room else find_location(ch, arg.toString())
    if (location == null) {
        send_to_char("No such location.\n", ch)
        return
    }

    /*    if (!is_room_owner(ch,location) && ch.in_room != location  */
    if (ch.room != location
            && room_is_private(location) && !IS_TRUSTED(ch, IMPLEMENTOR)) {
        send_to_char("That room is private right now.\n", ch)
        return
    }
    val buf = TextBuffer()
    if (ch.room.affected_by != 0L) {
        buf.sprintf("Affected by %s\n", raffect_bit_name(ch.room.affected_by))
        send_to_char(buf, ch)
    }

    if (ch.room.room_flags != 0L) {
        buf.sprintf("Roomflags %s\n", flag_room_name(ch.room.room_flags))
        send_to_char(buf, ch)
    }

    buf.sprintf("Name: '%s'\nArea: '%s'\nOwner: '%s'\n",
            location.name,
            location.area.name,
            location.owner)
    send_to_char(buf, ch)

    buf.sprintf(
            "Vnum: %d  Sector: %d  Light: %d  Healing: %d  Mana: %d\n",
            location.vnum,
            location.sector_type,
            location.light,
            location.heal_rate,
            location.mana_rate)
    send_to_char(buf, ch)

    buf.sprintf(
            "Room flags: %d.\nDescription:\n%s",
            location.room_flags,
            location.description)
    send_to_char(buf, ch)

    if (location.extra_descr != null) {
        var ed: EXTRA_DESC_DATA?

        send_to_char("Extra description keywords: '", ch)
        ed = location.extra_descr
        while (ed != null) {
            send_to_char(ed.keyword, ch)
            if (ed.next != null) {
                send_to_char(" ", ch)
            }
            ed = ed.next
        }
        send_to_char("'.\n", ch)
    }

    send_to_char("Characters:", ch)
    for (rch in location.people) {
        if (can_see(ch, rch)) {
            send_to_char(" ", ch)
            arg.setLength(0)
            one_argument(rch.name, arg)
            send_to_char(arg, ch)
        }
    }

    send_to_char(".\nObjects:   ", ch)
    for (obj in location.objects) {
        send_to_char(" ", ch)
        arg.setLength(0)
        one_argument(obj.name, arg)
        send_to_char(arg, ch)
    }
    send_to_char(".\n", ch)

    while (door <= 5) {
        val pexit = location.exit[door]

        if (pexit != null) {
            buf.sprintf(
                    "Door: %d.  To: %d.  Key: %d.  Exit flags: %d.\nKeyword: '%s'.  Description: %s",
                    door,
                    pexit.to_room.vnum,
                    pexit.key,
                    pexit.exit_info,
                    pexit.keyword,
                    if (pexit.description.isNotEmpty())
                        pexit.description
                    else
                        "(none).\n")
            send_to_char(buf, ch)
        }
        door++
    }
    send_to_char("Tracks:\n", ch)
    rh = location.history
    while (rh != null) {
        buf.sprintf("%s took door %i.\n", rh.name, rh.went)
        send_to_char(buf, ch)
        rh = rh.next
    }
}


fun do_ostat(ch: CHAR_DATA, argument: String) {

    val obj = get_obj_world(ch, argument)
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Stat what?\n", ch)
        return
    }

    if (obj == null) {
        send_to_char("Nothing like that in hell, earth, or heaven.\n", ch)
        return
    }
    val buf = TextBuffer()
    buf.sprintf("Name(s): %s\n",
            obj.name)
    send_to_char(buf, ch)

    buf.sprintf("Vnum: %d  Format: %s  Type: %s  Resets: %d\n",
            obj.pIndexData.vnum, if (obj.pIndexData.new_format) "new" else "old",
            obj.item_type.displayName, obj.pIndexData.reset_num)
    send_to_char(buf, ch)

    buf.sprintf("Short description: %s\nLong description: %s\n",
            obj.short_desc, obj.description)
    send_to_char(buf, ch)

    buf.sprintf("Wear bits: %s\nExtra bits: %s\n",
            wear_bit_name(obj.wear_flags), extra_bit_name(obj.extra_flags))
    send_to_char(buf, ch)

    buf.sprintf("Number: %d/%d  Weight: %d/%d/%d (10th pounds)\n", 1, get_obj_number(obj),
            obj.weight, get_obj_weight(obj), get_true_weight(obj))
    send_to_char(buf, ch)

    buf.sprintf("Level: %d  Cost: %d  Condition: %d  Timer: %d Count: %d\n",
            obj.level, obj.cost, obj.condition, obj.timer, obj.pIndexData.count)
    send_to_char(buf, ch)

    val carried_by = obj.carried_by
    buf.sprintf(
            "In room: %d  In object: %s  Carried by: %s  Wear_loc: %d\n",
            if (obj.in_room == null) 0 else obj.in_room!!.vnum,
            if (obj.in_obj == null) "(none)" else obj.in_obj!!.short_desc,
            when {
                carried_by == null -> "(none)"
                can_see(ch, carried_by) -> carried_by.name
                else -> "someone"
            },
            obj.wear_loc)
    send_to_char(buf, ch)

    buf.sprintf("Values: %d %d %d %d %d\n",
            obj.value[0], obj.value[1], obj.value[2], obj.value[3],
            obj.value[4])
    send_to_char(buf, ch)

    /* now give out vital statistics as per identify */
    when (obj.item_type) {
        ItemType.Scroll, ItemType.Potion, ItemType.Pill -> {
            buf.sprintf("Level %d spells of:", obj.value[0])
            send_to_char(buf, ch)

            if (obj.value[1] in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[obj.value[1].toInt()].skillName, ch)
                send_to_char("'", ch)
            }

            if (obj.value[2] in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[obj.value[2].toInt()].skillName, ch)
                send_to_char("'", ch)
            }

            if (obj.value[3] in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[obj.value[3].toInt()].skillName, ch)
                send_to_char("'", ch)
            }

            if (obj.value[4] in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[obj.value[4].toInt()].skillName, ch)
                send_to_char("'", ch)
            }

            send_to_char(".\n", ch)
        }

        ItemType.Wand, ItemType.Staff -> {
            buf.sprintf("Has %d(%d) charges of level %d",
                    obj.value[1], obj.value[2], obj.value[0])
            send_to_char(buf, ch)

            if (obj.value[3] in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[obj.value[3].toInt()].skillName, ch)
                send_to_char("'", ch)
            }

            send_to_char(".\n", ch)
        }

        ItemType.DrinkContainer -> {
            buf.sprintf("It holds %s-colored %s.\n", Liquid.of(obj.value[2]).liq_color, Liquid.of(obj.value[2]).liq_name)
            send_to_char(buf, ch)
        }


        ItemType.Weapon -> {
            send_to_char("Weapon type is ${obj.weaponType.typeName}.\n", ch)
            if (obj.pIndexData.new_format) {
                buf.sprintf("Damage is %dd%d (average %d)\n", obj.value[1], obj.value[2], (1 + obj.value[2]) * obj.value[1] / 2)
            } else {
                buf.sprintf("Damage is %d to %d (average %d)\n", obj.value[1], obj.value[2], (obj.value[1] + obj.value[2]) / 2)
            }
            send_to_char(buf, ch)

            buf.sprintf("Damage noun is %s.\n", AttackType.of(obj.value[3]).noun)
            send_to_char(buf, ch)

            if (obj.value[4] != 0L)
            /* weapon flags */ {
                buf.sprintf("Weapons flags: %s\n", weapon_bit_name(obj.value[4]))
                send_to_char(buf, ch)
            }
        }

        ItemType.Armor -> {
            buf.sprintf("Armor class is %d pierce, %d bash, %d slash, and %d vs. magic\n",
                    obj.value[0], obj.value[1], obj.value[2], obj.value[3])
            send_to_char(buf, ch)
        }

        ItemType.Container -> {
            buf.sprintf("Capacity: %d#  Maximum weight: %d#  flags: %s\n", obj.value[0], obj.value[3], cont_bit_name(obj.value[1]))
            send_to_char(buf, ch)
            if (obj.value[4] != 100L) {
                buf.sprintf("Weight multiplier: %d%%\n", obj.value[4])
                send_to_char(buf, ch)
            }
        }
        else -> {
        }
    }

    if (obj.extra_desc.description.isNotEmpty() || obj.pIndexData.extra_descr != null) {
        var ed: EXTRA_DESC_DATA?

        send_to_char("Extra description keywords: '", ch)

        ed = obj.extra_desc
        while (ed != null) {
            send_to_char(ed.keyword, ch)
            if (ed.next != null) {
                send_to_char(" ", ch)
            }
            ed = ed.next
        }

        ed = obj.pIndexData.extra_descr
        while (ed != null) {
            send_to_char(ed.keyword, ch)
            if (ed.next != null) {
                send_to_char(" ", ch)
            }
            ed = ed.next
        }

        send_to_char("'\n", ch)
    }

    var paf = obj.affected
    while (paf != null) {
        buf.sprintf("Affects %s by %d, level %d", paf.location.locName, paf.modifier, paf.level)
        send_to_char(buf, ch)
        if (paf.duration > -1) {
            buf.sprintf(", %d hours.\n", paf.duration)
        } else {
            buf.sprintf(".\n")
        }
        send_to_char(buf, ch)
        if (paf.bitvector != 0L) {
            when (paf.where) {
                TO_AFFECTS -> buf.sprintf("Adds %s affect.\n", affect_bit_name(paf.bitvector))
                TO_WEAPON -> buf.sprintf("Adds %s weapon flags.\n", weapon_bit_name(paf.bitvector))
                TO_OBJECT -> buf.sprintf("Adds %s object flag.\n", extra_bit_name(paf.bitvector))
                TO_IMMUNE -> buf.sprintf("Adds immunity to %s.\n", imm_bit_name(paf.bitvector))
                TO_RESIST -> buf.sprintf("Adds resistance to %s.\n", imm_bit_name(paf.bitvector))
                TO_VULN -> buf.sprintf("Adds vulnerability to %s.\n", imm_bit_name(paf.bitvector))
                else -> buf.sprintf("Unknown bit %d: %d\n", paf.where, paf.bitvector)
            }
            send_to_char(buf, ch)
        }
        paf = paf.next
    }

    if (!obj.enchanted) {
        paf = obj.pIndexData.affected
        while (paf != null) {
            buf.sprintf("Affects %s by %d, level %d.\n", paf.location.locName, paf.modifier, paf.level)
            send_to_char(buf, ch)
            if (paf.bitvector != 0L) {
                when (paf.where) {
                    TO_AFFECTS -> buf.sprintf("Adds %s affect.\n", affect_bit_name(paf.bitvector))
                    TO_OBJECT -> buf.sprintf("Adds %s object flag.\n", extra_bit_name(paf.bitvector))
                    TO_IMMUNE -> buf.sprintf("Adds immunity to %s.\n", imm_bit_name(paf.bitvector))
                    TO_RESIST -> buf.sprintf("Adds resistance to %s.\n", imm_bit_name(paf.bitvector))
                    TO_VULN -> buf.sprintf("Adds vulnerability to %s.\n", imm_bit_name(paf.bitvector))
                    else -> buf.sprintf("Unknown bit %d: %d\n", paf.where, paf.bitvector)
                }
                send_to_char(buf, ch)
            }
            paf = paf.next
        }
    }
    buf.sprintf("Object progs: ")
    if (obj.pIndexData.progtypes != 0L) {
        if (IS_SET(obj.progtypes, OPROG_GET)) {
            buf.append("get ")
        }
        if (IS_SET(obj.progtypes, OPROG_DROP)) {
            buf.append("drop ")
        }
        if (IS_SET(obj.progtypes, OPROG_SAC)) {
            buf.append("sacrifice ")
        }
        if (IS_SET(obj.progtypes, OPROG_GIVE)) {
            buf.append("give ")
        }
        if (IS_SET(obj.progtypes, OPROG_FIGHT)) {
            buf.append("fight ")
        }
        if (IS_SET(obj.progtypes, OPROG_DEATH)) {
            buf.append("death ")
        }
        if (IS_SET(obj.progtypes, OPROG_SPEECH)) {
            buf.append("speech ")
        }
        if (IS_SET(obj.progtypes, OPROG_AREA)) {
            buf.append("area ")
        }
    }
    buf.append("\n")
    send_to_char(buf, ch)
    buf.sprintf("Damage condition : %d (%s) ", obj.condition, get_cond_alias(obj))
    send_to_char(buf, ch)
    send_to_char("\n", ch)
}


fun do_mstat(ch: CHAR_DATA, argument: String) {
    val victim = get_char_room(ch, argument)
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Stat whom?\n", ch)
        return
    }

    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }
    val buf = TextBuffer()
    val v_pcdata = victim.pcdata
    buf.sprintf("Name: [%s] Reset Zone: [%s] Logon: %s\r",
            victim.name,
            if (v_pcdata == null) victim.room.area.name else "?",
            Date(ch.logon * 1000L))
    send_to_char(buf, ch)

    buf.sprintf("Vnum: %d  Format: %s  Race: %s(%s)  Group: %d  Sex: %s  Room: %d\n",
            if (v_pcdata == null) victim.pIndexData.vnum else 0,
            if (v_pcdata == null) "new" else "pc",
            victim.race.name, victim.race.name,
            if (v_pcdata == null) victim.group else 0,
            victim.sex.title,
            victim.room.vnum
    )
    send_to_char(buf, ch)

    if (v_pcdata == null) {
        buf.sprintf("Count: %d  Killed: %d  ---  Status: %d  Cabal: %d\n",
                victim.pIndexData.count, victim.pIndexData.killed,
                victim.status, victim.cabal)
        send_to_char(buf, ch)
    }

    buf.sprintf(
            "Str: %d(%d)  Int: %d(%d)  Wis: %d(%d)  Dex: %d(%d)  Con: %d(%d) Cha: %d(%d)\n",
            victim.perm_stat[PrimeStat.Strength],
            victim.curr_stat(PrimeStat.Strength),
            victim.perm_stat[PrimeStat.Intelligence],
            victim.curr_stat(PrimeStat.Intelligence),
            victim.perm_stat[PrimeStat.Wisdom],
            victim.curr_stat(PrimeStat.Wisdom),
            victim.perm_stat[PrimeStat.Dexterity],
            victim.curr_stat(PrimeStat.Dexterity),
            victim.perm_stat[PrimeStat.Constitution],
            victim.curr_stat(PrimeStat.Constitution),
            victim.perm_stat[PrimeStat.Charisma],
            victim.curr_stat(PrimeStat.Charisma))
    send_to_char(buf, ch)


    buf.sprintf("Hp: %d/%d  Mana: %d/%d  Move: %d/%d  Practices: %d\n",
            victim.hit, victim.max_hit,
            victim.mana, victim.max_mana,
            victim.move, victim.max_move,
            if (ch.pcdata == null) 0 else victim.practice)
    send_to_char(buf, ch)

    val buf2 = TextBuffer()
    if (v_pcdata == null) {
        sprintf(buf2, "%d", victim.alignment)
    } else {
        buf2.append(victim.ethos.prefix).append("-").append(victim.align.prefix)
    }
    buf.sprintf("It belives the religion of %s.\n",
            if (v_pcdata == null) "Chronos" else victim.religion.leader)
    send_to_char(buf, ch)
    buf.sprintf(
            "Lv: %d  Class: %s  Align: %s  Gold: %d  Silver: %d  Exp: %d\n",
            victim.level,
            if (v_pcdata == null) "mobile" else victim.clazz.name,
            buf2,
            victim.gold, victim.silver, victim.exp)
    send_to_char(buf, ch)

    buf.sprintf("Armor: pierce: %d  bash: %d  slash: %d  magic: %d\n",
            GET_AC(victim, AC_PIERCE), GET_AC(victim, AC_BASH),
            GET_AC(victim, AC_SLASH), GET_AC(victim, AC_EXOTIC))
    send_to_char(buf, ch)

    buf.sprintf(
            "Hit: %d  Dam: %d  Saves: %d  Size: %s  Position: %s  Wimpy: %d\n",
            GET_HITROLL(victim), GET_DAMROLL(victim), victim.saving_throw,
            victim.size.title, victim.position.adj, victim.wimpy)
    send_to_char(buf, ch)

    if (v_pcdata == null) {
        buf.sprintf("Damage: %dd%d  Message:  %s\n", victim.damage[DICE_NUMBER], victim.damage[DICE_TYPE], victim.attack.noun)
        send_to_char(buf, ch)
    }
    val v_fighting = victim.fighting
    buf.sprintf("Fighting: %s Death: %d Carry number: %d  Carry weight: %d\n",
            v_fighting?.name ?: "(none)", v_pcdata?.death ?: 0,
            victim.carry_number, get_carry_weight(victim) / 10)
    send_to_char(buf, ch)

    if (v_pcdata != null) {
        buf.sprintf("Thirst: %d  Hunger: %d  Full: %d  Drunk: %d Bloodlust: %d Desire: %d\n",
                v_pcdata.condition[COND_THIRST],
                v_pcdata.condition[COND_HUNGER],
                v_pcdata.condition[COND_FULL],
                v_pcdata.condition[COND_DRUNK],
                v_pcdata.condition[COND_BLOODLUST],
                v_pcdata.condition[COND_DESIRE])
        send_to_char(buf, ch)
    }


    if (v_pcdata != null) {
        buf.sprintf(
                "Age: %d  Played: %d  Last Level: %d  Timer: %d\n",
                get_age(victim),
                (victim.played + current_time - victim.logon).toInt() / 3600,
                v_pcdata.last_level,
                victim.timer)
        send_to_char(buf, ch)
    }

    buf.sprintf("Act: %s\n", act_bit_name(victim.act))
    send_to_char(buf, ch)

    if (victim.comm != 0L) {
        buf.sprintf("Comm: %s\n", comm_bit_name(victim.comm))
        send_to_char(buf, ch)
    }

    if (v_pcdata == null && victim.off_flags != 0L) {
        buf.sprintf("Offense: %s\n", off_bit_name(victim.off_flags))
        send_to_char(buf, ch)
    }

    if (victim.imm_flags != 0L) {
        buf.sprintf("Immune: %s\n", imm_bit_name(victim.imm_flags))
        send_to_char(buf, ch)
    }

    if (victim.res_flags != 0L) {
        buf.sprintf("Resist: %s\n", imm_bit_name(victim.res_flags))
        send_to_char(buf, ch)
    }

    if (victim.vuln_flags != 0L) {
        buf.sprintf("Vulnerable: %s\n", imm_bit_name(victim.vuln_flags))
        send_to_char(buf, ch)
    }


    buf.sprintf("Form: %s\nParts: %s\n", form_bit_name(victim.form), part_bit_name(victim.parts))
    send_to_char(buf, ch)

    if (victim.affected_by != 0L) {
        buf.sprintf("Affected by %s\n", affect_bit_name(victim.affected_by))
        send_to_char(buf, ch)
    }

    val v_master = victim.master
    val v_leader = victim.leader
    val v_pet = victim.pet
    buf.sprintf("Master: %s  Leader: %s  Pet: %s\n", v_master?.name ?: "(none)", v_leader?.name ?: "(none)", v_pet?.name ?: "(none)")
    send_to_char(buf, ch)

    buf.sprintf("Short description: %s\nLong  description: %s",
            victim.short_desc,
            if (victim.long_desc.isNotEmpty()) victim.long_desc else "(none)\n")
    send_to_char(buf, ch)

    if (v_pcdata == null && victim.spec_fun != null) {
        buf.sprintf("Mobile has special procedure %s.\n", spec_name(victim.spec_fun))
        send_to_char(buf, ch)
    }

    var paf: Affect? = victim.affected
    while (paf != null) {
        buf.sprintf("Spell: '%s' modifies %s by %d for %d hours with bits %s, level %d.\n",
                paf.type.skillName,
                paf.location.locName,
                paf.modifier,
                paf.duration,
                affect_bit_name(paf.bitvector),
                paf.level
        )
        send_to_char(buf, ch)
        paf = paf.next
    }

    if (v_pcdata != null) {
        if (IS_SET(victim.act, PLR_QUESTOR)) {
            buf.sprintf("Questgiver: %d QuestPnts: %d  Questnext: %d\n",
                    v_pcdata.questgiver, v_pcdata.questpoints,
                    v_pcdata.nextquest)
            send_to_char(buf, ch)
            buf.sprintf("QuestCntDown: %d  QuestObj: %d    Questmob: %d\n",
                    v_pcdata.countdown, v_pcdata.questobj,
                    v_pcdata.questmob)
            send_to_char(buf, ch)
        }
        if (!IS_SET(victim.act, PLR_QUESTOR)) {
            buf.sprintf("QuestPnts: %d Questnext: %d    NOT QUESTING\n",
                    v_pcdata.questpoints, v_pcdata.nextquest)
            send_to_char(buf, ch)
        }
    }

    if (v_pcdata == null) {
        if (victim.pIndexData.progtypes != 0L) {
            buf.sprintf("Mobile progs: ")
            if (IS_SET(victim.progtypes, MPROG_BRIBE)) {
                buf.append("bribe ")
            }
            if (IS_SET(victim.progtypes, MPROG_SPEECH)) {
                buf.append("speech ")
            }
            if (IS_SET(victim.progtypes, MPROG_GIVE)) {
                buf.append("give ")
            }
            if (IS_SET(victim.progtypes, MPROG_DEATH)) {
                buf.append("death ")
            }
            if (IS_SET(victim.progtypes, MPROG_GREET)) {
                buf.append("greet ")
            }
            if (IS_SET(victim.progtypes, MPROG_ENTRY)) {
                buf.append("entry ")
            }
            if (IS_SET(victim.progtypes, MPROG_FIGHT)) {
                buf.append("fight ")
            }
            if (IS_SET(victim.progtypes, MPROG_AREA)) {
                buf.append("area ")
            }
            buf.append("\n")
            send_to_char(buf, ch)
        }
    }
    buf.sprintf("Last fought: %10s  Last fight time: %s",
            if (victim.last_fought != null) victim.last_fought!!.name else "none",
            Date(victim.last_fight_time * 1000L))
    send_to_char(buf, ch)

    buf.sprintf("In_mind: [%s] Hunting: [%s]\n", victim.in_mind, victim.hunting?.name ?: "none")
    send_to_char(buf, ch)
}

fun do_vnum(ch: CHAR_DATA, argument: String) {
    val string: String
    val argb = StringBuilder()
    string = one_argument(argument, argb)

    if (argb.isEmpty()) {
        send_to_char("Syntax:\n", ch)
        send_to_char("  vnum obj <name>\n", ch)
        send_to_char("  vnum mob <name>\n", ch)
        return
    }
    val arg = argb.toString()
    if (eq(arg, "obj")) {
        do_ofind(ch, string)
        return
    }

    if (eq(arg, "mob") || eq(arg, "char")) {
        do_mfind(ch, string)
        return
    }

    /* do both */
    do_mfind(ch, argument)
    do_ofind(ch, argument)
}


fun do_mfind(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        send_to_char("Find whom?\n", ch)
        return
    }

    /*
         * Yeah, so iterating over all vnum's takes 10,000 loops.
         * Get_mob_index is fast, and I don't feel like threading another link.
         * Do you?
         * -- Furey
         */
    var found = false
    for (pMobIndex in Index.MOB_INDEX.values) {
        if (is_name(argument, pMobIndex.player_name)) {
            found = true
            val buf = TextBuffer()
            buf.sprintf("[%5d] %s\n", pMobIndex.vnum, pMobIndex.short_descr)
            send_to_char(buf, ch)
        }
    }

    if (!found) {
        send_to_char("No mobiles by that name.\n", ch)
    }

}


fun do_ofind(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        send_to_char("Find what?\n", ch)
        return
    }

    /*
         * Yeah, so iterating over all vnum's takes 10,000 loops.
         * Get_obj_index is fast, and I don't feel like threading another link.
         * Do you?
         * -- Furey
         */
    var found = false
    for (pObjIndex in Index.OBJ_INDEX.values) {
        if (is_name(argument, pObjIndex.name)) {
            found = true
            val buf = TextBuffer()
            buf.sprintf("[%5d] %s%s\n", pObjIndex.vnum, pObjIndex.short_descr,
                    if (IS_OBJ_STAT(pObjIndex, ITEM_GLOW) && CAN_WEAR(pObjIndex, ITEM_WEAR_HEAD)) " (Glowing)" else "")
            send_to_char(buf, ch)
        }
    }

    if (!found) {
        send_to_char("No objects by that name.\n", ch)
    }

}


fun do_owhere(ch: CHAR_DATA, argument: String) {
    if (argument.isEmpty()) {
        send_to_char("Find what?\n", ch)
        return
    }

    var number = 0
    val max_found = 200
    var found = false
    val buf = TextBuffer()
    val buffer = StringBuilder()
    for (obj in Index.OBJECTS) {
        if (!can_see_obj(ch, obj) || !is_name(argument, obj.name) || ch.level < obj.level) {
            continue
        }
        found = true
        number++

        var in_obj = obj
        while (in_obj.in_obj != null) {
            in_obj = in_obj.in_obj!!
        }

        val carried_by = in_obj.carried_by
        if (carried_by != null && can_see(ch, carried_by)) {
            buf.sprintf("%3d) %s is carried by %s [Room %d]\n", number, obj.short_desc,
                    PERS(carried_by, ch), carried_by.room.vnum)
        } else {
            val in_room = in_obj.in_room
            if (in_room != null && can_see_room(ch, in_room)) {
                buf.sprintf("%3d) %s is in %s [Room %d]\n", number, obj.short_desc, in_room.name, in_room.vnum)
            } else {
                buf.sprintf("%3d) %s is somewhere\n", number, obj.short_desc)
            }
        }

        buf.buffer.setCharAt(0, UPPER(buf.buffer[0]))
        buffer.append(buf)

        if (number >= max_found) {
            break
        }
    }

    if (!found) {
        send_to_char("Nothing like that in heaven or earth.\n", ch)
    } else {
        page_to_char(buffer, ch)
    }
}


fun do_mwhere(ch: CHAR_DATA, argument: String) {
    var count = 0
    val buffer = StringBuilder()
    val buf = TextBuffer()

    if (argument.isEmpty()) {
        /* show characters logged */
        for (pc in Index.PLAYERS) {
            if (can_see(ch, pc.ch) && can_see_room(ch, pc.ch.room)) {
                val victim = pc.ch
                count++
                if (pc.o_ch != pc.ch) {
                    buf.sprintf("%3d) %s (in the body of %s) is in %s [%d]\n",
                            count, pc.o_ch.name, victim.short_desc,
                            victim.room.name, victim.room.vnum)
                } else {
                    buf.sprintf("%3d) %s is in %s [%d]\n", count, victim.name, victim.room.name, victim.room.vnum)
                }
                buffer.append(buf)
            }
        }
        page_to_char(buffer, ch)
        return
    }

    var found = false
    for (victim in Index.CHARS) {
        if (is_name(argument, victim.name)) {
            found = true
            count++
            buf.sprintf("%3d) [%5d] %-28s [%5d] %s\n", count,
                    if (victim.pcdata == null) victim.pIndexData.vnum else 0,
                    if (victim.pcdata == null) victim.short_desc else victim.name,
                    victim.room.vnum,
                    victim.room.name)
            buffer.append(buf)
        }
    }
    if (!found) {
        act("You didn't find any \$T.", ch, null, argument, TO_CHAR)
    } else {
        page_to_char(buffer, ch)
    }


}


fun do_reboo(ch: CHAR_DATA) {
    send_to_char("If you want to REBOOT, spell it out.\n", ch)
}


fun do_shutdow(ch: CHAR_DATA) {
    send_to_char("If you want to SHUTDOWN, spell it out.\n", ch)
}


fun do_shutdown(ch: CHAR_DATA) {
    val buf = TextBuffer()
    if (ch.invis_level < LEVEL_HERO) {
        buf.sprintf("Shutdown by %s.", ch.name)
    }
    buf.append("\n")
    if (ch.invis_level < LEVEL_HERO) {
        do_echo(ch, buf.toString())
    }
    reboot_nightworks(false, NIGHTWORKS_SHUTDOWN)
}

fun do_protect(ch: CHAR_DATA, argument: String) {
    val victim = get_char_world(ch, argument)

    if (argument.isEmpty()) {
        send_to_char("Protect whom from snooping?\n", ch)
        return
    }

    if (victim == null) {
        send_to_char("You can't find them.\n", ch)
        return
    }

    if (IS_SET(victim.comm, COMM_SNOOP_PROOF)) {
        act("\$N is no longer snoop-proof.", ch, null, victim, TO_CHAR, Pos.Dead)
        send_to_char("Your snoop-proofing was just removed.\n", victim)
        victim.comm = REMOVE_BIT(victim.comm, COMM_SNOOP_PROOF)
    } else {
        act("\$N is now snoop-proof.", ch, null, victim, TO_CHAR, Pos.Dead)
        send_to_char("You are now immune to snooping.\n", victim)
        victim.comm = SET_BIT(victim.comm, COMM_SNOOP_PROOF)
    }
}


fun do_snoop(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Snoop whom?\n", ch)
        return
    }

    val victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    val v_con = get_pc(victim)?.con
    if (v_con == null) {
        send_to_char("No descriptor to snoop.\n", ch)
        return
    }

    if (victim == ch) {
        send_to_char("Cancelling all snoops.\n", ch)
        wiznet("\$N stops being such a snoop.", ch, null, Wiznet.Snoops, Wiznet.Secure, get_trust(ch))
        Index.CONNECTIONS
                .filter { it.snoop_by == v_con }
                .forEach { it.snoop_by = null }
        return
    }

    if (v_con.snoop_by != null) {
        send_to_char("Busy already.\n", ch)
        return
    }

    if (!is_room_owner(ch, victim.room) && ch.room != victim.room
            && room_is_private(victim.room) && !IS_TRUSTED(ch, IMPLEMENTOR)) {
        send_to_char("That character is in a private room.\n", ch)
        return
    }

    if (get_trust(victim) >= get_trust(ch) || IS_SET(victim.comm, COMM_SNOOP_PROOF)) {
        send_to_char("You failed.\n", ch)
        return
    }

    val pc = get_pc(ch)
    if (pc == null) {
        send_to_char("Who am I?.\n", ch)
        return
    }

    var d = pc.con.snoop_by
    while (d != null) {
        if (d.ch == victim || pc.o_ch == victim) {
            send_to_char("No snoop loops.\n", ch)
            return
        }
        d = d.snoop_by
    }

    v_con.snoop_by = pc.con
    val name = if (ch.pcdata == null) victim.short_desc else victim.name
    wiznet("\$N starts snooping on $name", ch, null, Wiznet.Snoops, Wiznet.Secure, get_trust(ch))
    send_to_char("Ok.\n", ch)
}


fun do_switch(ch: CHAR_DATA, argument: String) {

    val victim: CHAR_DATA?
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Switch into whom?\n", ch)
        return
    }

    val pc = get_pc(ch) ?: return

    if (pc.o_ch != pc.ch) {
        send_to_char("You are already switched.\n", ch)
        return
    }

    victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim == ch) {
        send_to_char("Ok.\n", ch)
        return
    }

    if (victim.pcdata != null) {
        send_to_char("You can only switch into mobiles.\n", ch)
        return
    }

    if (!is_room_owner(ch, victim.room) && ch.room != victim.room
            && room_is_private(victim.room) && !IS_TRUSTED(ch, IMPLEMENTOR)) {
        send_to_char("That character is in a private room.\n", ch)
        return
    }

    val v_pc = get_pc(victim)
    if (v_pc != null) {
        send_to_char("Character in use.\n", ch)
        return
    }
    wiznet("\$N switches into ${victim.short_desc}", ch, null, Wiznet.Switches, Wiznet.Secure, get_trust(ch))

    //todo: what about pcdata?
    pc.ch = victim

    /* change communications to match */
    if (!ch.prompt.isEmpty()) {
        victim.prompt = ch.prompt
    }
    victim.comm = ch.comm
    victim.lines = ch.lines
    send_to_char("Ok.\n", victim)
}


fun do_return(ch: CHAR_DATA) {
    val pc = get_pc(ch) ?: return

    if (pc.o_ch == pc.ch) {
        send_to_char("You aren't switched.\n", ch)
        return
    }
    send_to_char("You return to your original body. Type replay to see any missed tells.\n", ch)
    if (!ch.prompt.isEmpty()) {
        ch.prompt = ""
    }
    wiznet("\$N returns from ${ch.short_desc}.", pc.o_ch, null, Wiznet.Switches, Wiznet.Secure, get_trust(ch))
    pc.ch.prompt = ""
    pc.ch = pc.o_ch
}

/** trust levels for load and clone */
fun obj_check(ch: CHAR_DATA, obj: Obj): Boolean {
    return IS_TRUSTED(ch, GOD)
            || IS_TRUSTED(ch, IMMORTAL) && obj.level <= 20 && obj.cost <= 1000
            || IS_TRUSTED(ch, DEMI) && obj.level <= 10 && obj.cost <= 500
            || IS_TRUSTED(ch, ANGEL) && obj.level <= 5 && obj.cost <= 250
            || IS_TRUSTED(ch, AVATAR) && obj.level == 0 && obj.cost <= 100
}

/* for clone, to insure that cloning goes many levels deep */

fun recursive_clone(ch: CHAR_DATA, obj: Obj, clone: Obj) {
    for (c_obj in obj.contains) {
        if (obj_check(ch, c_obj)) {
            val t_obj = create_object(c_obj.pIndexData, 0)
            clone_object(c_obj, t_obj)
            obj_to_obj(t_obj, clone)
            recursive_clone(ch, c_obj, t_obj)
        }
    }
}

/* command that is similar to load */

fun do_clone(ch: CHAR_DATA, argument: String) {
    val mob: CHAR_DATA?
    val argb = StringBuilder()
    val rest = one_argument(argument, argb)
    val arg = argb.toString()

    if (arg.isEmpty()) {
        send_to_char("Clone what?\n", ch)
        return
    }


    val obj: Obj?
    when {
        startsWith(arg, "object") -> {
            mob = null
            obj = get_obj_here(ch, rest)
            if (obj == null) {
                send_to_char("You don't see that here.\n", ch)
                return
            }
        }
        startsWith(arg, "mobile") || startsWith(arg, "character") -> {
            obj = null
            mob = get_char_room(ch, rest)
            if (mob == null) {
                send_to_char("You don't see that here.\n", ch)
                return
            }
        }
        else -> /* find both */ {
            mob = get_char_room(ch, argument)
            obj = get_obj_here(ch, argument)
            if (mob == null && obj == null) {
                send_to_char("You don't see that here.\n", ch)
                return
            }
        }
    }

    /* clone an object */
    if (obj != null) {
        val clone = create_object(obj.pIndexData, 0)

        if (!obj_check(ch, obj)) {
            send_to_char("Your powers are not great enough for such a task.\n", ch)
            return
        }

        clone_object(obj, clone)
        if (obj.carried_by != null) {
            obj_to_char(clone, ch)
        } else {
            obj_to_room(clone, ch.room)
        }
        recursive_clone(ch, obj, clone)

        act("\$n has created \$p.", ch, clone, null, TO_ROOM)
        act("You clone \$p.", ch, clone, null, TO_CHAR)
        wiznet("\$N clones \$p.", ch, clone, Wiznet.Load, Wiznet.Secure, get_trust(ch))
    } else {
        // always true: if (mob != null)
        if (mob!!.pcdata != null) {
            send_to_char("You can only clone mobiles.\n", ch)
            return
        }

        if (mob.level > 20 && !IS_TRUSTED(ch, GOD)
                || mob.level > 10 && !IS_TRUSTED(ch, IMMORTAL)
                || mob.level > 5 && !IS_TRUSTED(ch, DEMI)
                || mob.level > 0 && !IS_TRUSTED(ch, ANGEL)
                || !IS_TRUSTED(ch, AVATAR)) {
            send_to_char(
                    "Your powers are not great enough for such a task.\n", ch)
            return
        }

        val clone = create_mobile(mob.pIndexData)
        clone_mobile(mob, clone)

        for (o in mob.carrying) {
            if (obj_check(ch, o)) {
                val new_obj = create_object(o.pIndexData, 0)
                clone_object(o, new_obj)
                recursive_clone(ch, o, new_obj)
                obj_to_char(new_obj, clone)
                new_obj.wear_loc = o.wear_loc
            }
        }
        char_to_room(clone, ch.room)
        act("\$n has created \$N.", ch, null, clone, TO_ROOM)
        act("You clone \$N.", ch, null, clone, TO_CHAR)
        wiznet("\$N clones ${clone.short_desc}.", ch, null, Wiznet.Load, Wiznet.Secure, get_trust(ch))
    }
}

/* RT to replace the two load commands */

fun do_load(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val argb = StringBuilder()
    rest = one_argument(rest, argb)

    if (argb.isEmpty()) {
        send_to_char("Syntax:\n", ch)
        send_to_char("  load mob <vnum>\n", ch)
        send_to_char("  load obj <vnum> <level>\n", ch)
        return
    }
    val arg = argb.toString()

    if (eq(arg, "mob") || eq(arg, "char")) {
        do_mload(ch, rest)
        return
    }

    if (eq(arg, "obj")) {
        do_oload(ch, rest)
        return
    }
    /* echo syntax */
    do_load(ch, "")
}


fun do_mload(ch: CHAR_DATA, argument: String) {
    val pMobIndex: MOB_INDEX_DATA?
    val victim: CHAR_DATA

    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty() || !is_number(arg)) {
        send_to_char("Syntax: load mob <vnum>.\n", ch)
        return
    }

    pMobIndex = get_mob_index(atoi(arg.toString()))
    if (pMobIndex == null) {
        send_to_char("No mob has that vnum.\n", ch)
        return
    }

    victim = create_mobile(pMobIndex)
    char_to_room(victim, ch.room)
    act("\$n has created \$N!", ch, null, victim, TO_ROOM)
    wiznet("\$N loads ${victim.short_desc}.", ch, null, Wiznet.Load, Wiznet.Secure, get_trust(ch))
    send_to_char("Ok.\n", ch)
}


fun do_oload(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val pObjIndex: ObjProto?
    val obj: Obj
    var level: Int

    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg1)
    one_argument(rest, arg2)

    if (arg1.isEmpty() || !is_number(arg1)) {
        send_to_char("Syntax: load obj <vnum> <level>.\n", ch)
        return
    }

    level = get_trust(ch) /* default */

    if (arg2.isNotEmpty())
    /* load with a level */ {
        if (!is_number(arg2)) {
            send_to_char("Syntax: oload <vnum> <level>.\n", ch)
            return
        }
        level = atoi(arg2.toString())
        if (level < 0 || level > get_trust(ch)) {
            send_to_char("Level must be be between 0 and your level.\n", ch)
            return
        }
    }

    pObjIndex = get_obj_index(atoi(arg1.toString()))
    if (pObjIndex == null) {
        send_to_char("No object has that vnum.\n", ch)
        return
    }

    obj = create_object(pObjIndex, level)
    if (CAN_WEAR(obj, ITEM_TAKE)) {
        obj_to_char(obj, ch)
    } else {
        obj_to_room(obj, ch.room)
    }
    act("\$n has created \$p!", ch, obj, null, TO_ROOM)
    wiznet("\$N loads \$p.", ch, obj, Wiznet.Load, Wiznet.Secure, get_trust(ch))
    send_to_char("Ok.\n", ch)
}


fun do_purge(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        /* 'purge' */
        ch.room.people
                .filter { it.pcdata == null && !IS_SET(it.act, ACT_NOPURGE) && it != ch /* safety precaution */ }
                .forEach { extract_char(it, true) }

        ch.room.objects
                .filterNot { IS_OBJ_STAT(it, ITEM_NOPURGE) }
                .forEach { extract_obj(it) }

        act("\$n purges the room!", ch, null, null, TO_ROOM)
        send_to_char("Ok.\n", ch)
        return
    }

    val victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    val v_pcdata = victim.pcdata
    if (v_pcdata != null) {
        if (ch == victim) {
            send_to_char("Ho ho ho.\n", ch)
            return
        }

        if (get_trust(ch) <= get_trust(victim)) {
            send_to_char("Maybe that wasn't a good idea...\n", ch)
            val buf = TextBuffer()
            buf.sprintf("%s tried to purge you!\n", ch.name)
            send_to_char(buf, victim)
            return
        }

        act("\$n disintegrates \$N.", ch, null, victim, TO_NOTVICT)

        if (victim.level > 1) {
            save_char_obj(victim)
        }
        val d = get_pc(victim)
        extract_char(victim, true)
        if (d != null) {
            close_socket(d.con)
        }
        return
    }

    act("\$n purges \$N.", ch, null, victim, TO_NOTVICT)
    extract_char(victim, true)
}


fun do_trust(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg1)
    one_argument(rest, arg2)

    if (arg1.isEmpty() || arg2.isEmpty() || !is_number(arg2)) {
        send_to_char("Syntax: trust <char> <level>.\n", ch)
        return
    }

    val victim = get_char_world(ch, arg1.toString())
    if (victim == null) {
        send_to_char("That player is not here.\n", ch)
        return
    }

    val level = atoi(arg2.toString())
    if (level < 0 || level > 100) {
        send_to_char("Level must be 0 (reset) or 1 to 100.\n", ch)
        return
    }

    if (level > get_trust(ch)) {
        send_to_char("Limited to your trust.\n", ch)
        return
    }

    victim.trust = level
}


fun do_restore(ch: CHAR_DATA, argument: String) {
    var victim: CHAR_DATA?
    val argb = StringBuilder()
    one_argument(argument, argb)
    val arg = argb.toString()
    if (arg.isEmpty() || eq(arg, "room")) {
        /* cure room */

        for (vch in ch.room.people) {
            affect_strip(vch, Skill.plague)
            affect_strip(vch, Skill.poison)
            affect_strip(vch, Skill.blindness)
            affect_strip(vch, Skill.sleep)
            affect_strip(vch, Skill.curse)

            vch.hit = vch.max_hit
            vch.mana = vch.max_mana
            vch.move = vch.max_move
            update_pos(vch)
            act("\$n has restored you.", ch, null, vch, TO_VICT)
        }
        wiznet("\$N restored room ${ch.room.vnum}.", ch, null, Wiznet.Restore, Wiznet.Secure, get_trust(ch))
        send_to_char("Room restored.\n", ch)
        return

    }

    if (get_trust(ch) >= MAX_LEVEL - 1 && eq(arg, "all")) {
        /* cure all */

        for (d in Index.CONNECTIONS) {
            victim = d.ch

            if (victim == null || victim.pcdata == null) {
                continue
            }

            affect_strip(victim, Skill.plague)
            affect_strip(victim, Skill.poison)
            affect_strip(victim, Skill.blindness)
            affect_strip(victim, Skill.sleep)
            affect_strip(victim, Skill.curse)

            victim.hit = victim.max_hit
            victim.mana = victim.max_mana
            victim.move = victim.max_move
            update_pos(victim)
            if (victim.room.vnum != ROOM_VNUM_LIMBO) {
                act("\$n has restored you.", ch, null, victim, TO_VICT)
            }
        }
        send_to_char("All active players restored.\n", ch)
        return
    }

    victim = get_char_world(ch, arg)
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    affect_strip(victim, Skill.plague)
    affect_strip(victim, Skill.poison)
    affect_strip(victim, Skill.blindness)
    affect_strip(victim, Skill.sleep)
    affect_strip(victim, Skill.curse)
    victim.hit = victim.max_hit
    victim.mana = victim.max_mana
    victim.move = victim.max_move
    update_pos(victim)
    act("\$n has restored you.", ch, null, victim, TO_VICT)
    val name = if (victim.pcdata == null) victim.short_desc else victim.name
    wiznet("\$N restored $name", ch, null, Wiznet.Restore, Wiznet.Secure, get_trust(ch))
    send_to_char("Ok.\n", ch)
}


fun do_freeze(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Freeze whom?\n", ch)
        return
    }

    victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim.pcdata == null) {
        send_to_char("Not on NPC's.\n", ch)
        return
    }

    if (get_trust(victim) >= get_trust(ch)) {
        send_to_char("You failed.\n", ch)
        return
    }

    if (IS_SET(victim.act, PLR_FREEZE)) {
        victim.act = REMOVE_BIT(victim.act, PLR_FREEZE)
        send_to_char("You can play again.\n", victim)
        send_to_char("FREEZE removed.\n", ch)
        wiznet("\$N thaws ${victim.name}.", ch, null, Wiznet.Penalties, Wiznet.Secure, 0)
    } else {
        victim.act = SET_BIT(victim.act, PLR_FREEZE)
        send_to_char("You can't do ANYthing!\n", victim)
        send_to_char("FREEZE set.\n", ch)
        wiznet("\$N puts ${victim.name} in the deep freeze.", ch, null, Wiznet.Penalties, Wiznet.Secure, 0)
    }

    save_char_obj(victim)
}


fun do_log(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?
    val argb = StringBuilder()
    one_argument(argument, argb)

    if (argb.isEmpty()) {
        send_to_char("Log whom?\n", ch)
        return
    }

    val arg = argb.toString()
    if (eq(arg, "all")) {
        if (fLogAll) {
            fLogAll = false
            send_to_char("Log ALL off.\n", ch)
        } else {
            fLogAll = true
            send_to_char("Log ALL on.\n", ch)
        }
        return
    }

    victim = get_char_world(ch, arg)
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim.pcdata == null) {
        send_to_char("Not on NPC's.\n", ch)
        return
    }

    /*
         * No level check, gods can log anyone.
         */
    if (IS_SET(victim.act, PLR_LOG)) {
        victim.act = REMOVE_BIT(victim.act, PLR_LOG)
        send_to_char("LOG removed.\n", ch)
    } else {
        victim.act = SET_BIT(victim.act, PLR_LOG)
        send_to_char("LOG set.\n", ch)
    }

}


fun do_noemote(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Noemote whom?\n", ch)
        return
    }

    victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }


    if (get_trust(victim) >= get_trust(ch)) {
        send_to_char("You failed.\n", ch)
        return
    }
    if (IS_SET(victim.comm, COMM_NOEMOTE)) {
        victim.comm = REMOVE_BIT(victim.comm, COMM_NOEMOTE)
        send_to_char("You can emote again.\n", victim)
        send_to_char("NOEMOTE removed.\n", ch)
        wiznet("\$N restores emotes to ${victim.name}.", ch, null, Wiznet.Penalties, Wiznet.Secure, 0)
    } else {
        victim.comm = SET_BIT(victim.comm, COMM_NOEMOTE)
        send_to_char("You can't emote!\n", victim)
        send_to_char("NOEMOTE set.\n", ch)
        wiznet("\$N revokes ${victim.name}'s emotes.", ch, null, Wiznet.Penalties, Wiznet.Secure, 0)
    }

}


fun do_noshout(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Noshout whom?\n", ch)
        return
    }

    victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim.pcdata == null) {
        send_to_char("Not on NPC's.\n", ch)
        return
    }

    if (get_trust(victim) >= get_trust(ch)) {
        send_to_char("You failed.\n", ch)
        return
    }

    if (IS_SET(victim.comm, COMM_NOSHOUT)) {
        victim.comm = REMOVE_BIT(victim.comm, COMM_NOSHOUT)
        send_to_char("You can shout again.\n", victim)
        send_to_char("NOSHOUT removed.\n", ch)
        wiznet(victim.name, ch, null, Wiznet.Penalties, Wiznet.Secure, 0)
    } else {
        victim.comm = SET_BIT(victim.comm, COMM_NOSHOUT)
        send_to_char("You can't shout!\n", victim)
        send_to_char("NOSHOUT set.\n", ch)
        wiznet("\$N revokes ${victim.name}'s shouts.", ch, null, Wiznet.Penalties, Wiznet.Secure, 0)
    }
}


fun do_notell(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Notell whom?", ch)
        return
    }

    victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (get_trust(victim) >= get_trust(ch)) {
        send_to_char("You failed.\n", ch)
        return
    }
    if (IS_SET(victim.comm, COMM_NOTELL)) {
        victim.comm = REMOVE_BIT(victim.comm, COMM_NOTELL)
        send_to_char("You can tell again.\n", victim)
        send_to_char("NOTELL removed.\n", ch)
        wiznet("\$N restores tells to ${victim.name}.", ch, null, Wiznet.Penalties, Wiznet.Secure, 0)
    } else {
        victim.comm = SET_BIT(victim.comm, COMM_NOTELL)
        send_to_char("You can't tell!\n", victim)
        send_to_char("NOTELL set.\n", ch)
        wiznet("\$N revokes ${victim.name}'s tells.", ch, null, Wiznet.Penalties, Wiznet.Secure, 0)
    }

}


fun do_peace(ch: CHAR_DATA) {
    for (rch in ch.room.people) {
        if (rch.fighting != null) stop_fighting(rch, true)
        if (rch.pcdata == null && IS_SET(rch.act, ACT_AGGRESSIVE)) rch.act = REMOVE_BIT(rch.act, ACT_AGGRESSIVE)
    }
    send_to_char("Ok.\n", ch)
}

fun do_wizlock(ch: CHAR_DATA) {
    Server.wizlock = !Server.wizlock

    if (Server.wizlock) {
        wiznet("\$N has wizlocked the game.", ch, null, null, null, 0)
        send_to_char("Game wizlocked.\n", ch)
    } else {
        wiznet("\$N removes wizlock.", ch, null, null, null, 0)
        send_to_char("Game un-wizlocked.\n", ch)
    }

}

/* RT anti-newbie code */


fun do_newlock(ch: CHAR_DATA) {
    Server.newlock = !Server.newlock

    if (Server.newlock) {
        wiznet("\$N locks out new characters.", ch, null, null, null, 0)
        send_to_char("New characters have been locked out.\n", ch)
    } else {
        wiznet("\$N allows new characters back in.", ch, null, null, null, 0)
        send_to_char("Newlock removed.\n", ch)
    }

}


fun do_slookup(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        send_to_char("Lookup which skill or spell?\n", ch)
        return
    }
    val buf = TextBuffer()
    if (eq(arg.toString(), "all")) {
        for (sn in Skill.skills) {
            buf.sprintf("Sn: %3d  Slot: %3d  Skill/spell: '%s'\n", sn, sn.slot, sn.skillName)
            send_to_char(buf, ch)
        }
    } else {
        val sn = Skill.lookup(arg.toString())
        if (sn == null) {
            send_to_char("No such skill or spell.\n", ch)
            return
        }

        buf.sprintf("Sn: %3d  Slot: %3d  Skill/spell: '%s'\n", sn, sn.slot, sn.skillName)
        send_to_char(buf, ch)
    }

}

/* RT set replaces sset, mset, oset, and rset */

fun do_set(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val argb = StringBuilder()
    rest = one_argument(rest, argb)

    if (argb.isEmpty()) {
        send_to_char("Syntax:\n", ch)
        send_to_char("  set mob   <name> <field> <value>\n", ch)
        send_to_char("  set obj   <name> <field> <value>\n", ch)
        send_to_char("  set room  <room> <field> <value>\n", ch)
        send_to_char("  set skill <name> <spell or skill> <value>\n", ch)
        return
    }

    val arg = argb.toString()
    if (startsWith(arg, "mobile") || startsWith(arg, "character")) {
        do_mset(ch, rest)
        return
    }

    if (startsWith(arg, "skill") || startsWith(arg, "spell")) {
        do_sset(ch, rest)
        return
    }

    if (startsWith(arg, "object")) {
        do_oset(ch, rest)
        return

    }

    if (startsWith(arg, "room")) {
        do_rset(ch, rest)
        return
    }
    /* echo syntax */
    do_set(ch, "")
}


fun do_sset(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val victim: CHAR_DATA?
    val value: Int
    val fAll: Boolean

    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    val arg3 = StringBuilder()
    rest = one_argument(rest, arg1)
    rest = one_argument(rest, arg2)
    one_argument(rest, arg3)

    if (arg1.isEmpty() || arg2.isEmpty() || arg3.isEmpty()) {
        send_to_char("Syntax:\n", ch)
        send_to_char("  set skill <name> <spell or skill> <value>\n", ch)
        send_to_char("  set skill <name> all <value>\n", ch)
        send_to_char("   (use the name of the skill, not the number)\n", ch)
        return
    }

    victim = get_char_world(ch, arg1.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    val v_pcdata = victim.pcdata
    if (v_pcdata == null) {
        send_to_char("Not on NPC's.\n", ch)
        return
    }

    fAll = eq(arg2.toString(), "all")
    val sn = Skill.lookup(arg2.toString())
    if (!fAll && sn == null) {
        send_to_char("No such skill or spell.\n", ch)
        return
    }

    if (!is_number(arg3.toString())) {
        send_to_char("Value must be numeric.\n", ch)
        return
    }

    value = atoi(arg3.toString())
    if (value < 0 || value > 100) {
        send_to_char("Value range is 0 to 100.\n", ch)
        return
    }

    if (fAll) {
        Skill.skills
                .filter { (it.cabal == victim.cabal || it.cabal == Clan.None) && RACE_OK(victim, it) }
                .forEach { v_pcdata.learned[it.ordinal] = value }
    } else {
        v_pcdata.learned[sn!!.ordinal] = value
    }

}


fun do_string(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg1 = StringBuilder()
    val arg2b = StringBuilder()
    val typeb = StringBuilder()
    rest = smash_tilde(rest)
    rest = one_argument(rest, typeb)
    rest = one_argument(rest, arg1)
    rest = one_argument(rest, arg2b)
    var arg3 = rest
    val type = typeb.toString()


    if (type.isEmpty() || arg1.isEmpty() || arg2b.isEmpty() || arg3.isEmpty()) {
        send_to_char("Syntax:\n", ch)
        send_to_char("  string char <name> <field> <string>\n", ch)
        send_to_char("    fields: name int long desc title spec\n", ch)
        send_to_char("  string obj  <name> <field> <string>\n", ch)
        send_to_char("    fields: name int long extended\n", ch)
        return
    }

    val arg2 = arg2b.toString()

    if (startsWith(type, "character") || startsWith(type, "mobile")) {
        val victim = get_char_world(ch, arg1.toString())
        if (victim == null) {
            send_to_char("They aren't here.\n", ch)
            return
        }

        /* string something */
        when {
            startsWith(arg2, "name") -> {
                if (victim.pcdata != null) {
                    send_to_char("Not on PC's.\n", ch)
                    return
                }
                victim.name = arg3
            }
            startsWith(arg2, "description") -> victim.description = arg3
            startsWith(arg2, "short") -> victim.short_desc = arg3
            startsWith(arg2, "long") -> {
                arg3 += "\n"
                victim.long_desc = arg3
            }
            startsWith(arg2, "title") -> {
                if (victim.pcdata == null) {
                    send_to_char("Not on NPC's.\n", ch)
                } else {
                    set_title(victim, arg3)
                }
            }
            startsWith(arg2, "spec") -> {
                if (victim.pcdata != null) {
                    send_to_char("Not on PC's.\n", ch)
                } else {
                    victim.spec_fun = spec_lookup(arg3)
                    if (victim.spec_fun == null) {
                        send_to_char("No such spec fun.\n", ch)
                        return
                    }
                }
            }
        }
        return
    }

    if (startsWith(type, "object")) {
        /* string an obj */

        val obj = get_obj_world(ch, arg1.toString())
        if (obj == null) {
            send_to_char("Nothing like that in heaven or earth.\n", ch)
            return
        }
        when {
            startsWith(arg2, "name") -> obj.name = arg3
            startsWith(arg2, "short") -> obj.short_desc = arg3
            startsWith(arg2, "long") -> obj.description = arg3
            startsWith(arg2, "ed") || startsWith(arg2, "extended") -> {
                arg1.setLength(0)
                rest = one_argument(rest, arg1)
                if (rest.isEmpty()) {
                    send_to_char("Syntax: oset <object> ed <keyword> <string>\n", ch)
                    return
                }

                rest += "\n"

                val ed = EXTRA_DESC_DATA()

                ed.keyword = arg3
                ed.description = rest
                ed.next = obj.extra_desc
                obj.extra_desc = ed
                return
            }
        }
        return
    }

    /* echo bad use message */
    do_string(ch, "")
}


fun do_oset(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg1 = StringBuilder()
    val arg2b = StringBuilder()

    rest = smash_tilde(rest)
    rest = one_argument(rest, arg1)
    rest = one_argument(rest, arg2b)
    val arg3 = rest

    if (arg1.isEmpty() || arg2b.isEmpty() || arg3.isEmpty()) {
        send_to_char("Syntax:\n", ch)
        send_to_char("  set obj <object> <field> <value>\n", ch)
        send_to_char("  Field being one of:\n", ch)
        send_to_char("    value0 value1 value2 value3 value4 (v1-v4)\n", ch)
        send_to_char("    extra wear level weight cost timer\n", ch)
        return
    }

    val obj = get_obj_world(ch, arg1.toString())
    if (obj == null) {
        send_to_char("Nothing like that in heaven or earth.\n", ch)
        return
    }

    val value = atoi(arg3)

    /* Set something. */
    val arg2 = arg2b.toString()
    when {
        eq(arg2, "value0") || eq(arg2, "v0") -> obj.value[0] = Math.min(50, value).toLong()
        eq(arg2, "value1") || eq(arg2, "v1") -> obj.value[1] = value.toLong()
        eq(arg2, "value2") || eq(arg2, "v2") -> obj.value[2] = value.toLong()
        eq(arg2, "value3") || eq(arg2, "v3") -> obj.value[3] = value.toLong()
        eq(arg2, "value4") || eq(arg2, "v4") -> obj.value[4] = value.toLong()
        startsWith(arg2, "extra") -> obj.extra_flags = value.toLong()
        startsWith(arg2, "wear") -> obj.wear_flags = value.toLong()
        startsWith(arg2, "level") -> obj.level = value
        startsWith(arg2, "weight") -> obj.weight = value
        startsWith(arg2, "cost") -> obj.cost = value
        startsWith(arg2, "timer") -> obj.timer = value
        else -> do_oset(ch, "")
    }
}


fun do_rset(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = smash_tilde(rest)
    rest = one_argument(rest, arg1)
    rest = one_argument(rest, arg2)
    val arg3 = rest

    if (arg1.isEmpty() || arg2.isEmpty() || arg3.isEmpty()) {
        send_to_char("Syntax:\n", ch)
        send_to_char("  set room <location> <field> <value>\n", ch)
        send_to_char("  Field being one of:\n", ch)
        send_to_char("    flags sector\n", ch)
        return
    }

    val location = find_location(ch, arg1.toString())
    if (location == null) {
        send_to_char("No such location.\n", ch)
        return
    }

    /*    if (!is_room_owner(ch,location) && ch.in_room != location  */
    if (ch.room != location
            && room_is_private(location) && !IS_TRUSTED(ch, IMPLEMENTOR)) {
        send_to_char("That room is private right now.\n", ch)
        return
    }

    if (!is_number(arg3)) {
        send_to_char("Value must be numeric.\n", ch)
        return
    }
    val value = atoi(arg3)

    when {
        startsWith(arg2.toString(), "flags") -> location.room_flags = value.toLong()
        startsWith(arg2.toString(), "sector") -> location.sector_type = value
        else -> do_rset(ch, "")
    }
}


fun do_sockets(ch: CHAR_DATA, argument: String) {
    var count = 0
    val argb = StringBuilder()
    one_argument(argument, argb)
    val arg = argb.toString()
    val buf = TextBuffer()
    for (pc in Index.PLAYERS) {
        if (can_see(ch, pc.ch) && (arg.isEmpty() || is_name(arg, pc.o_ch.name))) {
            count++
            buf.sprintf(false, "[%3d %s] %s@\n", pc.con.hashCode(), pc.con.host, pc.o_ch.name)
        }
    }
    if (count == 0) {
        send_to_char("No one by that name is connected.\n", ch)
        return
    }

    buf.sprintf(false, "%d user%s\n", count, if (count == 1) "" else "s")
    page_to_char(buf, ch)
}

fun do_force(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg = StringBuilder()
    rest = one_argument(rest, arg)

    if (arg.isEmpty() || rest.isEmpty()) {
        send_to_char("Force whom to do what?\n", ch)
        return
    }

    val arg2 = StringBuilder()
    one_argument(rest, arg2)

    if (eq(arg2.toString(), "delete")) {
        send_to_char("That will NOT be done.\n", ch)
        return
    }

    val msg = "\$n forces you to '$rest'."
    when {
        eq(arg.toString(), "all") -> {
            if (get_trust(ch) < MAX_LEVEL - 3) {
                send_to_char("Not at your level!\n", ch)
                return
            }

            for (vch in Index.CHARS) {
                if (vch.pcdata != null && get_trust(vch) < get_trust(ch)) {
                    act(msg, ch, null, vch, TO_VICT)
                    interpret(vch, rest, true)
                }
            }
        }
        eq(arg.toString(), "players") -> {
            if (get_trust(ch) < MAX_LEVEL - 2) {
                send_to_char("Not at your level!\n", ch)
                return
            }

            for (vch in Index.CHARS) {
                if (vch.pcdata != null && get_trust(vch) < get_trust(ch) && vch.level < LEVEL_HERO) {
                    act(msg, ch, null, vch, TO_VICT)
                    interpret(vch, rest, false)
                }
            }
        }
        eq(arg.toString(), "gods") -> {
            if (get_trust(ch) < MAX_LEVEL - 2) {
                send_to_char("Not at your level!\n", ch)
                return
            }

            for (vch in Index.CHARS) {
                if (vch.pcdata != null && get_trust(vch) < get_trust(ch) && vch.level >= LEVEL_HERO) {
                    act(msg, ch, null, vch, TO_VICT)
                    interpret(vch, rest, false)
                }
            }
        }
        else -> {
            val victim = get_char_world(ch, arg.toString())

            if (victim == null) {
                send_to_char("They aren't here.\n", ch)
                return
            }

            if (victim == ch) {
                send_to_char("Aye aye, right away!\n", ch)
                return
            }

            if (!is_room_owner(ch, victim.room)
                    && ch.room != victim.room
                    && room_is_private(victim.room) && !IS_TRUSTED(ch, IMPLEMENTOR)) {
                send_to_char("That character is in a private room.\n", ch)
                return
            }

            if (get_trust(victim) >= get_trust(ch)) {
                send_to_char("Do it yourself!\n", ch)
                return
            }

            if (victim.pcdata != null && get_trust(ch) < MAX_LEVEL - 3) {
                send_to_char("Not at your level!\n", ch)
                return
            }

            act(msg, ch, null, victim, TO_VICT)
            interpret(victim, rest, false)
        }
    }

    send_to_char("Ok.\n", ch)
}

/*
* New routines by Dionysos.
*/

fun do_invis(ch: CHAR_DATA, argument: String) {
    val level: Int

    /* RT code for taking a level argument */
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty())
    /* take the default path */ {
        if (ch.invis_level != 0) {
            ch.invis_level = 0
            act("\$n slowly fades into existence.", ch, null, null, TO_ROOM)
            send_to_char("You slowly fade back into existence.\n", ch)
        } else {
            ch.invis_level = get_trust(ch)
            act("\$n slowly fades into thin air.", ch, null, null, TO_ROOM)
            send_to_char("You slowly vanish into thin air.\n", ch)
        }
    } else
    /* do the level thing */ {
        level = atoi(arg.toString())
        if (level < 2 || level > get_trust(ch)) {
            send_to_char("Invis level must be between 2 and your level.\n", ch)
        } else {
            ch.reply = null
            ch.invis_level = level
            act("\$n slowly fades into thin air.", ch, null, null, TO_ROOM)
            send_to_char("You slowly vanish into thin air.\n", ch)
        }
    }

}


fun do_incognito(ch: CHAR_DATA, argument: String) {
    val level: Int

    /* RT code for taking a level argument */
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty())
    /* take the default path */ {
        if (ch.incog_level != 0) {
            ch.incog_level = 0
            act("\$n is no longer cloaked.", ch, null, null, TO_ROOM)
            send_to_char("You are no longer cloaked.\n", ch)
        } else {
            ch.incog_level = get_trust(ch)
            act("\$n cloaks \$s presence.", ch, null, null, TO_ROOM)
            send_to_char("You cloak your presence.\n", ch)
        }
    } else
    /* do the level thing */ {
        level = atoi(arg.toString())
        if (level < 2 || level > get_trust(ch)) {
            send_to_char("Incog level must be between 2 and your level.\n", ch)
        } else {
            ch.reply = null
            ch.incog_level = level
            act("\$n cloaks \$s presence.", ch, null, null, TO_ROOM)
            send_to_char("You cloak your presence.\n", ch)
        }
    }

}


fun do_holylight(ch: CHAR_DATA) {
    if (ch.pcdata == null) {
        return
    }

    if (IS_SET(ch.act, PLR_HOLYLIGHT)) {
        ch.act = REMOVE_BIT(ch.act, PLR_HOLYLIGHT)
        send_to_char("Holy light mode off.\n", ch)
    } else {
        ch.act = SET_BIT(ch.act, PLR_HOLYLIGHT)
        send_to_char("Holy light mode on.\n", ch)
    }

}

/* prefix command: it will put the string typed on each line typed */

fun do_prefi(ch: CHAR_DATA) {
    send_to_char("You cannot abbreviate the prefix command.\n", ch)
}

fun do_prefix(ch: CHAR_DATA, argument: String) {

    if (argument.isEmpty()) {
        if (ch.prefix.isEmpty()) {
            send_to_char("You have no prefix to clear.\n", ch)
            return
        }

        send_to_char("Prefix removed.\n", ch)
        ch.prefix = ""
        return
    }

    val msg = when {
        ch.prefix.isNotEmpty() -> "Prefix changed to $argument.\n"
        else -> "Prefix set to $argument.\n"
    }

    ch.prefix = argument
    send_to_char(msg, ch)

}

/* RT nochannels command, for those spammers */
fun do_grant(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Grant whom induct privileges?", ch)
        return
    }

    val victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (IS_SET(victim.act, PLR_CANINDUCT)) {
        victim.act = REMOVE_BIT(victim.act, PLR_CANINDUCT)
        send_to_char("You have the lost the power to INDUCT.\n", victim)
        send_to_char("INDUCT powers removed.\n", ch)
    } else {
        victim.act = SET_BIT(victim.act, PLR_CANINDUCT)
        send_to_char("You have been given the power to INDUCT.\n", victim)
        send_to_char("INDUCT powers given.\n", ch)
    }

}

fun do_advance(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg1)
    one_argument(rest, arg2)

    if (arg1.isEmpty() || arg2.isEmpty() || !is_number(arg2)) {
        send_to_char("Syntax: advance <char> <level>.\n", ch)
        return
    }

    val victim = get_char_room(ch, arg1.toString())
    if (victim == null) {
        send_to_char("That player is not here.\n", ch)
        return
    }

    if (victim.pcdata == null) {
        send_to_char("Not on NPC's.\n", ch)
        return
    }

    val level = atoi(arg2.toString())
    if (level < 1 || level > 100) {
        send_to_char("Level must be 1 to 100.\n", ch)
        return
    }

    if (level > get_trust(ch)) {
        send_to_char("Limited to your trust level.\n", ch)
        return
    }

    /* Level counting */
    if (ch.level <= 5 || ch.level > LEVEL_HERO) {
        if (level in 6..LEVEL_HERO) {
            total_levels += level - 5
        }
    } else {
        if (level in 6..LEVEL_HERO) {
            total_levels += level - ch.level
        } else {
            total_levels -= ch.level - 5
        }
    }

    /*
         * Lower level:
         *   Reset to level 1.
         *   Then raise again.
         *   Currently, an imp can lower another imp.
         *   -- Swiftest
         */
    if (level <= victim.level) {
        val temp_prac = victim.practice

        send_to_char("Lowering a player's level!\n", ch)
        send_to_char("**** OOOOHHHHHHHHHH  NNNNOOOO ****\n", victim)
        victim.level = 1
        victim.exp = exp_to_level(victim)
        victim.max_hit = 10
        victim.max_mana = 100
        victim.max_move = 100
        victim.practice = 0
        victim.hit = victim.max_hit
        victim.mana = victim.max_mana
        victim.move = victim.max_move
        advance_level(victim)
        victim.practice = temp_prac
    } else {
        send_to_char("Raising a player's level!\n", ch)
        send_to_char("**** OOOOHHHHHHHHHH  YYYYEEEESSS ****\n", victim)
    }

    for (iLevel in victim.level until level) {
        send_to_char("You raise a level!!  ", victim)
        victim.exp += exp_to_level(victim)
        victim.level += 1
        advance_level(victim)
    }
    victim.trust = 0
    save_char_obj(victim)
}

fun do_mset(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = smash_tilde(rest)
    rest = one_argument(rest, arg1)
    rest = one_argument(rest, arg2)
    val arg3 = rest

    if (arg1.isEmpty() || arg2.isEmpty() || arg3.isEmpty()) {
        send_to_char("Syntax:\n", ch)
        send_to_char("  set char <name> <field> <value>\n", ch)
        send_to_char("  Field being one of:\n", ch)
        send_to_char("    str int wis dex con cha sex class level\n", ch)
        send_to_char("    race gold hp mana move practice align\n", ch)
        send_to_char("    train thirst drunk full hometown ethos\n", ch)
        send_to_char("    questp questt relig bloodlust desire\n", ch)
        return
    }

    val victim = get_char_world(ch, arg1.toString())
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    var value = if (is_number(arg3)) atoi(arg3) else -1

    val buf = TextBuffer()
    val arg2str = arg2.toString()
    when {
        eq(arg2str, "str") -> {
            if (value < 3 || value > victim.max(PrimeStat.Strength)) {
                buf.sprintf("Strength range is 3 to %d\n.", victim.max(PrimeStat.Strength))
                send_to_char(buf, ch)
            } else {
                victim.perm_stat[PrimeStat.Strength] = value
            }
        }
        eq(arg2str, "int") -> {
            if (value < 3 || value > victim.max(PrimeStat.Intelligence)) {
                buf.sprintf("Intelligence range is 3 to %d.\n", victim.max(PrimeStat.Intelligence))
                send_to_char(buf, ch)
            } else {
                victim.perm_stat[PrimeStat.Intelligence] = value
            }
        }
        eq(arg2str, "wis") -> {
            if (value < 3 || value > victim.max(PrimeStat.Wisdom)) {
                buf.sprintf("Wisdom range is 3 to %d.\n", victim.max(PrimeStat.Wisdom))
                send_to_char(buf, ch)
            } else {
                victim.perm_stat[PrimeStat.Wisdom] = value
            }
        }
        else -> {
            val v_pcdata = victim.pcdata
            when {
                eq(arg2str, "questp") -> {
                    if (value == -1) {
                        value = 0
                    }
                    if (v_pcdata != null) {
                        v_pcdata.questpoints = value
                    }
                }
                eq(arg2str, "questt") -> {
                    if (value == -1) {
                        value = 30
                    }
                    if (v_pcdata != null) {
                        v_pcdata.nextquest = value
                    }
                }
                eq(arg2str, "relig") -> {
                    if (value < 0 || value >= Religion.values().size) {
                        value = 0
                    }
                    victim.religion = Religion.values()[value]
                }
                eq(arg2str, "dex") -> {
                    if (value < 3 || value > victim.max(PrimeStat.Dexterity)) {
                        buf.sprintf("Dexterity ranges is 3 to %d.\n", victim.max(PrimeStat.Dexterity))
                        send_to_char(buf, ch)
                    } else {
                        victim.perm_stat[PrimeStat.Dexterity] = value
                    }
                }
                eq(arg2str, "con") -> {
                    if (value < 3 || value > victim.max(PrimeStat.Constitution)) {
                        buf.sprintf("Constitution range is 3 to %d.\n", victim.max(PrimeStat.Constitution))
                        send_to_char(buf, ch)
                    } else {
                        victim.perm_stat[PrimeStat.Constitution] = value
                    }
                }
                eq(arg2str, "cha") -> {
                    if (value < 3 || value > victim.max(PrimeStat.Charisma)) {
                        buf.sprintf("Constitution range is 3 to %d.\n", victim.max(PrimeStat.Charisma))
                        send_to_char(buf, ch)
                    } else {
                        victim.perm_stat[PrimeStat.Charisma] = value
                    }
                }
                startsWith(arg2str, "sex") -> {
                    val newSex = Sex.of(value)
                    when (newSex) {
                        null -> send_to_char("Sex range is 0 (none), 1 (male) or 2 (female).\n", ch)
                        else -> {
                            victim.sex = newSex
                            v_pcdata?.true_sex = newSex
                        }
                    }
                }
                startsWith(arg2str, "class") -> {
                    if (v_pcdata == null) {
                        send_to_char("Mobiles have no class.\n", ch)
                        return
                    }

                    val clazz = Clazz.lookup(arg3)
                    if (clazz == null) {
                        buf.sprintf("Possible classes are: ")
                        val classes = Clazz.classes
                        for (i in classes.indices) {
                            val c = classes[i]
                            if (i > 0) {
                                buf.append(" ")
                            }
                            buf.append(c.name)
                        }
                        buf.append(".\n")
                        send_to_char(buf, ch)
                        return
                    }

                    victim.clazz = clazz
                    victim.exp = victim.level * exp_per_level(victim)
                    return
                }
                startsWith(arg2str, "level") -> {
                    if (v_pcdata != null) {
                        send_to_char("Not on PC's.\n", ch)
                        return
                    }

                    if (value < 0 || value > 100) {
                        send_to_char("Level range is 0 to 100.\n", ch)
                        return
                    }
                    victim.level = value
                    return
                }
                startsWith(arg2str, "gold") -> {
                    victim.gold = value
                    return
                }
                startsWith(arg2str, "hp") -> {
                    if (value < -10 || value > 30000) {
                        send_to_char("Hp range is -10 to 30,000 hit points.\n", ch)
                        return
                    }
                    victim.max_hit = value
                    if (v_pcdata != null) {
                        v_pcdata.perm_hit = value
                    }
                    return
                }
                startsWith(arg2str, "mana") -> {
                    if (value < 0 || value > 60000) {
                        send_to_char("Mana range is 0 to 60,000 mana points.\n", ch)
                        return
                    }
                    victim.max_mana = value
                    if (v_pcdata != null) {
                        v_pcdata.perm_mana = value
                    }
                    return
                }
                startsWith(arg2str, "move") -> {
                    if (value < 0 || value > 60000) {
                        send_to_char("Move range is 0 to 60,000 move points.\n", ch)
                        return
                    }
                    victim.max_move = value
                    if (v_pcdata != null) {
                        v_pcdata.perm_move = value
                    }
                    return
                }
                startsWith(arg2str, "practice") -> {
                    if (value < 0 || value > 250) {
                        send_to_char("Practice range is 0 to 250 sessions.\n", ch)
                        return
                    }
                    victim.practice = value
                    return
                }
                startsWith(arg2str, "train") -> {
                    if (value < 0 || value > 50) {
                        send_to_char("Training session range is 0 to 50 sessions.\n", ch)
                        return
                    }
                    victim.train = value
                    return
                }
                startsWith(arg2str, "align") -> {
                    if (value < -1000 || value > 1000) {
                        send_to_char("Alignment range is -1000 to 1000.\n", ch)
                        return
                    }
                    victim.alignment = value
                    send_to_char("Remember to check their hometown.\n", ch)
                    return
                }
                startsWith(arg2str, "ethos") -> {
                    if (v_pcdata == null) {
                        send_to_char("Mobiles don't have an ethos.\n", ch)
                        return
                    }
                    if (value < 0 || value >= Ethos.values().size) {
                        send_to_char("The values are Lawful - 0, Neutral - 1, Chaotic - 2\n", ch)
                        return
                    }

                    victim.ethos = Ethos.fromInt(value)
                    return
                }
                startsWith(arg2str, "hometown") -> {
                    if (v_pcdata == null) {
                        send_to_char("Mobiles don't have hometowns.\n", ch)
                        return
                    }
                    val town = HomeTown.ofOrNull(value)
                    if (town == null) {
                        send_to_char("Please choose one of the following :.\n", ch)
                        send_to_char("Town        Alignment       Value\n", ch)
                        send_to_char("----        ---------       -----\n", ch)
                        send_to_char("Midgaard     Any              0\n", ch)
                        send_to_char("New Thalos   Any              1\n", ch)
                        send_to_char("Titan        Any              2\n", ch)
                        send_to_char("Ofcol        Neutral          3\n", ch)
                        send_to_char("Old Midgaard Evil             4\n", ch)
                        return
                    }

                    if (town.isAlignAllowed(victim.align)) {
                        victim.hometown = town
                    } else {
                        send_to_char("The hometown doesn't match this character's alignment.\n", ch)
                    }
                }
                startsWith(arg2str, "thirst") -> {
                    if (v_pcdata == null) {
                        send_to_char("Not on NPC's.\n", ch)
                        return
                    }

                    if (value < -1 || value > 100) {
                        send_to_char("Thirst range is -1 to 100.\n", ch)
                        return
                    }

                    v_pcdata.condition[COND_THIRST] = value
                    return
                }
                startsWith(arg2str, "drunk") -> {
                    if (v_pcdata == null) {
                        send_to_char("Not on NPC's.\n", ch)
                        return
                    }

                    if (value < -1 || value > 100) {
                        send_to_char("Drunk range is -1 to 100.\n", ch)
                        return
                    }

                    v_pcdata.condition[COND_DRUNK] = value
                    return
                }
                startsWith(arg2str, "full") -> {
                    if (v_pcdata == null) {
                        send_to_char("Not on NPC's.\n", ch)
                        return
                    }

                    if (value < -1 || value > 100) {
                        send_to_char("Full range is -1 to 100.\n", ch)
                        return
                    }

                    v_pcdata.condition[COND_FULL] = value
                    return
                }
                startsWith(arg2str, "bloodlust") -> {
                    if (v_pcdata == null) {
                        send_to_char("Not on NPC's.\n", ch)
                        return
                    }

                    if (value < -1 || value > 100) {
                        send_to_char("Full range is -1 to 100.\n", ch)
                        return
                    }

                    v_pcdata.condition[COND_BLOODLUST] = value
                    return
                }
                startsWith(arg2str, "desire") -> {
                    if (v_pcdata == null) {
                        send_to_char("Not on NPC's.\n", ch)
                        return
                    }

                    if (value < -1 || value > 100) {
                        send_to_char("Full range is -1 to 100.\n", ch)
                        return
                    }

                    v_pcdata.condition[COND_DESIRE] = value
                    return
                }
                startsWith(arg2str, "race") -> {
                    val race = Race.lookup(arg3)
                    if (race == null) {
                        send_to_char("That is not a valid race.\n", ch)
                        return
                    }

                    if (v_pcdata != null) {
                        for (sn in Skill.skills) {
                            if (!RACE_OK(victim, sn)) {
                                v_pcdata.learned[sn.ordinal] = 0
                            }

                            if (victim.race === sn.race) {
                                v_pcdata.learned[sn.ordinal] = 70
                            }
                        }
                    }

                    if (victim.race === victim.race) {
                        victim.race = race
                    }
                    victim.race = race

                    victim.exp = victim.level * exp_per_level(victim)
                    return
                }
                else -> do_mset(ch, "")
            }
        }
    }
}

fun do_induct(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg1 = StringBuilder()
    rest = one_argument(rest, arg1)
    val arg2 = rest

    if (arg1.isEmpty() || arg2.isEmpty()) {
        send_to_char("Usage: induct <player> <cabal>\n", ch)
        return
    }


    val victim = get_char_world(ch, arg1.toString())
    if (victim == null) {
        send_to_char("That player istat_buf.setLength(0)'t on.\n", ch)
        return
    }

    val v_pcdata = victim.pcdata
    if (v_pcdata == null) {
        act("\$N is not smart enough to join a cabal.", ch, null, victim, TO_CHAR)
        return
    }

    if (CANT_CHANGE_TITLE(victim)) {
        act("\$N has tried to join a cabal, but failed.", ch, null, victim, TO_CHAR)
        return
    }

    val c = Clan.lookup(arg2)
    if (c == null) {
        send_to_char("I've never heard of that cabal.\n", ch)
        return
    }

    if (victim.clazz === Clazz.WARRIOR && c == Clan.Shalafi) {
        act("But \$N is a filthy warrior!", ch, null, victim, TO_CHAR)
        return
    }

    if (c == Clan.Ruler && victim.curr_stat(PrimeStat.Intelligence) < 20) {
        act("\$N is not clever enough to become a Ruler!", ch, null, victim, TO_CHAR)
        return
    }

    if (!IS_TRUSTED(ch, LEVEL_IMMORTAL) && !(IS_SET(ch.act, PLR_CANINDUCT) && (c == Clan.None && ch.cabal == victim.cabal || c != Clan.None && ch.cabal == c && victim.cabal == Clan.None))) {
        send_to_char("You do not have that power.\n", ch)
        return
    }
    val prev_cabal = victim.cabal
    victim.cabal = c
    victim.act = REMOVE_BIT(victim.act, PLR_CANINDUCT)
    val cabal = c.long_name


    /* set cabal skills to 70, remove other cabal skills */
    for (sn in Skill.skills) {
        if (victim.cabal != Clan.None && sn.cabal == victim.cabal) {
            v_pcdata.learned[sn.ordinal] = 70
        } else if (sn.cabal != Clan.None) {
            v_pcdata.learned[sn.ordinal] = 0
        }
    }

    act("\$n has been inducted into $cabal.", victim, null, null, TO_NOTVICT)
    act("You have been inducted into $cabal.", victim, null, null, TO_CHAR)
    if (ch.room != victim.room) send_to_char("${victim.name} has been inducted into $cabal.\n", ch)
    if (victim.cabal == Clan.None && prev_cabal != Clan.None) {
        val name = when (prev_cabal) {
            Clan.BattleRager -> "The LOVER OF MAGIC."
            Clan.Shalafi -> "The HATER OF MAGIC."
            Clan.Knight -> "The UNHONOURABLE FIGHTER."
            Clan.Invader, Clan.Chaos, Clan.Lion, Clan.Hunter, Clan.Ruler -> "NO MORE CABALS."
            else -> return
        }
        set_title(victim, name)
        victim.act = SET_BIT(victim.act, PLR_NO_TITLE)
    }
}

fun do_smite(ch: CHAR_DATA, argument: String) {
    val victim = get_char_world(ch, argument)

    if (argument.isEmpty()) {
        send_to_char("You are so frustrated you smite yourself!  OWW!\n",
                ch)
        return
    }

    if (victim == null) {
        send_to_char("You'll have to smite them some other day.\n", ch)
        return
    }

    if (victim.pcdata == null) {
        send_to_char("That poor mob never did anything to you.\n", ch)
        return
    }

    if (victim.trust > ch.trust) {
        send_to_char("How dare you!\n", ch)
        return
    }

    if (victim.position < Pos.Sleeping) {
        send_to_char("Take pity on the poor thing.\n", ch)
        return
    }

    act("A bolt comes down out of the heavens and smites you!", victim, null,
            ch, TO_CHAR)
    act("You reach down and smite \$n!", victim, null, ch, TO_VICT)
    act("A bolt from the heavens smites \$n!", victim, null, ch, TO_NOTVICT)
    victim.hit = victim.hit / 2
}

fun do_popularity(ch: CHAR_DATA) {
    val buf = TextBuffer()
    buf.sprintf("Area popularity statistics (in String  ticks)\n")

    for ((i, area) in Index.AREAS.withIndex()) {
        if (area.count >= 5000000) {
            buf.sprintf(false, "%-20s overflow       ", area.name)
        } else {
            buf.sprintf(false, "%-20s %-8lu       ", area.name, area.count)
        }
        if (i % 2 == 0) {
            buf.append("\n")
        }
    }
    buf.append("\r\n\n")
    page_to_char(buf, ch)
}

fun do_ititle(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val victim: CHAR_DATA?
    val arg = StringBuilder()
    rest = one_argument(rest, arg)

    if (arg.isEmpty()) {
        send_to_char("Change whose title to what?\n", ch)
        return
    }

    victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("Nobody is playing with that name.\n", ch)
        return
    }

    if (ch.pcdata == null) {
        return
    }

    if (rest.isEmpty()) {
        send_to_char("Change the title to what?\n", ch)
        return
    }

    if (rest.length > 45) {
        rest = rest.substring(45)
    }

    rest = smash_tilde(rest)
    set_title(victim, rest)
    send_to_char("Ok.\n", ch)
}


fun do_rename(ch: CHAR_DATA, argument: String) {
    var rest = argument

    val victim: CHAR_DATA?
    val old_name = StringBuilder()
    val new_name = StringBuilder()
    rest = one_argument(rest, old_name)
    one_argument(rest, new_name)

    if (old_name.isEmpty()) {
        send_to_char("Rename who?\n", ch)
        return
    }

    victim = get_char_world(ch, old_name.toString())

    if (victim == null) {
        send_to_char("There is no such a person online.\n", ch)
        return
    }

    if (victim.pcdata == null) {
        send_to_char("You cannot use Rename on NPCs.\n", ch)
        return
    }

    if (victim != ch && get_trust(victim) >= get_trust(ch)) {
        send_to_char("You failed.\n", ch)
        return
    }

    val pc = get_pc(victim)
    if (pc == null) {
        send_to_char("This player has lost his link or is inside a pager or the like.\n", ch)
        return
    }

    if (new_name.isEmpty()) {
        send_to_char("Rename to what new name?\n", ch)
        return
    }

    /*
    if (victim.cabal)
    {
     send_to_char ("This player is member of a cabal, remove him from there first.\n",ch);
     return;
    }
*/

    new_name.setCharAt(0, UPPER(new_name[0]))
    if (!check_parse_name(new_name.toString())) {
        send_to_char("The new name is illegal.\n", ch)
        return
    }

    var strsave = nw_config.lib_player_dir + "/" + capitalize(new_name.toString())
    if (File(strsave).exists()) {
        send_to_char("A player with that name already exists!\n", ch)
        return
    }

    if (get_char_world(ch, new_name.toString()) != null) {
        send_to_char("A player with the name you specified already exists!\n", ch)
        return
    }

    strsave = nw_config.lib_player_dir + "/" + capitalize(victim.name)

    /*
 * NOTE: Players who are level 1 do NOT get saved under a new name
 */
    victim.name = capitalize_nn(new_name.toString())

    save_char_obj(victim)


    File(strsave).delete()
    send_to_char("Character renamed.\n", ch)
    victim.position = Pos.Standing
    act("\$n has renamed you to \$N!", ch, null, victim, TO_VICT)

}

fun do_notitle(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?

    if (!IS_IMMORTAL(ch)) {
        return
    }
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Usage:\n  notitle <player>\n", ch)
        return
    }

    victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("He is not currently playing.\n", ch)
        return
    }

    if (IS_SET(victim.act, PLR_NO_TITLE)) {
        victim.act = REMOVE_BIT(victim.act, PLR_NO_TITLE)
        send_to_char("You can change your title again.\n", victim)
        send_to_char("Ok.\n", ch)
    } else {
        victim.act = SET_BIT(victim.act, PLR_NO_TITLE)
        send_to_char("You won't be able to change your title anymore.\n", victim)
        send_to_char("Ok.\n", ch)
    }
}


fun do_noaffect(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?

    if (!IS_IMMORTAL(ch)) {
        return
    }
    val arg = StringBuilder()
    one_argument(argument, arg)


    if (arg.isEmpty()) {
        send_to_char("Usage:\n  noaffect <player>\n", ch)
        return
    }

    victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("He is not currently playing.\n", ch)
        return
    }


    var paf: Affect? = victim.affected
    while (paf != null) {
        val paf_next = paf.next
        if (paf.duration >= 0) {
            if (!paf.type.msg_off.isNotEmpty()) {
                send_to_char(paf.type.msg_off, victim)
                send_to_char("\n", victim)
            }
            affect_remove(victim, paf)
        }
        paf = paf_next
    }

}

fun do_affrooms(ch: CHAR_DATA) {
    if (top_affected_room == null) {
        send_to_char("No affected room.\n", ch)
    }
    var count = 0
    var room = top_affected_room
    while (room != null) {
        val room_next = room.aff_next
        count++
        val buf = TextBuffer()
        buf.sprintf("%d) [Vnum : %5d] %s\n", count, room.vnum, room.name)
        send_to_char(buf, ch)
        room = room_next
    }
}

fun do_find(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        send_to_char("Ok. But what I should find?\n", ch)
        return
    }

    val location = find_location(ch, arg.toString())
    if (location == null) {
        send_to_char("No such location.\n", ch)
        return
    }

    val buf = TextBuffer()
    buf.sprintf("%s.\n", find_way(ch, ch.room, location))
    send_to_char(buf, ch)
    buf.sprintf("From %d to %d: %s", ch.room.vnum, location.vnum, buf)
    log_string(buf.toString())
}


fun do_reboot(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Usage: reboot now\n", ch)
        send_to_char("Usage: reboot <ticks to reboot>\n", ch)
        send_to_char("Usage: reboot cancel\n", ch)
        send_to_char("Usage: reboot status\n", ch)
        return
    }

    if (is_name(arg.toString(), "cancel")) {
        if (time_sync != 0) {
            send_to_char("Time synchronization is activated, you cannot cancel the reboot.\n", ch)
            return
        }
        reboot_counter = -1
        send_to_char("Reboot canceled.\n", ch)
        return
    }

    if (is_name(arg.toString(), "now")) {
        reboot_nightworks(true, NIGHTWORKS_REBOOT)
        return
    }

    if (is_name(arg.toString(), "status")) {
        if (time_sync != 0) {
            send_to_char("Time synchronization is activated.\n", ch)
            return
        }
        val buf = TextBuffer()
        if (reboot_counter == -1) {
            buf.sprintf("Only time synchronization reboot is activated.\n")
        } else {
            buf.sprintf("Reboot in %i minutes.\n", reboot_counter)
        }
        send_to_char(buf, ch)
        return
    }

    if (is_number(arg)) {
        if (time_sync != 0) {
            send_to_char("Time synchronization is activated, you cannot change the reboot.\n", ch)
            return
        }
        reboot_counter = atoi(arg.toString())
        send_to_char("Nightworks will reboot in $reboot_counter ticks.\n", ch)
        return
    }

    do_reboot(ch, "")
}


fun reboot_nightworks(fMessage: Boolean, fType: Int) {
    val buf: String
    val log_buf: String
    if (fType == NIGHTWORKS_REBOOT) {
        log_buf = "Rebooting NIGHTWORKS."
        buf = "Nightworks is going down for reboot NOW!\n"
    } else {
        log_buf = "Shutting down NIGHTWORKS."
        buf = "Nightworks is going down for halt NOW!\n"
    }
    log_string(log_buf)

    Server.shudown = true
    Server.nw_exit = fType

    for (pc in Index.PLAYERS) {
        if (fMessage) {
            write_to_buffer(pc.con, buf)
        }
        save_char_obj(pc.o_ch)
        close_socket(pc.con)
    }
}


fun do_premort(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?

    if (!IS_IMMORTAL(ch)) {
        return
    }
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Usage:\n  premort <player>\n", ch)
        return
    }

    victim = get_char_world(ch, arg.toString())
    if (victim == null) {
        send_to_char("He is not currently playing.\n", ch)
        return
    }

    if (victim.pcdata == null || victim.level < LEVEL_HERO) {
        send_to_char("You cannot give remorting permissions to that!.\n", ch)
        return
    }
    if (IS_SET(victim.act, PLR_CANREMORT)) {
        victim.act = REMOVE_BIT(victim.act, PLR_CANREMORT)
        send_to_char("You have lost your remorting permission.\n", victim)
        send_to_char("Ok.\n", ch)
    } else {
        victim.act = SET_BIT(victim.act, PLR_CANREMORT)
        send_to_char("You are given the permission to remort.\n", victim)
        send_to_char("Ok.\n", ch)
    }
}

fun do_maximum(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val argb = StringBuilder()
    rest = one_argument(rest, argb)

    if (argb.isEmpty()) {
        send_to_char("Usage: maximum status\n", ch)
        send_to_char("Usage: maximum reset\n", ch)
        send_to_char("Usage: maximum newbies <number of newbies>\n", ch)
        send_to_char("Usage: maximum oldies <number of oldies>\n", ch)
        return
    }

    var arg = argb.toString()
    if (is_name(arg, "status")) {
        send_to_char("Maximum oldies allowed: $max_oldies.\n", ch)
        send_to_char("Maximum newbies allowed: $max_newbies.\n", ch)
        send_to_char("Current number of players: $iNumPlayers.\n", ch)
        return
    }

    if (is_name(arg, "reset")) {
        max_newbies = MAX_NEWBIES
        max_oldies = MAX_OLDIES
        send_to_char("Maximum newbies and oldies have been reset.\n", ch)
        do_maximum(ch, "status")
        return
    }

    if (is_name(arg, "newbies")) {
        argb.setLength(0)
        one_argument(rest, argb)
        arg = argb.toString()
        if (!is_number(arg)) {
            do_maximum(ch, "")
            return
        }
        max_newbies = atoi(arg)
        if (max_newbies < 0) {
            send_to_char("No newbies are allowed!!!\n", ch)
        } else {
            send_to_char("Now maximum newbies allowed: $max_newbies.\n", ch)
        }
        return
    }

    if (is_name(arg, "oldies")) {
        argb.setLength(0)
        one_argument(rest, argb)
        arg = argb.toString()
        if (!is_number(arg)) {
            do_maximum(ch, "")
            return
        }
        max_oldies = atoi(arg)
        if (max_oldies < 0) {
            send_to_char("No oldies are allowed!!!\n", ch)
        } else {
            send_to_char("Now maximum oldies allowed: $max_oldies.\n", ch)
        }
        return
    }

    do_maximum(ch, "")
}
