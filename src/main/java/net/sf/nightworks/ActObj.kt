package net.sf.nightworks

import net.sf.nightworks.act.comm.do_yell
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.Apply
import net.sf.nightworks.model.AttackType
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.Clan
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Liquid
import net.sf.nightworks.model.Obj
import net.sf.nightworks.model.Pos
import net.sf.nightworks.model.PrimeStat
import net.sf.nightworks.model.Shop
import net.sf.nightworks.model.Size
import net.sf.nightworks.model.WeaponType
import net.sf.nightworks.model.Wear
import net.sf.nightworks.model.Wiznet
import net.sf.nightworks.model.carry
import net.sf.nightworks.model.curr_stat
import net.sf.nightworks.util.CAN_WEAR
import net.sf.nightworks.util.IS_AFFECTED
import net.sf.nightworks.util.IS_AWAKE
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_OBJ_STAT
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_WATER
import net.sf.nightworks.util.IS_WEAPON_STAT
import net.sf.nightworks.util.MOUNTED
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.URANGE
import net.sf.nightworks.util.WAIT_STATE
import net.sf.nightworks.util.WEIGHT_MULT
import net.sf.nightworks.util.atoi
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.get_carry_weight
import net.sf.nightworks.util.is_number
import net.sf.nightworks.util.number_fuzzy
import net.sf.nightworks.util.randomPercent
import net.sf.nightworks.util.number_range
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.random
import net.sf.nightworks.util.smash_tilde
import net.sf.nightworks.util.startsWith

/* RT part of the corpse looting code */

private val _loot = true

fun can_loot(ch: CHAR_DATA, obj: Obj): Boolean {
    if (_loot) {
        return true
    }
    if (IS_IMMORTAL(ch)) {
        return true
    }

    val obj_owner = obj.owner ?: return true
    val owner = Index.CHARS.firstOrNull { eq(it.name, obj_owner) } ?: return true

    return when {
        eq(ch.name, owner.name) -> true
        owner.pcdata != null && IS_SET(owner.act, PLR_CANLOOT) -> true
        else -> is_same_group(ch, owner)
    }
}


fun get_obj(ch: CHAR_DATA, obj: Obj, container: Obj?) {
    if (!CAN_WEAR(obj, ITEM_TAKE)) {
        send_to_char("You can't take that.\n", ch)
        return
    }

    if (obj.pIndexData.limit != -1) {
        if (IS_OBJ_STAT(obj, ITEM_ANTI_EVIL) && ch.isEvil()
                || IS_OBJ_STAT(obj, ITEM_ANTI_GOOD) && ch.isGood()
                || IS_OBJ_STAT(obj, ITEM_ANTI_NEUTRAL) && ch.isNeutralA()) {
            act("You are zapped by \$p and drop it.", ch, obj, null, TO_CHAR)
            act("\$n is zapped by \$p and drops it.", ch, obj, null, TO_ROOM)
            return
        }
    }

    if (ch.carry_number + get_obj_number(obj) > can_carry_items(ch)) {
        act("\$d: you can't carry that many items.",
                ch, null, obj.name, TO_CHAR)
        return
    }


    if (get_carry_weight(ch) + get_obj_weight(obj) > can_carry_weight(ch)) {
        act("\$d: you can't carry that much weight.",
                ch, null, obj.name, TO_CHAR)
        return
    }

    val in_room = obj.in_room
    if (in_room != null) {
        for (gch in in_room.people) {
            if (gch.on == obj) {
                act("\$N appears to be using \$p.", ch, obj, gch, TO_CHAR)
                return
            }
        }
    }


    if (container != null) {
        if (container.pIndexData.vnum == OBJ_VNUM_INVADER_SKULL
                || container.pIndexData.vnum == OBJ_VNUM_RULER_STAND
                || container.pIndexData.vnum == OBJ_VNUM_BATTLE_THRONE
                || container.pIndexData.vnum == OBJ_VNUM_CHAOS_ALTAR
                || container.pIndexData.vnum == OBJ_VNUM_SHALAFI_ALTAR
                || container.pIndexData.vnum == OBJ_VNUM_KNIGHT_ALTAR
                || container.pIndexData.vnum == OBJ_VNUM_LIONS_ALTAR
                || container.pIndexData.vnum == OBJ_VNUM_HUNTER_ALTAR) {
            act("You get \$p from \$P.", ch, obj, container, TO_CHAR)
            if (!IS_AFFECTED(ch, AFF_SNEAK)) {
                act("\$n gets \$p from \$P.", ch, obj, container, TO_ROOM)
            }
            obj_from_obj(obj)
            act("\$p fades to black, then dissapears!", ch, container, null, TO_ROOM)
            act("\$p fades to black, then dissapears!", ch, container, null, TO_CHAR)
            extract_obj(container)
            obj_to_char(obj, ch)

            Index.PLAYERS
                    .filter { it.ch.cabal.obj_ptr === obj }
                    .forEach { act("{gYou feel a shudder in your Cabal Power!{x", it.ch, null, null, TO_CHAR, Pos.Dead) }

            if (IS_SET(obj.progtypes, OPROG_GET)) {
                obj.pIndexData.oprogs.get_prog!!(obj, ch)
            }
            return
        }

        if (container.pIndexData.vnum == OBJ_VNUM_PIT && !CAN_WEAR(container, ITEM_TAKE) && !IS_OBJ_STAT(obj, ITEM_HAD_TIMER)) {
            obj.timer = 0
        }
        act("You get \$p from \$P.", ch, obj, container, TO_CHAR)
        if (!IS_AFFECTED(ch, AFF_SNEAK)) {
            act("\$n gets \$p from \$P.", ch, obj, container, TO_ROOM)
        }
        obj.extra_flags = REMOVE_BIT(obj.extra_flags, ITEM_HAD_TIMER)
        obj_from_obj(obj)
    } else {
        act("You get \$p.", ch, obj, container, TO_CHAR)
        if (!IS_AFFECTED(ch, AFF_SNEAK)) {
            act("\$n gets \$p.", ch, obj, container, TO_ROOM)
        }
        obj_from_room(obj)
    }

    if (obj.item_type == ItemType.Money) {
        ch.silver += obj.value[0].toInt()
        ch.gold += obj.value[1].toInt()
        if (IS_SET(ch.act, PLR_AUTO_SPLIT)) { /* AUTOSPLIT code */
            val members = ch.room.people.count { !IS_AFFECTED(it, AFF_CHARM) && is_same_group(it, ch) }
            if (members > 1 && (obj.value[0] > 1 || obj.value[1] != 0L)) {
                do_split(ch, obj.value[0].toString() + " " + obj.value[1])
            }
        }

        extract_obj(obj)
    } else {
        obj_to_char(obj, ch)
        if (IS_SET(obj.progtypes, OPROG_GET)) {
            obj.pIndexData.oprogs.get_prog!!(obj, ch)
        }
    }

}


fun do_get(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val container: Obj?
    var found: Boolean

    val arg1b = StringBuilder()
    val arg2b = StringBuilder()
    rest = one_argument(rest, arg1b)
    rest = one_argument(rest, arg2b)

    val arg1 = arg1b.toString()
    var arg2 = arg2b.toString()
    if (eq(arg2, "from")) {
        one_argument(rest, arg2b)
    }
    arg2 = arg2b.toString()

    /* Get type. */
    if (arg1.isEmpty()) {
        send_to_char("Get what?\n", ch)
        return
    }

    if (is_number(arg1)) {
        val weight: Int
        var gold = 0
        var silver = 0

        val amount = atoi(arg1)
        if (amount <= 0 || !eq(arg2, "coins") && !eq(arg2, "coin") && !eq(arg2, "gold") && !eq(arg2, "silver")) {
            send_to_char("Usage: <get> <number> <silver|gold|coin|coins>\n", ch)
            return
        }

        weight = when {
            eq(arg2, "gold") -> amount * 2 / 5
            else -> amount / 10
        }

        if (get_carry_weight(ch) + weight > can_carry_weight(ch)) {
            act("You can't carry that much weight.", ch, null, null, TO_CHAR)
            return
        }


        for (o in ch.room.objects) {
            when (o.pIndexData.vnum) {
                OBJ_VNUM_SILVER_ONE -> silver += 1
                OBJ_VNUM_GOLD_ONE -> gold += 1
                OBJ_VNUM_SILVER_SOME -> silver += o.value[0].toInt()
                OBJ_VNUM_GOLD_SOME -> gold += o.value[1].toInt()
                OBJ_VNUM_COINS -> {
                    silver += o.value[0].toInt()
                    gold += o.value[1].toInt()
                }
            }
        }

        if (eq(arg2, "gold") && amount > gold || !eq(arg2, "gold") && amount > silver) {
            send_to_char("There's not that much coins there.\n", ch)
            return
        }

        if (eq(arg2, "gold")) {
            gold = amount
            silver = 0
        } else {
            silver = amount
            gold = 0
        }

        for (obj in ch.room.objects) {
            when (obj.pIndexData.vnum) {
                OBJ_VNUM_SILVER_ONE -> if (silver != 0) {
                    silver -= 1
                    extract_obj(obj)
                }

                OBJ_VNUM_GOLD_ONE -> if (gold != 0) {
                    gold -= 1
                    extract_obj(obj)
                }

                OBJ_VNUM_SILVER_SOME -> if (silver != 0) {
                    if (silver >= obj.value[0].toInt()) {
                        silver -= obj.value[0].toInt()
                        extract_obj(obj)
                    } else {
                        obj.value[0] -= silver.toLong()
                        silver = 0
                    }
                }

                OBJ_VNUM_GOLD_SOME -> if (gold != 0) {
                    if (gold >= obj.value[1]) {
                        gold -= obj.value[1].toInt()
                        extract_obj(obj)
                    } else {
                        obj.value[1] -= gold.toLong()
                        gold = 0
                    }
                }

                OBJ_VNUM_COINS -> {
                    if (silver != 0) {
                        if (silver >= obj.value[0]) {
                            silver -= obj.value[0].toInt()
                            gold = obj.value[1].toInt()
                            extract_obj(obj)
                            val m = create_money(gold, 0)
                            obj_to_room(m, ch.room)
                            gold = 0
                        } else {
                            obj.value[0] -= silver.toLong()
                            silver = 0
                        }
                    }
                    if (gold != 0) {
                        if (gold >= obj.value[1]) {
                            gold -= obj.value[1].toInt()
                            silver = obj.value[0].toInt()
                            extract_obj(obj)
                            val m = create_money(0, silver)
                            obj_to_room(m, ch.room)
                            silver = 0
                        } else {
                            obj.value[1] -= gold.toLong()
                            gold = 0
                        }
                    }
                }
            }
            if (silver == 0 && gold == 0) {
                break
            }
        }

        /* restore the amount */
        if (eq(arg2, "gold")) {
            gold = amount
            silver = 0
        } else {
            silver = amount
            gold = 0
        }

        if (silver != 0) {
            ch.silver += amount
        } else {
            ch.gold += amount
        }

        act("You get some money from the floor.", ch, null, null, TO_CHAR)
        if (!IS_AFFECTED(ch, AFF_SNEAK)) {
            act("\$n gets some money from the floor.", ch, null, null, TO_ROOM)
        }

        if (IS_SET(ch.act, PLR_AUTO_SPLIT)) { /* AUTOSPLIT code */
            val members = ch.room.people.count { !IS_AFFECTED(it, AFF_CHARM) && is_same_group(it, ch) }
            if (members > 1 && amount > 1) {
                do_split(ch, silver.toString() + " " + gold)
            }
        }


        return
    }

    if (arg2.isEmpty()) {
        if (!eq(arg1, "all") && !startsWith("all.", arg1)) {
            val obj = get_obj_list(ch, arg1, ch.room.objects)
            if (obj == null) {
                act("I see no \$T here.", ch, null, arg1, TO_CHAR)
                return
            }

            get_obj(ch, obj, null)
        } else {
            /* 'get all' or 'get all.obj' */
            found = false
            for (o in ch.room.objects) {
                if ((arg1.length < 4 || is_name(arg1.substring(4), o.name)) && can_see_obj(ch, o)) {
                    found = true
                    get_obj(ch, o, null)
                }
            }

            if (!found) {
                if (arg1.length < 4) {
                    send_to_char("I see nothing here.\n", ch)
                } else {
                    act("I see no \$T here.", ch, null, arg1.substring(4), TO_CHAR)
                }
            }
        }
    } else {
        /* 'get ... container' */
        if (eq(arg2, "all") || startsWith("all.", arg2)) {
            send_to_char("You can't do that.\n", ch)
            return
        }

        container = get_obj_here(ch, arg2)
        if (container == null) {
            act("I see no \$T here.", ch, null, arg2, TO_CHAR)
            return
        }

        when (container.item_type) {

            ItemType.Container, ItemType.CorpseNpc -> {
            }

            ItemType.CorpsePc -> {
                if (!can_loot(ch, container)) {
                    send_to_char("You can't do that.\n", ch)
                    return
                }
            }
            else -> {
                send_to_char("That's not a container.\n", ch)
                return
            }
        }

        if (IS_SET(container.value[1], CONT_CLOSED)) {
            act("The \$d is closed.", ch, null, container.name, TO_CHAR)
            return
        }

        if (!eq(arg1, "all") && !startsWith("all.", arg1)) {
            /* 'get obj container' */
            val obj = get_obj_list(ch, arg1, container.contains)
            if (obj == null) {
                act("I see nothing like that in the \$T.",
                        ch, null, arg2, TO_CHAR)
                return
            }
            get_obj(ch, obj, container)
        } else {
            /* 'get all container' or 'get all.obj container' */
            found = false
            for (o in container.contains) {
                if ((arg1.length == 3 || is_name(arg1.substring(4), o.name)) && can_see_obj(ch, o)) {
                    found = true
                    if (container.pIndexData.vnum == OBJ_VNUM_PIT && !IS_IMMORTAL(ch)) {
                        send_to_char("Don't be so greedy!\n", ch)
                        return
                    }
                    get_obj(ch, o, container)
                }
            }

            if (!found) {
                if (arg1.length < 4) {
                    act("I see nothing in the \$T.", ch, null, arg2, TO_CHAR)
                } else {
                    act("I see nothing like that in the \$T.", ch, null, arg2, TO_CHAR)
                }
            }
        }
    }
}


