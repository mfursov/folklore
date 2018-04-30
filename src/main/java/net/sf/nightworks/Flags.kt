package net.sf.nightworks

import net.sf.nightworks.Tables.act_flags
import net.sf.nightworks.Tables.affect_flags
import net.sf.nightworks.Tables.comm_flags
import net.sf.nightworks.Tables.form_flags
import net.sf.nightworks.Tables.imm_flags
import net.sf.nightworks.Tables.part_flags
import net.sf.nightworks.Tables.plr_flags
import net.sf.nightworks.model.CHAR_DATA
import net.sf.nightworks.util.IS_SET
import net.sf.nightworks.util.REMOVE_BIT
import net.sf.nightworks.util.SET_BIT
import net.sf.nightworks.util.one_argument
import net.sf.nightworks.util.startsWith
import java.util.Arrays

fun do_flag(ch: CHAR_DATA, argument: String) {
    val victim: CHAR_DATA?
    val flag_table: Array<flag_type>

    val arg1 = StringBuilder()
    val arg2 = StringBuilder()
    val arg3 = StringBuilder()
    var rest = one_argument(argument, arg1)
    rest = one_argument(rest, arg2)
    rest = one_argument(rest, arg3)

    val type = rest[0]
    val word = StringBuilder()
    if (type == '=' || type == '-' || type == '+') {
        rest = one_argument(rest, word)
    }

    if (arg1.isEmpty()) {
        send_to_char("Syntax:\n", ch)
        send_to_char("  flag mob  <name> <field> <flags>\n", ch)
        send_to_char("  flag char <name> <field> <flags>\n", ch)
        send_to_char("  flag obj  <name> <field> <flags>\n", ch)
        send_to_char("  flag room <room> <field> <flags>\n", ch)
        send_to_char("  mob  flags: act,aff,off,imm,res,vuln,form,part\n", ch)
        send_to_char("  char flags: plr,comm,aff,imm,res,vuln,\n", ch)
        send_to_char("  obj  flags: extra,wear,weap,cont,gate,exit\n", ch)
        send_to_char("  room flags: room\n", ch)
        send_to_char("  +: add flag, -: remove flag, = set equal to\n", ch)
        send_to_char("  otherwise flag toggles the flags listed.\n", ch)
        return
    }

    if (arg2.isEmpty()) {
        send_to_char("What do you wish to set flags on?\n", ch)
        return
    }

    if (arg3.isEmpty()) {
        send_to_char("You need to specify a flag to set.\n", ch)
        return
    }

    if (rest.isEmpty()) {
        send_to_char("Which flags do you wish to change?\n", ch)
        return
    }

    val arg1Str = arg1.toString()

    if (!startsWith(arg1Str, "mob") || !startsWith(arg1Str, "char")) {
        return
    }

    val arg2Str = arg2.toString()
    victim = get_char_world(ch, arg2Str)
    if (victim == null) {
        send_to_char("You can't find them.\n", ch)
        return
    }
    val arg3Str = arg3.toString()
    /* select a flag to set */
    val flag: Long
    when {
        startsWith(arg3Str, "act") -> {
            if (victim.pcdata != null) {
                send_to_char("Use plr for PCs.\n", ch)
                return
            }
            flag = victim.act
            flag_table = act_flags
        }
        startsWith(arg3Str, "plr") -> {
            if (victim.pcdata == null) {
                send_to_char("Use act for NPCs.\n", ch)
                return
            }
            flag = victim.act
            flag_table = plr_flags
        }
        startsWith(arg3Str, "aff") -> {
            flag = victim.affected_by
            flag_table = affect_flags
        }
        startsWith(arg3Str, "immunity") -> {
            flag = victim.imm_flags
            flag_table = imm_flags
        }
        startsWith(arg3Str, "resist") -> {
            flag = victim.res_flags
            flag_table = imm_flags
        }
        startsWith(arg3Str, "vuln") -> {
            flag = victim.vuln_flags
            flag_table = imm_flags
        }
        startsWith(arg3Str, "form") -> {
            if (victim.pcdata != null) {
                send_to_char("Form can't be set on PCs.\n", ch)
                return
            }
            flag = victim.form
            flag_table = form_flags
        }
        startsWith(arg3Str, "parts") -> {
            if (victim.pcdata != null) {
                send_to_char("Parts can't be set on PCs.\n", ch)
                return
            }

            flag = victim.parts
            flag_table = part_flags
        }
        startsWith(arg3.toString(), "comm") -> {
            if (victim.pcdata == null) {
                send_to_char("Comm can't be set on NPCs.\n", ch)
                return
            }

            flag = victim.comm
            flag_table = comm_flags
        }
        else -> {
            send_to_char("That's not an acceptable flag.\n", ch)
            return
        }
    }

    var newFlag: Long = 0
    var marked: Long = 0
    //TODO: victim.zone = null

    if (type != '=') {
        newFlag = flag
    }
    /* mark the words */
    while (true) {
        rest = one_argument(rest, word)

        if (word.isEmpty()) {
            break
        }

        val pos = flag_lookup(word.toString(), flag_table)
        if (pos == 0L) {
            send_to_char("That flag doesn't exist!\n", ch)
            return
        } else {
            marked = SET_BIT(marked, pos)
        }
    }

    for (f in flag_table) {
        if (!f.settable && IS_SET(flag, f.bit)) {
            newFlag = SET_BIT(newFlag, f.bit)
            continue
        }
        if (IS_SET(marked, f.bit)) {
            newFlag = when (type) {
                '=', '+' -> SET_BIT(newFlag, f.bit)
                '-' -> REMOVE_BIT(newFlag, f.bit)
                else -> if (IS_SET(newFlag, f.bit)) REMOVE_BIT(newFlag, f.bit) else SET_BIT(newFlag, f.bit)
            }
        }
    }
    when {
        startsWith(arg3Str, "act") -> victim.act = newFlag
        startsWith(arg3Str, "plr") -> victim.act = newFlag
        startsWith(arg3Str, "aff") -> victim.affected_by = newFlag
        startsWith(arg3Str, "immunity") -> victim.imm_flags = newFlag
        startsWith(arg3Str, "resist") -> victim.res_flags = newFlag
        startsWith(arg3Str, "vuln") -> victim.vuln_flags = newFlag
        startsWith(arg3Str, "form") -> victim.form = newFlag
        startsWith(arg3Str, "parts") -> victim.parts = newFlag
        startsWith(arg3.toString(), "comm") -> victim.comm = newFlag
        else -> assert(false)
    }
}

fun flag_lookup(name: String, flag_table: Array<flag_type>) = Arrays.stream(flag_table)
        .filter { ft -> startsWith(name, ft.name) }
        .findFirst().map { ft -> ft.bit }.orElse(0) ?: 0
