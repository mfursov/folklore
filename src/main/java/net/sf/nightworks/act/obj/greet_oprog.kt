package net.sf.nightworks.act.obj

import net.sf.nightworks.model.OPROG_FUN_GREET

fun create_greet_prog(name: String): OPROG_FUN_GREET {
    throw IllegalArgumentException("Prog not found: $name")
}