fun do_put(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg1b = StringBuilder()
    val arg2b = StringBuilder()
    rest = one_argument(rest, arg1b)
    rest = one_argument(rest, arg2b)
    var arg2 = arg2b.toString()

    if (eq(arg2, "in") || eq(arg2, "on")) {
        one_argument(rest, arg2b)
    }

    if (arg1b.isEmpty() || arg2b.isEmpty()) {
        send_to_char("Put what in what?\n", ch)
        return
    }
    val arg1 = arg1b.toString()
    arg2 = arg2b.toString()

    if (eq(arg2, "all") || startsWith("all.", arg2)) {
        send_to_char("You can't do that.\n", ch)
        return
    }

    val container = get_obj_here(ch, arg2)
    if (container == null) {
        act("I see no \$T here.", ch, null, arg2, TO_CHAR)
        return
    }

    if (container.item_type != ItemType.Container) {
        send_to_char("That's not a container.\n", ch)
        return
    }

    if (IS_SET(container.value[1], CONT_CLOSED)) {
        act("The \$d is closed.", ch, null, container.name, TO_CHAR)
        return
    }

    if (!eq(arg1, "all") && !startsWith("all.", arg1)) {
        /* 'put obj container' */
        val obj = get_obj_carry(ch, arg1)
        if (obj == null) {
            send_to_char("You do not have that item.\n", ch)
            return
        }

        if (obj == container) {
            send_to_char("You can't fold it into itself.\n", ch)
            return
        }

        if (!can_drop_obj(ch, obj)) {
            send_to_char("You can't let go of it.\n", ch)
            return
        }

        if (WEIGHT_MULT(obj) != 100) {
            send_to_char("You have a feeling that would be a bad idea.\n", ch)
            return
        }

        if (obj.pIndexData.limit != -1 && !IS_SET(container.value[1], CONT_ST_LIMITED)) {
            act("This unworthy container won't hold \$p.", ch, obj, null, TO_CHAR)
            return
        }
        /*
    if ( IS_SET(container.value[1],CONT_FOR_ARROW)
        && (obj.item_type != ItemType.Weapon
        || obj.value[0]  != WEAPON_ARROW ) )
    {
     act("You can only put arrows in $p.",ch,container,null,TO_CHAR);
     return;
    }
*/
        if (get_obj_weight(obj) + get_true_weight(container) > container.value[0] * 10 || get_obj_weight(obj) > container.value[3] * 10) {
            send_to_char("It won't fit.\n", ch)
            return
        }

        if (obj.item_type == ItemType.Potion && IS_SET(container.wear_flags, ITEM_TAKE)) {
            var pcount = 0
            for (objc in container.contains) {
                if (objc.item_type == ItemType.Potion) {
                    pcount++
                }
            }
            if (pcount > 15) {
                act("It's not safe to put more potions into \$p.", ch, container, null, TO_CHAR)
                return
            }
        }

        val pcount = container.contains.size
        if (pcount > container.value[0]) {
            act("It's not safe to put that much item into \$p.", ch, container, null, TO_CHAR)
            return
        }

        if (container.pIndexData.vnum == OBJ_VNUM_PIT && !CAN_WEAR(container, ITEM_TAKE)) {
            if (obj.timer != 0) {
                obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_HAD_TIMER)
            } else {
                obj.timer = number_range(100, 200)
            }
        }

        obj_from_char(obj)
        obj_to_obj(obj, container)

        if (IS_SET(container.value[1], CONT_PUT_ON)) {
            act("\$n puts \$p on \$P.", ch, obj, container, TO_ROOM)
            act("You put \$p on \$P.", ch, obj, container, TO_CHAR)
        } else {
            act("\$n puts \$p in \$P.", ch, obj, container, TO_ROOM)
            act("You put \$p in \$P.", ch, obj, container, TO_CHAR)
        }
    } else {
        var pcount = container.contains.size
        /* 'put all container' or 'put all.obj container' */
        for (obj in ch.carrying) {
            if ((arg1.length < 4 || is_name(arg1.substring(4), obj.name))
                    && can_see_obj(ch, obj)
                    && WEIGHT_MULT(obj) == 100
                    && obj.wear_loc.isNone()
                    && obj != container
                    && can_drop_obj(ch, obj)
                    && get_obj_weight(obj) + get_true_weight(container) <= container.value[0] * 10
                    && get_obj_weight(obj) < container.value[3] * 10) {
                if (container.pIndexData.vnum == OBJ_VNUM_PIT && !CAN_WEAR(obj, ITEM_TAKE)) {
                    if (obj.timer != 0) {
                        obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_HAD_TIMER)
                    } else {
                        obj.timer = number_range(100, 200)
                    }
                }

                if (obj.pIndexData.limit != -1) {
                    act("This unworthy container won't hold \$p.", ch, obj, null, TO_CHAR)
                    continue
                }

                if (obj.item_type == ItemType.Potion && IS_SET(container.wear_flags, ITEM_TAKE)) {
                    pcount = 0
                    for (objc in container.contains) {
                        if (objc.item_type == ItemType.Potion) {
                            pcount++
                        }
                    }
                    if (pcount > 15) {
                        act("It's not safe to put more potions into \$p.", ch, container, null, TO_CHAR)
                        continue
                    }
                }

                pcount++
                if (pcount > container.value[0]) {
                    act("It's not safe to put that much item into \$p.", ch, container, null, TO_CHAR)
                    return
                }
                obj_from_char(obj)
                obj_to_obj(obj, container)

                if (IS_SET(container.value[1], CONT_PUT_ON)) {
                    act("\$n puts \$p on \$P.", ch, obj, container, TO_ROOM)
                    act("You put \$p on \$P.", ch, obj, container, TO_CHAR)
                } else {
                    act("\$n puts \$p in \$P.", ch, obj, container, TO_ROOM)
                    act("You put \$p in \$P.", ch, obj, container, TO_CHAR)
                }
            }
        }
    }

}


fun do_drop(ch: CHAR_DATA, argument: String) {
    var rest = argument
    var found: Boolean

    val argb = StringBuilder()
    rest = one_argument(rest, argb)

    if (argb.isEmpty()) {
        send_to_char("Drop what?\n", ch)
        return
    }
    var arg = argb.toString()

    if (is_number(arg)) {
        /* 'drop NNNN coins' */
        var gold = 0
        var silver = 0

        val amount = atoi(arg)
        one_argument(rest, argb)
        arg = argb.toString()
        if (amount <= 0 || !eq(arg, "coins") && !eq(arg, "coin") && !eq(arg, "gold") && !eq(arg, "silver")) {
            send_to_char("Sorry, you can't do that.\n", ch)
            return
        }

        if (eq(arg, "coins") || eq(arg, "coin") || eq(arg, "silver")) {
            if (ch.silver < amount) {
                send_to_char("You don't have that much silver.\n", ch)
                return
            }

            ch.silver -= amount
            silver = amount
        } else {
            if (ch.gold < amount) {
                send_to_char("You don't have that much gold.\n", ch)
                return
            }

            ch.gold -= amount
            gold = amount
        }

        for (o in ch.room.objects) {

            when (o.pIndexData.vnum) {
                OBJ_VNUM_SILVER_ONE -> {
                    silver += 1
                    extract_obj(o)
                }

                OBJ_VNUM_GOLD_ONE -> {
                    gold += 1
                    extract_obj(o)
                }

                OBJ_VNUM_SILVER_SOME -> {
                    silver += o.value[0].toInt()
                    extract_obj(o)
                }

                OBJ_VNUM_GOLD_SOME -> {
                    gold += o.value[1].toInt()
                    extract_obj(o)
                }

                OBJ_VNUM_COINS -> {
                    silver += o.value[0].toInt()
                    gold += o.value[1].toInt()
                    extract_obj(o)
                }
            }
        }

        val obj = create_money(gold, silver)
        obj_to_room(obj, ch.room)
        if (!IS_AFFECTED(ch, AFF_SNEAK)) {
            act("\$n drops some coins.", ch, null, null, TO_ROOM)
        }
        send_to_char("OK.\n", ch)
        if (IS_WATER(ch.room)) {
            extract_obj(obj)
            if (!IS_AFFECTED(ch, AFF_SNEAK)) {
                act("The coins sink down, and disapear in the water.", ch, null, null, TO_ROOM)
            }
            act("The coins sink down, and disapear in the water.", ch, null, null, TO_CHAR)
        }
        return
    }

    if (!eq(arg, "all") && !startsWith("all.", arg)) {
        /* 'drop obj' */
        val obj = get_obj_carry(ch, arg)
        if (obj == null) {
            send_to_char("You do not have that item.\n", ch)
            return
        }

        if (!can_drop_obj(ch, obj)) {
            send_to_char("You can't let go of it.\n", ch)
            return
        }

        obj_from_char(obj)
        obj_to_room(obj, ch.room)
        if (!IS_AFFECTED(ch, AFF_SNEAK)) {
            act("\$n drops \$p.", ch, obj, null, TO_ROOM)
        }
        act("You drop \$p.", ch, obj, null, TO_CHAR)
        if (obj.pIndexData.vnum == OBJ_VNUM_POTION_VIAL && randomPercent() < 40) {
            if (!IS_SET(ch.room.sector_type, SECT_FOREST) &&
                    !IS_SET(ch.room.sector_type, SECT_DESERT) &&
                    !IS_SET(ch.room.sector_type, SECT_AIR) &&
                    !IS_WATER(ch.room)) {
                act("\$p cracks and shaters into tiny pieces.", ch, obj, null, TO_ROOM)
                act("\$p cracks and shaters into tiny pieces.", ch, obj, null, TO_CHAR)
                extract_obj(obj)
                return
            }
        }
        if (IS_SET(obj.progtypes, OPROG_DROP)) {
            obj.pIndexData.oprogs.drop_prog!!(obj, ch)
        }

        if (!may_float(obj) && cant_float(obj) && IS_WATER(ch.room)) {
            if (!IS_AFFECTED(ch, AFF_SNEAK)) {
                act("\$p sinks down the water.", ch, obj, null, TO_ROOM)
            }
            act("\$p sinks down the water.", ch, obj, null, TO_CHAR)
            extract_obj(obj)
        } else if (IS_OBJ_STAT(obj, ITEM_MELT_DROP)) {
            if (!IS_AFFECTED(ch, AFF_SNEAK)) {
                act("\$p dissolves into smoke.", ch, obj, null, TO_ROOM)
            }
            act("\$p dissolves into smoke.", ch, obj, null, TO_CHAR)
            extract_obj(obj)
        }
    } else {
        /* 'drop all' or 'drop all.obj' */
        found = false
        for (o in ch.carrying) {
            if ((arg.length < 4 || is_name(arg.substring(4), o.name))
                    && can_see_obj(ch, o)
                    && o.wear_loc.isNone()
                    && can_drop_obj(ch, o)) {
                found = true
                obj_from_char(o)
                obj_to_room(o, ch.room)
                if (!IS_AFFECTED(ch, AFF_SNEAK)) {
                    act("\$n drops \$p.", ch, o, null, TO_ROOM)
                }
                act("You drop \$p.", ch, o, null, TO_CHAR)
                if (o.pIndexData.vnum == OBJ_VNUM_POTION_VIAL && randomPercent() < 70) {
                    if (!IS_SET(ch.room.sector_type, SECT_FOREST) &&
                            !IS_SET(ch.room.sector_type, SECT_DESERT) &&
                            !IS_SET(ch.room.sector_type, SECT_AIR) &&
                            !IS_WATER(ch.room)) {
                        if (!IS_AFFECTED(ch, AFF_SNEAK)) {
                            act("\$p cracks and shaters into tiny pieces.", ch, o, null, TO_ROOM)
                        }
                        act("\$p cracks and shaters into tiny pieces.", ch, o, null, TO_CHAR)
                        extract_obj(o)
                        continue
                    }
                }

                if (IS_SET(o.progtypes, OPROG_DROP)) {
                    o.pIndexData.oprogs.drop_prog!!(o, ch)
                }

                if (!may_float(o) && cant_float(o) && IS_WATER(ch.room)) {
                    if (!IS_AFFECTED(ch, AFF_SNEAK)) {
                        act("\$p sinks down the water.", ch, o, null, TO_ROOM)
                    }
                    act("\$p sinks down the water.", ch, o, null, TO_CHAR)
                    extract_obj(o)
                } else if (IS_OBJ_STAT(o, ITEM_MELT_DROP)) {
                    if (!IS_AFFECTED(ch, AFF_SNEAK)) {
                        act("\$p dissolves into smoke.", ch, o, null, TO_ROOM)
                    }
                    act("\$p dissolves into smoke.", ch, o, null, TO_CHAR)
                    extract_obj(o)
                }
            }
        }

        if (!found) {
            if (arg.length < 4) {
                act("You are not carrying anything.", ch, null, arg, TO_CHAR)
            } else {
                act("You are not carrying any \$T.", ch, null, arg.substring(4), TO_CHAR)
            }
        }
    }

}


fun do_drag(ch: CHAR_DATA, argument: String) {
    var rest = argument

    val arg1b = StringBuilder()
    val arg2b = StringBuilder()
    rest = one_argument(rest, arg1b)
    one_argument(rest, arg2b)

    /* Get type. */
    if (arg1b.isEmpty() || arg2b.isEmpty()) {
        send_to_char("Drag what to which direction?\n", ch)
        return
    }

    val arg1 = arg1b.toString()
    val arg2 = arg2b.toString()
    if (eq(arg1, "all") || startsWith("all.", arg1)) {
        send_to_char("You can't do that.\n", ch)
        return
    }

    val obj = get_obj_list(ch, arg1, ch.room.objects)
    if (obj == null) {
        act("I see no \$T here.", ch, null, arg1, TO_CHAR)
        return
    }

    if (!CAN_WEAR(obj, ITEM_TAKE)) {
        send_to_char("You can't take that.\n", ch)
        return
    }

    if (obj.pIndexData.limit != -1) {
        if (IS_OBJ_STAT(obj, ITEM_ANTI_EVIL) && ch.isEvil()
                || IS_OBJ_STAT(obj, ITEM_ANTI_GOOD) && ch.isGood()
                || IS_OBJ_STAT(obj, ITEM_ANTI_NEUTRAL) && ch.isNeutralA()) {
            act("You are zapped by \$p and drop it.", ch, obj, null, TO_CHAR)
            act("\$n is zapped by \$p and drops it.", ch, obj, null, TO_ROOM)
            return
        }
    }

    val in_room = obj.in_room
    if (in_room != null) {
        for (gch in in_room.people) {
            if (gch.on == obj) {
                act("\$N appears to be using \$p.", ch, obj, gch, TO_CHAR)
                return
            }
        }
    }

    if (get_carry_weight(ch) + get_obj_weight(obj) > 2 * can_carry_weight(ch)) {
        act("\$d: you can't drag that much weight.", ch, null, obj.name, TO_CHAR)
        return
    }

    if (get_eq_char(ch, Wear.Left) != null || get_eq_char(ch, Wear.Right) != null || get_eq_char(ch, Wear.Both) != null) {
        send_to_char("You need your both hands free.\n", ch)
        return
    }

    val direction = find_exit(ch, arg2)
    if (direction < 0) {
        return
    }

    val pexit = ch.room.exit[direction]
    if (pexit == null || !can_see_room(ch, pexit.to_room)) {
        send_to_char("Alas, you cannot go that way.\n", ch)
        return
    }
    val buf = TextBuffer()
    buf.sprintf("You grab \$p to drag towards %s.", dir_name[direction])
    act(buf.toString(), ch, obj, null, TO_CHAR)
    if (!IS_AFFECTED(ch, AFF_SNEAK)) {
        buf.sprintf("\$n grabs \$p to drag towards %s.", dir_name[direction])
        act(buf.toString(), ch, obj, null, TO_ROOM)
    }

    obj_from_room(obj)
    obj_to_char(obj, ch)

    if (IS_SET(obj.progtypes, OPROG_GET)) {
        obj.pIndexData.oprogs.get_prog!!(obj, ch)
    }

    if (obj.carried_by != ch) {
        return
    }

    val was_in_room = ch.room
    move_char(ch, direction)

    if (was_in_room == ch.room) {
        send_to_char("You cannot drag that way.\n", ch)
    } else {
        if (!IS_AFFECTED(ch, AFF_SNEAK)) {
            act("\$n drops \$p.", ch, obj, null, TO_ROOM)
        }
        act("You drop \$p.", ch, obj, null, TO_CHAR)
        WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
    }

    obj_from_char(obj)
    obj_to_room(obj, ch.room)

}


