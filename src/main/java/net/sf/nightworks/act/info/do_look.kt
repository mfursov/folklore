package net.sf.nightworks.act.info

import net.sf.nightworks.AFF_CHARM
import net.sf.nightworks.COMM_BRIEF
import net.sf.nightworks.CONT_CLOSED
import net.sf.nightworks.EX_CLOSED
import net.sf.nightworks.EX_IS_DOOR
import net.sf.nightworks.PLR_AUTO_EXIT
import net.sf.nightworks.PLR_HOLYLIGHT
import net.sf.nightworks.Skill
import net.sf.nightworks.TO_AFFECTS
import net.sf.nightworks.TO_CHAR
import net.sf.nightworks.TO_NOTVICT
import net.sf.nightworks.TO_VICT
import net.sf.nightworks.act
import net.sf.nightworks.add_follower
import net.sf.nightworks.affect_strip
import net.sf.nightworks.affect_to_char
import net.sf.nightworks.can_see_obj
import net.sf.nightworks.check_blind
import net.sf.nightworks.get_char_room
import net.sf.nightworks.get_extra_desc
import net.sf.nightworks.get_obj_here
import net.sf.nightworks.is_affected
import net.sf.nightworks.is_name
import net.sf.nightworks.model.Affect
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.ItemType
import net.sf.nightworks.model.Liquid
import net.sf.nightworks.model.Pos
import net.sf.nightworks.number_argument
import net.sf.nightworks.room_is_dark
import net.sf.nightworks.send_to_char
import net.sf.nightworks.show_char_to_char
import net.sf.nightworks.show_char_to_char_1
import net.sf.nightworks.show_list_to_char
import net.sf.nightworks.stop_follower
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.number_fuzzy
import net.sf.nightworks.util.one_argument

