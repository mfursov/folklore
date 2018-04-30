package net.sf.nightworks.act.db

import net.sf.nightworks.Index
import net.sf.nightworks.PLR_COLOR
import net.sf.nightworks.model.Area
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.page_to_char
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.IS_SET
import java.util.Formatter

fun do_areas(ch: CHAR_DATA, arg: String) {
    if (arg.isEmpty()) {
        send_to_char("No argument is used with this command.\n", ch)
        return
    }

    val bufpage = StringBuilder(1024)
    bufpage.append("Current areas: \n")
    val color = IS_SET(ch.act, PLR_COLOR)
    val f = Formatter(bufpage)
    val half = (Index.AREAS.size + 1) / 2
    Index.AREAS.zipWithNext()
    var i = 0
    while (i < half) {
        val a1 = Index.AREAS[i]
        val a2 = if (half + i == Index.AREAS.size) null else Index.AREAS[half + i]
        val buf1 = formatAreaDetails(a1)
        val buf2 = if (a2 != null) formatAreaDetails(a2) else "\n"
        if (color) {
            f.format("%-69s %s\n", buf1, buf2)
        } else {
            f.format("%-39s %s\n", buf1, buf2)
        }
        i++
    }
    bufpage.append("\n")
    page_to_char(bufpage, ch)
}

private fun formatAreaDetails(pArea: Area): String {
    val f = Formatter()
    f.format("{W%2d %3d{x} {b%s {c%s{x", pArea.low_range, pArea.high_range, pArea.writer, pArea.credits)
    return f.toString()
}