fun do_give(ch: CHAR_DATA, argument: String) {
    var rest = argument

    val arg1b = StringBuilder()
    val arg2b = StringBuilder()
    rest = one_argument(rest, arg1b)
    rest = one_argument(rest, arg2b)

    if (arg1b.isEmpty() || arg2b.isEmpty()) {
        send_to_char("Give what to whom?\n", ch)
        return
    }
    val arg1 = arg1b.toString()
    var arg2 = arg2b.toString()

    if (is_number(arg1)) {
        /* 'give NNNN coins victim' */
        val amount = atoi(arg1)
        if (amount <= 0 || !eq(arg2, "coins") && !eq(arg2, "coin") && !eq(arg2, "gold") && !eq(arg2, "silver")) {
            send_to_char("Sorry, you can't do that.\n", ch)
            return
        }

        val silver = !eq(arg2, "gold")

        one_argument(rest, arg2b)
        if (arg2b.isEmpty()) {
            send_to_char("Give what to whom?\n", ch)
            return
        }

        arg2 = arg2b.toString()
        val victim = get_char_room(ch, arg2)
        if (victim == null) {
            send_to_char("They aren't here.\n", ch)
            return
        }

        if (!silver && ch.gold < amount || silver && ch.silver < amount) {
            send_to_char("You haven't got that much.\n", ch)
            return
        }

        var weight = if (!silver) amount * 2 / 5 else amount / 10

        if (victim.pcdata != null && get_carry_weight(victim) + weight > can_carry_weight(victim)) {
            act("\$N can't carry that much weight.", ch, null, victim, TO_CHAR)
            return
        }

        if (silver) {
            ch.silver -= amount
            victim.silver += amount
        } else {
            ch.gold -= amount
            victim.gold += amount
        }
        val buf = TextBuffer()
        buf.sprintf("\$n gives you %d %s.", amount, if (silver) "silver" else "gold")
        act(buf.toString(), ch, null, victim, TO_VICT)
        act("\$n gives \$N some coins.", ch, null, victim, TO_NOTVICT)
        buf.sprintf("You give \$N %d %s.", amount, if (silver) "silver" else "gold")
        act(buf.toString(), ch, null, victim, TO_CHAR)
        if (IS_SET(victim.progtypes, MPROG_BRIBE)) {
            victim.pIndexData.mprogs.bribe_prog!!(victim, ch, amount)
        }

        if (victim.pcdata == null && IS_SET(victim.act, ACT_IS_CHANGER)) {

            val change = if (silver) 95 * amount / 100 / 100 else 95 * amount
            weight = if (silver) change * 2 / 5 else change / 10

            weight -= if (!silver) {
                amount * 2 / 5
            } else {
                amount / 10
            }

            if (ch.pcdata != null && get_carry_weight(ch) + weight > can_carry_weight(ch)) {
                act("You can't carry that much weight.", ch, null, null, TO_CHAR)
                return
            }

            if (!silver && change > victim.silver) {
                victim.silver += change
            }

            if (silver && change > victim.gold) {
                victim.gold += change
            }

            if (change < 1 && can_see(victim, ch)) {
                act("\$n tells you 'I'm sorry, you did not give me enough to change.'", victim, null, ch, TO_VICT)
                ch.reply = victim
                buf.sprintf("%d %s %s", amount, if (silver) "silver" else "gold", ch.name)
                do_give(victim, buf.toString())
            } else if (can_see(victim, ch)) {
                buf.sprintf("%d %s %s", change, if (silver) "gold" else "silver", ch.name)
                do_give(victim, buf.toString())
                if (silver) {
                    buf.sprintf("%d silver %s", 95 * amount / 100 - change * 100, ch.name)
                    do_give(victim, buf.toString())
                }
                act("\$n tells you 'Thank you, come again.'", victim, null, ch, TO_VICT)
                ch.reply = victim
            }
        }
        return
    }

    val obj = get_obj_carry(ch, arg1)
    if (obj == null) {
        send_to_char("You do not have that item.\n", ch)
        return
    }

    if (obj.wear_loc.isOn()) {
        send_to_char("You must remove it first.\n", ch)
        return
    }

    val victim = get_char_room(ch, arg2)
    if (victim == null) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim.pcdata == null && victim.pIndexData.pShop != null && !IS_SET(victim.progtypes, MPROG_GIVE)) {
        act("\$N tells you 'Sorry, you'll have to sell that.'", ch, null, victim, TO_CHAR)
        ch.reply = victim
        return
    }

    if (!can_drop_obj(ch, obj)) {
        send_to_char("You can't let go of it.\n", ch)
        return
    }

    if (victim.carry_number + get_obj_number(obj) > can_carry_items(victim)) {
        act("\$N has \$S hands full.", ch, null, victim, TO_CHAR)
        return
    }

    if (get_carry_weight(victim) + get_obj_weight(obj) > can_carry_weight(victim)) {
        act("\$N can't carry that much weight.", ch, null, victim, TO_CHAR)
        return
    }

    if (!can_see_obj(victim, obj)) {
        act("\$N can't see it.", ch, null, victim, TO_CHAR)
        return
    }

    if (obj.pIndexData.limit != -1) {
        if (IS_OBJ_STAT(obj, ITEM_ANTI_EVIL) && victim.isEvil()
                || IS_OBJ_STAT(obj, ITEM_ANTI_GOOD) && victim.isGood()
                || IS_OBJ_STAT(obj, ITEM_ANTI_NEUTRAL) && victim.isNeutralA()) {
            send_to_char("Your victim's alignment doesn't match with the objects align.", ch)
            return
        }
    }

    obj_from_char(obj)
    obj_to_char(obj, victim)
    act("\$n gives \$p to \$N.", ch, obj, victim, TO_NOTVICT)
    act("\$n gives you \$p.", ch, obj, victim, TO_VICT)
    act("You give \$p to \$N.", ch, obj, victim, TO_CHAR)

    if (IS_SET(obj.progtypes, OPROG_GIVE)) {
        obj.pIndexData.oprogs.give_prog!!(obj, ch, victim)
    }

    if (IS_SET(obj.progtypes, OPROG_GET)) {
        obj.pIndexData.oprogs.get_prog!!(obj, victim)
    }

    if (IS_SET(victim.progtypes, MPROG_GIVE)) {
        victim.pIndexData.mprogs.give_prog!!(victim, ch, obj)
    }

}


fun do_bury(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    val argS = arg.toString()

    if (skill_failure_check(ch, Skill.bury, false, 0, "You can't do that.\n")) {
        return
    }

    if (arg.isEmpty()) {
        send_to_char("Bury whose corpse?\n", ch)
        return
    }

    val shovel = get_weapon_char(ch, WeaponType.Mace)
    if (shovel == null || !is_name("shovel", shovel.name)) {
        send_to_char("You don't have shovel do dig!\n", ch)
        return
    }

    val obj = get_obj_list(ch, argS, ch.room.objects)
    if (obj == null) {
        act("I see no \$T here.", ch, null, argS, TO_CHAR)
        return
    }

    if (obj.item_type != ItemType.CorpsePc && obj.item_type != ItemType.CorpseNpc) {
        send_to_char("Why do you want to bury that?\n", ch)
        return
    }

    when (ch.room.sector_type) {
        SECT_CITY, SECT_INSIDE -> {
            send_to_char("The floor is too hard to dig through.\n", ch)
            return
        }
        SECT_WATER_SWIM, SECT_WATER_NO_SWIM -> {
            send_to_char("You cannot bury something here.\n", ch)
            return
        }
        SECT_AIR -> {
            send_to_char("What?  In the air?!\n", ch)
            return
        }
    }

    var move = obj.weight * 5 / ch.curr_stat(PrimeStat.Strength)
    move = URANGE(2, move, 1000)
    if (move > ch.move) {
        send_to_char("You don't have enough energy to bury something of that size.\n", ch)
        return
    }
    ch.move -= move

    act("You solemnly bury \$p...", ch, obj, null, TO_CHAR)
    act("\$n solemnly buries \$p...", ch, obj, null, TO_ROOM)

    obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_BURIED)
    WAIT_STATE(ch, 4 * PULSE_VIOLENCE)

    obj.timer = -1

    val buf = TextBuffer()
    var bufp = obj.short_desc
    while (bufp.isNotEmpty()) {
        bufp = one_argument(bufp, arg)
        if (!(eq(argS, "The") || eq(argS, "undead") || eq(argS, "body") || eq(argS, "corpse") || eq(argS, "of"))) {
            if (buf.isEmpty()) {
                buf.append(arg)
            } else {
                buf.append(" ")
                buf.append(arg)
            }
        }
    }
    arg.setLength(0)
    arg.append(buf.toString())

    val stone = create_object(get_obj_index_nn(OBJ_VNUM_GRAVE_STONE), ch.level)

    buf.sprintf(stone.description, arg)
    stone.description = buf.toString()

    buf.sprintf(stone.short_desc, arg)
    stone.short_desc = buf.toString()

    obj_to_room(stone, ch.room)

    /*
         * a little trick here... :)
         * although grave stone is not a container....
         * protects corpse from area affect attacks.
         * but what about earthquake
         */
    obj_from_room(obj)
    obj_to_obj(obj, stone)

}

fun do_dig(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    val argS = arg.toString()

    if (arg.isEmpty()) {
        send_to_char("Dig which grave?\n", ch)
        return
    }

    val shovel = get_weapon_char(ch, WeaponType.Mace)
    if (shovel == null || !is_name("shovel", shovel.name)) {
        send_to_char("You don't have shovel do dig!\n", ch)
        return
    }

    val obj = get_obj_list(ch, argS, ch.room.objects)
    if (obj == null) {
        act("I see no \$T here.", ch, null, argS, TO_CHAR)
        return
    }

    if (obj.pIndexData.vnum != OBJ_VNUM_GRAVE_STONE) {
        send_to_char("I don't think that it is a grave.\n", ch)
        return
    }

    var move = obj.weight * 5 / ch.curr_stat(PrimeStat.Strength)
    move = URANGE(2, move, 1000)
    if (move > ch.move) {
        send_to_char("You don't have enough energy to dig something of that size.\n", ch)
        return
    }
    ch.move -= move

    act("You start digging \$p...", ch, obj, null, TO_CHAR)
    act("\$n starts diggin \$p...", ch, obj, null, TO_ROOM)

    WAIT_STATE(ch, 4 * PULSE_VIOLENCE)

    val corpse = obj.contains.firstOrNull()
    if (corpse == null) {
        act("Digging reveals nothing.\n", ch, null, null, TO_ALL)
        return
    }

    corpse.extra_flags = REMOVE_BIT(corpse.extra_flags, ITEM_BURIED)
    obj_from_obj(corpse)
    obj_to_room(corpse, ch.room)
    extract_obj(obj)
    corpse.timer = number_range(25, 40)
    act("Digging reveals \$p.\n", ch, corpse, null, TO_ALL)

}

/* for poisoning weapons and food/drink */

fun do_envenom(ch: CHAR_DATA, argument: String) {
    /* find out what */
    if (argument.isEmpty()) {
        send_to_char("Envenom what item?\n", ch)
        return
    }

    val obj = get_obj_list(ch, argument, ch.carrying)

    if (obj == null) {
        send_to_char("You don't have that item.\n", ch)
        return
    }

    val skill = get_skill(ch, Skill.envenom)
    if (skill < 1) {
        send_to_char("Are you crazy? You'd poison yourself!\n", ch)
        return
    }

    if (obj.item_type == ItemType.Food || obj.item_type == ItemType.DrinkContainer) {
        if (IS_OBJ_STAT(obj, ITEM_BLESS) || IS_OBJ_STAT(obj, ITEM_BURN_PROOF)) {
            act("You fail to poison \$p.", ch, obj, null, TO_CHAR)
            return
        }

        if (randomPercent() < skill)
        /* success! */ {
            act("\$n treats \$p with deadly poison.", ch, obj, null, TO_ROOM)
            act("You treat \$p with deadly poison.", ch, obj, null, TO_CHAR)
            if (obj.value[3] == 0L) {
                obj.value[3] = 1
                check_improve(ch, Skill.envenom, true, 4)
            }
            WAIT_STATE(ch, Skill.envenom.beats)
            return
        }

        act("You fail to poison \$p.", ch, obj, null, TO_CHAR)
        if (obj.value[3] == 0L) {
            check_improve(ch, Skill.envenom, false, 4)
        }
        WAIT_STATE(ch, Skill.envenom.beats)
        return
    }

    if (obj.item_type == ItemType.Weapon) {
        if (IS_WEAPON_STAT(obj, WEAPON_FLAMING)
                || IS_WEAPON_STAT(obj, WEAPON_FROST)
                || IS_WEAPON_STAT(obj, WEAPON_VAMPIRIC)
                || IS_WEAPON_STAT(obj, WEAPON_SHARP)
                || IS_WEAPON_STAT(obj, WEAPON_VORPAL)
                || IS_WEAPON_STAT(obj, WEAPON_SHOCKING)
                || IS_WEAPON_STAT(obj, WEAPON_HOLY)
                || IS_OBJ_STAT(obj, ITEM_BLESS) || IS_OBJ_STAT(obj, ITEM_BURN_PROOF)) {
            act("You can't seem to envenom \$p.", ch, obj, null, TO_CHAR)
            return
        }

        if (obj.value[3] < 0 || AttackType.of(obj.value[3]).damage == DT.Bash) {
            send_to_char("You can only envenom edged weapons.\n", ch)
            return
        }

        if (IS_WEAPON_STAT(obj, WEAPON_POISON)) {
            act("\$p is already envenomed.", ch, obj, null, TO_CHAR)
            return
        }

        val percent = randomPercent()
        if (percent < skill) {
            val af = Affect()
            af.where = TO_WEAPON
            af.type = Skill.poison
            af.level = ch.level * percent / 100
            af.duration = ch.level * percent / 100
            af.bitvector = WEAPON_POISON
            affect_to_obj(obj, af)

            if (!IS_AFFECTED(ch, AFF_SNEAK)) {
                act("\$n coats \$p with deadly venom.", ch, obj, null, TO_ROOM)
            }
            act("You coat \$p with venom.", ch, obj, null, TO_CHAR)
            check_improve(ch, Skill.envenom, true, 3)
            WAIT_STATE(ch, Skill.envenom.beats)
            return
        } else {
            act("You fail to envenom \$p.", ch, obj, null, TO_CHAR)
            check_improve(ch, Skill.envenom, false, 3)
            WAIT_STATE(ch, Skill.envenom.beats)
            return
        }
    }

    act("You can't poison \$p.", ch, obj, null, TO_CHAR)
}

fun do_fill(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Fill what?\n", ch)
        return
    }

    val obj = get_obj_carry(ch, arg.toString())
    if (obj == null) {
        send_to_char("You do not have that item.\n", ch)
        return
    }

    val fountain: Obj? = ch.room.objects.firstOrNull { it.item_type == ItemType.Fountain }
    if (fountain == null) {
        send_to_char("There is no fountain here!\n", ch)
        return
    }

    if (obj.item_type != ItemType.DrinkContainer) {
        send_to_char("You can't fill that.\n", ch)
        return
    }

    if (obj.value[1] != 0L && obj.value[2] != fountain.value[2]) {
        send_to_char("There is already another liquid in it.\n", ch)
        return
    }

    if (obj.value[1] >= obj.value[0]) {
        send_to_char("Your container is full.\n", ch)
        return
    }
    val buf = TextBuffer()
    buf.sprintf("You fill \$p with %s from \$P.", Liquid.of(fountain.value[2]).liq_name)
    act(buf.toString(), ch, obj, fountain, TO_CHAR)
    buf.sprintf("\$n fills \$p with %s from \$P.", Liquid.of(fountain.value[2]).liq_name)
    act(buf.toString(), ch, obj, fountain, TO_ROOM)
    obj.value[2] = fountain.value[2]
    obj.value[1] = obj.value[0]

}

fun do_pour(ch: CHAR_DATA, argument: String) {
    var rest = argument
    val arg = StringBuilder()
    rest = one_argument(rest, arg)

    if (arg.isEmpty() || rest.isEmpty()) {
        send_to_char("Pour what into what?\n", ch)
        return
    }

    val out = get_obj_carry(ch, arg.toString())
    if (out == null) {
        send_to_char("You don't have that item.\n", ch)
        return
    }

    if (out.item_type != ItemType.DrinkContainer) {
        send_to_char("That's not a drink container.\n", ch)
        return
    }

    if (eq(rest, "out")) {
        if (out.value[1] == 0L) {
            send_to_char("It's already empty.\n", ch)
            return
        }

        out.value[1] = 0
        out.value[3] = 0
        val buf = TextBuffer()
        val liquid = Liquid.of(out.value[2])
        if (!IS_WATER(ch.room)) {
            buf.sprintf("You invert \$p, spilling %s all over the ground.", liquid.liq_name)
            act(buf.toString(), ch, out, null, TO_CHAR)

            buf.sprintf("\$n inverts \$p, spilling %s all over the ground.", liquid.liq_name)
            act(buf.toString(), ch, out, null, TO_ROOM)
        } else {
            buf.sprintf("You invert \$p, spilling %s in to the water.", liquid.liq_name)
            act(buf.toString(), ch, out, null, TO_CHAR)

            buf.sprintf("\$n inverts \$p, spilling %s in to the water.", liquid.liq_name)
            act(buf.toString(), ch, out, null, TO_ROOM)
        }
        return
    }

    var vch: CHAR_DATA? = null
    var obj_in = get_obj_here(ch, rest)
    if (obj_in == null) {
        vch = get_char_room(ch, rest)
        if (vch == null) {
            send_to_char("Pour into what?\n", ch)
            return
        }

        obj_in = get_hold_char(vch)

        if (obj_in == null) {
            send_to_char("They aren't holding anything.", ch)
            return
        }
    }

    if (obj_in.item_type != ItemType.DrinkContainer) {
        send_to_char("You can only pour into other drink containers.\n", ch)
        return
    }

    if (obj_in == out) {
        send_to_char("You cannot change the laws of physics!\n", ch)
        return
    }

    if (obj_in.value[1] != 0L && obj_in.value[2] != out.value[2]) {
        send_to_char("They don't hold the same liquid.\n", ch)
        return
    }

    if (out.value[1] == 0L) {
        act("There's nothing in \$p to pour.", ch, out, null, TO_CHAR)
        return
    }

    if (obj_in.value[1] >= obj_in.value[0]) {
        act("\$p is already filled to the top.", ch, obj_in, null, TO_CHAR)
        return
    }

    val amount = Math.min(out.value[1], obj_in.value[0] - obj_in.value[1])

    obj_in.value[1] += amount
    out.value[1] -= amount
    obj_in.value[2] = out.value[2]

    val liq_name = Liquid.of(out.value[2]).liq_name
    if (vch == null) {
        act("You pour $liq_name from \$p into \$P.", ch, out, obj_in, TO_CHAR)
        act("\$n pours $liq_name from \$p into \$P.", ch, out, obj_in, TO_ROOM)
    } else {
        act("You pour some $liq_name for \$N.", ch, null, vch, TO_CHAR)
        act("\$n pours you some $liq_name.", ch, null, vch, TO_VICT)
        act("\$n pours some $liq_name for \$N.", ch, null, vch, TO_NOTVICT)
    }

}

