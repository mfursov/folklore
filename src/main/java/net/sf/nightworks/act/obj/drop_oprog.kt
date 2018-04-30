package net.sf.nightworks.act.obj

import net.sf.nightworks.model.OPROG_FUN_DROP

fun create_drop_prog(name: String): OPROG_FUN_DROP {
    throw IllegalArgumentException("Prog not found: $name")
}