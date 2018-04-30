package net.sf.nightworks.act.obj

import net.sf.nightworks.model.OPROG_FUN_ENTRY

fun create_entry_prog(name: String): OPROG_FUN_ENTRY {
    throw IllegalArgumentException("Prog not found: $name")
}