fun do_drink(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    var obj: Obj? = null
    if (arg.isEmpty()) {
        for (o in ch.room.objects) {
            if (o.item_type == ItemType.Fountain) {
                obj = o
                break
            }
        }

        if (obj == null) {
            send_to_char("Drink what?\n", ch)
            return
        }
    } else {
        obj = get_obj_here(ch, arg.toString())
        if (obj == null) {
            send_to_char("You can't find it.\n", ch)
            return
        }
    }

    val pcdata = ch.pcdata
    if (pcdata != null && pcdata.condition[COND_DRUNK] > 10) {
        send_to_char("You fail to reach your mouth.  *Hic*\n", ch)
        return
    }

    var amount: Int
    val liquid = Liquid.of(obj.value[2])
    when (obj.item_type) {
        ItemType.Fountain -> amount = liquid.size * 3

        ItemType.DrinkContainer -> {
            if (obj.value[1] <= 0) {
                send_to_char("It is already empty.\n", ch)
                return
            }
            amount = liquid.size
            amount = Math.min(amount, obj.value[1].toInt())
        }
        else -> {
            send_to_char("You can't drink from that.\n", ch)
            return
        }
    }
    if (pcdata != null && !IS_IMMORTAL(ch) && pcdata.condition[COND_FULL] > 80) {
        send_to_char("You're too full to drink more.\n", ch)
        return
    }

    act("\$n drinks \$T from \$p.", ch, obj, liquid.liq_name, TO_ROOM)
    act("You drink \$T from \$p.", ch, obj, liquid.liq_name, TO_CHAR)

    if (ch.fighting != null) {
        WAIT_STATE(ch, 3 * PULSE_VIOLENCE)
    }

    gain_condition(ch, COND_DRUNK, amount * liquid.proof / 36)
    gain_condition(ch, COND_FULL, amount * liquid.full / 2)
    gain_condition(ch, COND_THIRST, amount * liquid.thirst)
    gain_condition(ch, COND_HUNGER, amount * liquid.food)

    if (pcdata != null && pcdata.condition[COND_DRUNK] > 10) {
        send_to_char("You feel drunk.\n", ch)
    }
    if (pcdata != null && pcdata.condition[COND_FULL] > 60) {
        send_to_char("You are full.\n", ch)
    }
    if (pcdata != null && pcdata.condition[COND_THIRST] > 60) {
        send_to_char("Your thirst is quenched.\n", ch)
    }

    if (obj.value[3] != 0L) {
        /* The drink was poisoned ! */
        act("\$n chokes and gags.", ch, null, null, TO_ROOM)
        send_to_char("You choke and gag.\n", ch)
        val af = Affect()
        af.type = Skill.poison
        af.level = number_fuzzy(amount)
        af.duration = 3 * amount
        af.bitvector = AFF_POISON
        affect_join(ch, af)
    }

    if (obj.value[0] > 0) {
        obj.value[1] -= amount.toLong()
    }
}


fun do_eat(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty()) {
        send_to_char("Eat what?\n", ch)
        return
    }
    val obj = get_obj_carry(ch, arg.toString())
    if (obj == null) {
        send_to_char("You do not have that item.\n", ch)
        return
    }

    val pcdata = ch.pcdata
    if (!IS_IMMORTAL(ch)) {
        if (obj.item_type != ItemType.Food && obj.item_type != ItemType.Pill) {
            send_to_char("That's not edible.\n", ch)
            return
        }

        if (pcdata != null && pcdata.condition[COND_FULL] > 80) {
            send_to_char("You are too full to eat more.\n", ch)
            return
        }
    }

    act("\$n eats \$p.", ch, obj, null, TO_ROOM)
    act("You eat \$p.", ch, obj, null, TO_CHAR)
    if (ch.fighting != null) {
        WAIT_STATE(ch, 3 * PULSE_VIOLENCE)
    }

    when (obj.item_type) {
        ItemType.Food -> {
            if (pcdata != null) {
                val condition = pcdata.condition[COND_HUNGER]
                gain_condition(ch, COND_FULL, obj.value[0].toInt() * 2)
                gain_condition(ch, COND_HUNGER, obj.value[1].toInt() * 2)
                if (condition == 0 && pcdata.condition[COND_HUNGER] > 0) {
                    send_to_char("You are no longer hungry.\n", ch)
                } else if (pcdata.condition[COND_FULL] > 60) {
                    send_to_char("You are full.\n", ch)
                }
            }

            if (obj.value[3] != 0L) {
                /* The food was poisoned! */
                act("\$n chokes and gags.", ch, null, null, TO_ROOM)
                send_to_char("You choke and gag.\n", ch)
                val af = Affect()
                af.type = Skill.poison
                af.level = number_fuzzy(obj.value[0].toInt())
                af.duration = 2 * obj.value[0].toInt()
                af.bitvector = AFF_POISON
                affect_join(ch, af)
            }
        }

        ItemType.Pill -> {
            val level = obj.value[0].toInt()
            obj_cast_spell(Skill.skills[obj.value[1].toInt()], level, ch, ch, null)
            obj_cast_spell(Skill.skills[obj.value[2].toInt()], level, ch, ch, null)
            obj_cast_spell(Skill.skills[obj.value[3].toInt()], level, ch, ch, null)
        }
        else -> {
        }
    }

    extract_obj(obj)
}

/** Remove an object. Only for non-multi-wear locations */
fun remove_obj_loc(ch: CHAR_DATA, iWear: Wear, fReplace: Boolean): Boolean {
    val obj = get_eq_char(ch, iWear) ?: return true

    if (!fReplace) {
        return false
    }

    if (IS_SET(obj.extra_flags, ITEM_NOREMOVE)) {
        act("You can't remove \$p.", ch, obj, null, TO_CHAR)
        return false
    }

    if (obj.item_type == ItemType.Tattoo && !IS_IMMORTAL(ch)) {
        act("You must scratch it to remove \$p.", ch, obj, null, TO_CHAR)
        return false
    }

    if (iWear == Wear.StuckIn) {
        unequip_char(ch, obj)

        if (get_eq_char(ch, Wear.StuckIn) == null) {
            if (is_affected(ch, Skill.arrow)) {
                affect_strip(ch, Skill.arrow)
            }
            if (is_affected(ch, Skill.spear)) {
                affect_strip(ch, Skill.spear)
            }
        }
        act("You remove \$p, in pain.", ch, obj, null, TO_CHAR)
        act("\$n remove \$p, in pain.", ch, obj, null, TO_ROOM)
        WAIT_STATE(ch, 4)
        return true
    }

    unequip_char(ch, obj)
    act("\$n stops using \$p.", ch, obj, null, TO_ROOM)
    act("You stop using \$p.", ch, obj, null, TO_CHAR)

    return true
}

/* Remove an object. */

fun remove_obj(ch: CHAR_DATA, obj: Obj?, fReplace: Boolean) = when {
    obj == null -> true
    !fReplace -> false
    IS_SET(obj.extra_flags, ITEM_NOREMOVE) -> {
        act("You can't remove \$p.", ch, obj, null, TO_CHAR)
        false
    }
    obj.item_type == ItemType.Tattoo && !IS_IMMORTAL(ch) -> {
        act("You must scratch it to remove \$p.", ch, obj, null, TO_CHAR)
        false
    }
    obj.wear_loc == Wear.StuckIn -> {
        unequip_char(ch, obj)

        if (get_eq_char(ch, Wear.StuckIn) == null) {
            if (is_affected(ch, Skill.arrow)) {
                affect_strip(ch, Skill.arrow)
            }
            if (is_affected(ch, Skill.spear)) {
                affect_strip(ch, Skill.spear)
            }
        }
        act("You remove \$p, in pain.", ch, obj, null, TO_CHAR)
        act("\$n remove \$p, in pain.", ch, obj, null, TO_ROOM)
        WAIT_STATE(ch, 4)
        true
    }
    else -> {
        unequip_char(ch, obj)
        act("\$n stops using \$p.", ch, obj, null, TO_ROOM)
        act("You stop using \$p.", ch, obj, null, TO_CHAR)
        true
    }
}

/**
 * Wear one object.
 * Optional replacement of existing objects.
 * Big repetitive code, ick.
 */
fun wear_obj(ch: CHAR_DATA, obj: Obj, fReplace: Boolean) {
    var wear_level = ch.level

    if (ch.clazz.fMana && obj.item_type == ItemType.Armor || !ch.clazz.fMana && obj.item_type == ItemType.Weapon) {
        wear_level += 3
    }

    when {
        wear_level < obj.level -> {
            val buf = TextBuffer()
            buf.sprintf("You must be level %d to use this object.\n", obj.level)
            send_to_char(buf, ch)
            act("\$n tries to use \$p, but is too inexperienced.", ch, obj, null, TO_ROOM)
        }
        obj.item_type == ItemType.Light -> {
            when {
                get_eq_char(ch, Wear.Both) != null -> {
                    if (!remove_obj_loc(ch, Wear.Both, fReplace)) {
                        return
                    }
                    hold_a_light(ch, obj, Wear.Left)
                }
                get_eq_char(ch, Wear.Left) == null -> hold_a_light(ch, obj, Wear.Left)
                get_eq_char(ch, Wear.Right) == null -> hold_a_light(ch, obj, Wear.Right)
                remove_obj_loc(ch, Wear.Left, fReplace) -> hold_a_light(ch, obj, Wear.Left)
                remove_obj_loc(ch, Wear.Right, fReplace) -> hold_a_light(ch, obj, Wear.Right)
                else -> send_to_char("You can't hold a light right now.\n", ch)
            }
        }
        CAN_WEAR(obj, ITEM_WEAR_FINGER) -> wear_multi(ch, obj, Wear.Finger, fReplace)
        CAN_WEAR(obj, ITEM_WEAR_NECK) -> wear_multi(ch, obj, Wear.Neck, fReplace)
        CAN_WEAR(obj, ITEM_WEAR_BODY) -> {
            if (!remove_obj_loc(ch, Wear.Body, fReplace)) {
                return
            }
            act("\$n wears \$p on \$s torso.", ch, obj, null, TO_ROOM)
            act("You wear \$p on your torso.", ch, obj, null, TO_CHAR)
            equip_char(ch, obj, Wear.Body)
            return
        }
        CAN_WEAR(obj, ITEM_WEAR_HEAD) -> {
            if (!remove_obj_loc(ch, Wear.Head, fReplace)) {
                return
            }
            act("\$n wears \$p on \$s head.", ch, obj, null, TO_ROOM)
            act("You wear \$p on your head.", ch, obj, null, TO_CHAR)
            equip_char(ch, obj, Wear.Head)
            return
        }
        CAN_WEAR(obj, ITEM_WEAR_LEGS) -> {
            if (!remove_obj_loc(ch, Wear.Legs, fReplace)) {
                return
            }
            act("\$n wears \$p on \$s legs.", ch, obj, null, TO_ROOM)
            act("You wear \$p on your legs.", ch, obj, null, TO_CHAR)
            equip_char(ch, obj, Wear.Legs)
            return
        }
        CAN_WEAR(obj, ITEM_WEAR_FEET) -> {
            if (!remove_obj_loc(ch, Wear.Feet, fReplace)) {
                return
            }
            act("\$n wears \$p on \$s feet.", ch, obj, null, TO_ROOM)
            act("You wear \$p on your feet.", ch, obj, null, TO_CHAR)
            equip_char(ch, obj, Wear.Feet)
        }
        CAN_WEAR(obj, ITEM_WEAR_HANDS) -> {
            if (!remove_obj_loc(ch, Wear.Hands, fReplace)) {
                return
            }
            act("\$n wears \$p on \$s hands.", ch, obj, null, TO_ROOM)
            act("You wear \$p on your hands.", ch, obj, null, TO_CHAR)
            equip_char(ch, obj, Wear.Hands)
        }
        CAN_WEAR(obj, ITEM_WEAR_ARMS) -> {
            if (!remove_obj_loc(ch, Wear.Arms, fReplace)) {
                return
            }
            act("\$n wears \$p on \$s arms.", ch, obj, null, TO_ROOM)
            act("You wear \$p on your arms.", ch, obj, null, TO_CHAR)
            equip_char(ch, obj, Wear.Arms)
        }
        CAN_WEAR(obj, ITEM_WEAR_ABOUT) -> {
            if (!remove_obj_loc(ch, Wear.About, fReplace)) {
                return
            }
            act("\$n wears \$p about \$s torso.", ch, obj, null, TO_ROOM)
            act("You wear \$p about your torso.", ch, obj, null, TO_CHAR)
            equip_char(ch, obj, Wear.About)
        }
        CAN_WEAR(obj, ITEM_WEAR_WAIST) -> {
            if (!remove_obj_loc(ch, Wear.Waist, fReplace)) {
                return
            }
            act("\$n wears \$p about \$s waist.", ch, obj, null, TO_ROOM)
            act("You wear \$p about your waist.", ch, obj, null, TO_CHAR)
            equip_char(ch, obj, Wear.Waist)
        }
        CAN_WEAR(obj, ITEM_WEAR_WRIST) -> wear_multi(ch, obj, Wear.Wrist, fReplace)
        CAN_WEAR(obj, ITEM_WEAR_SHIELD) -> {
            when {
                get_eq_char(ch, Wear.Both) != null -> {
                    if (!remove_obj_loc(ch, Wear.Both, fReplace)) {
                        return
                    }
                    hold_a_shield(ch, obj, Wear.Left)
                }
                get_eq_char(ch, Wear.Left) == null -> hold_a_shield(ch, obj, Wear.Left)
                get_eq_char(ch, Wear.Right) == null -> hold_a_shield(ch, obj, Wear.Right)
                remove_obj_loc(ch, Wear.Left, fReplace) -> hold_a_shield(ch, obj, Wear.Left)
                remove_obj_loc(ch, Wear.Right, fReplace) -> hold_a_shield(ch, obj, Wear.Right)
                else -> send_to_char("You can't hold a shield right now.\n", ch)
            }
        }
        CAN_WEAR(obj, ITEM_WIELD) -> wear_a_wield(ch, obj, fReplace)
        CAN_WEAR(obj, ITEM_HOLD) -> {
            when {
                get_eq_char(ch, Wear.Both) != null -> {
                    if (!remove_obj_loc(ch, Wear.Both, fReplace)) {
                        return
                    }
                    hold_a_thing(ch, obj, Wear.Left)
                }
                get_eq_char(ch, Wear.Left) == null -> hold_a_thing(ch, obj, Wear.Left)
                get_eq_char(ch, Wear.Right) == null -> hold_a_thing(ch, obj, Wear.Right)
                remove_obj_loc(ch, Wear.Left, fReplace) -> hold_a_thing(ch, obj, Wear.Left)
                remove_obj_loc(ch, Wear.Right, fReplace) -> hold_a_thing(ch, obj, Wear.Right)
                else -> send_to_char("You can't hold a thing right now.\n", ch)
            }
        }
        CAN_WEAR(obj, ITEM_WEAR_FLOAT) -> {
            if (!remove_obj_loc(ch, Wear.Float, fReplace)) {
                return
            }
            act("\$n releases \$p to float next to \$m.", ch, obj, null, TO_ROOM)
            act("You release \$p and it floats next to you.", ch, obj, null, TO_CHAR)
            equip_char(ch, obj, Wear.Float)
        }
        CAN_WEAR(obj, ITEM_WEAR_TATTOO) && IS_IMMORTAL(ch) -> wear_multi(ch, obj, Wear.Tattoo, fReplace)
        fReplace -> send_to_char("You can't wear, wield, or hold that.\n", ch)
    }

}


fun do_wear(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()

    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Wear, wield, or hold what?\n", ch)
        return
    }

    if (eq(arg.toString(), "all")) {
        ch.carrying
                .filter { it.wear_loc.isNone() && can_see_obj(ch, it) }
                .forEach { wear_obj(ch, it, false) }
    } else {
        val obj = get_obj_carry(ch, arg.toString())
        if (obj == null) {
            send_to_char("You do not have that item.\n", ch)
            return
        }
        wear_obj(ch, obj, true)
    }
}


fun do_remove(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Remove what?\n", ch)
        return
    }


    if (eq(arg.toString(), "all")) {
        ch.carrying
                .filter { it.wear_loc.isOn() && can_see_obj(ch, it) }
                .forEach { remove_obj(ch, it, true) }
        return
    }
    val obj = get_obj_wear(ch, arg.toString())
    if (obj == null) {
        send_to_char("You do not have that item.\n", ch)
        return
    }

    remove_obj(ch, obj, true)
}


