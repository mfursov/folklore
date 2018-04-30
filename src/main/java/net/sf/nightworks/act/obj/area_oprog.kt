package net.sf.nightworks.act.obj

import net.sf.nightworks.model.OPROG_FUN_AREA

fun create_area_prog(name: String): OPROG_FUN_AREA {
    throw IllegalArgumentException("Prog not found: $name")
}