package net.sf.nightworks.act.mob

import net.sf.nightworks.model.MPROG_FUN_ENTRY

fun create_entry_prog(name: String): MPROG_FUN_ENTRY {
    throw IllegalArgumentException("Prog not found: $name")
}