fun do_sacrifice(ch: CHAR_DATA, argument: String) {
    val argb = StringBuilder()
    one_argument(argument, argb)
    val arg = argb.toString()

    if (arg.isEmpty() || eq(arg, ch.name)) {
        act("\$n offers \$mself to gods, who graciously declines.",
                ch, null, null, TO_ROOM)
        send_to_char(
                "Gods appreciates your offer and may accept it later.\n", ch)
        return
    }

    val obj = get_obj_list(ch, arg, ch.room.objects)
    if (obj == null) {
        send_to_char("You can't find it.\n", ch)
        return
    }

    if (obj.item_type == ItemType.CorpsePc && ch.level < MAX_LEVEL) {
        send_to_char("Gods wouldn't like that.\n", ch)
        return
    }


    if (!CAN_WEAR(obj, ITEM_TAKE) || CAN_WEAR(obj, ITEM_NO_SAC)) {
        act("\$p is not an acceptable sacrifice.", ch, obj, null, TO_CHAR)
        return
    }

    var silver = Math.max(1, number_fuzzy(obj.level))

    if (obj.item_type != ItemType.CorpseNpc && obj.item_type != ItemType.CorpsePc) {
        silver = Math.min(silver, obj.cost)
    }

    val buf = TextBuffer()
    if (silver == 1) {
        send_to_char("Gods give you one silver coin for your sacrifice.\n", ch)
    } else {

        buf.sprintf("Gods give you %d silver coins for your sacrifice.\n", silver)
        send_to_char(buf, ch)
    }

    ch.silver += silver

    if (IS_SET(ch.act, PLR_AUTO_SPLIT)) { /* AUTOSPLIT code */
        val members = ch.room.people.count { is_same_group(it, ch) }
        if (members > 1 && silver > 1) {
            do_split(ch, "" + silver)
        }
    }

    act("\$n sacrifices \$p to gods.", ch, obj, null, TO_ROOM)

    if (IS_SET(obj.progtypes, OPROG_SAC)) {
        if (obj.pIndexData.oprogs.sac_prog!!(obj, ch)) {
            return
        }
    }

    wiznet("\$N sends up \$p as a burnt offering.", ch, obj, Wiznet.Saccing, null, 0)
    var fScatter = true
    var iScatter = 0
    if (obj.item_type == ItemType.CorpseNpc || obj.item_type == ItemType.CorpsePc) {
        val two_objs = arrayOfNulls<Obj>(2)
        for (obj_content in obj.contains) {
            two_objs[if (iScatter < 1) 0 else 1] = obj_content
            obj_from_obj(obj_content)
            obj_to_room(obj_content, ch.room)
            iScatter++
        }
        val to0 = two_objs[0]
        val to1 = two_objs[1]
        if (iScatter == 1) {
            act("Your sacrifice reveals \$p.", ch, to0!!, null, TO_CHAR)
            act("\$p is revealed by \$n's sacrifice.", ch, to0, null, TO_ROOM)
        }
        if (iScatter == 2) {
            act("Your sacrifice reveals \$p and \$P.", ch, to0!!, to1!!, TO_CHAR)
            act("\$p and \$P are revealed by \$n's sacrifice.", ch, to0, to1, TO_ROOM)
        }
        buf.sprintf("As you sacrifice the corpse, ")
        val buf2 = StringBuilder("As \$n sacrifices the corpse, ")
        when {
            iScatter < 3 -> fScatter = false
            iScatter < 5 -> {
                buf.append("few things ")
                buf2.append("few things ")
            }
            iScatter < 9 -> {
                buf.append("a bunch of objects ")
                buf2.append("a bunch of objects ")
            }
            iScatter < 15 -> {
                buf.append("many things ")
                buf2.append("many things ")
            }
            else -> {
                buf.append("a lot of objects ")
                buf2.append("a lot of objects ")
            }
        }
        buf.append("on it, ")
        buf2.append("on it, ")

        when (ch.room.sector_type) {
            SECT_FIELD -> {
                buf.append("scatter on the dirt.")
                buf2.append("scatter on the dirt.")
            }
            SECT_FOREST -> {
                buf.append("scatter on the dirt.")
                buf2.append("scatter on the dirt.")
            }
            SECT_WATER_SWIM -> {
                buf.append("scatter over the water.")
                buf2.append("scatter over the water.")
            }
            SECT_WATER_NO_SWIM -> {
                buf.append("scatter over the water.")
                buf2.append("scatter over the water.")
            }
            else -> {
                buf.append("scatter around.")
                buf2.append("scatter around.")
            }
        }
        if (fScatter) {
            act(buf.toString(), ch, null, null, TO_CHAR)
            act(buf2.toString(), ch, null, null, TO_ROOM)
        }
    }

    extract_obj(obj)
}


fun do_quaff(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (ch.cabal == Clan.BattleRager && !IS_IMMORTAL(ch)) {
        send_to_char("You are a BattleRager, not a filthy magician!\n", ch)
        return
    }

    if (arg.isEmpty()) {
        send_to_char("Quaff what?\n", ch)
        return
    }

    val obj = get_obj_carry(ch, arg.toString())
    if (obj == null) {
        send_to_char("You do not have that potion.\n", ch)
        return
    }

    if (obj.item_type != ItemType.Potion) {
        send_to_char("You can quaff only potions.\n", ch)
        return
    }

    if (ch.level < obj.level) {
        send_to_char("This liquid is too powerful for you to drink.\n", ch)
        return
    }


    act("\$n quaffs \$p.", ch, obj, null, TO_ROOM)
    act("You quaff \$p.", ch, obj, null, TO_CHAR)

    obj_cast_spell(Skill.skills[obj.value[1].toInt()], obj.value[0].toInt(), ch, ch, null)
    obj_cast_spell(Skill.skills[obj.value[2].toInt()], obj.value[0].toInt(), ch, ch, null)
    obj_cast_spell(Skill.skills[obj.value[3].toInt()], obj.value[0].toInt(), ch, ch, null)

    if (ch.last_fight_time != -1L && current_time - ch.last_fight_time < FIGHT_DELAY_TIME || ch.fighting != null) {
        WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
    }

    extract_obj(obj)
    obj_to_char(create_object(get_obj_index_nn(OBJ_VNUM_POTION_VIAL), 0), ch)

    if (ch.pcdata == null) {
        do_drop(ch, "vial")
    }

}


fun do_recite(ch: CHAR_DATA, argument: String) {
    var rest = argument
    if (ch.cabal == Clan.BattleRager) {
        send_to_char("RECITE?!  You are a battle rager, not a filthy magician!\n", ch)
        return
    }

    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg1)
    one_argument(rest, arg2)

    val scroll = get_obj_carry(ch, arg1.toString())
    if (scroll == null) {
        send_to_char("You do not have that scroll.\n", ch)
        return
    }

    if (scroll.item_type != ItemType.Scroll) {
        send_to_char("You can recite only scrolls.\n", ch)
        return
    }


    if (ch.level < scroll.level) {
        send_to_char("This scroll is too complex for you to comprehend.\n", ch)
        return
    }

    var obj: Obj? = null
    val victim: CHAR_DATA?
    if (arg2.isEmpty()) {
        victim = ch
    } else {
        victim = get_char_room(ch, arg2.toString())
        obj = get_obj_here(ch, arg2.toString())
        if (victim == null && obj == null) {
            send_to_char("You can't find it.\n", ch)
            return
        }
    }

    act("\$n recites \$p.", ch, scroll, null, TO_ROOM)
    act("You recite \$p.", ch, scroll, null, TO_CHAR)

    if (randomPercent() >= get_skill(ch, Skill.scrolls) * 4 / 5) {
        send_to_char("You mispronounce a syllable.\n", ch)
        check_improve(ch, Skill.scrolls, false, 2)
    } else {
        obj_cast_spell(Skill.skills[scroll.value[1].toInt()], scroll.value[0].toInt(), ch, victim, obj)
        obj_cast_spell(Skill.skills[scroll.value[2].toInt()], scroll.value[0].toInt(), ch, victim, obj)
        obj_cast_spell(Skill.skills[scroll.value[3].toInt()], scroll.value[0].toInt(), ch, victim, obj)
        check_improve(ch, Skill.scrolls, true, 2)

        if (ch.last_fight_time != -1L && current_time - ch.last_fight_time < FIGHT_DELAY_TIME || ch.fighting != null) {
            WAIT_STATE(ch, 2 * PULSE_VIOLENCE)
        }
    }

    extract_obj(scroll)
}


fun do_brandish(ch: CHAR_DATA) {
    if (ch.cabal == Clan.BattleRager) {
        send_to_char("You are not a filthy magician!\n", ch)
        return
    }

    val staff = get_hold_char(ch)
    if (staff == null) {
        send_to_char("You hold nothing in your hand.\n", ch)
        return
    }

    if (staff.item_type != ItemType.Staff) {
        send_to_char("You can brandish only with a staff.\n", ch)
        return
    }

    val sn = staff.value[3].toInt()
    if (sn < 0 || sn >= MAX_SKILL || !Skill.skills[sn].isSpell) {
        bug("Do_brandish: bad sn %s.", sn)
        return
    }
    val skill = Skill.skills[sn]

    WAIT_STATE(ch, 2 * PULSE_VIOLENCE)

    if (staff.value[2] > 0) {
        act("\$n brandishes \$p.", ch, staff, null, TO_ROOM)
        act("You brandish \$p.", ch, staff, null, TO_CHAR)
        if (ch.level + 3 < staff.level || randomPercent() >= 10 + get_skill(ch, Skill.staves) * 4 / 5) {
            act("You fail to invoke \$p.", ch, staff, null, TO_CHAR)
            act("...and nothing happens.", ch, null, null, TO_ROOM)
            check_improve(ch, Skill.staves, false, 2)
        } else {
            loop@ for (vch in ch.room.people) {
                when (skill.target) {
                    TAR_IGNORE -> if (vch != ch) continue@loop
                    TAR_CHAR_OFFENSIVE -> if (ch.pcdata == null == (vch.pcdata == null)) continue@loop
                    TAR_CHAR_DEFENSIVE -> if (ch.pcdata == null != (vch.pcdata == null)) continue@loop
                    TAR_CHAR_SELF -> if (vch != ch) continue@loop
                    else -> {
                        bug("Do_brandish: bad target for sn %d.", sn)
                        return
                    }
                }
                obj_cast_spell(Skill.skills[staff.value[3].toInt()], staff.value[0].toInt(), ch, vch, null)
                check_improve(ch, Skill.staves, true, 2)
            }
        }
    }

    if (--staff.value[2] <= 0) {
        act("\$n's \$p blazes bright and is gone.", ch, staff, null, TO_ROOM)
        act("Your \$p blazes bright and is gone.", ch, staff, null, TO_CHAR)
        extract_obj(staff)
    }

}


fun do_zap(ch: CHAR_DATA, argument: String) {
    if (ch.cabal == Clan.BattleRager) {
        send_to_char("You'd destroy the magic, not use it!\n", ch)
        return
    }

    val arg = StringBuilder()
    one_argument(argument, arg)
    if (arg.isEmpty() && ch.fighting == null) {
        send_to_char("Zap whom or what?\n", ch)
        return
    }

    val wand = get_hold_char(ch)
    if (wand == null) {
        send_to_char("You hold nothing in your hand.\n", ch)
        return
    }

    if (wand.item_type != ItemType.Wand) {
        send_to_char("You can zap only with a wand.\n", ch)
        return
    }

    var obj: Obj? = null
    val victim: CHAR_DATA?
    if (arg.isEmpty()) {
        if (ch.fighting != null) {
            victim = ch.fighting
        } else {
            send_to_char("Zap whom or what?\n", ch)
            return
        }
    } else {
        victim = get_char_room(ch, arg.toString())
        obj = get_obj_here(ch, arg.toString())
        if (victim == null && obj == null) {
            send_to_char("You can't find it.\n", ch)
            return
        }
    }

    WAIT_STATE(ch, 2 * PULSE_VIOLENCE)

    if (wand.value[2] > 0) {
        if (victim != null) {
            act("\$n zaps \$N with \$p.", ch, wand, victim, TO_ROOM)
            act("You zap \$N with \$p.", ch, wand, victim, TO_CHAR)
        } else {
            act("\$n zaps \$P with \$p.", ch, wand, obj!!, TO_ROOM)
            act("You zap \$P with \$p.", ch, wand, obj, TO_CHAR)
        }

        if (ch.level + 5 < wand.level || randomPercent() >= 20 + get_skill(ch, Skill.wands) * 4 / 5) {
            act("Your efforts with \$p produce only smoke and sparks.", ch, wand, null, TO_CHAR)
            act("\$n's efforts with \$p produce only smoke and sparks.", ch, wand, null, TO_ROOM)
            check_improve(ch, Skill.wands, false, 2)
        } else {
            obj_cast_spell(Skill.skills[wand.value[3].toInt()], wand.value[0].toInt(), ch, victim, obj)
            check_improve(ch, Skill.wands, true, 2)
        }
    }

    if (--wand.value[2] <= 0) {
        act("\$n's \$p explodes into fragments.", ch, wand, null, TO_ROOM)
        act("Your \$p explodes into fragments.", ch, wand, null, TO_CHAR)
        extract_obj(wand)
    }
}


fun do_steal(ch: CHAR_DATA, argument: String) {
    var rest = argument

    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg1)
    one_argument(rest, arg2)

    if (arg1.isEmpty() || arg2.isEmpty()) {
        send_to_char("Steal what from whom?\n", ch)
        return
    }

    if (ch.pcdata == null && IS_SET(ch.affected_by, AFF_CHARM) && ch.master != null) {
        send_to_char("You are to dazed to steal anything.\n", ch)
        return
    }

    val victim = get_char_room(ch, arg2.toString())
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
        send_to_char("You'd better not -- you might get hit.\n", ch)
        return
    }

    ch.last_death_time = -1

    val ch_doppel = ch.doppel
    val tmp_ch = if (ch_doppel != null && !IS_IMMORTAL(victim)) ch_doppel else ch

    WAIT_STATE(ch, Skill.steal.beats)
    var percent = get_skill(ch, Skill.steal) + if (IS_AWAKE(victim)) -10 else 30
    percent -= if (can_see(victim, ch)) 10 else 0
    percent -= if (victim.pcdata == null && victim.pIndexData.pShop != null) 25 else 0
    percent -= if (victim.level > ch.level) (victim.level - ch.level) * 2 else 0

    var obj: Obj? = null
    val arg1str = arg1.toString()
    if (!eq(arg1str, "coin")
            && !eq(arg1str, "coins")
            && !eq(arg1str, "silver")
            && !eq(arg1str, "gold")) {
        obj = get_obj_carry(victim, arg1str)
        if (obj == null) {
            send_to_char("You can't find it.\n", ch)
            return
        }

    }

    if (obj != null && obj.pIndexData.limit != -1) {
        if (IS_OBJ_STAT(obj, ITEM_ANTI_EVIL) && ch.isEvil()
                || IS_OBJ_STAT(obj, ITEM_ANTI_GOOD) && ch.isGood()
                || IS_OBJ_STAT(obj, ITEM_ANTI_NEUTRAL) && ch.isNeutralA()) {
            act("You are zapped by \$p.", ch, obj, null, TO_CHAR)
            act("\$n is zapped by \$p.", ch, obj, null, TO_ROOM)
            percent = 0
        }

        if (obj.pIndexData.limit < obj.pIndexData.count) {
            act("Gods doesn't allow \$p to be stolen.", ch, obj, null, TO_CHAR)
            act("Gods doesn't approve \$n's behaviour.", ch, obj, null, TO_ROOM)
            percent = 0
        }
    }

    val number = if (obj != null) get_obj_number(obj) else 0

    if (ch.carry_number + number > can_carry_items(ch)) {
        send_to_char("You can't carry that much item.\n", ch)
        return
    }

    if (victim.position == Pos.Fighting || randomPercent() > percent) {
        /*
            * Failure.
            */

        send_to_char("Oops.\n", ch)
        if (!IS_AFFECTED(victim, AFF_SLEEP)) {
            victim.position = if (victim.position == Pos.Sleeping)
                Pos.Standing
            else
                victim.position
            act("\$n tried to steal from you.\n", ch, null, victim, TO_VICT)
        }
        act("\$n tried to steal from \$N.\n", ch, null, victim, TO_NOTVICT)

        val buf = TextBuffer()
        when (number_range(0, 3)) {
            0 -> buf.sprintf("%s is a lousy thief!", tmp_ch.name)
            1 -> buf.sprintf("%s couldn't rob %s way out of a paper bag!",
                    tmp_ch.name, if (tmp_ch.sex.isFemale()) "her" else "his")
            2 -> buf.sprintf("%s tried to rob me!", tmp_ch.name)
            3 -> buf.sprintf("Keep your hands out of there, %s!", tmp_ch.name)
        }
        if (IS_AWAKE(victim)) {
            do_yell(victim, buf.toString())
        }
        if (ch.pcdata != null) {
            if (victim.pcdata == null) {
                check_improve(ch, Skill.steal, false, 2)
                multi_hit(victim, ch, null)
            }
        }

        return
    }

    if (eq(arg1str, "coin") || eq(arg1str, "coins") || eq(arg1str, "silver") || eq(arg1str, "gold")) {
        var amount_s = 0
        var amount_g = 0
        when {
            eq(arg1str, "silver") || eq(arg1str, "coin") || eq(arg1str, "coins") -> amount_s = victim.silver * number_range(1, 20) / 100
            eq(arg1str, "gold") -> amount_g = victim.gold * number_range(1, 7) / 100
        }

        if (amount_s <= 0 && amount_g <= 0) {
            send_to_char("You couldn't get any coins.\n", ch)
            return
        }

        ch.gold += amount_g
        victim.gold -= amount_g
        ch.silver += amount_s
        victim.silver -= amount_s
        val buf = TextBuffer()
        buf.sprintf("Bingo!  You got %d %s coins.\n",
                if (amount_s != 0) amount_s else amount_g,
                if (amount_s != 0) "silver" else "gold")

        send_to_char(buf, ch)
        check_improve(ch, Skill.steal, true, 2)
        return
    }

    if (!can_drop_obj(ch, obj)) {
        send_to_char("You can't pry it away.\n", ch)
        return
    }/* ||   IS_SET(obj.extra_flags, ITEM_INVENTORY)*//* ||  obj.level > ch.level */

    if (ch.carry_number + get_obj_number(obj) > can_carry_items(ch)) {
        send_to_char("You have your hands full.\n", ch)
        return
    }

    if (ch.carry_weight + get_obj_weight(obj) > can_carry_weight(ch)) {
        send_to_char("You can't carry that much weight.\n", ch)
        return
    }

    if (!IS_SET(obj!!.extra_flags, ITEM_INVENTORY)) {
        obj_from_char(obj)
        obj_to_char(obj, ch)
        send_to_char("You got it!\n", ch)
        check_improve(ch, Skill.steal, true, 2)
        if (IS_SET(obj.progtypes, OPROG_GET)) {
            obj.pIndexData.oprogs.get_prog!!(obj, ch)
        }
    } else {
        val obj_inve = create_object(obj.pIndexData, 0)
        clone_object(obj, obj_inve)
        obj_inve.extra_flags = REMOVE_BIT(obj_inve.extra_flags, ITEM_INVENTORY)
        obj_to_char(obj_inve, ch)
        send_to_char("You got one of them!\n", ch)
        check_improve(ch, Skill.steal, true, 1)
        if (IS_SET(obj_inve.progtypes, OPROG_GET)) {
            obj_inve.pIndexData.oprogs.get_prog!!(obj_inve, ch)
        }
    }

}

