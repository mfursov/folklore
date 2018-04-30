package net.sf.nightworks.act.spell


import net.sf.nightworks.DT
import net.sf.nightworks.is_name
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.saves_spell
import net.sf.nightworks.send_to_char
import net.sf.nightworks.target_name
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.sprintf

fun spell_ventriloquate(level: Int, ch: CHAR_DATA) {
    val speaker = StringBuilder()
    target_name = one_argument(target_name, speaker)
    val buf1 = TextBuffer()
    val buf2 = TextBuffer()
    sprintf(buf1, "%s says '%s'.\n", speaker, target_name)
    sprintf(buf2, "Someone makes %s say '%s'.\n", speaker, target_name)
    buf1.upfirst()

    val speakerName = speaker.toString()
    ch.room.people
            .filterNot { is_name(speakerName, it.name) }
            .forEach { send_to_char(if (saves_spell(level, it, DT.Other)) buf2 else buf1, it) }
}