fun do_look(ch: CHAR_DATA, argument: String) {
    var rest = argument

    if (ch.position < Pos.Sleeping) {
        send_to_char("You can't see anything but stars!\n", ch)
        return
    }

    if (ch.position == Pos.Sleeping) {
        send_to_char("You can't see anything, you're sleeping!\n", ch)
        return
    }

    if (!check_blind(ch)) {
        return
    }

    val pcdata = ch.pcdata
    val room_people = ch.room.people
    if (pcdata != null && !IS_SET(ch.act, PLR_HOLYLIGHT) && room_is_dark(ch)) {
        send_to_char("It is pitch black ... \n", ch)
        if (room_people.isNotEmpty()) {
            show_char_to_char(room_people, ch)
        }
        return
    }

    val arg1b = StringBuilder()
    val arg2b = StringBuilder()
    val arg3b = StringBuilder()
    rest = one_argument(rest, arg1b)
    one_argument(rest, arg2b)
    val number = number_argument(arg1b.toString(), arg3b)
    var count = 0

    val arg1 = arg1b.toString()
    if (arg1.isEmpty() || eq(arg1, "auto")) {
        /* 'look' or 'look auto' */
        send_to_char("{W" + ch.room.name + "{x", ch)

        if (IS_IMMORTAL(ch) && (pcdata == null || IS_SET(ch.act, PLR_HOLYLIGHT))) {
            val buf = TextBuffer()
            buf.sprintf(" {x[Room %d]{x", ch.room.vnum)
            send_to_char(buf, ch)
        }

        send_to_char("\n", ch)

        if (arg1.isEmpty() || pcdata != null && !IS_SET(ch.comm, COMM_BRIEF)) {
            send_to_char("  ", ch)
            send_to_char(ch.room.description, ch)
        }

        if (pcdata != null && IS_SET(ch.act, PLR_AUTO_EXIT)) {
            send_to_char("\n", ch)
            do_exits(ch, "auto")
        }

        show_list_to_char(ch.room.objects, ch, false, false)
        if (room_people.isNotEmpty()) {
            show_char_to_char(room_people, ch)
        }
        return
    }

    val arg2 = arg2b.toString()
    if (eq(arg1, "i") || eq(arg1, "in") || eq(arg1, "on")) {
        /* 'look in' */
        if (arg2.isEmpty()) {
            send_to_char("Look in what?\n", ch)
            return
        }

        val obj = get_obj_here(ch, arg2)
        if (obj == null) {
            send_to_char("You do not see that here.\n", ch)
            return
        }

        when (obj.item_type) {

            ItemType.DrinkContainer -> {
                if (obj.value[1] <= 0) {
                    send_to_char("It is empty.\n", ch)
                    return
                }
                run {
                    val buf = TextBuffer()
                    buf.sprintf("It's %sfilled with  a %s liquid.\n",
                            when {
                                obj.value[1] < obj.value[0] / 4 -> "less than half-"
                                obj.value[1] < 3 * obj.value[0] / 4 -> "about half-"
                                else -> "more than half-"
                            },
                            Liquid.of(obj.value[2]).liq_color
                    )
                    send_to_char(buf, ch)
                }
            }

            ItemType.Container, ItemType.CorpseNpc, ItemType.CorpsePc -> {
                if (IS_SET(obj.value[1], CONT_CLOSED)) {
                    send_to_char("It is closed.\n", ch)
                    return
                }

                act("\$p holds:", ch, obj, null, TO_CHAR)
                show_list_to_char(obj.contains, ch, true, true)
            }
            else -> send_to_char("That is not a container.\n", ch)
        }
        return
    }

    val victim = get_char_room(ch, arg1)
    if (victim != null) {
        show_char_to_char_1(victim, ch)
        /* Love potion */
        if (is_affected(ch, Skill.love_potion) && victim != ch) {
            affect_strip(ch, Skill.love_potion)
            if (ch.master != null) {
                stop_follower(ch)
            }
            add_follower(ch, victim)
            ch.leader = victim

            val af = Affect()
            af.type = Skill.charm_person
            af.level = ch.level
            af.duration = number_fuzzy(victim.level / 4)
            af.bitvector = AFF_CHARM
            affect_to_char(ch, af)

            act("Isn't \$n just so nice?", victim, null, ch, TO_VICT)
            act("\$N looks at you with adoring eyes.", victim, null, ch, TO_CHAR)
            act("\$N looks at \$n with adoring eyes.", victim, null, ch, TO_NOTVICT)
        }

        return
    }

    val arg3 = arg3b.toString()
    for (obj in ch.carrying) {
        if (can_see_obj(ch, obj)) {  /* player can see object */
            var pdesc = get_extra_desc(arg3, obj.extra_desc)
            if (pdesc != null) {
                if (++count == number) {
                    send_to_char(pdesc, ch)
                    return
                } else {
                    continue
                }
            }

            pdesc = get_extra_desc(arg3, obj.pIndexData.extra_descr)
            if (pdesc != null) {
                if (++count == number) {
                    send_to_char(pdesc, ch)
                    return
                } else {
                    continue
                }
            }

            if (is_name(arg3, obj.name)) {
                if (++count == number) {
                    send_to_char("You see nothing special about it.\n", ch)
                    return
                }
            }

        }
    }

    for (obj in ch.room.objects) {
        if (can_see_obj(ch, obj)) {
            var pdesc = get_extra_desc(arg3, obj.extra_desc)
            if (pdesc != null) {
                if (++count == number) {
                    send_to_char(pdesc, ch)
                    return
                }
            }

            pdesc = get_extra_desc(arg3, obj.pIndexData.extra_descr)
            if (pdesc != null) {
                if (++count == number) {
                    send_to_char(pdesc, ch)
                    return
                }
            }
        }

        if (is_name(arg3, obj.name)) {
            if (++count == number) {
                send_to_char(obj.description, ch)
                send_to_char("\n", ch)
                return
            }
        }
    }

    val pdesc = get_extra_desc(arg3, ch.room.extra_descr)
    if (pdesc != null) {
        if (++count == number) {
            send_to_char(pdesc, ch)
            return
        }
    }

    if (count > 0 && count != number) {
        val buf = TextBuffer()
        if (count == 1) {
            buf.sprintf("You only see one %s here.\n", arg3)
        } else {
            buf.sprintf("You only see %d of those here.\n", count)
        }

        send_to_char(buf, ch)
        return
    }

    val door = when {
        eq(arg1, "n") || eq(arg1, "north") -> 0
        eq(arg1, "e") || eq(arg1, "east") -> 1
        eq(arg1, "s") || eq(arg1, "south") -> 2
        eq(arg1, "w") || eq(arg1, "west") -> 3
        eq(arg1, "u") || eq(arg1, "up") -> 4
        eq(arg1, "d") || eq(arg1, "down") -> 5
        else -> {
            send_to_char("You do not see that here.\n", ch)
            return
        }
    }

    /* 'look direction' */
    val pexit = ch.room.exit[door]
    if (pexit == null) {
        send_to_char("Nothing special there.\n", ch)
        return
    }

    if (pexit.description.isNotEmpty()) {
        send_to_char(pexit.description, ch)
    } else {
        send_to_char("Nothing special there.\n", ch)
    }

    if (pexit.keyword.isNotEmpty() && pexit.keyword[0] != ' ') {
        if (IS_SET(pexit.exit_info, EX_CLOSED)) {
            act("The \$d is closed.", ch, null, pexit.keyword, TO_CHAR)
        } else if (IS_SET(pexit.exit_info, EX_IS_DOOR)) {
            act("The \$d is open.", ch, null, pexit.keyword, TO_CHAR)
        }
    }
}