/*
 * Shopping commands.
 */
fun find_keeper(ch: CHAR_DATA): CHAR_DATA? {
    var pShop: Shop? = null
    var keeper: CHAR_DATA? = null
    for (k in ch.room.people) {
        pShop = k.pIndexData.pShop
        if (k.pcdata == null && pShop != null) {
            keeper = k
            break
        }
    }

    if (pShop == null) {
        send_to_char("You can't do that here.\n", ch)
        return null
    }

    if (IS_SET(keeper!!.room.area.area_flag, AREA_HOMETOWN) && ch.pcdata != null && IS_SET(ch.act, PLR_WANTED)) {
        do_say(keeper, "Criminals are not welcome!")
        val buf = TextBuffer()
        buf.sprintf("%s the CRIMINAL is over here!\n", ch.name)
        do_yell(keeper, buf.toString())
        return null
    }

    /* Shop hours. */
    if (time_info.hour < pShop.open_hour) {
        do_say(keeper, "Sorry, I am closed. Come back later.")
        return null
    }

    if (time_info.hour > pShop.close_hour) {
        do_say(keeper, "Sorry, I am closed. Come back tomorrow.")
        return null
    }

    /* Invisible or hidden people.*/
    if (!can_see(keeper, ch) && !IS_IMMORTAL(ch)) {
        do_say(keeper, "I don't trade with folks I can't see.")
        return null
    }

    return keeper
}

/** insert an object at the right spot for the keeper */
fun obj_to_keeper(obj: Obj, ch: CHAR_DATA) {
    /* see if any duplicates are found */
    var added = false
    for ((i, o) in ch.carrying.withIndex()) {
        if (obj.pIndexData == o.pIndexData && eq(obj.short_desc, o.short_desc)) {
            if (IS_OBJ_STAT(o, ITEM_INVENTORY)) {
                extract_obj(obj)
                return
            }
            obj.cost = o.cost /* keep it standard */
            ch.carrying.add(i, obj)
            added = true
            break
        }
    }
    if (!added) {
        ch.carrying.add(obj)
    }

    obj.carried_by = ch
    obj.in_room = null
    obj.in_obj = null
    ch.carry_number += get_obj_number(obj)
    ch.carry_weight += get_obj_weight(obj)
}

/** find an object from a shopkeeper's list */

private fun find_obj_keeper(ch: CHAR_DATA, keeper: CHAR_DATA, argument: String): Obj? {
    val arg = StringBuilder()
    val number = number_argument(argument, arg)

    return keeper.carrying
            .filter { it.wear_loc.isNone() && can_see_obj(keeper, it) && can_see_obj(ch, it) && is_name(arg.toString(), it.name) }
            .filterIndexed { count, _ -> count + 1 == number }
            .firstOrNull()
}

fun get_cost(keeper: CHAR_DATA, obj: Obj?, fBuy: Boolean): Int {
    val pShop = keeper.pIndexData.pShop
    if (obj == null || pShop == null) {
        return 0
    }

    if (IS_OBJ_STAT(obj, ITEM_NOSELL)) {
        return 0
    }

    var cost: Int
    if (fBuy) {
        cost = obj.cost * pShop.profit_buy / 100
    } else {
        cost = if (pShop.buy_type.any { obj.item_type === it }) obj.cost * pShop.profit_sell / 100 else 0

        if (!IS_OBJ_STAT(obj, ITEM_SELL_EXTRACT)) {
            keeper.carrying
                    .filter { obj.pIndexData == it.pIndexData && eq(obj.short_desc, it.short_desc) }
                    .forEach { return 0 }
            /*
                if (IS_OBJ_STAT(obj2,ITEM_INVENTORY))
                    cost /= 2;
                else
                    cost = cost * 3 / 4;
            */
        }
    }

    if (obj.item_type == ItemType.Staff || obj.item_type == ItemType.Wand) {
        if (obj.value[1] == 0L) {
            cost /= 4
        } else {
            cost = (cost * obj.value[2] / obj.value[1]).toInt()
        }
    }

    return cost
}


fun do_buy(ch: CHAR_DATA, argument: String) {
    if (argument.isEmpty()) {
        send_to_char("Buy what?\n", ch)
        return
    }
    if (IS_SET(ch.room.room_flags, ROOM_PET_SHOP)) {
        buy_pet(ch, argument)
        return
    }

    val keeper = find_keeper(ch) ?: return
    val arg = StringBuilder()
    val number = multiply_argument(argument, arg)
    if (number < -1 || number > 100) {
        act("\$n tells you 'Get real!", keeper, null, ch, TO_VICT)
        ch.reply = keeper
        return
    }

    val obj = find_obj_keeper(ch, keeper, arg.toString())
    var cost = get_cost(keeper, obj, true)

    if (cost <= 0 || !can_see_obj(ch, obj)) {
        act("\$n tells you 'I don't sell that -- try 'list''.", keeper, null, ch, TO_VICT)
        ch.reply = keeper
        return
    }

    if (obj != null && !IS_OBJ_STAT(obj, ITEM_INVENTORY)) {
        val count = keeper.carrying.count { it.pIndexData == obj.pIndexData && eq(it.short_desc, obj.short_desc) }
        if (count < number) {
            act("\$n tells you 'I don't have that many in stock.'", keeper, null, ch, TO_VICT)
            ch.reply = keeper
            return
        }
    } else if (obj!!.pIndexData.limit != -1) {
        val count = 1 + obj.pIndexData.limit - obj.pIndexData.count
        if (count < 1) {
            act("\$n tells you 'Gods will not approve me to sell that.'", keeper, null, ch, TO_VICT)
            ch.reply = keeper
            return
        }
        if (count < number) {
            act("\$n tells you 'I don't have that many in stock.'", keeper, null, ch, TO_VICT)
            ch.reply = keeper
            return
        }
    }

    if (ch.silver + ch.gold * 100 < cost * number) {
        if (number > 1) {
            act("\$n tells you 'You can't afford to buy that many.", keeper, obj, ch, TO_VICT)
        } else {
            act("\$n tells you 'You can't afford to buy \$p'.", keeper, obj, ch, TO_VICT)
        }
        ch.reply = keeper
        return
    }

    if (obj.level > ch.level) {
        act("\$n tells you 'You can't use \$p yet'.", keeper, obj, ch, TO_VICT)
        ch.reply = keeper
        return
    }

    if (ch.carry_number + number * get_obj_number(obj) > can_carry_items(ch)) {
        send_to_char("You can't carry that many items.\n", ch)
        return
    }

    if (ch.carry_weight + number * get_obj_weight(obj) > can_carry_weight(ch)) {
        send_to_char("You can't carry that much weight.\n", ch)
        return
    }

    /* haggle */
    val roll = randomPercent()
    if (!IS_OBJ_STAT(obj, ITEM_SELL_EXTRACT) && roll < get_skill(ch, Skill.haggle)) {
        cost -= obj.cost / 2 * roll / 100
        act("You haggle with \$N.", ch, null, keeper, TO_CHAR)
        check_improve(ch, Skill.haggle, true, 4)
    }

    if (number > 1) {
        act("\$n buys \$p[$number].", ch, obj, null, TO_ROOM)
        act("You buy \$p[$number] for ${cost * number} silver.", ch, obj, null, TO_CHAR)
    } else {
        act("\$n buys \$p.", ch, obj, null, TO_ROOM)
        act("You buy \$p for $cost silver.", ch, obj, null, TO_CHAR)
    }
    deduct_cost(ch, cost * number)
    keeper.gold += cost * number / 100
    keeper.silver += cost * number - cost * number / 100 * 100

    var count = 0
    while (count < number) {
        var t_obj: Obj
        if (IS_SET(obj.extra_flags, ITEM_INVENTORY)) {
            t_obj = create_object(obj.pIndexData, obj.level)
        } else {
            t_obj = find_obj_keeper(ch, keeper, arg.toString())!!
            obj_from_char(t_obj)
        }

        if (t_obj.timer > 0 && !IS_OBJ_STAT(t_obj, ITEM_HAD_TIMER)) {
            t_obj.timer = 0
        }
        t_obj.extra_flags = REMOVE_BIT(t_obj.extra_flags, ITEM_HAD_TIMER)
        obj_to_char(t_obj, ch)
        if (cost < t_obj.cost) {
            t_obj.cost = cost
        }
        count++
    }
}

private fun buy_pet(ch: CHAR_DATA, argument: String) {
    var rest = smash_tilde(argument)

    if (ch.pcdata == null) {
        return
    }

    val arg = StringBuilder()
    rest = one_argument(rest, arg)

    /* hack to make new thalos pets work */

    val pRoomIndexNext = if (ch.room.vnum == 9621) get_room_index(9706) else get_room_index(ch.room.vnum + 1)
    if (pRoomIndexNext == null) {
        bug("Do_buy: bad pet shop at vnum %d.", ch.room.vnum)
        send_to_char("Sorry, you can't buy that here.\n", ch)
        return
    }

    val in_room = ch.room
    ch.room = pRoomIndexNext
    var pet = get_char_room(ch, arg.toString())
    ch.room = in_room

    if (pet == null || !IS_SET(pet.act, ACT_PET) || pet.pcdata != null) {
        send_to_char("Sorry, you can't buy that here.\n", ch)
        return
    }

    if (IS_SET(pet.act, ACT_RIDEABLE) && ch.cabal == Clan.Knight && MOUNTED(ch) == null) {
        val cost = 10 * pet.level * pet.level

        if (ch.silver + 100 * ch.gold < cost) {
            send_to_char("You can't afford it.\n", ch)
            return
        }

        if (ch.level < pet.level + 5) {
            send_to_char("You're not powerful enough to master this pet.\n", ch)
            return
        }

        deduct_cost(ch, cost)
        pet = create_mobile(pet.pIndexData)
        pet.comm = COMM_NOTELL or COMM_NOSHOUT or COMM_NOCHANNELS

        char_to_room(pet, ch.room)
        do_mount(ch, pet.name)
        send_to_char("Enjoy your mount.\n", ch)
        act("\$n bought \$N as a mount.", ch, null, pet, TO_ROOM)
        return
    }

    if (ch.pet != null) {
        send_to_char("You already own a pet.\n", ch)
        return
    }

    var cost = 10 * pet.level * pet.level

    if (ch.silver + 100 * ch.gold < cost) {
        send_to_char("You can't afford it.\n", ch)
        return
    }

    if (ch.level < pet.level) {
        send_to_char("You're not powerful enough to master this pet.\n", ch)
        return
    }

    /* haggle */
    val buf = TextBuffer()
    val roll = randomPercent()
    if (roll < get_skill(ch, Skill.haggle)) {
        cost -= cost / 2 * roll / 100
        buf.sprintf("You haggle the price down to %d coins.\n", cost)
        send_to_char(buf, ch)
        check_improve(ch, Skill.haggle, true, 4)
    }

    deduct_cost(ch, cost)
    pet = create_mobile(pet.pIndexData)
    pet.act = SET_BIT(pet.act, ACT_PET)
    pet.affected_by = SET_BIT(pet.affected_by, AFF_CHARM)
    pet.comm = COMM_NOTELL or COMM_NOSHOUT or COMM_NOCHANNELS

    one_argument(rest, arg)
    if (arg.isNotEmpty()) {
        pet.name = pet.name + " " + arg
    }

    buf.sprintf("%sA neck tag says 'I belong to %s'.\n", pet.description, ch.name)
    pet.description = buf.toString()

    char_to_room(pet, ch.room)
    add_follower(pet, ch)
    pet.leader = ch
    ch.pet = pet
    send_to_char("Enjoy your pet.\n", ch)
    act("\$n bought \$N as a pet.", ch, null, pet, TO_ROOM)
}


fun do_list(ch: CHAR_DATA, argument: String) {

    if (IS_SET(ch.room.room_flags, ROOM_PET_SHOP)) {
        /* hack to make new thalos pets work */

        val pRoomIndexNext = if (ch.room.vnum == 9621) get_room_index(9706) else get_room_index(ch.room.vnum + 1)

        if (pRoomIndexNext == null) {
            bug("Do_list: bad pet shop at vnum %d.", ch.room.vnum)
            send_to_char("You can't do that here.\n", ch)
            return
        }

        var found = false
        val buf = TextBuffer()
        for (pet in pRoomIndexNext.people) {
            if (pet.pcdata != null) {
                continue
            }
            if (IS_SET(pet.act, ACT_PET)) {
                if (!found) {
                    found = true
                    send_to_char("Pets for sale:\n", ch)
                }
                buf.sprintf("[%2d] %8d - %s\n", pet.level, 10 * pet.level * pet.level, pet.short_desc)
                send_to_char(buf, ch)
            }
        }
        if (!found) {
            send_to_char("Sorry, we're out of pets right now.\n", ch)
        }
    } else {
        val keeper = find_keeper(ch) ?: return
        val arg = StringBuilder()

        one_argument(argument, arg)

        var found = false
        val buf = TextBuffer()

        var i = 0
        while (i < keeper.carrying.size) {
            val obj = keeper.carrying[i]
            val cost = get_cost(keeper, obj, true)
            if (obj.wear_loc.isNone() && can_see_obj(ch, obj) && cost > 0 && (arg.isEmpty() || is_name(arg.toString(), obj.name))) {
                if (!found) {
                    found = true
                    send_to_char("[Lv Price Qty] Item\n", ch)
                }
                if (IS_OBJ_STAT(obj, ITEM_INVENTORY)) {
                    val availablity = if (obj.pIndexData.limit != -1) if (obj.pIndexData.count > obj.pIndexData.limit) " (NOT AVAILABLE NOW)" else " (AVAILABLE)" else ""
                    buf.sprintf("[%2d %5d -- ] %s%s\n", obj.level, cost, obj.short_desc, availablity)
                } else {
                    var count = 1
                    while (i + 1 < keeper.carrying.size) {
                        val next = keeper.carrying[i + 1]
                        if (obj.pIndexData != next.pIndexData || !eq(obj.short_desc, next.short_desc)) {
                            break
                        }
                        count++
                        i++
                    }
                    buf.sprintf("[%2d %5d %2d ] %s\n", obj.level, cost, count, obj.short_desc)
                }
                send_to_char(buf, ch)
            }
            i++
        }

        if (!found) {
            send_to_char("You can't buy anything here.\n", ch)
        }
    }
}


