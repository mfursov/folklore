package net.sf.nightworks.act.info

import net.sf.nightworks.Index
import net.sf.nightworks.get_trust
import net.sf.nightworks.is_name
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.page_to_char
import net.sf.nightworks.send_to_char
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.one_argument

fun do_help(ch: CHAR_DATA, argument: String) {
    var rest = argument
    if (rest.isEmpty()) {
        rest = "summary"
    }

    /// this part handles help a b so that it returns help 'a b'
    val args = StringBuilder()
    val arg1 = StringBuilder()
    while (rest.isNotEmpty()) {
        arg1.setLength(0)
        rest = one_argument(rest, arg1)
        if (args.isNotEmpty()) {
            args.append(" ")
        }
        args.append(arg1)
    }

    val argallstr = args.toString()
    for (pHelp in Index.HELP) {
        if (pHelp.level > get_trust(ch)) continue
        if (!is_name(argallstr, pHelp.keyword)) continue

        if (pHelp.level >= 0 && !eq(argallstr, "imotd")) {
            send_to_char(pHelp.keyword, ch)
            send_to_char("\n", ch)
        }

        // Strip leading '.' to allow initial blanks.
        page_to_char(pHelp.text, ch)
        return
    }
    send_to_char("No help on that word.\n", ch)
}