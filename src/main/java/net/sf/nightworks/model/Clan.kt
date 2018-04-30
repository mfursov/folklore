package net.sf.nightworks.model

import net.sf.nightworks.VNum
import net.sf.nightworks.util.startsWith

enum class Clan(val long_name: String,
                val short_name: String,
                val obj_vnum: VNum,
                val room_vnum: VNum,
        // If empty - there is no object
                var obj_ptr: Obj? = null) {
    None("None", "None", -1, -1),
    Ruler("the Rulers of Nightworks", "RULER", 511, 512),
    Invader("the Dark Raiders of Nightworks", "INVADER", 561, 568),
    Chaos("the Barons of Chaos", "CHAOS", 552, 554),
    Shalafi("the Masters of the Arcane Arts", "SHALAFI", 531, 530),
    BattleRager("the Masters of the Martial Arts", "BATTLERAGER", 541, 548),
    Knight("the Knights of Nightworks", "KNIGHT", 522, 524),
    Lion("the Leaders of Forests", "LION", 502, 504),
    Hunter("the Mercanary of Nightworks", "HUNTER", 571, 573);

    companion object {
        fun lookup(argument: String) = values().firstOrNull { startsWith(argument, it.short_name) }
        fun of(id: Int) = values().firstOrNull { it.ordinal == id }
    }

}