fun do_sell(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Sell what?\n", ch)
        return
    }

    val keeper = find_keeper(ch) ?: return

    val obj = get_obj_carry(ch, arg.toString())
    if (obj == null) {
        act("\$n tells you 'You don't have that item'.", keeper, null, ch, TO_VICT)
        ch.reply = keeper
        return
    }

    if (!can_drop_obj(ch, obj)) {
        send_to_char("You can't let go of it.\n", ch)
        return
    }

    if (!can_see_obj(keeper, obj)) {
        act("\$n doesn't see what you are offering.", keeper, null, ch, TO_VICT)
        return
    }

    var cost = get_cost(keeper, obj, false)
    if (cost <= 0) {
        act("\$n looks uninterested in \$p.", keeper, obj, ch, TO_VICT)
        return
    }
    if (cost > keeper.silver + 100 * keeper.gold) {
        act("\$n tells you 'I'm afraid I don't have enough wealth to buy \$p.",
                keeper, obj, ch, TO_VICT)
        return
    }

    act("\$n sells \$p.", ch, obj, null, TO_ROOM)
    /* haggle */
    var roll = randomPercent()
    if (!IS_OBJ_STAT(obj, ITEM_SELL_EXTRACT) && roll < get_skill(ch, Skill.haggle)) {
        roll = get_skill(ch, Skill.haggle) + number_range(1, 20) - 10
        send_to_char("You haggle with the shopkeeper.\n", ch)
        cost += obj.cost / 2 * roll / 100
        cost = Math.min(cost, 95 * get_cost(keeper, obj, true) / 100)
        cost = Math.min(cost, keeper.silver + 100 * keeper.gold)
        check_improve(ch, Skill.haggle, true, 4)
    }

    val silver = cost - cost / 100 * 100
    val gold = cost / 100

    val buf = TextBuffer()
    val buf2 = TextBuffer()
    buf2.sprintf("You sell \$p for %s %s%spiece%s.",
            if (silver != 0) "%d silver" else "", /* silvers  */
            if (silver != 0 && gold != 0) "and " else "", /*   and    */
            if (gold != 0) "%d gold " else "", /*  golds   */
            if (silver + gold > 1) "s" else "")               /* piece(s) */
    buf.sprintf(buf2.toString(), silver, gold)

    act(buf.toString(), ch, obj, null, TO_CHAR)
    ch.gold += gold
    ch.silver += silver
    deduct_cost(keeper, cost)
    if (keeper.gold < 0) {
        keeper.gold = 0
    }
    if (keeper.silver < 0) {
        keeper.silver = 0
    }

    if (obj.item_type == ItemType.Trash || IS_OBJ_STAT(obj, ITEM_SELL_EXTRACT)) {
        extract_obj(obj)
    } else {
        obj_from_char(obj)
        if (obj.timer != 0) {
            obj.extra_flags = SET_BIT(obj.extra_flags, ITEM_HAD_TIMER)
        } else {
            obj.timer = number_range(50, 100)
        }
        obj_to_keeper(obj, keeper)
    }

}


fun do_value(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Value what?\n", ch)
        return
    }

    val keeper = find_keeper(ch) ?: return

    val obj = get_obj_carry(ch, arg.toString())
    if (obj == null) {
        act("\$n tells you 'You don't have that item'.", keeper, null, ch, TO_VICT)
        ch.reply = keeper
        return
    }

    if (!can_see_obj(keeper, obj)) {
        act("\$n doesn't see what you are offering.", keeper, null, ch, TO_VICT)
        return
    }

    if (!can_drop_obj(ch, obj)) {
        send_to_char("You can't let go of it.\n", ch)
        return
    }

    val cost = get_cost(keeper, obj, false)
    if (cost <= 0) {
        act("\$n looks uninterested in \$p.", keeper, obj, ch, TO_VICT)
        return
    }

    val buf = TextBuffer()
    buf.sprintf("\$n tells you 'I'll give you %d silver and %d gold coins for \$p'.", cost - cost / 100 * 100, cost / 100)
    act(buf.toString(), keeper, obj, ch, TO_VICT)
    ch.reply = keeper

}

fun do_wanted(ch: CHAR_DATA, argument: String) {
    var rest = argument
    if (skill_failure_check(ch, Skill.wanted, false, 0, null)) {
        return
    }

    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    rest = one_argument(rest, arg1)
    one_argument(rest, arg2)

    if (arg1.isEmpty() || arg2.isEmpty()) {
        send_to_char("Usage: wanted <player> <Y|N>\n", ch)
        return
    }

    val victim = get_char_world(ch, arg1.toString())
    if (victim == null || !can_see(ch, victim)) {
        send_to_char("They aren't here.\n", ch)
        return
    }

    if (victim.level >= LEVEL_IMMORTAL && ch.level < victim.level) {
        act("You do not have the power to arrest \$N.", ch, null, victim, TO_CHAR)
        return
    }

    if (victim == ch) {
        send_to_char("You cannot do that to yourself.\n", ch)
        return
    }

    when (arg2[0]) {
        'Y', 'y' -> if (IS_SET(victim.act, PLR_WANTED)) {
            act("\$n is already wanted.", ch, null, null, TO_CHAR)
        } else {
            victim.act = SET_BIT(victim.act, PLR_WANTED)
            act("\$n is now WANTED!!!", victim, null, ch, TO_NOTVICT)
            send_to_char("You are now WANTED!!!\n", victim)
            send_to_char("Ok.\n", ch)
        }
        'N', 'n' -> if (!IS_SET(victim.act, PLR_WANTED)) {
            act("\$N is not wanted.", ch, null, victim, TO_CHAR)
        } else {
            victim.act = REMOVE_BIT(victim.act, PLR_WANTED)
            act("\$n is no longer wanted.", victim, null, ch, TO_NOTVICT)
            send_to_char("You are no longer wanted.\n", victim)
            send_to_char("Ok.\n", ch)
        }
        else -> send_to_char("Usage: wanted <player> <Y|N>\n", ch)
    }
}

fun do_herbs(ch: CHAR_DATA, argument: String) {
    val arg = StringBuilder()
    one_argument(argument, arg)

    if (skill_failure_check(ch, Skill.herbs, false, 0, null)) {
        return
    }

    if (is_affected(ch, Skill.herbs)) {
        send_to_char("You can't find any more herbs.\n", ch)
        return
    }

    val victim: CHAR_DATA?
    if (arg.isEmpty()) {
        victim = ch
    } else {
        victim = get_char_room(ch, arg.toString())
        if (victim == null) {
            send_to_char("They're not here.\n", ch)
            return
        }
    }

    WAIT_STATE(ch, Skill.herbs.beats)

    if (ch.room.sector_type != SECT_INSIDE && ch.room.sector_type != SECT_CITY &&
            (ch.pcdata == null || randomPercent() < get_skill(ch, Skill.herbs))) {
        val af = Affect()
        af.type = Skill.herbs
        af.level = ch.level
        af.duration = 5

        affect_to_char(ch, af)

        send_to_char("You gather some beneficial herbs.\n", ch)
        act("\$n gathers some herbs.", ch, null, null, TO_ROOM)

        if (ch != victim) {
            act("\$n gives you some herbs to eat.", ch, null, victim, TO_VICT)
            act("You give the herbs to \$N.", ch, null, victim, TO_CHAR)
            act("\$n gives the herbs to \$N.", ch, null, victim, TO_NOTVICT)
        }

        if (victim.hit < victim.max_hit) {
            send_to_char("You feel better.\n", victim)
            act("\$n looks better.", victim, null, null, TO_ROOM)
        }
        victim.hit = Math.min(victim.max_hit, victim.hit + 5 * ch.level)
        check_improve(ch, Skill.herbs, true, 1)
        if (is_affected(victim, Skill.plague)) {
            if (check_dispel(ch.level, victim, Skill.plague)) {
                send_to_char("Your sores vanish.\n", victim)
                act("\$n looks relieved as \$s sores vanish.", victim, null, null, TO_ROOM)
            }
        }
    } else {
        send_to_char("You search for herbs but find none here.\n", ch)
        act("\$n looks around for herbs.", ch, null, null, TO_ROOM)
        check_improve(ch, Skill.herbs, false, 1)
    }
}

private val orig_lore = true

fun do_lore(ch: CHAR_DATA, argument: String) {
    val arg1 = StringBuilder()
    one_argument(argument, arg1)

    if (skill_failure_check(ch, Skill.lore, true, 0, "The meaning of this object escapes you for the moment.\n")) {
        return
    }

    val obj = get_obj_carry(ch, arg1.toString())
    if (obj == null) {
        send_to_char("You do not have that object.\n", ch)
        return
    }

    if (ch.mana < 30) {
        send_to_char("You don't have enough mana.\n", ch)
        return
    }

    /* a random lore */
    val chance = randomPercent()

    val buf = TextBuffer()
    when {
        get_skill(ch, Skill.lore) < 20 -> {
            buf.sprintf("Object '%s'.\n", obj.name)
            send_to_char(buf, ch)
            ch.mana -= 30
            check_improve(ch, Skill.lore, true, 8)
            return
        }
        get_skill(ch, Skill.lore) < 40 -> {
            buf.sprintf("Object '%s'.  Weight is %d, value is %d.\n",
                    obj.name,
                    if (chance < 60) obj.weight else number_range(1, 2 * obj.weight),
                    if (chance < 60) number_range(1, 2 * obj.cost) else obj.cost
            )
            send_to_char(buf, ch)
            if (!eq(obj.material, "oldstyle")) {
                buf.sprintf("Material is %s.\n", obj.material)
                send_to_char(buf, ch)
            }
            ch.mana -= 30
            check_improve(ch, Skill.lore, true, 7)
            return
        }
        get_skill(ch, Skill.lore) < 60 -> {
            buf.sprintf("Object '%s' has weight %d.\nValue is %d, level is %d.\nMaterial is %s.\n",
                    obj.name,
                    obj.weight,
                    if (chance < 60) number_range(1, 2 * obj.cost) else obj.cost,
                    if (chance < 60) obj.level else number_range(1, 2 * obj.level),
                    if (!eq(obj.material, "oldstyle")) obj.material else "unknown"
            )
            send_to_char(buf, ch)
            ch.mana -= 30
            check_improve(ch, Skill.lore, true, 6)
            return
        }
        get_skill(ch, Skill.lore) < 80 -> {
            buf.sprintf("Object '%s' is type %s, extra flags %s.\nWeight is %d, value is %d, level is %d.\nMaterial is %s.\n",
                    obj.name,
                    obj.item_type.displayName,
                    extra_bit_name(obj.extra_flags),
                    obj.weight,
                    if (chance < 60) number_range(1, 2 * obj.cost) else obj.cost,
                    if (chance < 60) obj.level else number_range(1, 2 * obj.level),
                    if (!eq(obj.material, "oldstyle")) obj.material else "unknown"
            )
            send_to_char(buf, ch)
            ch.mana -= 30
            check_improve(ch, Skill.lore, true, 5)
            return
        }
        get_skill(ch, Skill.lore) < 85 -> {
            buf.sprintf("Object '%s' is type %s, extra flags %s.\nWeight is %d, value is %d, level is %d.\nMaterial is %s.\n",
                    obj.name,
                    obj.item_type.displayName,
                    extra_bit_name(obj.extra_flags),
                    obj.weight,
                    obj.cost,
                    obj.level,
                    if (!eq(obj.material, "oldstyle")) obj.material else "unknown"
            )
            send_to_char(buf, ch)
        }
        else -> {
            buf.sprintf("Object '%s' is type %s, extra flags %s.\nWeight is %d, value is %d, level is %d.\nMaterial is %s.\n",
                    obj.name,
                    obj.item_type.displayName,
                    extra_bit_name(obj.extra_flags),
                    obj.weight,
                    obj.cost,
                    obj.level,
                    if (!eq(obj.material, "oldstyle")) obj.material else "unknown"
            )
            send_to_char(buf, ch)
        }
    }

    ch.mana -= 30

    var value0 = obj.value[0].toInt()
    var value1 = obj.value[1].toInt()
    var value2 = obj.value[2].toInt()
    var value3 = obj.value[3].toInt()

    when (obj.item_type) {
        ItemType.Scroll, ItemType.Potion, ItemType.Pill -> {
            if (get_skill(ch, Skill.lore) < 85) {
                value0 = number_range(1, 60)
                if (chance > 40) {
                    value1 = number_range(1, MAX_SKILL - 1)
                    if (chance > 60) {
                        value2 = number_range(1, MAX_SKILL - 1)
                        if (chance > 80) {
                            value3 = number_range(1, MAX_SKILL - 1)
                        }
                    }
                }
            } else {
                if (chance > 60) {
                    value1 = number_range(1, MAX_SKILL - 1)
                    if (chance > 80) {
                        value2 = number_range(1, MAX_SKILL - 1)
                        if (chance > 95) {
                            value3 = number_range(1, MAX_SKILL - 1)
                        }
                    }
                }
            }

            buf.sprintf("Level %d spells of:", value0)
            send_to_char(buf, ch)

            if (value1 in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[value1].skillName, ch)
                send_to_char("'", ch)
            }

            if (value2 in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[value2].skillName, ch)
                send_to_char("'", ch)
            }

            if (value3 in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[value3].skillName, ch)
                send_to_char("'", ch)
            }

            send_to_char(".\n", ch)
        }

        ItemType.Wand, ItemType.Staff -> {
            if (get_skill(ch, Skill.lore) < 85) {
                value0 = number_range(1, 60)
                if (chance > 40) {
                    value3 = number_range(1, MAX_SKILL - 1)
                    if (chance > 60) {
                        value2 = number_range(0, (2 * obj.value[2]).toInt())
                        if (chance > 80) {
                            value1 = number_range(0, value2)
                        }
                    }
                }
            } else {
                if (chance > 60) {
                    value3 = number_range(1, MAX_SKILL - 1)
                    if (chance > 80) {
                        value2 = number_range(0, (2 * obj.value[2]).toInt())
                        if (chance > 95) {
                            value1 = number_range(0, value2)
                        }
                    }
                }
            }

            buf.sprintf("Has %d(%d) charges of level %d", value1, value2, value0)
            send_to_char(buf, ch)

            if (value3 in 0..(MAX_SKILL - 1)) {
                send_to_char(" '", ch)
                send_to_char(Skill.skills[value3].skillName, ch)
                send_to_char("'", ch)
            }

            send_to_char(".\n", ch)
        }

        ItemType.Weapon -> {
            send_to_char("Weapon type is ", ch)
            var weaponType = obj.weaponType
            if (get_skill(ch, Skill.lore) < 85) {
                weaponType = WeaponType.values()[number_range(0, WeaponType.values().size)]
                if (chance > 33) {
                    value1 = number_range(1, (2 * obj.value[1]).toInt())
                    if (chance > 66) {
                        value2 = number_range(1, (2 * obj.value[2]).toInt())
                    }
                }
            } else {
                if (chance > 50) {
                    value1 = number_range(1, (2 * obj.value[1]).toInt())
                    if (chance > 75) {
                        value2 = number_range(1, (2 * obj.value[2]).toInt())
                    }
                }
            }
            send_to_char("${weaponType.typeName}.\n", ch)
            if (obj.pIndexData.new_format) {
                buf.sprintf("Damage is %dd%d (average %d).\n", value1, value2, (1 + value2) * value1 / 2)
            } else {
                buf.sprintf("Damage is %d to %d (average %d).\n", value1, value2, (value1 + value2) / 2)
            }
            send_to_char(buf, ch)
        }

        ItemType.Armor -> {
            if (get_skill(ch, Skill.lore) < 85) {
                if (chance > 25) {
                    value2 = number_range(0, (2 * obj.value[2]).toInt())
                    if (chance > 45) {
                        value0 = number_range(0, (2 * obj.value[0]).toInt())
                        if (chance > 65) {
                            value3 = number_range(0, (2 * obj.value[3]).toInt())
                            if (chance > 85) {
                                value1 = number_range(0, (2 * obj.value[1]).toInt())
                            }
                        }
                    }
                }
            } else {
                if (chance > 45) {
                    value2 = number_range(0, (2 * obj.value[2]).toInt())
                    if (chance > 65) {
                        value0 = number_range(0, (2 * obj.value[0]).toInt())
                        if (chance > 85) {
                            value3 = number_range(0, (2 * obj.value[3]).toInt())
                            if (chance > 95) {
                                value1 = number_range(0, (2 * obj.value[1]).toInt())
                            }
                        }
                    }
                }
            }

            buf.sprintf("Armor class is %d pierce, %d bash, %d slash, and %d vs. magic.\n", value0, value1, value2, value3)
            send_to_char(buf, ch)
        }
        else -> {
        }
    }

    if (get_skill(ch, Skill.lore) < 87) {
        check_improve(ch, Skill.lore, true, 5)
    }
    if (orig_lore) {
        return
    }

    if (!obj.enchanted) {
        var paf: Affect? = obj.pIndexData.affected
        while (paf != null) {
            if (paf.location != Apply.None && paf.modifier != 0) {
                buf.sprintf("Affects %s by %d.\n", paf.location.locName, paf.modifier)
                send_to_char(buf, ch)
            }
            paf = paf.next
        }
    }

    var paf = obj.affected
    while (paf != null) {
        if (paf.location != Apply.None && paf.modifier != 0) {
            buf.sprintf("Affects %s by %d.\n", paf.location.locName, paf.modifier)
            send_to_char(buf, ch)
        }
        paf = paf.next
    }
    check_improve(ch, Skill.lore, true, 5)
}


