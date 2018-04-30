package net.sf.nightworks.act.db

import net.sf.nightworks.Index
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.send_to_char
import net.sf.nightworks.top_affect
import net.sf.nightworks.top_ed
import net.sf.nightworks.top_reset
import java.util.Formatter

fun do_memory(ch: CHAR_DATA) {
    val buf = StringBuilder(1024)
    val f = Formatter(buf)
    f.format("Affects %5d\n", top_affect)
    f.format("Areas   %5d\n", Index.AREAS.size)
    f.format("ExDes   %5d\n", top_ed)
    f.format("Helps   %5d\n", Index.HELP.size)
    f.format("Socials %5d\n", Index.SOCIALS.size)
    f.format("Resets  %5d\n", top_reset)
    f.format("Rooms   %5d\n", Index.ROOM_INDEX.size)
    f.format("Shops   %5d\n", Index.SHOPS.size)
    send_to_char(f.toString(), ch)

}