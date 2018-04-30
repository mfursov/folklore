package net.sf.nightworks

import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.model.NOTE_DATA
import net.sf.nightworks.util.DikuTextFile
import net.sf.nightworks.util.IS_IMMORTAL
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.IS_TRUSTED
import net.sf.nightworks.util.TextBuffer
import net.sf.nightworks.util.atoi
import net.sf.nightworks.util.bug
import net.sf.nightworks.util.eq
import net.sf.nightworks.util.exit
import net.sf.nightworks.util.isSpace
import net.sf.nightworks.util.is_number
import net.sf.nightworks.util.log_string
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.smash_tilde
import net.sf.nightworks.util.startsWith
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.ArrayList
import java.util.Date
import java.util.Formatter


private val note_list = ArrayList<NOTE_DATA>()
private val idea_list = ArrayList<NOTE_DATA>()
private val penalty_list = ArrayList<NOTE_DATA>()
private val news_list = ArrayList<NOTE_DATA>()
private val changes_list = ArrayList<NOTE_DATA>()

fun count_spool(ch: CHAR_DATA, spool: List<NOTE_DATA>): Int {
    return spool.stream().filter { note -> !hide_note(ch, note) }.count().toInt()
}

fun do_unread(ch: CHAR_DATA, argument: String) {
    var found = false

    if (ch.pcdata == null) {
        return
    }

    var count = count_spool(ch, news_list)
    if (count > 0) {
        found = true
        send_to_char("There " + (if (count > 1) "are" else "is") + " " + count + " new news article" + (if (count > 1) "s" else "") + " waiting.\n", ch)
    }
    count = count_spool(ch, changes_list)
    if (count > 0) {
        found = true
        send_to_char("There " + (if (count > 1) "are" else "is") + " " + count + " change" + (if (count > 1) "s" else "") + " waiting to be read.\n", ch)
    }
    count = count_spool(ch, note_list)
    if (count > 0) {
        found = true
        send_to_char("You have " + count + " new note" + (if (count > 1) "s" else "") + " waiting.\n", ch)
    }
    count = count_spool(ch, idea_list)
    if (count > 0) {
        found = true
        send_to_char("You have " + count + " unread idea" + (if (count > 1) "s" else "") + " to peruse.\n", ch)
    }

    count = count_spool(ch, penalty_list)
    if (IS_TRUSTED(ch, ANGEL) && count > 0) {
        found = true
        send_to_char(count.toString() + " " + (if (count > 1) "penalties have" else "penalty has") + " been added.\n", ch)
    }

    if (!found && !eq(argument, "login")) {
        send_to_char("You have no unread notes.\n", ch)
    }
}

fun do_note(ch: CHAR_DATA, argument: String) {
    parse_note(ch, argument, NOTE_NOTE)
}

fun do_idea(ch: CHAR_DATA, argument: String) {
    parse_note(ch, argument, NOTE_IDEA)
}

fun do_penalty(ch: CHAR_DATA, argument: String) {
    parse_note(ch, argument, NOTE_PENALTY)
}

fun do_news(ch: CHAR_DATA, argument: String) {
    parse_note(ch, argument, NOTE_NEWS)
}

fun do_changes(ch: CHAR_DATA, argument: String) {
    parse_note(ch, argument, NOTE_CHANGES)
}