fun do_butcher(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.butcher, true, 0, "You don't have the precision instruments for that.\n")) {
        return
    }

    val arg = StringBuilder()
    one_argument(argument, arg)

    if (arg.isEmpty()) {
        send_to_char("Butcher what?\n", ch)
        return
    }
    val obj = get_obj_here(ch, arg.toString())
    if (obj == null) {
        send_to_char("You do not see that here.\n", ch)
        return
    }

    if (obj.item_type != ItemType.CorpsePc && obj.item_type != ItemType.CorpseNpc) {
        send_to_char("You can't butcher that.\n", ch)
        return
    }

    if (obj.carried_by != null) {
        send_to_char("Put it down first.\n", ch)
        return
    }

    obj_from_room(obj)

    for (tmp_obj in obj.contains) {
        obj_from_obj(tmp_obj)
        obj_to_room(tmp_obj, ch.room)
    }


    if (ch.pcdata == null || randomPercent() < get_skill(ch, Skill.butcher)) {
        val numsteaks = random(4) + 1
        val buf = TextBuffer()
        if (numsteaks > 1) {
            act("\$n butchers \$p and creates $numsteaks steaks.", ch, obj, null, TO_ROOM)
            act("You butcher \$p and create $numsteaks steaks.", ch, obj, null, TO_CHAR)
        } else {
            act("\$n butchers \$p and creates a steak.", ch, obj, null, TO_ROOM)
            act("You butcher \$p and create a steak.", ch, obj, null, TO_CHAR)
        }
        check_improve(ch, Skill.butcher, true, 1)

        for (i in 0 until numsteaks) {
            val steak = create_object(get_obj_index_nn(OBJ_VNUM_STEAK), 0)
            buf.sprintf(steak.short_desc, obj.short_desc)
            steak.short_desc = buf.toString()

            buf.sprintf(steak.description, obj.short_desc)
            steak.description = buf.toString()

            obj_to_room(steak, ch.room)
        }
    } else {
        act("You fail and destroy \$p.", ch, obj, null, TO_CHAR)
        act("\$n fails to butcher \$p and destroys it.", ch, obj, null, TO_ROOM)

        check_improve(ch, Skill.butcher, false, 1)
    }
    extract_obj(obj)
}


fun do_balance(ch: CHAR_DATA) {
    val pcdata = ch.pcdata
    if (pcdata == null) {
        send_to_char("You don't have a bank account.\n", ch)
        return
    }

    if (!IS_SET(ch.room.room_flags, ROOM_BANK)) {
        send_to_char("You are not in a bank.\n", ch)
        return
    }


    if (pcdata.bank_s + pcdata.bank_g == 0) {
        send_to_char("You don't have any money in the bank.\n", ch)
        return
    }

    val bank_g = pcdata.bank_g.toLong()
    val bank_s = pcdata.bank_s.toLong()
    val buf = TextBuffer()
    val buf2 = TextBuffer()
    buf.sprintf("You have %s%s%s coin%s in the bank.\n",
            if (bank_g != 0L) "%d gold" else "",
            if (bank_g != 0L && bank_s != 0L) " and " else "",
            if (bank_s != 0L) "%d silver" else "",
            if (bank_s + bank_g > 1) "s" else "")

    if (bank_g == 0L) {
        buf2.sprintf(buf.toString(), bank_s)
    } else {
        buf2.sprintf(buf.toString(), bank_g, bank_s)
    }

    send_to_char(buf2, ch)
}

fun do_withdraw(ch: CHAR_DATA, argument: String) {
    var rest = argument

    val pcdata = ch.pcdata
    if (pcdata == null) {
        send_to_char("You don't have a bank account.\n", ch)
        return
    }

    if (!IS_SET(ch.room.room_flags, ROOM_BANK)) {
        send_to_char("The mosquito by your feet will not give you any money.\n", ch)
        return
    }

    val arg = StringBuilder()
    rest = one_argument(rest, arg)
    if (arg.isEmpty()) {
        send_to_char("Withdraw how much?\n", ch)
        return
    }

    var amount_s = Math.abs(atoi(arg.toString()))
    val amount_g: Int
    if (eq(rest, "silver") || rest.isEmpty()) {
        amount_g = 0
    } else if (eq(rest, "gold")) {
        amount_g = amount_s
        amount_s = 0
    } else {
        send_to_char("You can withdraw gold and silver coins only.", ch)
        return
    }

    if (amount_g > pcdata.bank_g) {
        send_to_char("Sorry, we don't give loans.\n", ch)
        return
    }

    if (amount_s > pcdata.bank_s) {
        send_to_char("Sorry, we don't give loans.\n", ch)
        return
    }

    var weight = amount_g * 2 / 5
    weight += amount_s / 10

    if (get_carry_weight(ch) + weight > can_carry_weight(ch)) {
        act("You can't carry that much weight.", ch, null, null, TO_CHAR)
        return
    }

    pcdata.bank_g -= amount_g
    pcdata.bank_s -= amount_s
    ch.gold += (0.98 * amount_g).toInt()
    ch.silver += (0.90 * amount_s).toInt()
    val buf = TextBuffer()
    if (amount_s in 1..9) {
        if (amount_s == 1) {
            buf.sprintf("One coin??? You cheapskate!\n")
        } else {
            buf.sprintf("%d coins??? You cheapskate!\n", amount_s)
        }
    } else {
        buf.sprintf("Here are your %d %s coins, minus a %d coin withdrawal fee.\n",
                if (amount_s != 0) amount_s else amount_g,
                if (amount_s != 0) "silver" else "gold",
                if (amount_s != 0)
                    Math.max(1, (0.10 * amount_s).toInt()).toLong()
                else
                    Math.max(1, (0.02 * amount_g).toInt()).toLong())
    }
    send_to_char(buf, ch)
    act("\$n steps up to the teller window.", ch, null, null, TO_ROOM)
}

fun do_deposit(ch: CHAR_DATA, argument: String) {
    var rest = argument

    val pcdata = ch.pcdata
    if (pcdata == null) {
        send_to_char("You don't have a bank account.\n", ch)
        return
    }

    if (!IS_SET(ch.room.room_flags, ROOM_BANK)) {
        send_to_char("The ant by your feet can't carry your gold.\n", ch)
        return
    }

    val arg = StringBuilder()
    rest = one_argument(rest, arg)
    if (arg.isEmpty()) {
        send_to_char("Deposit how much?\n", ch)
        return
    }
    val amount_g: Int
    var amount_s = Math.abs(atoi(arg.toString()))
    if (eq(rest, "silver") || rest.isEmpty()) {
        amount_g = 0
    } else if (eq(rest, "gold")) {
        amount_g = amount_s
        amount_s = 0
    } else {
        send_to_char("You can deposit gold and silver coins only.", ch)
        return
    }

    if (amount_g > ch.gold) {
        send_to_char("That's more than you've got.\n", ch)
        return
    }
    if (amount_s > ch.silver) {
        send_to_char("That's more than you've got.\n", ch)
        return
    }

    if (amount_g + pcdata.bank_g > 400000) {
        send_to_char("Bank cannot accept more than 400,000 gold.\n", ch)
        return
    }

    pcdata.bank_s += amount_s
    pcdata.bank_g += amount_g
    ch.gold -= amount_g
    ch.silver -= amount_s
    val buf = TextBuffer()
    if (amount_s == 1) {
        buf.sprintf("Oh boy! One gold coin!\n")
    } else {
        buf.sprintf("%d %s coins deposited. Come again soon!\n",
                if (amount_s != 0) amount_s else amount_g,
                if (amount_s != 0) "silver" else "gold")
    }

    send_to_char(buf, ch)
    act("\$n steps up to the teller window.", ch, null, null, TO_ROOM)
}


fun do_enchant(ch: CHAR_DATA, argument: String) {
    if (skill_failure_check(ch, Skill.enchant_sword, false, 0, null)) {
        return
    }

    if (argument.isEmpty()) {
        send_to_char("Wear which weapon to enchant?\n", ch)
        return
    }

    val obj = get_obj_carry(ch, argument)

    if (obj == null) {
        send_to_char("You don't have that item.\n", ch)
        return
    }


    var wear_level = ch.level

    if (ch.clazz.fMana && obj.item_type == ItemType.Armor || !ch.clazz.fMana && obj.item_type == ItemType.Weapon) {
        wear_level += 3
    }

    if (wear_level < obj.level) {
        send_to_char("You must be level ${obj.level} to be able to enchant this object.\n", ch)
        act("\$n tries to enchant \$p, but is too inexperienced.", ch, obj, null, TO_ROOM)
        return
    }

    if (ch.mana < 100) {
        send_to_char("You don't have enough mana.\n", ch)
        return
    }

    if (randomPercent() > get_skill(ch, Skill.enchant_sword)) {
        send_to_char("You lost your concentration.\n", ch)
        act("\$n tries to enchant \$p, but he forgets how for a moment.",
                ch, obj, null, TO_ROOM)
        WAIT_STATE(ch, Skill.enchant_sword.beats)
        check_improve(ch, Skill.enchant_sword, false, 6)
        ch.mana -= 50
        return
    }
    ch.mana -= 100
    spell_enchant_weapon(ch.level, ch, obj)
    check_improve(ch, Skill.enchant_sword, true, 2)
    WAIT_STATE(ch, Skill.enchant_sword.beats)
}


fun hold_a_light(ch: CHAR_DATA, obj: Obj, iWear: Wear) {
    act("\$n lights \$p and holds it.", ch, obj, null, TO_ROOM)
    act("You light \$p and hold it.", ch, obj, null, TO_CHAR)
    equip_char(ch, obj, iWear)
}

fun hold_a_shield(ch: CHAR_DATA, obj: Obj, iWear: Wear) {
    act("\$n wears \$p as a shield.", ch, obj, null, TO_ROOM)
    act("You wear \$p as a shield.", ch, obj, null, TO_CHAR)
    equip_char(ch, obj, iWear)
}

fun hold_a_thing(ch: CHAR_DATA, obj: Obj, iWear: Wear) {
    act("\$n holds \$p in \$s hand.", ch, obj, null, TO_ROOM)
    act("You hold \$p in your hand.", ch, obj, null, TO_CHAR)
    equip_char(ch, obj, iWear)
}

/* wear object as a secondary weapon */

fun hold_a_wield(ch: CHAR_DATA, obj: Obj?, iWear: Wear) {
    if (obj == null) {
        bug("Hold_a_wield: Obj null")
        return
    }

    if (obj.item_type != ItemType.Weapon) {
        hold_a_thing(ch, obj, iWear)
        return
    }

    act("\$n wields \$p.", ch, obj, null, TO_ROOM)
    act("You wield \$p.", ch, obj, null, TO_CHAR)
    equip_char(ch, obj, iWear)

    val sn = if (get_wield_char(ch, true) == obj) get_weapon_sn(ch, true) else get_weapon_sn(ch, false) ?: return
    val skill = get_weapon_skill(ch, sn)
    when {
        skill >= 100 -> act("\$p feels like a part of you!", ch, obj, null, TO_CHAR)
        skill > 85 -> act("You feel quite confident with \$p.", ch, obj, null, TO_CHAR)
        skill > 70 -> act("You are skilled with \$p.", ch, obj, null, TO_CHAR)
        skill > 50 -> act("Your skill with \$p is adequate.", ch, obj, null, TO_CHAR)
        skill > 25 -> act("\$p feels a little clumsy in your hands.", ch, obj, null, TO_CHAR)
        skill > 1 -> act("You fumble and almost drop \$p.", ch, obj, null, TO_CHAR)
        else -> act("You don't even know which end is up on \$p.",
                ch, obj, null, TO_CHAR)
    }
}


fun wear_a_wield(ch: CHAR_DATA, obj: Obj, fReplace: Boolean) {
    if (ch.pcdata != null && get_obj_weight(obj) > ch.carry) {
        send_to_char("It is too heavy for you to wield.\n", ch)
        return
    }

    if (IS_WEAPON_STAT(obj, WEAPON_TWO_HANDS) && ch.pcdata != null && ch.size < Size.Large) {
        if (get_eq_char(ch, Wear.Both) != null) {
            if (!remove_obj_loc(ch, Wear.Both, fReplace)) {
                return
            }
            hold_a_wield(ch, obj, Wear.Both)
        } else {
            if (get_eq_char(ch, Wear.Right) != null && !remove_obj_loc(ch, Wear.Right, fReplace)) {
                return
            }
            if (get_eq_char(ch, Wear.Left) != null && !remove_obj_loc(ch, Wear.Left, fReplace)) {
                return
            }
            hold_a_wield(ch, obj, Wear.Both)
        }
    } else {
        when {
            get_eq_char(ch, Wear.Both) != null -> {
                if (remove_obj_loc(ch, Wear.Both, fReplace)) {
                    hold_a_wield(ch, obj, Wear.Right)
                }
            }
            get_eq_char(ch, Wear.Right) == null -> hold_a_wield(ch, obj, Wear.Right)
            get_eq_char(ch, Wear.Left) == null -> hold_a_wield(ch, obj, Wear.Left)
            remove_obj_loc(ch, Wear.Right, fReplace) -> hold_a_wield(ch, obj, Wear.Right)
            remove_obj_loc(ch, Wear.Left, fReplace) -> hold_a_wield(ch, obj, Wear.Left)
            else -> send_to_char("You found your hands full.\n", ch)
        }
    }
}


fun wear_multi(ch: CHAR_DATA, obj: Obj, w: Wear, fReplace: Boolean) {
    if (count_worn(ch, w) < max_can_wear(ch, w)) {
        when (w) {
            Wear.Finger -> {
                act("\$n wears \$p on one of \$s finger.", ch, obj, null, TO_ROOM)
                act("You wear \$p on one of your finger.", ch, obj, null, TO_CHAR)
            }
            Wear.Neck -> {
                act("\$n wears \$p around \$s neck.", ch, obj, null, TO_ROOM)
                act("You wear \$p around your neck.", ch, obj, null, TO_CHAR)
            }
            Wear.Wrist -> {
                act("\$n wears \$p around one of \$s wrist.", ch, obj, null, TO_ROOM)
                act("You wear \$p around one of your wrist.", ch, obj, null, TO_CHAR)
            }
            Wear.Tattoo -> {
                act("\$n now uses \$p as tattoo of \$s religion.", ch, obj, null, TO_ROOM)
                act("You now use \$p as the tattoo of your religion.", ch, obj, null, TO_CHAR)
            }
            else -> {
                act("\$n wears \$p around somewhere.", ch, obj, null, TO_ROOM)
                act("You wear \$p around somewhere.", ch, obj, null, TO_CHAR)
            }
        }
        equip_char(ch, obj, w)
    } else if (fReplace) {
        var not_worn = true
        for (o in ch.carrying) {
            if (o.wear_loc === w && !IS_SET(o.extra_flags, ITEM_NOREMOVE) && (o.item_type != ItemType.Tattoo || IS_IMMORTAL(ch))) {
                unequip_char(ch, o)
                act("\$n stops using \$p.", ch, o, null, TO_ROOM)
                act("You stop using \$p.", ch, o, null, TO_CHAR)
                wear_multi(ch, obj, w, true)
                not_worn = false
                break
            }
        }
        if (not_worn) {
            act("You couldn't remove anything to replace with \$p.", ch, obj, null, TO_CHAR)
        }
    }
}
