package net.sf.nightworks.model

import net.sf.nightworks.Index
import net.sf.nightworks.util.startsWith

/** Structure for a social in the socials table. */
class Social {
    var name = ""
    var minPos: Pos = Pos.Dead

    var found_char = ""
    var found_victim = ""
    var found_novictim = ""

    var noarg_char = ""
    var noarg_room = ""
    var self_char = ""
    var self_room = ""

    var not_found_char = ""

    companion object {
        fun lookup(name: String) = Index.SOCIALS.firstOrNull { startsWith(name, it.name) }
    }
}