fun save_notes(type: Int) {
    val name: String
    val list: List<NOTE_DATA>

    when (type) {
        NOTE_NOTE -> {
            name = nw_config.note_note_file
            list = note_list
        }
        NOTE_IDEA -> {
            name = nw_config.note_idea_file
            list = idea_list
        }
        NOTE_PENALTY -> {
            name = nw_config.note_penalty_file
            list = penalty_list
        }
        NOTE_NEWS -> {
            name = nw_config.note_news_file
            list = news_list
        }
        NOTE_CHANGES -> {
            name = nw_config.note_changes_file
            list = changes_list
        }
        else -> return
    }
    try {
        val fw = BufferedWriter(FileWriter(name))
        val fp = Formatter(fw)
        fw.use { _ ->
            for (note in list) {
                prepareNote(fp, note)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }

}

private fun prepareNote(fp: Formatter, note: NOTE_DATA) {
    fp.format("Sender  %s~\n", note.sender)
    fp.format("Date    %s~\n", note.date)
    fp.format("Stamp   %d\n", note.timestamp)
    fp.format("To      %s~\n", note.to_list)
    fp.format("Subject %s~\n", note.subject)
    fp.format("Text\n%s~\n", note.text)
}

fun load_notes() {
    note_list.addAll(load_thread(nw_config.note_note_file, NOTE_NOTE, 14 * 24 * 60 * 60))
    idea_list.addAll(load_thread(nw_config.note_idea_file, NOTE_IDEA, 28 * 24 * 60 * 60))
    penalty_list.addAll(load_thread(nw_config.note_penalty_file, NOTE_PENALTY, 0))
    news_list.addAll(load_thread(nw_config.note_news_file, NOTE_NEWS, 0))
    changes_list.addAll(load_thread(nw_config.note_changes_file, NOTE_CHANGES, 0))
}

fun load_thread(name: String, type: Int, free_time: Int): List<NOTE_DATA> {
    val file = File(name)
    if (!file.exists()) {
        return emptyList()
    }
    try {
        val fp = DikuTextFile(name)
        val list = ArrayList<NOTE_DATA>()
        while (true) {
            var letter: Char
            do {
                if (fp.feof()) {
                    break
                }
                letter = fp.read()
            } while (isSpace(letter))
            if (fp.feof()) {
                return list
            }
            fp.ungetc()

            val noteData = NOTE_DATA()

            if (!eq(fp.word(), "sender")) {
                break
            }
            noteData.sender = fp.string()

            if (!eq(fp.word(), "date")) {
                break
            }
            noteData.date = fp.string()

            if (!eq(fp.word(), "stamp")) {
                break
            }
            noteData.timestamp = fp.int().toLong()

            if (!eq(fp.word(), "to")) {
                break
            }
            noteData.to_list = fp.string()

            if (!eq(fp.word(), "subject")) {
                break
            }
            noteData.subject = fp.string()

            if (!eq(fp.word(), "text")) {
                break
            }
            noteData.text = fp.string()

            if (free_time != 0 && noteData.timestamp < current_time - free_time) {
                continue
            }

            noteData.type = type

            list.add(noteData)
        }
        bug("Load_notes: bad key word.")
        exit(1)
    } catch (e: IOException) {
        e.printStackTrace()
        log_string("login thread:" + name)
    }

    return emptyList()
}

fun append_note(note: NOTE_DATA) {
    val name: String
    when (note.type) {
        NOTE_NOTE -> {
            name = nw_config.note_note_file
            note_list.add(note)
        }
        NOTE_IDEA -> {
            name = nw_config.note_idea_file
            idea_list.add(note)
        }
        NOTE_PENALTY -> {
            name = nw_config.note_penalty_file
            penalty_list.add(note)
        }
        NOTE_NEWS -> {
            name = nw_config.note_news_file
            news_list.add(note)
        }
        NOTE_CHANGES -> {
            name = nw_config.note_changes_file
            changes_list.add(note)
        }
        else -> return
    }

    try {
        val bw = BufferedWriter(FileWriter(name, true))
        val fp = Formatter(bw)
        bw.use { _ -> prepareNote(fp, note) }
    } catch (e: IOException) {
        e.printStackTrace()
        log_string(name)
    }

}

fun is_note_to(ch: CHAR_DATA, note: NOTE_DATA): Boolean {
    if (eq(ch.name, note.sender)) {
        return true
    }

    if (eq("all", note.to_list)) {
        return true
    }

    if (IS_IMMORTAL(ch) && is_name("immortal", note.to_list)) {
        return true
    }


    return if (is_name(ch.name, note.to_list)) {
        true
    } else is_name(ch.cabal.short_name, note.to_list)

}


fun note_attach(ch: CHAR_DATA, type: Int) {
    if (ch.noteData != null) {
        return
    }

    val noteData = NOTE_DATA()
    noteData.sender = ch.name
    noteData.type = type
    ch.noteData = noteData
}


fun note_remove(ch: CHAR_DATA, note: NOTE_DATA, delete: Boolean) {

    if (!delete) {
        /* make a new list */
        val to_new = StringBuilder()
        val to_one = StringBuilder()
        var to_list: String? = note.to_list
        while (to_list != null && to_list.isNotEmpty()) {
            to_one.setLength(0)
            to_list = one_argument(to_list, to_one)
            if (to_one.isNotEmpty() && !eq(ch.name, to_one.toString())) {
                to_new.append(" ")
                to_new.append(to_one)
            }
        }
        /* Just a simple recipient removal? */
        if (!eq(ch.name, note.sender) && to_new.isNotEmpty()) {
            note.to_list = to_new.substring(1)
            return
        }
    }
    /* nuke the whole note */
    when (note.type) {
        NOTE_NOTE -> note_list.remove(note)
        NOTE_IDEA -> idea_list.remove(note)
        NOTE_PENALTY -> penalty_list.remove(note)
        NOTE_NEWS -> news_list.remove(note)
        NOTE_CHANGES -> changes_list.remove(note)
        else -> return
    }

    // Remove note from linked list.
    if (note.type != NOTE_INVALID) {
        bug("Note_remove: note not found.")
        return
    }
    save_notes(note.type)
}


fun hide_note(ch: CHAR_DATA, note: NOTE_DATA): Boolean {
    val last_read: Long

    val pcdata = ch.pcdata ?: return true

    when (note.type) {
        NOTE_NOTE -> last_read = pcdata.last_note
        NOTE_IDEA -> last_read = pcdata.last_idea
        NOTE_PENALTY -> last_read = pcdata.last_penalty
        NOTE_NEWS -> last_read = pcdata.last_news
        NOTE_CHANGES -> last_read = pcdata.last_changes
        else -> return true
    }

    if (note.timestamp <= last_read) {
        return true
    }
    return if (eq(ch.name, note.sender)) true else !is_note_to(ch, note)

}

fun update_read(ch: CHAR_DATA, note: NOTE_DATA) {
    val stamp = note.timestamp

    val pcdata = ch.pcdata ?: return

    when (note.type) {
        NOTE_NOTE -> pcdata.last_note = Math.max(pcdata.last_note, stamp)
        NOTE_IDEA -> pcdata.last_idea = Math.max(pcdata.last_idea, stamp)
        NOTE_PENALTY -> pcdata.last_penalty = Math.max(pcdata.last_penalty, stamp)
        NOTE_NEWS -> pcdata.last_news = Math.max(pcdata.last_news, stamp)
        NOTE_CHANGES -> pcdata.last_changes = Math.max(pcdata.last_changes, stamp)
        else -> return
    }
}

fun parse_note(ch: CHAR_DATA, argument: String, type: Int) {
    val pcdata = ch.pcdata ?: return

    val list: List<NOTE_DATA>
    val list_name: String
    when (type) {
        NOTE_NOTE -> {
            list = note_list
            list_name = "notes"
        }
        NOTE_IDEA -> {
            list = idea_list
            list_name = "ideas"
        }
        NOTE_PENALTY -> {
            list = penalty_list
            list_name = "penalties"
        }
        NOTE_NEWS -> {
            list = news_list
            list_name = "news"
        }
        NOTE_CHANGES -> {
            list = changes_list
            list_name = "changes"
        }
        else -> return
    }

    val arg = StringBuilder()
    var argRest = one_argument(argument, arg)
    argRest = smash_tilde(argRest)
    val anum: Int
    when {
        arg.isEmpty() || startsWith(arg.toString(), "read") -> {
            val fAll: Boolean
            when {
                eq(argRest, "all") -> {
                    fAll = true
                    anum = 0
                }
                argRest.isEmpty() || startsWith(argRest, "next") -> /* read next unread note */ {
                    var vnum = 0
                    for (note in list) {
                        if (!hide_note(ch, note)) {
                            val buf = TextBuffer()
                            buf.sprintf("[%3d] %s: %s\n%s\nTo: %s\n",
                                    vnum,
                                    note.sender,
                                    note.subject,
                                    note.date,
                                    note.to_list)
                            send_to_char(buf, ch)
                            page_to_char(note.text, ch)
                            update_read(ch, note)
                            return
                        } else if (is_note_to(ch, note)) {
                            vnum++
                        }
                    }
                    val buf = TextBuffer()
                    buf.sprintf("You have no unread %s.\n", list_name)
                    send_to_char(buf, ch)
                    return
                }
                is_number(argRest) -> {
                    fAll = false
                    anum = atoi(argRest)
                }
                else -> {
                    send_to_char("Read which number?\n", ch)
                    return
                }
            }

            var vnum = 0
            for (note in list) {
                if (is_note_to(ch, note) && (vnum++ == anum || fAll)) {
                    val buf = TextBuffer()
                    buf.sprintf("[%3d] %s: %s\n%s\nTo: %s\n",
                            vnum - 1,
                            note.sender,
                            note.subject,
                            note.date,
                            note.to_list
                    )
                    send_to_char(buf, ch)
                    page_to_char(note.text, ch)
                    update_read(ch, note)
                    return
                }
            }

            val buf = TextBuffer()
            buf.sprintf("There aren't that many %s.\n", list_name)
            send_to_char(buf, ch)
        }
        startsWith(arg.toString(), "list") -> {
            var vnum = 0
            val buf = TextBuffer()
            for (note in list) {
                if (is_note_to(ch, note)) {
                    buf.sprintf("[%3d%s] %s: %s\n",
                            vnum, if (hide_note(ch, note)) " " else "N",
                            note.sender, note.subject)
                    send_to_char(buf, ch)
                    vnum++
                }
            }
        }
        startsWith(arg.toString(), "remove") -> {
            if (!is_number(argRest)) {
                send_to_char("Note remove which number?\n", ch)
                return
            }

            anum = atoi(argRest)
            var vnum = 0
            for (note in list) {
                if (is_note_to(ch, note) && vnum++ == anum) {
                    note_remove(ch, note, false)
                    send_to_char("Ok.\n", ch)
                    return
                }
            }

            val buf = TextBuffer()
            buf.sprintf("There aren't that many %s.", list_name)
            send_to_char(buf, ch)
        }
        startsWith(arg.toString(), "delete") && get_trust(ch) >= MAX_LEVEL - 1 -> {
            if (!is_number(argRest)) {
                send_to_char("Note delete which number?\n", ch)
                return
            }

            anum = atoi(argRest)
            var vnum = 0
            for (note in list) {
                if (is_note_to(ch, note) && vnum++ == anum) {
                    note_remove(ch, note, true)
                    send_to_char("Ok.\n", ch)
                    return
                }
            }

            val buf = TextBuffer()
            buf.sprintf("There aren't that many %s.", list_name)
            send_to_char(buf, ch)
        }
        startsWith(arg.toString(), "catchup") -> {
            when (type) {
                NOTE_NOTE -> pcdata.last_note = current_time
                NOTE_IDEA -> pcdata.last_idea = current_time
                NOTE_PENALTY -> pcdata.last_penalty = current_time
                NOTE_NEWS -> pcdata.last_news = current_time
                NOTE_CHANGES -> pcdata.last_changes = current_time
            }
        }

        /* below this point only certain people can edit notes */
        type == NOTE_NEWS && !IS_TRUSTED(ch, ANGEL) || type == NOTE_CHANGES && !IS_TRUSTED(ch, CREATOR) -> {
            val buf = TextBuffer()
            buf.sprintf("You aren't high enough level to write %s.", list_name)
        }
        eq(arg.toString(), "+") -> {
            note_attach(ch, type)
            val noteData = ch.noteData
            if (noteData == null) {
                send_to_char("You have no note in progress.\n", ch)
                return
            }
            if (noteData.type != type) {
                send_to_char("You already have a different note in progress.\n", ch)
                return
            }


            if (noteData.text.length + argRest.length >= 4096) {
                send_to_char("Note too long.\n", ch)
                return
            }

            val buffer = TextBuffer()
            buffer.append(noteData.text)
            buffer.append(argRest)
            buffer.append("\n")
            noteData.text = buffer.toString()
            send_to_char("Ok.\n", ch)
        }
        eq(arg.toString(), "-") -> {
            var found = false

            note_attach(ch, type)
            val noteData = ch.noteData
            if (noteData == null) {
                send_to_char("You have no note in progress.\n", ch)
                return
            }

            if (noteData.type != type) {
                send_to_char("You already have a different note in progress.\n", ch)
                return
            }

            if (noteData.text.isEmpty() || noteData.text.isEmpty()) {
                send_to_char("No lines left to remove.\n", ch)
                return
            }
            val buf = noteData.text
            var len = buf.length
            while (--len >= 0) {
                if (buf[len] == '\r') {
                    if (!found)
                    /* back it up */ {
                        if (len > 0) {
                            len--
                        }
                        found = true
                    } else
                    /* found the second one */ {
                        noteData.text = buf.substring(0, len)
                        return
                    }
                }
            }
            noteData.text = buf
        }
        startsWith(arg.toString(), "subject") -> {
            note_attach(ch, type)
            val noteData = ch.noteData
            if (noteData == null) {
                send_to_char("You have no note in progress.\n", ch)
                return
            }
            if (noteData.type != type) {
                send_to_char("You already have a different note in progress.\n", ch)
                return
            }

            noteData.subject = argRest
            send_to_char("Ok.\n", ch)
        }
        startsWith(arg.toString(), "to") -> {
            if (is_name(argRest, "all") && !(IS_IMMORTAL(ch) || IS_SET(ch.act, PLR_CANINDUCT))) {
                send_to_char("Only immortals and cabal leaders can send notes to all.\n", ch)
                return
            }
            note_attach(ch, type)
            val noteData = ch.noteData
            if (noteData == null) {
                send_to_char("You have no note in progress.\n", ch)
                return
            }
            if (noteData.type != type) {
                send_to_char("You already have a different note in progress.\n", ch)
                return
            }
            noteData.to_list = argRest
            send_to_char("Ok.\n", ch)
        }
        startsWith(arg.toString(), "clear") -> {
            if (ch.noteData != null) {
                ch.noteData = null
            }

            send_to_char("Ok.\n", ch)
            return
        }
        startsWith(arg.toString(), "show") -> {
            val noteData = ch.noteData
            if (noteData == null) {
                send_to_char("You have no note in progress.\n", ch)
                return
            }

            if (noteData.type != type) {
                send_to_char("You aren't working on that kind of note.\n", ch)
                return
            }

            val buf = TextBuffer()
            buf.sprintf("%s: %s\nTo: %s\n", noteData.sender, noteData.subject, noteData.to_list)
            send_to_char(buf, ch)
            send_to_char(noteData.text, ch)
        }
        startsWith(arg.toString(), "post") || startsWith(arg.toString(), "send") -> {
            val noteData = ch.noteData
            if (noteData == null) {
                send_to_char("You have no note in progress.\n", ch)
                return
            }

            if (noteData.type != type) {
                send_to_char("You aren't working on that kind of note.\n", ch)
                return
            }

            if (eq(noteData.to_list, "")) {
                send_to_char("You need to provide a recipient (name, all, or immortal).\n", ch)
                return
            }

            if (eq(noteData.subject, "")) {
                send_to_char("You need to provide a subject.\n", ch)
                return
            }

            noteData.date = Date(current_time * 1000L).toString()
            noteData.timestamp = current_time

            append_note(noteData)
            ch.noteData = null
        }
        else -> send_to_char("You can't do that.\n", ch)
    }
}

