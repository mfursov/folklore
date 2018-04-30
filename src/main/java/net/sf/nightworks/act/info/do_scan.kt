package net.sf.nightworks.act.info

import net.sf.nightworks.EX_CLOSED
import net.sf.nightworks.TO_ROOM
import net.sf.nightworks.act
import net.sf.nightworks.can_see
import net.sf.nightworks.check_blind
import net.sf.nightworks.dir_name
import net.sf.nightworks.do_scan2
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.ROOM_INDEX_DATA
import net.sf.nightworks.send_to_char
import net.sf.nightworks.show_char_to_char
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.UPPER
import net.sf.nightworks.util.one_argument

fun do_scan(ch: CHAR_DATA, argument: String) {
    val dir = StringBuilder()
    one_argument(argument, dir)

    if (dir.isEmpty()) {
        do_scan2(ch)
        return
    }

    val door = when (UPPER(dir[0])) {
        'N' -> 0
        'E' -> 1
        'S' -> 2
        'W' -> 3
        'U' -> 4
        'D' -> 5
        else -> {
            send_to_char("That's not a direction.\n", ch)
            return
        }
    }
    val dir2 = dir_name[door]
    val buf = TextBuffer()
    buf.sprintf("You scan %s.\n", dir2)
    send_to_char(buf, ch)
    buf.sprintf("\$n scans %s.", dir2)
    act(buf.toString(), ch, null, null, TO_ROOM)

    if (!check_blind(ch)) {
        return
    }

    val range = 1 + ch.level / 10

    var in_room: ROOM_INDEX_DATA? = ch.room

    var i = 1
    while (i <= range && in_room != null) {
        val exit = in_room.exit[door]
        val to_room = exit?.to_room
        if (exit == null || to_room == null || IS_SET(exit.exit_info, EX_CLOSED)) {
            return
        }

        var numpeople = 0
        for (person in to_room.people) {
            if (can_see(ch, person)) {
                numpeople++
            }
        }

        if (numpeople != 0) {
            buf.sprintf("***** Range %d *****\n", i)
            send_to_char(buf, ch)
            show_char_to_char(to_room.people, ch)
            send_to_char("\n", ch)
        }
        in_room = to_room
        i++
    }
}